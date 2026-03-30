package com.cmm.certificates.feature.emailsending.domain

fun buildEmailHtmlBody(
    body: String,
    signatureHtml: String,
): String? {
    val trimmedSignature = signatureHtml.trim()
    val escapedBody = linkifyBody(body)
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "<br>")
    if (escapedBody.isBlank() && trimmedSignature.isBlank()) return null
    val baseHtml = when {
        escapedBody.isBlank() -> ""
        trimmedSignature.isBlank() -> escapedBody
        else -> "$escapedBody<br><br>"
    }
    return baseHtml + trimmedSignature
}

private val UrlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

private fun linkifyBody(text: String): String {
    return buildString {
        var cursor = 0
        for (match in UrlRegex.findAll(text)) {
            val start = match.range.first
            val endExclusive = match.range.last + 1
            append(escapeHtml(text.substring(cursor, start)))

            val (url, trailing) = splitTrailingPunctuation(match.value)
            val escapedUrl = escapeHtml(url)
            append("<a href=\"")
            append(escapedUrl)
            append("\">")
            append(escapedUrl)
            append("</a>")
            append(escapeHtml(trailing))
            cursor = endExclusive
        }
        append(escapeHtml(text.substring(cursor)))
    }
}

private fun splitTrailingPunctuation(value: String): Pair<String, String> {
    var endIndex = value.length
    while (endIndex > 0 && value[endIndex - 1] in setOf('.', ',', ';', ':', '!', '?', ')')) {
        endIndex--
    }
    return value.substring(0, endIndex) to value.substring(endIndex)
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
