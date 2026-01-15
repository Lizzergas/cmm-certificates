package com.cmm.certificates.docx

actual object DocxTemplate {
    actual fun loadTemplate(path: String): ByteArray {
        throw UnsupportedOperationException("DOCX templating is only supported on JVM desktop targets for now.")
    }

    actual fun fillTemplate(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        throw UnsupportedOperationException("DOCX templating is only supported on JVM desktop targets for now.")
    }
}
