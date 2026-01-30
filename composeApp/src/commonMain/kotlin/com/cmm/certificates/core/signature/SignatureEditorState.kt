package com.cmm.certificates.core.signature

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SignatureEditorController(
    private val defaultHtml: String,
) {
    private val _state = MutableStateFlow(SignatureEditorUiState())
    val state: StateFlow<SignatureEditorUiState> = _state

    fun open(initialHtml: String) {
        val parsed = SignatureHtmlCodec.tryParseBuilder(initialHtml)
        if (parsed != null) {
            val builder = parsed.withInputs()
            val normalized = SignatureHtmlCodec.buildHtml(parsed)
            _state.value = SignatureEditorUiState(
                isOpen = true,
                mode = SignatureEditorMode.Builder,
                draftHtml = normalized,
                builder = builder,
                isBuilderCompatible = true,
                isDirty = false,
            )
        } else {
            _state.value = SignatureEditorUiState(
                isOpen = true,
                mode = SignatureEditorMode.Html,
                draftHtml = initialHtml,
                builder = SignatureBuilderState(),
                isBuilderCompatible = false,
                isDirty = false,
            )
        }
    }

    fun close() {
        _state.update { it.copy(isOpen = false, validationError = null) }
    }

    fun setMode(mode: SignatureEditorMode) {
        if (!_state.value.isBuilderCompatible && mode == SignatureEditorMode.Builder) return
        _state.update { it.copy(mode = mode) }
    }

    fun setDraftHtml(html: String) {
        val compatible = SignatureHtmlCodec.tryParseBuilder(html) != null
        _state.update {
            it.copy(
                draftHtml = html,
                validationError = null,
                isDirty = true,
                isBuilderCompatible = compatible,
            )
        }
    }

    fun convertToBuilder() {
        val parsed = SignatureHtmlCodec.tryParseBuilder(_state.value.draftHtml) ?: return
        val builder = parsed.withInputs()
        _state.update {
            it.copy(
                builder = builder,
                draftHtml = SignatureHtmlCodec.buildHtml(builder),
                mode = SignatureEditorMode.Builder,
                validationError = null,
                isBuilderCompatible = true,
                isDirty = true,
            )
        }
    }

    fun resetToDefault() {
        val parsed = SignatureHtmlCodec.tryParseBuilder(defaultHtml)
        if (parsed != null) {
            val builder = parsed.withInputs()
            _state.update {
                it.copy(
                    draftHtml = SignatureHtmlCodec.buildHtml(builder),
                    builder = builder,
                    mode = SignatureEditorMode.Builder,
                    validationError = null,
                    isBuilderCompatible = true,
                    isDirty = true,
                )
            }
        } else {
            _state.update {
                it.copy(
                    draftHtml = defaultHtml,
                    mode = SignatureEditorMode.Html,
                    validationError = null,
                    isBuilderCompatible = false,
                    isDirty = true,
                )
            }
        }
    }

    fun validateDraft(): ValidationResult {
        val result = SignatureHtmlCodec.validate(_state.value.draftHtml)
        _state.update { it.copy(validationError = result.error, validationErrorDetails = result.details) }
        return result
    }

    fun updateStyle(update: (SignatureStyle) -> SignatureStyle) {
        val next = update(_state.value.builder.style)
        updateBuilder(_state.value.builder.copy(style = next))
    }

    fun setFont(font: SignatureFont) = updateStyle { it.copy(fontFamily = font) }

    fun setFontSize(input: String) {
        val sanitized = input.filter { it.isDigit() }
        val size = sanitized.toIntOrNull()
        _state.update { current ->
            val builder = current.builder.copy(fontSizeInput = input)
            if (size == null) {
                current.copy(builder = builder)
            } else {
                current.copy(builder = builder.copy(style = builder.style.copy(fontSizePt = size.coerceIn(6, 32))))
            }
        }
        updateDraftFromBuilder()
    }

    fun toggleItalic() = updateStyle { it.copy(italic = !it.italic) }

    fun toggleBold() = updateStyle { it.copy(bold = !it.bold) }

    fun setLineHeight(input: String) {
        val sanitized = input.replace(',', '.')
        val value = sanitized.toFloatOrNull()
        _state.update { current ->
            val builder = current.builder.copy(lineHeightInput = input)
            if (value == null) {
                current.copy(builder = builder)
            } else {
                current.copy(builder = builder.copy(style = builder.style.copy(lineHeight = value.coerceIn(1.0f, 2.0f))))
            }
        }
        updateDraftFromBuilder()
    }

    fun setColorHex(input: String) {
        val normalized = SignatureHtmlCodec.normalizeColorHex(input)
        val valid = SignatureHtmlCodec.isValidHexColor(input)
        _state.update { current ->
            val builder = current.builder.copy(colorInput = input)
            if (!valid) {
                current.copy(builder = builder)
            } else {
                current.copy(builder = builder.copy(style = builder.style.copy(colorHex = normalized)))
            }
        }
        updateDraftFromBuilder()
    }

    fun addLine() {
        val lines = _state.value.builder.lines + ""
        updateBuilder(_state.value.builder.copy(lines = lines))
    }

    fun removeLine(index: Int) {
        val lines = _state.value.builder.lines
        if (lines.size <= 1 || index !in lines.indices) return
        val updated = lines.toMutableList().apply { removeAt(index) }
        updateBuilder(_state.value.builder.copy(lines = updated))
    }

    fun moveLineUp(index: Int) {
        val lines = _state.value.builder.lines
        if (index <= 0 || index !in lines.indices) return
        val updated = lines.toMutableList()
        val item = updated.removeAt(index)
        updated.add(index - 1, item)
        updateBuilder(_state.value.builder.copy(lines = updated))
    }

    fun moveLineDown(index: Int) {
        val lines = _state.value.builder.lines
        if (index !in lines.indices || index == lines.lastIndex) return
        val updated = lines.toMutableList()
        val item = updated.removeAt(index)
        updated.add(index + 1, item)
        updateBuilder(_state.value.builder.copy(lines = updated))
    }

    fun setLineText(index: Int, text: String) {
        val lines = _state.value.builder.lines
        if (index !in lines.indices) return
        val updated = lines.toMutableList()
        updated[index] = text
        updateBuilder(_state.value.builder.copy(lines = updated))
    }

    private fun updateBuilder(builder: SignatureBuilderState) {
        val html = SignatureHtmlCodec.buildHtml(builder)
        _state.update {
            it.copy(
                builder = builder,
                draftHtml = html,
                validationError = null,
                isBuilderCompatible = true,
                isDirty = true,
            )
        }
    }

    private fun updateDraftFromBuilder() {
        val builder = _state.value.builder
        _state.update {
            it.copy(
                builder = builder,
                draftHtml = SignatureHtmlCodec.buildHtml(builder),
                validationError = null,
                isBuilderCompatible = true,
                isDirty = true,
            )
        }
    }
}

data class SignatureEditorUiState(
    val isOpen: Boolean = false,
    val mode: SignatureEditorMode = SignatureEditorMode.Builder,
    val draftHtml: String = "",
    val builder: SignatureBuilderState = SignatureBuilderState(),
    val validationError: SignatureValidationError? = null,
    val validationErrorDetails: String? = null,
    val isDirty: Boolean = false,
    val isBuilderCompatible: Boolean = true,
)

enum class SignatureEditorMode {
    Builder,
    Html,
    Preview,
}

data class SignatureBuilderState(
    val style: SignatureStyle = SignatureStyle(),
    val lines: List<String> = listOf("Pagarbiai"),
    val fontSizeInput: String = "8",
    val lineHeightInput: String = "1.25",
    val colorInput: String = "#000000",
)

data class SignatureStyle(
    val fontFamily: SignatureFont = SignatureFont.TimesNewRoman,
    val fontSizePt: Int = 8,
    val italic: Boolean = true,
    val bold: Boolean = false,
    val lineHeight: Float = 1.25f,
    val colorHex: String = "#000000",
)

enum class SignatureFont(val cssValue: String, val displayName: String) {
    TimesNewRoman("'Times New Roman', Times, serif", "Times New Roman"),
    Arial("Arial, Helvetica, sans-serif", "Arial"),
    Calibri("Calibri, Arial, sans-serif", "Calibri"),
}

enum class SignatureValidationError {
    MissingRoot,
    ForbiddenTag,
    ForbiddenAttribute,
    TooLong,
    TooManyLines,
    InvalidHtml,
}

data class ValidationResult(
    val error: SignatureValidationError? = null,
    val details: String? = null,
) {
    val isValid: Boolean
        get() = error == null
}

data class SignatureSummary(
    val isCustomHtml: Boolean,
    val lineCount: Int = 0,
    val font: SignatureFont? = null,
    val fontSizePt: Int? = null,
    val italic: Boolean = false,
    val bold: Boolean = false,
)

object SignatureSummaryParser {
    fun fromHtml(html: String): SignatureSummary {
        val parsed = SignatureHtmlCodec.tryParseBuilder(html)
        return if (parsed == null) {
            SignatureSummary(isCustomHtml = true)
        } else {
            SignatureSummary(
                isCustomHtml = false,
                lineCount = parsed.lines.size,
                font = parsed.style.fontFamily,
                fontSizePt = parsed.style.fontSizePt,
                italic = parsed.style.italic,
                bold = parsed.style.bold,
            )
        }
    }
}

private fun SignatureBuilderState.withInputs(): SignatureBuilderState {
    return copy(
        fontSizeInput = style.fontSizePt.toString(),
        lineHeightInput = style.lineHeight.toString(),
        colorInput = style.colorHex,
    )
}
