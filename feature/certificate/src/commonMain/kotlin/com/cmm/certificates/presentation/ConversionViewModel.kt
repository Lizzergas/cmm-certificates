package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_no_entries
import certificates.composeapp.generated.resources.conversion_refresh_docx_failed
import certificates.composeapp.generated.resources.conversion_refresh_docx_succeeded
import certificates.composeapp.generated.resources.conversion_refresh_xlsx_failed
import certificates.composeapp.generated.resources.conversion_refresh_xlsx_succeeded
import com.cmm.certificates.AppInstallation
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.core.openFile
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.config.CertificateConfigurationRepository
import com.cmm.certificates.data.config.CertificateConfigurationSource
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.GenerateCertificatesUseCase
import com.cmm.certificates.domain.PreviewCertificateUseCase
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.domain.config.manualField
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.domain.port.FileChangeObserver
import com.cmm.certificates.domain.port.FileChangeSubscription
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.preferredDefaultOutputDirectory
import com.cmm.certificates.shouldResetLegacyInstallOutputDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val configurationRepository: CertificateConfigurationRepository,
    private val fileChangeObserver: FileChangeObserver,
) : ViewModel() {
    private val logTag = "ConversionVM"
    private val installedTemplateFileName = "bazinis_šablonas.docx"
    private val refreshDebounceMillis = 600L
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

    private val formState = MutableStateFlow(ConversionFormState())
    private val filesState = MutableStateFlow(ConversionFilesState())
    private val entriesState = MutableStateFlow<List<RegistrationEntry>>(emptyList())
    private val hasAttemptedSubmitState = MutableStateFlow(false)
    private val previewLoadingState = MutableStateFlow(false)
    private val previewPdfPathState = MutableStateFlow<String?>(null)
    private val editingManualFieldState = MutableStateFlow<ConversionManualFieldEditorState?>(null)
    private val notificationState = MutableStateFlow<ConversionNotificationState?>(null)
    private var notificationIdCounter = 0L
    private var xlsxWatcher: FileChangeSubscription? = null
    private var templateWatcher: FileChangeSubscription? = null
    private var xlsxRefreshJob: Job? = null
    private var templateRefreshJob: Job? = null

    init {
        selectInstalledTemplateIfAvailable()
        observeConfigurationChanges()
    }

    private val baseUiState = combine(
        combine(
            combine(
                formState,
                filesState,
                entriesState,
                hasAttemptedSubmitState
            ) { form, files, entries, hasAttemptedSubmit ->
                ConversionInputSnapshot(form, files, entries, hasAttemptedSubmit)
            },
            settingsRepository.state,
            connectivityMonitor.isNetworkAvailable,
            configurationRepository.state,
            combine(editingManualFieldState, notificationState) { editingManualField, notification ->
                ConversionOverlayState(editingManualField, notification)
            },
        ) { snapshot, settings, networkAvailable, configState, overlay ->
            val configuration = configState.configuration
            val resolvedForm = resolveFormState(snapshot.form, configuration)
            val templateSupport =
                buildTemplateSupportState(configuration, snapshot.files.templateAvailableTags)
            val validation = buildConversionValidationState(
                files = snapshot.files,
                configuration = configuration,
                formValues = resolvedForm.manualValues,
                entriesCount = snapshot.entries.size,
                templateSupport = templateSupport,
                hasAttemptedSubmit = snapshot.hasAttemptedSubmit,
            )
            ConversionUiState(
                configuration = configuration,
                configSource = configState.source,
                configLoadFailureMessage = configState.loadFailureMessage,
                files = snapshot.files,
                form = resolvedForm,
                manualFields = buildManualFieldUiState(
                    configuration = configuration,
                    resolvedForm = resolvedForm,
                    templateSupport = templateSupport,
                    validation = validation,
                    enabled = capabilities.canRunConversion,
                    isTemplateInspectionInProgress = snapshot.files.isTemplateInspectionInProgress,
                ),
                templateSupport = templateSupport,
                validation = validation,
                isNetworkAvailable = networkAvailable,
                isSmtpAuthenticated = settings.smtp.isAuthenticated,
                supportsConversion = capabilities.canRunConversion,
                supportsEmailSending = capabilities.canSendEmails,
                entries = snapshot.entries,
                editingManualField = overlay.editingManualField,
                notification = overlay.notification,
            )
        },
        previewLoadingState,
        previewPdfPathState,
        emailProgressRepository.cachedEmails,
    ) { baseState, isPreviewLoading, previewPdfPath, cachedEmails ->
        baseState.copy(
            isPreviewLoading = isPreviewLoading,
            previewPdfPath = previewPdfPath,
            cachedEmailsCount = cachedEmails.entries.size,
        )
    }

    val uiState: StateFlow<ConversionUiState> = baseUiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConversionUiState(),
    )

    fun setTemplatePath(path: String) {
        if (!capabilities.canRunConversion) return
        restartTemplateWatcher(path)
        refreshTemplate(path, isAutoRefresh = false)
    }

    fun setManualFieldValue(fieldTag: String, value: String) {
        val field =
            configurationRepository.state.value.configuration.manualField(fieldTag) ?: return
        val sanitized = when (field.type) {
            CertificateFieldType.NUMBER -> value.filter { it in '0'..'9' }
            CertificateFieldType.DATE -> value.filter { it.isDigit() || it == '-' }.take(10)
            else -> value
        }
        formState.update { it.copy(manualValues = it.manualValues + (fieldTag to sanitized)) }
    }

    fun setFeedbackUrl(value: String) {
        formState.update { it.copy(feedbackUrl = value) }
    }

    fun selectXlsx(path: String) {
        if (!capabilities.canRunConversion) {
            entriesState.value = emptyList()
            return
        }
        restartXlsxWatcher(path)
        entriesState.value = emptyList()
        filesState.update {
            it.copy(
                xlsxPath = path,
                xlsxLoadError = null,
                xlsxHeaders = emptyList(),
            )
        }
        if (path.isBlank()) {
            entriesState.value = emptyList()
            return
        }
        refreshXlsx(path, isAutoRefresh = false)
    }

    fun generateDocuments(): Boolean {
        if (!capabilities.canRunConversion) return false
        hasAttemptedSubmitState.value = true
        if (currentValidationState().hasBlockingErrors) return false
        viewModelScope.launch {
            generateCertificates(buildGenerateRequest(uiState.value))
        }
        return true
    }

    fun previewDocument() {
        if (!capabilities.canRunConversion || previewLoadingState.value) return
        hasAttemptedSubmitState.value = true
        if (currentValidationState().hasBlockingErrors) return
        viewModelScope.launch {
            previewLoadingState.value = true
            try {
                previewPdfPathState.value = null
                val previewPath = previewCertificate(buildGenerateRequest(uiState.value))
                if (previewPath.isNullOrBlank()) return@launch
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

    fun consumeNotification(notificationId: Long) {
        notificationState.update { current ->
            if (current?.id == notificationId) null else current
        }
    }

    fun openManualFieldEditor(fieldTag: String) {
        val field =
            configurationRepository.state.value.configuration.manualField(fieldTag) ?: return
        editingManualFieldState.value = ConversionManualFieldEditorState(
            originalTag = fieldTag,
            draft = field.toDraft(),
            message = null,
        )
    }

    fun updateEditingManualField(update: (ManualTagFieldDraft) -> ManualTagFieldDraft) {
        editingManualFieldState.update { current ->
            current?.copy(
                draft = update(current.draft),
                message = null,
            )
        }
    }

    fun dismissManualFieldEditor() {
        editingManualFieldState.value = null
    }

    fun saveEditingManualField() {
        val editorState = editingManualFieldState.value ?: return
        val currentConfiguration = configurationRepository.state.value.configuration
        val fieldIndex =
            currentConfiguration.manualFields.indexOfFirst { it.tag == editorState.originalTag }
        if (fieldIndex < 0) {
            editingManualFieldState.update { it?.copy(message = "Nepavyko rasti redaguojamo lauko.") }
            return
        }
        val updatedField = editorState.draft.toField()
        val updatedDocumentNumberTag =
            if (currentConfiguration.documentNumberTag == editorState.originalTag) {
                updatedField.tag
            } else {
                currentConfiguration.documentNumberTag
            }
        val updatedConfiguration = currentConfiguration.copy(
            documentNumberTag = updatedDocumentNumberTag,
            manualFields = currentConfiguration.manualFields.updateItem(fieldIndex) { updatedField },
        )
        viewModelScope.launch {
            configurationRepository.save(updatedConfiguration)
                .onSuccess {
                    migrateManualFieldValue(editorState.originalTag, updatedField.tag)
                    editingManualFieldState.value = null
                }
                .onFailure { error ->
                    editingManualFieldState.update {
                        it?.copy(message = error.message ?: "Nepavyko išsaugoti lauko.")
                    }
                }
        }
    }

    private fun observeConfigurationChanges() {
        viewModelScope.launch {
            var previousConfiguration: CertificateConfiguration? = null
            configurationRepository.state.collect { state ->
                val newConfiguration = state.configuration
                if (previousConfiguration == null) {
                    previousConfiguration = newConfiguration
                    return@collect
                }
                if (previousConfiguration == newConfiguration) return@collect
                previousConfiguration = newConfiguration
                val selectedXlsx = filesState.value.xlsxPath
                if (selectedXlsx.isNotBlank()) {
                    refreshXlsx(selectedXlsx, isAutoRefresh = false)
                }
            }
        }
    }

    private fun refreshTemplate(path: String, isAutoRefresh: Boolean) {
        filesState.update {
            it.copy(
                templatePath = path,
                templateLoadError = if (isAutoRefresh) it.templateLoadError else null,
                templateAvailableTags = if (isAutoRefresh) it.templateAvailableTags else null,
                isTemplateInspectionInProgress = path.isNotBlank(),
            )
        }
        if (path.isBlank()) return
        viewModelScope.launch {
            val placeholders = runCatching {
                withContext(Dispatchers.IO) { documentGenerator.inspectTemplatePlaceholders(path) }
            }
            filesState.update { current ->
                if (current.templatePath != path) return@update current
                placeholders.fold(
                    onSuccess = {
                        if (isAutoRefresh) {
                            postNotification(UiMessage(Res.string.conversion_refresh_docx_succeeded))
                        }
                        current.copy(
                            templateLoadError = null,
                            templateAvailableTags = it,
                            isTemplateInspectionInProgress = false,
                        )
                    },
                    onFailure = { error ->
                        logError(
                            logTag,
                            "Failed to inspect DOCX template placeholders: $path",
                            error
                        )
                        if (isAutoRefresh) {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_docx_failed),
                                isError = true,
                            )
                        }
                        current.copy(
                            templateLoadError = docxInspectErrorMessage(),
                            isTemplateInspectionInProgress = false,
                        )
                    },
                )
            }
        }
    }

    private fun refreshXlsx(
        path: String,
        isAutoRefresh: Boolean,
    ) {
        viewModelScope.launch {
            val configuration = configurationRepository.state.value.configuration
            val inspected = runCatching {
                withContext(Dispatchers.IO) { parseRegistrations.inspect(path).getOrThrow() }
            }
            inspected.fold(
                onSuccess = { sheet ->
                    val missingHeaders = configuration.xlsxFields.mapNotNull { field ->
                        val expectedHeader = field.headerName?.trim().takeUnless { it.isNullOrBlank() }
                        if (expectedHeader == null || expectedHeader !in sheet.headers) {
                            val descriptor = field.label?.takeIf { it.isNotBlank() } ?: field.tag
                            "$descriptor -> ${expectedHeader ?: "?"}"
                        } else {
                            null
                        }
                    }
                    filesState.update { current ->
                        if (current.xlsxPath != path) return@update current
                        current.copy(
                            xlsxHeaders = sheet.headers,
                            xlsxLoadError = if (missingHeaders.isNotEmpty()) {
                                xlsxMissingHeadersErrorMessage(missingHeaders.joinToString(", "))
                            } else {
                                null
                            },
                        )
                    }
                    if (missingHeaders.isNotEmpty()) {
                        if (!isAutoRefresh) {
                            entriesState.value = emptyList()
                        } else {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_xlsx_failed),
                                isError = true,
                            )
                        }
                        return@fold
                    }

                    val parsed = runCatching {
                        withContext(Dispatchers.IO) { parseRegistrations(path, configuration).getOrThrow() }
                    }
                    val entries = parsed.getOrElse { error ->
                        logError(logTag, "Failed to parse XLSX: $path", error)
                        if (!isAutoRefresh) emptyList() else entriesState.value
                    }
                    if (parsed.isSuccess) {
                        entriesState.value = entries
                        if (isAutoRefresh) {
                            postNotification(UiMessage(Res.string.conversion_refresh_xlsx_succeeded))
                        }
                    } else if (isAutoRefresh) {
                        postNotification(
                            UiMessage(Res.string.conversion_refresh_xlsx_failed),
                            isError = true,
                        )
                    }
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
                },
                onFailure = { error ->
                    logError(logTag, "Failed to inspect XLSX: $path", error)
                    if (!isAutoRefresh) {
                        entriesState.value = emptyList()
                    }
                    filesState.update { current ->
                        if (current.xlsxPath != path) return@update current
                        current.copy(
                            xlsxLoadError = xlsxParseErrorMessage(),
                        )
                    }
                    if (isAutoRefresh) {
                        postNotification(
                            UiMessage(Res.string.conversion_refresh_xlsx_failed),
                            isError = true,
                        )
                    }
                },
            )
        }
    }

    private fun selectInstalledTemplateIfAvailable() {
        if (!capabilities.canRunConversion || filesState.value.templatePath.isNotBlank()) return
        val installedTemplatePath =
            AppInstallation.installedResourcePath(installedTemplateFileName) ?: return
        restartTemplateWatcher(installedTemplatePath)
        refreshTemplate(installedTemplatePath, isAutoRefresh = false)
    }

    private fun currentValidationState(): ConversionValidationState {
        val configuration = configurationRepository.state.value.configuration
        val resolvedForm = resolveFormState(formState.value, configuration)
        return buildConversionValidationState(
            files = filesState.value,
            configuration = configuration,
            formValues = resolvedForm.manualValues,
            entriesCount = entriesState.value.size,
            templateSupport = buildTemplateSupportState(
                configuration,
                filesState.value.templateAvailableTags
            ),
            hasAttemptedSubmit = true,
        )
    }

    private fun effectiveOutputDirectory(): String {
        val configuredOutputDirectory =
            settingsRepository.state.value.certificate.outputDirectory.trim()
        return when {
            configuredOutputDirectory.isBlank() -> defaultOutputDirectory
            shouldResetLegacyInstallOutputDirectory(
                configuredOutputDirectory,
                installationDirectoryPath
            ) -> defaultOutputDirectory

            OutputDirectory.canWrite(configuredOutputDirectory) -> configuredOutputDirectory
            else -> configuredOutputDirectory
        }
    }

    private fun buildGenerateRequest(snapshot: ConversionUiState): GenerateCertificatesRequest {
        return GenerateCertificatesRequest(
            configuration = snapshot.configuration,
            templatePath = snapshot.files.templatePath,
            entries = snapshot.entries,
            manualValues = snapshot.form.manualValues,
            docIdStart = snapshot.form.valueFor(snapshot.configuration.documentNumberTag),
            certificateName = snapshot.form.valueFor(CertificateNameFieldId),
            feedbackUrl = snapshot.form.feedbackUrl,
            outputDirectory = effectiveOutputDirectory(),
        )
    }

    private fun migrateManualFieldValue(oldTag: String, newTag: String) {
        if (oldTag == newTag) return
        formState.update { current ->
            val currentValue = current.manualValues[oldTag] ?: return@update current
            current.copy(
                manualValues = (current.manualValues - oldTag) + (newTag to currentValue),
            )
        }
    }

    private fun restartXlsxWatcher(path: String) {
        xlsxWatcher?.cancel()
        xlsxRefreshJob?.cancel()
        xlsxWatcher = if (path.isBlank()) {
            null
        } else {
            fileChangeObserver.watch(path) { scheduleXlsxAutoRefresh(path) }
        }
    }

    private fun restartTemplateWatcher(path: String) {
        templateWatcher?.cancel()
        templateRefreshJob?.cancel()
        templateWatcher = if (path.isBlank()) {
            null
        } else {
            fileChangeObserver.watch(path) { scheduleTemplateAutoRefresh(path) }
        }
    }

    private fun scheduleXlsxAutoRefresh(path: String) {
        xlsxRefreshJob?.cancel()
        xlsxRefreshJob = viewModelScope.launch {
            delay(refreshDebounceMillis)
            if (filesState.value.xlsxPath == path) {
                logInfo(logTag, "Detected XLSX file change: $path")
                refreshXlsx(path, isAutoRefresh = true)
            }
        }
    }

    private fun scheduleTemplateAutoRefresh(path: String) {
        templateRefreshJob?.cancel()
        templateRefreshJob = viewModelScope.launch {
            delay(refreshDebounceMillis)
            if (filesState.value.templatePath == path) {
                logInfo(logTag, "Detected DOCX file change: $path")
                refreshTemplate(path, isAutoRefresh = true)
            }
        }
    }

    private fun postNotification(message: UiMessage, isError: Boolean = false) {
        notificationIdCounter += 1
        notificationState.value = ConversionNotificationState(
            id = notificationIdCounter,
            message = message,
            isError = isError,
        )
    }

    override fun onCleared() {
        xlsxWatcher?.cancel()
        templateWatcher?.cancel()
        xlsxRefreshJob?.cancel()
        templateRefreshJob?.cancel()
        super.onCleared()
    }
}

data class ConversionUiState(
    val configuration: CertificateConfiguration = defaultCertificateConfiguration(),
    val configSource: CertificateConfigurationSource = CertificateConfigurationSource.CodeDefault,
    val configLoadFailureMessage: String? = null,
    val files: ConversionFilesState = ConversionFilesState(),
    val form: ConversionFormState = ConversionFormState(),
    val manualFields: List<ConversionManualFieldUiState> = emptyList(),
    val templateSupport: ConversionTemplateSupportState = ConversionTemplateSupportState(),
    val validation: ConversionValidationState = ConversionValidationState(),
    val isNetworkAvailable: Boolean = true,
    val isSmtpAuthenticated: Boolean = false,
    val supportsConversion: Boolean = true,
    val supportsEmailSending: Boolean = true,
    val isPreviewLoading: Boolean = false,
    val previewPdfPath: String? = null,
    val entries: List<RegistrationEntry> = emptyList(),
    val cachedEmailsCount: Int = 0,
    val editingManualField: ConversionManualFieldEditorState? = null,
    val notification: ConversionNotificationState? = null,
) {
    val canRetryCachedEmails: Boolean
        get() = cachedEmailsCount > 0 && supportsEmailSending && isNetworkAvailable && isSmtpAuthenticated
}

private data class ConversionInputSnapshot(
    val form: ConversionFormState,
    val files: ConversionFilesState,
    val entries: List<RegistrationEntry>,
    val hasAttemptedSubmit: Boolean,
)

private data class ConversionOverlayState(
    val editingManualField: ConversionManualFieldEditorState?,
    val notification: ConversionNotificationState?,
)

data class ConversionFilesState(
    val xlsxPath: String = "",
    val templatePath: String = "",
    val xlsxLoadError: UiMessage? = null,
    val templateLoadError: UiMessage? = null,
    val templateAvailableTags: Set<String>? = null,
    val isTemplateInspectionInProgress: Boolean = false,
    val xlsxHeaders: List<String> = emptyList(),
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

private fun String.toFileName(): String = substringAfterLast('/').substringAfterLast('\\')

data class ConversionFormState(
    val manualValues: Map<String, String> = emptyMap(),
    val feedbackUrl: String = "",
) {
    fun valueFor(fieldTag: String): String = manualValues[fieldTag].orEmpty()
}

data class ConversionManualFieldUiState(
    val tag: String,
    val type: CertificateFieldType,
    val label: String? = null,
    val value: String = "",
    val options: List<String> = emptyList(),
    val enabled: Boolean = true,
    val error: UiMessage? = null,
    val helper: UiMessage? = null,
    val tooltip: UiMessage? = null,
)

data class ConversionManualFieldEditorState(
    val originalTag: String,
    val draft: ManualTagFieldDraft,
    val message: String? = null,
)

data class ConversionNotificationState(
    val id: Long,
    val message: UiMessage,
    val isError: Boolean = false,
)

private fun resolveFormState(
    current: ConversionFormState,
    configuration: CertificateConfiguration,
): ConversionFormState {
    val resolvedValues = buildMap {
        configuration.manualFields.forEach { field ->
            val currentValue = current.manualValues[field.tag].orEmpty()
            val resolvedValue: String = when {
                field.type == CertificateFieldType.SELECT && currentValue.isNotBlank() && currentValue !in field.options -> {
                    field.options.firstOrNull().orEmpty()
                }

                currentValue.isNotBlank() -> currentValue
                field.defaultValue != null -> field.defaultValue.orEmpty()
                field.type == CertificateFieldType.SELECT -> field.options.firstOrNull().orEmpty()
                else -> ""
            }
            put(field.tag, resolvedValue)
        }
    }
    return current.copy(manualValues = resolvedValues)
}

private fun buildManualFieldUiState(
    configuration: CertificateConfiguration,
    resolvedForm: ConversionFormState,
    templateSupport: ConversionTemplateSupportState,
    validation: ConversionValidationState,
    enabled: Boolean,
    isTemplateInspectionInProgress: Boolean,
): List<ConversionManualFieldUiState> {
    return configuration.manualFields.map { field ->
        val availability = templateSupport.field(field.tag)
        ConversionManualFieldUiState(
            tag = field.tag,
            type = field.type,
            label = field.label,
            value = resolvedForm.valueFor(field.tag),
            options = field.options,
            enabled = enabled && !isTemplateInspectionInProgress && availability.isEnabled,
            error = validation.errorFor(field.tag),
            helper = availability.disabledSupportingText,
            tooltip = availability.disabledTooltip,
        )
    }
}
