package com.cmm.certificates.feature.certificate.data

import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.feature.certificate.domain.port.CertificateDocumentGenerator

class DocxCertificateDocumentGenerator : CertificateDocumentGenerator {
    override fun loadTemplate(path: String): ByteArray = DocxTemplate.loadTemplate(path)

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
}
