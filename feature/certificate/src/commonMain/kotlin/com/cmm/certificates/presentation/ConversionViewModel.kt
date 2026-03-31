package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.AppInstallation
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.preferredDefaultOutputDirectory
import com.cmm.certificates.shouldResetLegacyInstallOutputDirectory
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.openFile
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.data.defaultLectorLabel
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.GenerateCertificatesUseCase
import com.cmm.certificates.domain.PreviewCertificateUseCase
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
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
) : ViewModel() {
    private val logTag = "ConversionVM"
    private val installedTemplateFileName = "sablonas.docx"
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
    private val previewLoadingState = MutableStateFlow(false)

    init {
        selectInstalledTemplateIfAvailable()
    }

    val uiState: StateFlow<ConversionUiState> = combine(
        combine(
            formState,
            filesState,
            entriesState,
            settingsRepository.state,
            connectivityMonitor.isNetworkAvailable,
        ) { form, files, entries, settings, networkAvailable ->
            val options = parseAccreditedTypeOptions(settings.certificate.accreditedTypeOptions)
                .ifEmpty { defaultAccreditedTypeOptions }
            val resolvedType =
                if (form.accreditedType.isNotBlank() && form.accreditedType in options) {
                    form.accreditedType
                } else {
                    options.firstOrNull().orEmpty()
                }
            val resolvedForm = if (resolvedType == form.accreditedType) {
                form
            } else {
                form.copy(accreditedType = resolvedType)
            }
            ConversionUiState(
                files = files,
                form = resolvedForm,
                accreditedTypeOptions = options,
                isNetworkAvailable = networkAvailable,
                isSmtpAuthenticated = settings.smtp.isAuthenticated,
                supportsConversion = capabilities.canRunConversion,
                supportsEmailSending = capabilities.canSendEmails,
                entries = entries,
            )
        },
        previewLoadingState,
        emailProgressRepository.cachedEmails,
    ) { baseState, isPreviewLoading, cachedEmails ->
        baseState.copy(
            isPreviewLoading = isPreviewLoading,
            cachedEmailsCount = cachedEmails.entries.size,
        )
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
        filesState.update { it.copy(templatePath = path) }
    }

    fun setAccreditedId(value: String) {
        formState.update { it.copy(accreditedId = value) }
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
        filesState.update { it.copy(xlsxPath = path) }
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
            entriesState.value = parsed.getOrElse {
                logError(logTag, "Failed to parse XLSX: $path", it)
                emptyList()
            }
            logInfo(logTag, "Parsed ${entriesState.value.size} XLSX entries")
        }
    }

    fun generateDocuments() {
        if (!capabilities.canRunConversion) {
            logWarn(logTag, "Ignored conversion request because conversion is unsupported")
            return
        }
        viewModelScope.launch {
            logInfo(logTag, "Starting conversion request")
            generateDocumentsInternal()
        }
    }

    fun previewDocument() {
        if (!capabilities.canRunConversion) {
            logWarn(logTag, "Ignored preview request because conversion is unsupported")
            return
        }
        if (previewLoadingState.value) return
        viewModelScope.launch {
            previewLoadingState.value = true
            try {
                logInfo(logTag, "Starting preview request")
                val previewPath = previewCertificate(buildGenerateRequest(uiState.value))
                if (previewPath.isNullOrBlank()) {
                    logWarn(logTag, "Preview PDF was not generated")
                    return@launch
                }
                if (!openFile(previewPath)) {
                    logWarn(logTag, "Failed to open preview PDF: $previewPath")
                }
            } finally {
                previewLoadingState.value = false
            }
        }
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
        filesState.update { current ->
            if (current.templatePath.isBlank()) {
                current.copy(templatePath = installedTemplatePath)
            } else {
                current
            }
        }
    }

    private fun effectiveOutputDirectory(): String {
        val configuredOutputDirectory = settingsRepository.state.value.certificate.outputDirectory.trim()
        return when {
            configuredOutputDirectory.isBlank() -> defaultOutputDirectory
            shouldResetLegacyInstallOutputDirectory(configuredOutputDirectory, installationDirectoryPath) -> {
                logWarn(logTag, "Legacy installation output directory is not writable, falling back to default output directory")
                defaultOutputDirectory
            }

            OutputDirectory.canWrite(configuredOutputDirectory) -> configuredOutputDirectory
            else -> configuredOutputDirectory
        }
    }

    private fun buildGenerateRequest(snapshot: ConversionUiState): GenerateCertificatesRequest {
        return GenerateCertificatesRequest(
            templatePath = snapshot.files.templatePath,
            entries = snapshot.entries,
            accreditedId = snapshot.form.accreditedId,
            docIdStart = snapshot.form.docIdStart,
            accreditedType = snapshot.form.accreditedType,
            accreditedHours = snapshot.form.accreditedHours,
            certificateName = snapshot.form.certificateName,
            lector = snapshot.form.lector,
            lectorGender = snapshot.form.lectorGender,
            outputDirectory = effectiveOutputDirectory(),
        )
    }
}

data class ConversionUiState(
    val files: ConversionFilesState = ConversionFilesState(),
    val form: ConversionFormState = ConversionFormState(),
    val accreditedTypeOptions: List<String> = emptyList(),
    val isNetworkAvailable: Boolean = true,
    val isSmtpAuthenticated: Boolean = false,
    val supportsConversion: Boolean = true,
    val supportsEmailSending: Boolean = true,
    val isPreviewLoading: Boolean = false,
    val entries: List<RegistrationEntry> = emptyList(),
    val cachedEmailsCount: Int = 0,
) {
    val isConversionEnabled: Boolean
        get() = supportsConversion &&
                files.xlsxPath.isNotBlank() &&
                files.templatePath.isNotBlank() &&
                form.accreditedId.isNotBlank() &&
                form.docIdStart.isNotBlank() &&
                form.accreditedHours.isNotBlank() &&
                form.certificateName.isNotBlank() &&
                form.lector.isNotBlank() &&
                entries.isNotEmpty()

    val canRetryCachedEmails: Boolean
        get() = cachedEmailsCount > 0 && supportsEmailSending && isNetworkAvailable && isSmtpAuthenticated
}

data class ConversionFilesState(
    val xlsxPath: String = "",
    val templatePath: String = "",
) {
    val hasXlsx: Boolean
        get() = xlsxPath.isNotBlank()

    val hasTemplate: Boolean
        get() = templatePath.isNotBlank()
}

data class ConversionFormState(
    val accreditedId: String = "IVP-10",
    val docIdStart: String = "",
    val accreditedType: String = "",
    val accreditedHours: String = "",
    val certificateName: String = "",
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
