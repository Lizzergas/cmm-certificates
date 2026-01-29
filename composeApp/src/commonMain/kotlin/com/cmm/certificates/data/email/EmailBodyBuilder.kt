package com.cmm.certificates.data.email

fun buildEmailHtmlBody(
    body: String,
    signatureHtml: String,
): String? {
    val trimmedSignature = signatureHtml.trim()
    if (trimmedSignature.isBlank()) return null
    val escapedBody = escapeHtml(body)
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "<br>")
    val baseHtml = if (escapedBody.isBlank()) "" else "$escapedBody<br><br>"
    return baseHtml + trimmedSignature
}

private fun escapeHtml(text: String): String {
    return buildString(text.length) {
        text.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }
}
