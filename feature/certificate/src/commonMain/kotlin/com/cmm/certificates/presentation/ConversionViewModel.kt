package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.AppInstallation
import com.cmm.certificates.configeditor.ManualTagFieldDraft
import com.cmm.certificates.configeditor.toDraft
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.core.openFile
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.config.CertificateConfigurationRepository
import com.cmm.certificates.domain.GenerateCertificatesUseCase
import com.cmm.certificates.domain.PreviewCertificateUseCase
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.withRecipientEmailMapping
import com.cmm.certificates.domain.config.manualField
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.domain.port.FileChangeObserver
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.preferredDefaultOutputDirectory
import com.cmm.certificates.presentation.actions.GenerateCertificatesRequestBuilder
import com.cmm.certificates.presentation.actions.GenerateDocumentsAction
import com.cmm.certificates.presentation.actions.PreviewDocumentAction
import com.cmm.certificates.presentation.actions.PreviewDocumentResult
import com.cmm.certificates.presentation.manual.ConversionManualFieldEditorService
import com.cmm.certificates.presentation.manual.SaveManualFieldEditResult
import com.cmm.certificates.presentation.refresh.ConversionRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val requestBuilder = GenerateCertificatesRequestBuilder(
        defaultOutputDirectory = defaultOutputDirectory,
        installationDirectoryPath = installationDirectoryPath,
    )
    private val generateDocumentsAction = GenerateDocumentsAction(
        requestBuilder = requestBuilder,
        generateDocuments = generateCertificates::invoke,
    )
    private val previewDocumentAction = PreviewDocumentAction(
        requestBuilder = requestBuilder,
        previewDocument = previewCertificate::invoke,
        openFile = ::openFile,
    )
    private val manualFieldEditorService = ConversionManualFieldEditorService()

    private val formState = MutableStateFlow(ConversionFormState())
    private val runtimeMappingsState = MutableStateFlow(ConversionRuntimeMappingsState())
    private val filesState = MutableStateFlow(ConversionFilesState())
    private val parsedEntriesState = MutableStateFlow(ParsedEntriesState())
    private val hasAttemptedSubmitState = MutableStateFlow(false)
    private val previewLoadingState = MutableStateFlow(false)
    private val previewPdfPathState = MutableStateFlow<String?>(null)
    private val editingManualFieldState = MutableStateFlow<ConversionManualFieldEditorState?>(null)
    private val notificationState = MutableStateFlow<ConversionNotificationState?>(null)
    private var notificationIdCounter = 0L
    private val refreshCoordinator = ConversionRefreshCoordinator(
        scope = viewModelScope,
        fileChangeObserver = fileChangeObserver,
        parseRegistrations = parseRegistrations,
        documentGenerator = documentGenerator,
        ioDispatcher = Dispatchers.IO,
        refreshDebounceMillis = refreshDebounceMillis,
        currentConfiguration = { currentParsingConfiguration() },
        currentFiles = { filesState.value },
        currentEntries = { parsedEntriesState.value.entries },
        updateFiles = filesState::update,
        setParsedEntries = { path, entries ->
            parsedEntriesState.value = ParsedEntriesState(
                sourcePath = path,
                entries = entries,
            )
        },
        postNotification = ::postNotification,
    )

    init {
        selectInstalledTemplateIfAvailable()
        refreshCoordinator.observeConfigurationChanges(
            configurationRepository.state.map { it.configuration },
        )
    }

    private val inputState = combine(
        formState,
        filesState,
        parsedEntriesState,
        hasAttemptedSubmitState,
        runtimeMappingsState,
        ::ConversionInputSnapshot,
    )

    private val overlayState = combine(
        editingManualFieldState,
        notificationState,
        ::ConversionOverlayState,
    )

    private val computedUiState = combine(
        inputState,
        settingsRepository.state,
        connectivityMonitor.isNetworkAvailable,
        configurationRepository.state,
        overlayState,
    ) { snapshot, settings, networkAvailable, configState, overlay ->
        val effectiveConfiguration = applyRuntimeMappings(
            configuration = configState.configuration,
            runtimeMappings = snapshot.runtimeMappings,
        )
        buildConversionBaseUiState(
            snapshot = snapshot,
            configuration = effectiveConfiguration,
            configSource = configState.source,
            configLoadFailureMessage = configState.loadFailureMessage,
            overlay = overlay,
            isNetworkAvailable = networkAvailable,
            isSmtpAuthenticated = settings.smtp.isAuthenticated,
            supportsConversion = capabilities.canRunConversion,
            supportsEmailSending = capabilities.canSendEmails,
        )
    }

    private val baseUiState = combine(
        computedUiState,
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
        refreshCoordinator.restartTemplateWatcher(path)
        refreshCoordinator.refreshTemplate(path, isAutoRefresh = false)
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

    fun setRecipientEmailHeaderOverride(value: String) {
        runtimeMappingsState.update { it.copy(recipientEmailHeaderOverride = value) }
        refreshCurrentXlsxIfSelected()
    }

    fun saveRecipientEmailHeaderAsDefault() {
        val selectedHeader = uiState.value.recipientEmailMapping.selectedHeader.trim()
        if (selectedHeader.isBlank()) return
        val updatedConfiguration = configurationRepository.state.value.configuration
            .withRecipientEmailMapping(selectedHeader)
        viewModelScope.launch {
            configurationRepository.save(updatedConfiguration)
                .onSuccess {
                    runtimeMappingsState.value = ConversionRuntimeMappingsState()
                }
        }
    }

    fun selectXlsx(path: String) {
        if (!capabilities.canRunConversion) {
            parsedEntriesState.value = ParsedEntriesState()
            return
        }
        refreshCoordinator.restartXlsxWatcher(path)
        parsedEntriesState.value = ParsedEntriesState()
        filesState.update {
            it.copy(
                xlsxPath = path,
                xlsxLoadError = null,
                xlsxHeaders = emptyList(),
            )
        }
        if (path.isBlank()) {
            parsedEntriesState.value = ParsedEntriesState()
            return
        }
        refreshCoordinator.refreshXlsx(path, isAutoRefresh = false)
    }

    fun generateDocuments(): Boolean {
        if (!capabilities.canRunConversion) return false
        hasAttemptedSubmitState.value = true
        if (currentValidationState().hasBlockingErrors) return false
        viewModelScope.launch {
            generateDocumentsAction.execute(
                snapshot = uiState.value,
                settings = settingsRepository.state.value,
            )
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
                when (
                    val result = previewDocumentAction.execute(
                        snapshot = uiState.value,
                        settings = settingsRepository.state.value,
                        useInAppPdfPreview = settingsRepository.state.value.appearance.useInAppPdfPreview,
                    )
                ) {
                    PreviewDocumentResult.NoPreview -> Unit
                    is PreviewDocumentResult.ShowInApp -> {
                        previewPdfPathState.value = result.path
                    }

                    is PreviewDocumentResult.ExternalOpenFailed -> {
                        logWarn(logTag, "Failed to open preview PDF: ${result.path}")
                    }
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
        val savePlan = manualFieldEditorService.prepareSave(currentConfiguration, editorState)
        if (savePlan is SaveManualFieldEditResult.Failure) {
            editingManualFieldState.update { it?.copy(message = savePlan.message) }
            return
        }

        savePlan as SaveManualFieldEditResult.Success
        viewModelScope.launch {
            configurationRepository.save(savePlan.updatedConfiguration)
                .onSuccess {
                    formState.update { current ->
                        current.copy(
                            manualValues = manualFieldEditorService.migrateManualValues(
                                currentValues = current.manualValues,
                                oldTag = editorState.originalTag,
                                newTag = savePlan.updatedFieldTag,
                            ),
                        )
                    }
                    editingManualFieldState.value = null
                }
                .onFailure { error ->
                    editingManualFieldState.update {
                        it?.copy(message = error.message ?: "Nepavyko išsaugoti lauko.")
                    }
                }
        }
    }

    private fun selectInstalledTemplateIfAvailable() {
        if (!capabilities.canRunConversion || filesState.value.templatePath.isNotBlank()) return
        val installedTemplatePath =
            AppInstallation.installedResourcePath(installedTemplateFileName) ?: return
        refreshCoordinator.restartTemplateWatcher(installedTemplatePath)
        refreshCoordinator.refreshTemplate(installedTemplatePath, isAutoRefresh = false)
    }

    private fun currentValidationState(): ConversionValidationState {
        val configuration = currentParsingConfiguration()
        val resolvedForm = resolveConversionFormState(formState.value, configuration)
        return buildConversionValidationState(
            files = filesState.value,
            configuration = configuration,
            formValues = resolvedForm.manualValues,
            entriesCount = parsedEntriesState.value.entries.size,
            templateSupport = buildTemplateSupportState(
                configuration,
                filesState.value.templateAvailableTags
            ),
            hasAttemptedSubmit = true,
            requiresRecipientEmailSelection = uiState.value.recipientEmailMapping.isMissingSelection,
            hasEntriesForCurrentXlsx = parsedEntriesState.value.sourcePath == filesState.value.xlsxPath,
        )
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
        refreshCoordinator.cancel()
        super.onCleared()
    }

    private fun refreshCurrentXlsxIfSelected() {
        val selectedXlsx = filesState.value.xlsxPath
        if (selectedXlsx.isNotBlank()) {
            refreshCoordinator.refreshXlsx(selectedXlsx, isAutoRefresh = false)
        }
    }

    private fun currentParsingConfiguration(): CertificateConfiguration {
        return applyRuntimeMappings(
            configuration = configurationRepository.state.value.configuration,
            runtimeMappings = runtimeMappingsState.value,
        )
    }

    private fun applyRuntimeMappings(
        configuration: CertificateConfiguration,
        runtimeMappings: ConversionRuntimeMappingsState,
    ): CertificateConfiguration {
        val recipientHeaderOverride = runtimeMappings.recipientEmailHeaderOverride.trim()
        return if (recipientHeaderOverride.isBlank()) {
            configuration
        } else {
            configuration.withRecipientEmailMapping(recipientHeaderOverride)
        }
    }
}
