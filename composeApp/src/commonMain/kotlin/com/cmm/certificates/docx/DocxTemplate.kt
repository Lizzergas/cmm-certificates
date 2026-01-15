package com.cmm.certificates.docx

expect object DocxTemplate {
    fun loadTemplate(path: String): ByteArray

    fun fillTemplate(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    )

    fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    )
}
