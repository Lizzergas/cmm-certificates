package com.cmm.certificates.data.docx

expect object DocxTemplate {
    fun loadTemplate(path: String): ByteArray

    fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    )
}
