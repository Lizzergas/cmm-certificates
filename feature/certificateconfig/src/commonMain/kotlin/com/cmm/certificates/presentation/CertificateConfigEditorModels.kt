package com.cmm.certificates.presentation

import com.cmm.certificates.configeditor.ManualTagFieldDraft
import com.cmm.certificates.configeditor.toField
import com.cmm.certificates.data.config.CertificateConfigurationSource
import com.cmm.certificates.domain.config.CertificateConfiguration
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
