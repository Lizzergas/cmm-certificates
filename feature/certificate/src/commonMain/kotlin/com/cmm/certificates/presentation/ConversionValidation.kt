package com.cmm.certificates.presentation

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_accredited_hours_required
import certificates.composeapp.generated.resources.conversion_error_accredited_id_required
import certificates.composeapp.generated.resources.conversion_error_accredited_type_required
import certificates.composeapp.generated.resources.conversion_error_certificate_name_required
import certificates.composeapp.generated.resources.conversion_error_doc_id_required
import certificates.composeapp.generated.resources.conversion_error_docx_inspect
import certificates.composeapp.generated.resources.conversion_error_lector_gender_required
import certificates.composeapp.generated.resources.conversion_error_lector_required
import certificates.composeapp.generated.resources.conversion_error_no_entries
import certificates.composeapp.generated.resources.conversion_error_template_required
import certificates.composeapp.generated.resources.conversion_error_xlsx_parse
import certificates.composeapp.generated.resources.conversion_error_xlsx_required
import certificates.composeapp.generated.resources.conversion_missing_template_tag_supporting
import certificates.composeapp.generated.resources.conversion_missing_template_tag_tooltip
import com.cmm.certificates.core.presentation.UiMessage

internal const val AccreditedIdTag = "{{akreditacijos_id}}"
internal const val DocIdTag = "{{dokumento_id}}"
internal const val AccreditedTypeTag = "{{akreditacijos_tipas}}"
internal const val AccreditedHoursTag = "{{akreditacijos_valandos}}"
internal const val CertificateNameTag = "{{sertifikato_pavadinimas}}"
internal const val LectorTag = "{{destytojas}}"
internal const val LectorGenderTag = "{{destytojo_tipas}}"

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
    val accreditedId: TemplateFieldAvailability = TemplateFieldAvailability(AccreditedIdTag),
    val docIdStart: TemplateFieldAvailability = TemplateFieldAvailability(DocIdTag, disableWhenMissing = false),
    val accreditedType: TemplateFieldAvailability = TemplateFieldAvailability(AccreditedTypeTag),
    val accreditedHours: TemplateFieldAvailability = TemplateFieldAvailability(AccreditedHoursTag),
    val certificateName: TemplateFieldAvailability = TemplateFieldAvailability(CertificateNameTag),
    val lector: TemplateFieldAvailability = TemplateFieldAvailability(LectorTag),
    val lectorGender: TemplateFieldAvailability = TemplateFieldAvailability(LectorGenderTag),
)

data class ConversionValidationState(
    val xlsxError: UiMessage? = null,
    val templateError: UiMessage? = null,
    val accreditedIdError: UiMessage? = null,
    val docIdStartError: UiMessage? = null,
    val accreditedTypeError: UiMessage? = null,
    val accreditedHoursError: UiMessage? = null,
    val certificateNameError: UiMessage? = null,
    val lectorError: UiMessage? = null,
    val lectorGenderError: UiMessage? = null,
) {
    val hasBlockingErrors: Boolean
        get() = listOf(
            xlsxError,
            templateError,
            accreditedIdError,
            docIdStartError,
            accreditedTypeError,
            accreditedHoursError,
            certificateNameError,
            lectorError,
            lectorGenderError,
        ).any { it != null }
}

fun buildTemplateSupportState(templateAvailableTags: Set<String>?): ConversionTemplateSupportState {
    fun field(tag: String, disableWhenMissing: Boolean = true): TemplateFieldAvailability {
        val isPresent = templateAvailableTags?.contains(tag) ?: true
        return TemplateFieldAvailability(
            tag = tag,
            isPresentInTemplate = isPresent,
            disableWhenMissing = disableWhenMissing,
        )
    }

    return ConversionTemplateSupportState(
        accreditedId = field(AccreditedIdTag),
        docIdStart = field(DocIdTag, disableWhenMissing = false),
        accreditedType = field(AccreditedTypeTag),
        accreditedHours = field(AccreditedHoursTag),
        certificateName = field(CertificateNameTag),
        lector = field(LectorTag),
        lectorGender = field(LectorGenderTag),
    )
}

fun buildConversionValidationState(
    files: ConversionFilesState,
    form: ConversionFormState,
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

    return ConversionValidationState(
        xlsxError = xlsxError,
        templateError = templateError,
        accreditedIdError = requiredFieldError(
            value = form.accreditedId,
            availability = templateSupport.accreditedId,
            errorResource = Res.string.conversion_error_accredited_id_required,
        ),
        docIdStartError = requiredFieldError(
            value = form.docIdStart,
            availability = templateSupport.docIdStart,
            errorResource = Res.string.conversion_error_doc_id_required,
        ),
        accreditedTypeError = requiredFieldError(
            value = form.accreditedType,
            availability = templateSupport.accreditedType,
            errorResource = Res.string.conversion_error_accredited_type_required,
        ),
        accreditedHoursError = requiredFieldError(
            value = form.accreditedHours,
            availability = templateSupport.accreditedHours,
            errorResource = Res.string.conversion_error_accredited_hours_required,
        ),
        certificateNameError = requiredFieldError(
            value = form.certificateName,
            availability = templateSupport.certificateName,
            errorResource = Res.string.conversion_error_certificate_name_required,
        ),
        lectorError = requiredFieldError(
            value = form.lector,
            availability = templateSupport.lector,
            errorResource = Res.string.conversion_error_lector_required,
        ),
        lectorGenderError = requiredFieldError(
            value = form.lectorGender,
            availability = templateSupport.lectorGender,
            errorResource = Res.string.conversion_error_lector_gender_required,
        ),
    )
}

private fun requiredFieldError(
    value: String,
    availability: TemplateFieldAvailability,
    errorResource: org.jetbrains.compose.resources.StringResource,
): UiMessage? {
    if (!availability.isEnabled) return null
    return if (value.isBlank()) UiMessage(errorResource) else null
}

fun xlsxParseErrorMessage(): UiMessage = UiMessage(Res.string.conversion_error_xlsx_parse)

fun docxInspectErrorMessage(): UiMessage = UiMessage(Res.string.conversion_error_docx_inspect)
