package com.cmm.certificates.presentation

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_docx_inspect
import certificates.composeapp.generated.resources.conversion_error_dynamic_date_invalid
import certificates.composeapp.generated.resources.conversion_error_dynamic_required
import certificates.composeapp.generated.resources.conversion_error_no_entries
import certificates.composeapp.generated.resources.conversion_error_template_required
import certificates.composeapp.generated.resources.conversion_error_xlsx_missing_headers
import certificates.composeapp.generated.resources.conversion_error_xlsx_parse
import certificates.composeapp.generated.resources.conversion_error_xlsx_required
import certificates.composeapp.generated.resources.conversion_missing_template_tag_supporting
import certificates.composeapp.generated.resources.conversion_missing_template_tag_tooltip
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.tagForField
import com.cmm.certificates.domain.parseCertificateDateInput

data class TemplateFieldAvailability(
    val tag: String,
    val isPresentInTemplate: Boolean = true,
    val disableWhenMissing: Boolean = true,
) {
    val isEnabled: Boolean
        get() = isPresentInTemplate || !disableWhenMissing

    val disabledSupportingText: UiMessage?
        get() = if (isEnabled) null else UiMessage(
            Res.string.conversion_missing_template_tag_supporting,
            listOf(tag),
        )

    val disabledTooltip: UiMessage?
        get() = if (isEnabled) null else UiMessage(
            Res.string.conversion_missing_template_tag_tooltip,
            listOf(tag),
        )
}

data class ConversionTemplateSupportState(
    val availabilityByFieldId: Map<String, TemplateFieldAvailability> = emptyMap(),
    val documentNumberFieldId: String = "",
) {
    fun field(fieldId: String): TemplateFieldAvailability {
        return availabilityByFieldId[fieldId]
            ?: TemplateFieldAvailability(
                tag = "{{$fieldId}}",
                isPresentInTemplate = true,
                disableWhenMissing = fieldId != documentNumberFieldId,
            )
    }
}

data class ConversionValidationState(
    val xlsxError: UiMessage? = null,
    val templateError: UiMessage? = null,
    val fieldErrors: Map<String, UiMessage> = emptyMap(),
) {
    val hasBlockingErrors: Boolean
        get() = xlsxError != null || templateError != null || fieldErrors.isNotEmpty()

    fun errorFor(fieldId: String): UiMessage? = fieldErrors[fieldId]
}

fun buildTemplateSupportState(
    configuration: CertificateConfiguration,
    templateAvailableTags: Set<String>?,
): ConversionTemplateSupportState {
    val availability = configuration.manualFields.associate { field ->
        val isPresent =
            templateAvailableTags?.contains(configuration.tagForField(field.tag)) ?: true
        field.tag to TemplateFieldAvailability(
            tag = configuration.tagForField(field.tag),
            isPresentInTemplate = isPresent,
            disableWhenMissing = field.tag != configuration.documentNumberTag,
        )
    }

    return ConversionTemplateSupportState(
        availabilityByFieldId = availability,
        documentNumberFieldId = configuration.documentNumberTag,
    )
}

fun buildConversionValidationState(
    files: ConversionFilesState,
    configuration: CertificateConfiguration,
    formValues: Map<String, String>,
    entriesCount: Int,
    templateSupport: ConversionTemplateSupportState,
    hasAttemptedSubmit: Boolean,
): ConversionValidationState {
    val xlsxError = when {
        files.xlsxLoadError != null -> files.xlsxLoadError
        hasAttemptedSubmit && files.xlsxPath.isBlank() -> UiMessage(Res.string.conversion_error_xlsx_required)
        hasAttemptedSubmit && entriesCount == 0 -> UiMessage(Res.string.conversion_error_no_entries)
        else -> null
    }
    val templateError = when {
        files.templateLoadError != null -> files.templateLoadError
        hasAttemptedSubmit && files.templatePath.isBlank() -> UiMessage(Res.string.conversion_error_template_required)
        hasAttemptedSubmit &&
                files.templatePath.isNotBlank() &&
                files.templateAvailableTags == null &&
                !files.isTemplateInspectionInProgress -> {
            UiMessage(Res.string.conversion_error_docx_inspect)
        }

        else -> null
    }

    if (!hasAttemptedSubmit) {
        return ConversionValidationState(
            xlsxError = xlsxError,
            templateError = templateError,
        )
    }

    val fieldErrors = buildMap {
        configuration.manualFields.forEach { field ->
            val availability = templateSupport.field(field.tag)
            if (!availability.isEnabled) return@forEach

            val value = formValues[field.tag].orEmpty()
            val error = when {
                value.isBlank() -> UiMessage(
                    Res.string.conversion_error_dynamic_required,
                    listOf(conversionFieldLabel(field.tag, field.label)),
                )

                field.type == CertificateFieldType.DATE && parseCertificateDateInput(value) == null -> {
                    UiMessage(
                        Res.string.conversion_error_dynamic_date_invalid,
                        listOf(conversionFieldLabel(field.tag, field.label)),
                    )
                }

                else -> null
            }
            if (error != null) {
                put(field.tag, error)
            }
        }
    }

    return ConversionValidationState(
        xlsxError = xlsxError,
        templateError = templateError,
        fieldErrors = fieldErrors,
    )
}

fun xlsxParseErrorMessage(): UiMessage = UiMessage(Res.string.conversion_error_xlsx_parse)

fun xlsxMissingHeadersErrorMessage(details: String): UiMessage = UiMessage(
    Res.string.conversion_error_xlsx_missing_headers,
    listOf(details),
)

fun docxInspectErrorMessage(): UiMessage = UiMessage(Res.string.conversion_error_docx_inspect)
