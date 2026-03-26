package com.cmm.certificates.feature.certificate.domain.port

interface CertificateDocumentGenerator {
    fun loadTemplate(path: String): ByteArray

    fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    )
}
