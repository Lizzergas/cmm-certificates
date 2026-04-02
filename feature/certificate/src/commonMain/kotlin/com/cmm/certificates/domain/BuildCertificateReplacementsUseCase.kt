package com.cmm.certificates.domain

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.manualField
import com.cmm.certificates.domain.config.tagForField
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

class BuildCertificateReplacementsUseCase {
    operator fun invoke(
        request: GenerateCertificatesRequest,
        entry: RegistrationEntry,
        docId: Long,
    ): Map<String, String> {
        val values = linkedMapOf<String, String>()

        request.configuration.manualFields.forEach { field ->
            val rawValue = if (field.tag == request.configuration.documentNumberTag) {
                docId.toString()
            } else {
                request.manualValues[field.tag].orEmpty()
            }
            values[field.tag] = formatManualValue(
                configuration = request.configuration,
                fieldTag = field.tag,
                rawValue = rawValue,
            )
        }

        request.configuration.xlsxFields.forEach { field ->
            values[field.tag] = entry.fieldValues[field.tag].orEmpty()
        }

        return values.mapKeys { (fieldTag, _) -> request.configuration.tagForField(fieldTag) }
    }
}

private fun formatManualValue(
    configuration: CertificateConfiguration,
    fieldTag: String,
    rawValue: String,
): String {
    val field = configuration.manualField(fieldTag) ?: return rawValue
    return when (field.type) {
        CertificateFieldType.DATE -> parseCertificateDateInput(rawValue)
            ?.let(::formatCertificateDate)
            .orEmpty()

        else -> rawValue.trim()
    }
}
