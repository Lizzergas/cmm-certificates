package com.cmm.certificates.presentation

import com.cmm.certificates.data.config.CertificateConfigurationSource
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.ManualTagField
import com.cmm.certificates.domain.config.XlsxTagField

data class CertificateConfigUiState(
    val source: CertificateConfigurationSource = CertificateConfigurationSource.CodeDefault,
    val externalPath: String? = null,
    val loadFailureMessage: String? = null,
    val sampleXlsxPath: String = "",
    val sampleHeaders: List<String> = emptyList(),
    val documentNumberTag: String = "",
    val xlsxFields: List<XlsxTagFieldDraft> = emptyList(),
    val manualFields: List<ManualTagFieldDraft> = emptyList(),
    val message: String? = null,
)

data class XlsxTagFieldDraft(
    val tag: String = "",
    val label: String = "",
    val headerName: String = "",
)

data class ManualTagFieldDraft(
    val tag: String = "",
    val label: String = "",
    val type: CertificateFieldType = CertificateFieldType.TEXT,
    val defaultValue: String = "",
    val optionsText: String = "",
)

fun ManualTagField.toDraft(): ManualTagFieldDraft {
    return ManualTagFieldDraft(
        tag = tag,
        label = label.orEmpty(),
        type = type,
        defaultValue = defaultValue.orEmpty(),
        optionsText = options.joinToString("\n"),
    )
}

fun ManualTagFieldDraft.toField(): ManualTagField {
    return ManualTagField(
        tag = tag.trim(),
        label = label.trim().ifBlank { null },
        type = type,
        defaultValue = defaultValue.trim().ifBlank { null },
        options = optionsText.lineSequence().map(String::trim).filter(String::isNotBlank).toList(),
    )
}

fun XlsxTagField.toDraft(): XlsxTagFieldDraft {
    return XlsxTagFieldDraft(
        tag = tag,
        label = label.orEmpty(),
        headerName = headerName.orEmpty(),
    )
}

fun XlsxTagFieldDraft.toField(): XlsxTagField {
    return XlsxTagField(
        tag = tag.trim(),
        label = label.trim().ifBlank { null },
        headerName = headerName.trim().ifBlank { null },
    )
}

fun CertificateConfigUiState.toConfiguration(): CertificateConfiguration {
    return CertificateConfiguration(
        id = "default-certificate",
        documentNumberTag = documentNumberTag,
        xlsxFields = xlsxFields.map(XlsxTagFieldDraft::toField),
        manualFields = manualFields.map(ManualTagFieldDraft::toField),
    )
}

fun <T> List<T>.updateItem(index: Int, update: (T) -> T): List<T> {
    return mapIndexed { currentIndex, item -> if (currentIndex == index) update(item) else item }
}

fun <T> List<T>.removeItem(index: Int): List<T> {
    return filterIndexed { currentIndex, _ -> currentIndex != index }
}
