package com.cmm.certificates.data

import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.domain.port.CertificateDocumentGenerator

class DocxCertificateDocumentGenerator : CertificateDocumentGenerator {
    override fun loadTemplate(path: String): ByteArray = DocxTemplate.loadTemplate(path)

    override fun inspectTemplatePlaceholders(path: String): Set<String> =
        DocxTemplate.inspectTemplatePlaceholders(path)

    override fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        DocxTemplate.fillTemplateToPdf(
            templateBytes = templateBytes,
            outputPath = outputPath,
            replacements = replacements,
        )
    }

    override fun createPreviewPdf(
        templateBytes: ByteArray,
        replacements: Map<String, String>,
    ): String? {
        return DocxTemplate.createPreviewPdf(
            templateBytes = templateBytes,
            replacements = replacements,
        )
    }
}
