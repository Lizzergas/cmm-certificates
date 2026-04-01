package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_no_entries
import com.cmm.certificates.AppInstallation
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.core.openFile
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.defaultLectorLabel
import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.parseCertificateDateInput
import com.cmm.certificates.domain.GenerateCertificatesUseCase
import com.cmm.certificates.domain.PreviewCertificateUseCase
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.preferredDefaultOutputDirectory
import com.cmm.certificates.shouldResetLegacyInstallOutputDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversionViewModel(
    emailProgressRepository: EmailProgressRepository,
    private val settingsRepository: SettingsRepository,
    connectivityMonitor: ConnectivityMonitor,
    capabilityProvider: PlatformCapabilityProvider,
    private val parseRegistrations: ParseRegistrationsUseCase,
    private val generateCertificates: GenerateCertificatesUseCase,
    private val previewCertificate: PreviewCertificateUseCase,
    private val documentGenerator: CertificateDocumentGenerator,
) : ViewModel() {
    private val logTag = "ConversionVM"
    private val installedTemplateFileName = "bazinis_šablonas.docx"
    private val capabilities = capabilityProvider.capabilities
    private val installationDirectoryPath = if (capabilities.canResolveOutputDirectory) {
        AppInstallation.installationDirectoryPath()
    } else {
        null
    }
    private val defaultOutputDirectory = if (capabilities.canResolveOutputDirectory) {
        preferredDefaultOutputDirectory(AppInstallation.preferredOutputBaseDirectoryPath())
    } else {
        ""
    }
    private val defaultAccreditedTypeOptions = parseAccreditedTypeOptions(
        settingsRepository.state.value.certificate.accreditedTypeOptions,
    )
    private val formState = MutableStateFlow(
        ConversionFormState(
            accreditedType = defaultAccreditedTypeOptions.firstOrNull().orEmpty(),
        )
    )
    private val filesState = MutableStateFlow(ConversionFilesState())
    private val entriesState = MutableStateFlow<List<RegistrationEntry>>(emptyList())
    private val hasAttemptedSubmitState = MutableStateFlow(false)
    private val previewLoadingState = MutableStateFlow(false)
    private val previewPdfPathState = MutableStateFlow<String?>(null)

    init {
        selectInstalledTemplateIfAvailable()
    }

    private val baseUiState = combine(
        combine(
            combine(
                formState,
                filesState,
                entriesState,
                hasAttemptedSubmitState,
            ) { form, files, entries, hasAttemptedSubmit ->
                ConversionInputSnapshot(
                    form = form,
                    files = files,
                    entries = entries,
                    hasAttemptedSubmit = hasAttemptedSubmit,
                )
            },
            settingsRepository.state,
            connectivityMonitor.isNetworkAvailable,
        ) { snapshot, settings, networkAvailable ->
            val options = parseAccreditedTypeOptions(settings.certificate.accreditedTypeOptions)
                .ifEmpty { defaultAccreditedTypeOptions }
            val resolvedType =
                if (snapshot.form.accreditedType.isNotBlank() && snapshot.form.accreditedType in options) {
                    snapshot.form.accreditedType
                } else {
                    options.firstOrNull().orEmpty()
                }
            val resolvedForm = if (resolvedType == snapshot.form.accreditedType) {
                snapshot.form
            } else {
                snapshot.form.copy(accreditedType = resolvedType)
            }
            val templateSupport = buildTemplateSupportState(snapshot.files.templateAvailableTags)
            val validation = buildConversionValidationState(
                files = snapshot.files,
                form = resolvedForm,
                entriesCount = snapshot.entries.size,
                templateSupport = templateSupport,
                hasAttemptedSubmit = snapshot.hasAttemptedSubmit,
            )
            ConversionUiState(
                files = snapshot.files,
                form = resolvedForm,
                templateSupport = templateSupport,
                validation = validation,
                accreditedTypeOptions = options,
                isNetworkAvailable = networkAvailable,
                isSmtpAuthenticated = settings.smtp.isAuthenticated,
                supportsConversion = capabilities.canRunConversion,
                supportsEmailSending = capabilities.canSendEmails,
                entries = snapshot.entries,
            )
        },
        previewLoadingState,
        emailProgressRepository.cachedEmails,
    ) { baseState, isPreviewLoading, cachedEmails ->
        baseState.copy(
            isPreviewLoading = isPreviewLoading,
            cachedEmailsCount = cachedEmails.entries.size,
        )
    }

    val uiState: StateFlow<ConversionUiState> = combine(
        baseUiState,
        previewPdfPathState,
    ) { baseState, previewPdfPath ->
        baseState.copy(previewPdfPath = previewPdfPath)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConversionUiState(
            files = ConversionFilesState(),
            form = ConversionFormState(
                accreditedType = defaultAccreditedTypeOptions.firstOrNull().orEmpty(),
            ),
            accreditedTypeOptions = defaultAccreditedTypeOptions,
        ),
    )

    fun setTemplatePath(path: String) {
        if (!capabilities.canRunConversion) return
        logInfo(logTag, "Template selected: ${path.ifBlank { "<empty>" }}")
        updateTemplateSelection(path)
    }

    private fun updateTemplateSelection(path: String) {
        filesState.update {
            it.copy(
                templatePath = path,
                templateLoadError = null,
                templateAvailableTags = null,
                isTemplateInspectionInProgress = path.isNotBlank(),
            )
        }
        if (path.isBlank()) return
        viewModelScope.launch {
            val placeholders = runCatching {
                withContext(Dispatchers.IO) { documentGeneratorPlaceholders(path) }
            }
            filesState.update { current ->
                if (current.templatePath != path) return@update current
                placeholders.fold(
                    onSuccess = { tags ->
                        current.copy(
                            templateLoadError = null,
                            templateAvailableTags = tags,
                            isTemplateInspectionInProgress = false,
                        )
                    },
                    onFailure = { error ->
                        logError(
                            logTag,
                            "Failed to inspect DOCX template placeholders: $path",
                            error
                        )
                        current.copy(
                            templateLoadError = docxInspectErrorMessage(),
                            templateAvailableTags = null,
                            isTemplateInspectionInProgress = false,
                        )
                    },
                )
            }
        }
    }

    fun setAccreditedId(value: String) {
        formState.update { it.copy(accreditedId = value) }
    }

    fun setCertificateDate(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '-' }.take(10)
        formState.update { it.copy(certificateDate = sanitized) }
    }

    fun setDocIdStart(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        formState.update { it.copy(docIdStart = sanitized) }
    }

    fun setAccreditedType(value: String) {
        formState.update { it.copy(accreditedType = value) }
    }

    fun setAccreditedHours(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        formState.update { it.copy(accreditedHours = sanitized) }
    }

    fun setCertificateName(value: String) {
        formState.update { it.copy(certificateName = value) }
    }

    fun setFeedbackUrl(value: String) {
        formState.update { it.copy(feedbackUrl = value) }
    }

    fun setLector(value: String) {
        formState.update { it.copy(lector = value) }
    }

    fun setLectorGender(value: String) {
        formState.update { it.copy(lectorGender = value) }
    }

    fun selectXlsx(path: String) {
        if (!capabilities.canRunConversion) {
            entriesState.value = emptyList()
            logWarn(logTag, "Ignored XLSX selection because conversion is unsupported")
            return
        }
        filesState.update {
            it.copy(
                xlsxPath = path,
                xlsxLoadError = null,
            )
        }
        if (path.isBlank()) {
            entriesState.value = emptyList()
            logWarn(logTag, "Cleared XLSX selection")
            return
        }
        viewModelScope.launch {
            logInfo(logTag, "Parsing XLSX: $path")
            val parsed = runCatching {
                withContext(Dispatchers.IO) { parseRegistrations(path).getOrThrow() }
            }
            val entries = parsed.getOrElse {
                logError(logTag, "Failed to parse XLSX: $path", it)
                emptyList()
            }
            entriesState.value = entries
            filesState.update { current ->
                if (current.xlsxPath != path) return@update current
                current.copy(
                    xlsxLoadError = when {
                        parsed.isFailure -> xlsxParseErrorMessage()
                        entries.isEmpty() -> UiMessage(Res.string.conversion_error_no_entries)
                        else -> null
                    },
                )
            }
            logInfo(logTag, "Parsed ${entriesState.value.size} XLSX entries")
        }
    }

    fun generateDocuments(): Boolean {
        if (!capabilities.canRunConversion) {
            logWarn(logTag, "Ignored conversion request because conversion is unsupported")
            return false
        }
        hasAttemptedSubmitState.value = true
        val validation = currentValidationState()
        if (validation.hasBlockingErrors) {
            logWarn(logTag, "Ignored conversion request because validation failed")
            return false
        }
        viewModelScope.launch {
            logInfo(logTag, "Starting conversion request")
            generateDocumentsInternal()
        }
        return true
    }

    fun previewDocument() {
        if (!capabilities.canRunConversion) {
            logWarn(logTag, "Ignored preview request because conversion is unsupported")
            return
        }
        if (previewLoadingState.value) return
        hasAttemptedSubmitState.value = true
        val validation = currentValidationState()
        if (validation.hasBlockingErrors) {
            logWarn(logTag, "Ignored preview request because validation failed")
            return
        }
        viewModelScope.launch {
            previewLoadingState.value = true
            try {
                logInfo(logTag, "Starting preview request")
                previewPdfPathState.value = null
                val previewPath = previewCertificate(buildGenerateRequest(uiState.value))
                if (previewPath.isNullOrBlank()) {
                    logWarn(logTag, "Preview PDF was not generated")
                    return@launch
                }
                if (settingsRepository.state.value.appearance.useInAppPdfPreview) {
                    previewPdfPathState.value = previewPath
                } else if (!openFile(previewPath)) {
                    logWarn(logTag, "Failed to open preview PDF: $previewPath")
                }
            } finally {
                previewLoadingState.value = false
            }
        }
    }

    fun dismissPreview() {
        previewPdfPathState.value = null
    }

    private suspend fun generateDocumentsInternal() {
        generateCertificates(buildGenerateRequest(uiState.value))
    }

    private fun selectInstalledTemplateIfAvailable() {
        if (!capabilities.canRunConversion) return
        if (filesState.value.templatePath.isNotBlank()) return

        val installedTemplatePath = AppInstallation.installedResourcePath(installedTemplateFileName)
        if (installedTemplatePath.isNullOrBlank()) {
            logInfo(logTag, "No installed template found: $installedTemplateFileName")
            return
        }

        logInfo(logTag, "Auto-selected installed template: $installedTemplatePath")
        if (filesState.value.templatePath.isBlank()) {
            updateTemplateSelection(installedTemplatePath)
        }
    }

    private fun currentValidationState(): ConversionValidationState {
        return buildConversionValidationState(
            files = filesState.value,
            form = formState.value,
            entriesCount = entriesState.value.size,
            templateSupport = buildTemplateSupportState(filesState.value.templateAvailableTags),
            hasAttemptedSubmit = true,
        )
    }

    private fun documentGeneratorPlaceholders(path: String): Set<String> {
        return documentGenerator.inspectTemplatePlaceholders(path)
    }

    private fun effectiveOutputDirectory(): String {
        val configuredOutputDirectory =
            settingsRepository.state.value.certificate.outputDirectory.trim()
        return when {
            configuredOutputDirectory.isBlank() -> defaultOutputDirectory
            shouldResetLegacyInstallOutputDirectory(
                configuredOutputDirectory,
                installationDirectoryPath
            ) -> {
                logWarn(
                    logTag,
                    "Legacy installation output directory is not writable, falling back to default output directory"
                )
                defaultOutputDirectory
            }

            OutputDirectory.canWrite(configuredOutputDirectory) -> configuredOutputDirectory
            else -> configuredOutputDirectory
        }
    }

    private fun buildGenerateRequest(snapshot: ConversionUiState): GenerateCertificatesRequest {
        val certificateDate = parseCertificateDateInput(snapshot.form.certificateDate)
            ?.let(::formatCertificateDate)
            .orEmpty()
        return GenerateCertificatesRequest(
            templatePath = snapshot.files.templatePath,
            entries = snapshot.entries,
            certificateDate = certificateDate,
            accreditedId = snapshot.form.accreditedId,
            docIdStart = snapshot.form.docIdStart,
            accreditedType = snapshot.form.accreditedType,
            accreditedHours = snapshot.form.accreditedHours,
            certificateName = snapshot.form.certificateName,
            feedbackUrl = snapshot.form.feedbackUrl,
            lector = snapshot.form.lector,
            lectorGender = snapshot.form.lectorGender,
            outputDirectory = effectiveOutputDirectory(),
        )
    }
}

private data class ConversionInputSnapshot(
    val form: ConversionFormState,
    val files: ConversionFilesState,
    val entries: List<RegistrationEntry>,
    val hasAttemptedSubmit: Boolean,
)

data class ConversionUiState(
    val files: ConversionFilesState = ConversionFilesState(),
    val form: ConversionFormState = ConversionFormState(),
    val templateSupport: ConversionTemplateSupportState = ConversionTemplateSupportState(),
    val validation: ConversionValidationState = ConversionValidationState(),
    val accreditedTypeOptions: List<String> = emptyList(),
    val isNetworkAvailable: Boolean = true,
    val isSmtpAuthenticated: Boolean = false,
    val supportsConversion: Boolean = true,
    val supportsEmailSending: Boolean = true,
    val isPreviewLoading: Boolean = false,
    val previewPdfPath: String? = null,
    val entries: List<RegistrationEntry> = emptyList(),
    val cachedEmailsCount: Int = 0,
) {
    val canRetryCachedEmails: Boolean
        get() = cachedEmailsCount > 0 && supportsEmailSending && isNetworkAvailable && isSmtpAuthenticated
}

data class ConversionFilesState(
    val xlsxPath: String = "",
    val templatePath: String = "",
    val xlsxLoadError: UiMessage? = null,
    val templateLoadError: UiMessage? = null,
    val templateAvailableTags: Set<String>? = null,
    val isTemplateInspectionInProgress: Boolean = false,
) {
    val hasXlsx: Boolean
        get() = xlsxPath.isNotBlank()

    val hasTemplate: Boolean
        get() = templatePath.isNotBlank()

    val xlsxFileName: String?
        get() = xlsxPath.takeIf { it.isNotBlank() }?.toFileName()

    val templateFileName: String?
        get() = templatePath.takeIf { it.isNotBlank() }?.toFileName()
}

private fun String.toFileName(): String {
    return substringAfterLast('/').substringAfterLast('\\')
}

data class ConversionFormState(
    val certificateDate: String = "",
    val accreditedId: String = "IVP-10",
    val docIdStart: String = "",
    val accreditedType: String = "",
    val accreditedHours: String = "",
    val certificateName: String = "",
    val feedbackUrl: String = "",
    val lector: String = "",
    val lectorGender: String = defaultLectorLabel(),
)

private fun parseAccreditedTypeOptions(raw: String): List<String> {
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
