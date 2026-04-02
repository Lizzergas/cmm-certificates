package com.cmm.certificates.presentation

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType

internal fun resolveConversionFormState(
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

internal fun buildConversionManualFieldUiState(
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

internal fun conversionFieldLabel(fieldTag: String, explicitLabel: String?): String {
    return explicitLabel?.trim().takeUnless { it.isNullOrBlank() } ?: fieldTag
}
