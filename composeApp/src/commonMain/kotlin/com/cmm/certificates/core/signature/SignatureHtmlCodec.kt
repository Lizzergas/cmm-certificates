package com.cmm.certificates.core.signature

object SignatureHtmlCodec {
    private val rootRegex = Regex(
        """^<div\s+style=\"([^\"]*)\"\s*>(.*)</div>$""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val lineRegex = Regex("""<div>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    fun buildHtml(builder: SignatureBuilderState): String {
        val style = builder.style
        val styleBlock = buildString {
            append("font-family:${style.fontFamily.cssValue}; ")
            append("font-size:${style.fontSizePt}pt; ")
            append("font-style:${if (style.italic) "italic" else "normal"}; ")
            append("font-weight:${if (style.bold) "bold" else "normal"}; ")
            append("line-height:${String.format(java.util.Locale.US, "%.2f", style.lineHeight)}; ")
            append("color:${normalizeColorHex(style.colorHex)};")
        }
        val lines = builder.lines.ifEmpty { listOf("") }
        val body = lines.joinToString(separator = "\n") { "  <div>${escapeHtml(it)}</div>" }
        return "<div style=\"$styleBlock\">\n$body\n</div>"
    }

    fun tryParseBuilder(html: String): SignatureBuilderState? {
        val trimmed = html.trim()
        val rootMatch = rootRegex.matchEntire(trimmed) ?: return null
        val styleString = rootMatch.groupValues[1]
        val inner = rootMatch.groupValues[2]

        val lines = lineRegex.findAll(inner).map { match ->
            unescapeHtml(match.groupValues[1])
        }.toList()
        if (lines.isEmpty()) return null

        val remaining = lineRegex.replace(inner, "").trim()
        if (remaining.isNotEmpty()) return null

        val styleMap = styleString.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate {
                val parts = it.split(':', limit = 2)
                val key = parts.getOrNull(0)?.trim().orEmpty().lowercase()
                val value = parts.getOrNull(1)?.trim().orEmpty()
                key to value
            }

        val fontFamily = parseFontFamily(styleMap["font-family"])
        val fontSize = styleMap["font-size"]?.removeSuffix("pt")?.trim()?.toIntOrNull() ?: 8
        val italic = styleMap["font-style"]?.equals("italic", ignoreCase = true) == true
        val bold = styleMap["font-weight"]?.equals("bold", ignoreCase = true) == true
        val lineHeight = styleMap["line-height"]?.toFloatOrNull() ?: 1.25f
        val colorHex = styleMap["color"]?.let { normalizeColorHex(it) } ?: "#000000"

        return SignatureBuilderState(
            style = SignatureStyle(
                fontFamily = fontFamily,
                fontSizePt = fontSize,
                italic = italic,
                bold = bold,
                lineHeight = lineHeight,
                colorHex = colorHex,
            ),
            lines = lines,
        )
    }

    fun validate(html: String): ValidationResult {
        val trimmed = html.trim()
        if (trimmed.isEmpty()) return ValidationResult(SignatureValidationError.MissingRoot)
        if (!trimmed.contains("<div", ignoreCase = true)) {
            return ValidationResult(SignatureValidationError.MissingRoot)
        }
        if (trimmed.length > MAX_LENGTH) {
            return ValidationResult(SignatureValidationError.TooLong)
        }
        if (trimmed.count { it == '<' } > MAX_LINE_TAGS) {
            return ValidationResult(SignatureValidationError.TooManyLines)
        }
        val forbiddenTag = FORBIDDEN_TAGS.firstOrNull {
            Regex("""<\s*$it\b""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
        }
        if (forbiddenTag != null) {
            return ValidationResult(SignatureValidationError.ForbiddenTag, forbiddenTag)
        }
        if (Regex("""on\w+\s*=""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
            return ValidationResult(SignatureValidationError.ForbiddenAttribute)
        }
        return ValidationResult()
    }

    fun normalizeColorHex(raw: String): String {
        val sanitized = raw.trim().removePrefix("#")
        val hex = sanitized.filter { it.isLetterOrDigit() }.uppercase()
        val normalized = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> "000000"
        }
        return "#${normalized}"
    }

    fun isValidHexColor(raw: String): Boolean {
        val sanitized = raw.trim().removePrefix("#")
        val hex = sanitized.trim()
        if (hex.length != 3 && hex.length != 6) return false
        return hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
    }

    private fun parseFontFamily(value: String?): SignatureFont {
        val normalized = value.orEmpty().lowercase()
        return when {
            normalized.contains("times new roman") -> SignatureFont.TimesNewRoman
            normalized.contains("calibri") -> SignatureFont.Calibri
            normalized.contains("arial") -> SignatureFont.Arial
            else -> SignatureFont.TimesNewRoman
        }
    }

    private const val MAX_LENGTH = 10_000
    private const val MAX_LINE_TAGS = 200
    private val FORBIDDEN_TAGS = listOf("script", "iframe", "object", "embed", "link", "style")
}
