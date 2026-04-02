package com.cmm.certificates.domain.config

import kotlinx.serialization.Serializable

private const val CurrentCertificateConfigurationVersion = 1

@Serializable
data class CertificateConfiguration(
    val version: Int = CurrentCertificateConfigurationVersion,
    val id: String,
    val documentNumberTag: String,
    val recipientEmailTag: String? = null,
    val xlsxFields: List<XlsxTagField> = emptyList(),
    val manualFields: List<ManualTagField> = emptyList(),
)

@Serializable
data class XlsxTagField(
    val tag: String,
    val label: String? = null,
    val headerName: String? = null,
)

@Serializable
data class ManualTagField(
    val tag: String,
    val label: String? = null,
    val type: CertificateFieldType,
    val defaultValue: String? = null,
    val options: List<String> = emptyList(),
)

@Serializable
enum class CertificateFieldType {
    TEXT,
    NUMBER,
    DATE,
    SELECT,
    MULTILINE,
}

val CertificateConfiguration.allTags: Set<String>
    get() = (xlsxFields.map(XlsxTagField::tag) + manualFields.map(ManualTagField::tag)).toSet()

fun CertificateConfiguration.manualField(tag: String): ManualTagField? {
    return manualFields.firstOrNull { it.tag == tag }
}

fun CertificateConfiguration.xlsxField(tag: String): XlsxTagField? {
    return xlsxFields.firstOrNull { it.tag == tag }
}

fun CertificateConfiguration.recipientEmailField(): XlsxTagField? {
    val tag = recipientEmailTag?.trim().takeUnless { it.isNullOrBlank() } ?: return null
    return xlsxField(tag)
}

fun CertificateConfiguration.withRecipientEmailMapping(
    headerName: String,
    recipientTag: String = recipientEmailTag?.trim().takeUnless { it.isNullOrBlank() } ?: EmailFieldId,
): CertificateConfiguration {
    val normalizedHeader = headerName.trim()
    require(normalizedHeader.isNotBlank()) { "Recipient email header must not be blank" }

    val updatedField = (xlsxField(recipientTag) ?: XlsxTagField(tag = recipientTag, label = "Email"))
        .copy(headerName = normalizedHeader)
    val updatedFields = if (xlsxFields.any { it.tag == recipientTag }) {
        xlsxFields.map { existing ->
            if (existing.tag == recipientTag) {
                updatedField
            } else {
                existing
            }
        }
    } else {
        xlsxFields + updatedField
    }

    return copy(
        recipientEmailTag = recipientTag,
        xlsxFields = updatedFields,
    )
}

fun CertificateConfiguration.tagForField(tag: String): String = "{{$tag}}"
