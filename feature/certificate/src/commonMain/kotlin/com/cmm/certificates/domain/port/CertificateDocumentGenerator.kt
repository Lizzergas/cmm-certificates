package com.cmm.certificates.domain.port

interface CertificateDocumentGenerator {
    fun loadTemplate(path: String): ByteArray

    fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    )

    fun createPreviewPdf(
        templateBytes: ByteArray,
        replacements: Map<String, String>,
    ): String?
}
