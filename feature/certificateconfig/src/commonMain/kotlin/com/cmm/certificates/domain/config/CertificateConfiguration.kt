package com.cmm.certificates.domain.config

import kotlinx.serialization.Serializable

private const val CurrentCertificateConfigurationVersion = 1

@Serializable
data class CertificateConfiguration(
    val version: Int = CurrentCertificateConfigurationVersion,
    val id: String,
    val documentNumberTag: String,
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

fun CertificateConfiguration.tagForField(tag: String): String = "{{$tag}}"
