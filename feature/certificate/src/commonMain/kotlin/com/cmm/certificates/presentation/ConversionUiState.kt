package com.cmm.certificates.presentation

import com.cmm.certificates.configeditor.ManualTagFieldDraft
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.config.CertificateConfigurationSource
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.recipientEmailField
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

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
    val recipientEmailMapping: RecipientEmailMappingUiState = RecipientEmailMappingUiState(),
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

internal data class ConversionInputSnapshot(
    val form: ConversionFormState,
    val files: ConversionFilesState,
    val entries: List<RegistrationEntry>,
    val hasAttemptedSubmit: Boolean,
    val runtimeMappings: ConversionRuntimeMappingsState,
)

internal data class ConversionOverlayState(
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

data class ConversionFormState(
    val manualValues: Map<String, String> = emptyMap(),
    val feedbackUrl: String = "",
) {
    fun valueFor(fieldTag: String): String = manualValues[fieldTag].orEmpty()
}

data class ConversionRuntimeMappingsState(
    val recipientEmailHeaderOverride: String = "",
)

data class RecipientEmailMappingUiState(
    val availableHeaders: List<String> = emptyList(),
    val selectedHeader: String = "",
    val isVisible: Boolean = false,
    val isMissingSelection: Boolean = false,
    val canSaveAsDefault: Boolean = false,
)

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

internal fun buildConversionBaseUiState(
    snapshot: ConversionInputSnapshot,
    configuration: CertificateConfiguration,
    configSource: CertificateConfigurationSource,
    configLoadFailureMessage: String?,
    overlay: ConversionOverlayState,
    isNetworkAvailable: Boolean,
    isSmtpAuthenticated: Boolean,
    supportsConversion: Boolean,
    supportsEmailSending: Boolean,
): ConversionUiState {
    val resolvedForm = resolveConversionFormState(snapshot.form, configuration)
    val templateSupport =
        buildTemplateSupportState(configuration, snapshot.files.templateAvailableTags)
    val recipientEmailMapping = buildRecipientEmailMappingUiState(
        configuration = configuration,
        files = snapshot.files,
        runtimeMappings = snapshot.runtimeMappings,
        supportsEmailSending = supportsEmailSending,
    )
    val validation = buildConversionValidationState(
        files = snapshot.files,
        configuration = configuration,
        formValues = resolvedForm.manualValues,
        entriesCount = snapshot.entries.size,
        templateSupport = templateSupport,
        hasAttemptedSubmit = snapshot.hasAttemptedSubmit,
        requiresRecipientEmailSelection = recipientEmailMapping.isMissingSelection,
    )

    return ConversionUiState(
        configuration = configuration,
        configSource = configSource,
        configLoadFailureMessage = configLoadFailureMessage,
        files = snapshot.files,
        form = resolvedForm,
        manualFields = buildConversionManualFieldUiState(
            configuration = configuration,
            resolvedForm = resolvedForm,
            templateSupport = templateSupport,
            validation = validation,
            enabled = supportsConversion,
            isTemplateInspectionInProgress = snapshot.files.isTemplateInspectionInProgress,
        ),
        templateSupport = templateSupport,
        validation = validation,
        isNetworkAvailable = isNetworkAvailable,
        isSmtpAuthenticated = isSmtpAuthenticated,
        supportsConversion = supportsConversion,
        supportsEmailSending = supportsEmailSending,
        recipientEmailMapping = recipientEmailMapping,
        entries = snapshot.entries,
        editingManualField = overlay.editingManualField,
        notification = overlay.notification,
    )
}

private fun buildRecipientEmailMappingUiState(
    configuration: CertificateConfiguration,
    files: ConversionFilesState,
    runtimeMappings: ConversionRuntimeMappingsState,
    supportsEmailSending: Boolean,
): RecipientEmailMappingUiState {
    val persistedHeader = configuration.recipientEmailField()?.headerName.orEmpty()
    val selectedHeader = runtimeMappings.recipientEmailHeaderOverride.ifBlank { persistedHeader }
    val headers = files.xlsxHeaders
    return RecipientEmailMappingUiState(
        availableHeaders = headers,
        selectedHeader = selectedHeader,
        isVisible = supportsEmailSending && headers.isNotEmpty(),
        isMissingSelection = supportsEmailSending && headers.isNotEmpty() && selectedHeader.isBlank(),
        canSaveAsDefault = selectedHeader.isNotBlank() && selectedHeader != persistedHeader,
    )
}

private fun String.toFileName(): String = substringAfterLast('/').substringAfterLast('\\')
