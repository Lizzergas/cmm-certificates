package com.cmm.certificates.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_cancel
import certificates.composeapp.generated.resources.settings_signature_action_reset
import certificates.composeapp.generated.resources.settings_signature_action_save
import certificates.composeapp.generated.resources.settings_signature_action_validate
import certificates.composeapp.generated.resources.settings_signature_builder_add_line
import certificates.composeapp.generated.resources.settings_signature_builder_bold
import certificates.composeapp.generated.resources.settings_signature_builder_color
import certificates.composeapp.generated.resources.settings_signature_builder_font
import certificates.composeapp.generated.resources.settings_signature_builder_italic
import certificates.composeapp.generated.resources.settings_signature_builder_line_height
import certificates.composeapp.generated.resources.settings_signature_builder_lines
import certificates.composeapp.generated.resources.settings_signature_builder_move_down
import certificates.composeapp.generated.resources.settings_signature_builder_move_up
import certificates.composeapp.generated.resources.settings_signature_builder_remove
import certificates.composeapp.generated.resources.settings_signature_builder_size
import certificates.composeapp.generated.resources.settings_signature_custom_html_notice
import certificates.composeapp.generated.resources.settings_signature_dialog_title
import certificates.composeapp.generated.resources.settings_signature_error_forbidden_attr
import certificates.composeapp.generated.resources.settings_signature_error_forbidden_tag
import certificates.composeapp.generated.resources.settings_signature_error_invalid_html
import certificates.composeapp.generated.resources.settings_signature_error_missing_root
import certificates.composeapp.generated.resources.settings_signature_error_too_long
import certificates.composeapp.generated.resources.settings_signature_error_too_many_lines
import certificates.composeapp.generated.resources.settings_signature_html_convert
import certificates.composeapp.generated.resources.settings_signature_html_label
import certificates.composeapp.generated.resources.settings_signature_preview_unavailable
import certificates.composeapp.generated.resources.settings_signature_tab_builder
import certificates.composeapp.generated.resources.settings_signature_tab_html
import certificates.composeapp.generated.resources.settings_signature_tab_preview
import com.cmm.certificates.core.signature.SignatureBuilderState
import com.cmm.certificates.core.signature.SignatureEditorMode
import com.cmm.certificates.core.signature.SignatureEditorUiState
import com.cmm.certificates.core.signature.SignatureFont
import com.cmm.certificates.core.signature.SignatureValidationError
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignatureEditorDialog(
    state: SignatureEditorUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (SignatureEditorMode) -> Unit,
    onDraftHtmlChange: (String) -> Unit,
    onValidate: () -> Unit,
    onConvertToBuilder: () -> Unit,
    onSetFont: (SignatureFont) -> Unit,
    onSetFontSize: (String) -> Unit,
    onToggleItalic: () -> Unit,
    onToggleBold: () -> Unit,
    onSetLineHeight: (String) -> Unit,
    onSetColorHex: (String) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onMoveLineUp: (Int) -> Unit,
    onMoveLineDown: (Int) -> Unit,
    onLineTextChange: (Int, String) -> Unit,
) {
    if (!state.isOpen) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Grid.x3,
            shadowElevation = Grid.x3,
            modifier = Modifier.widthIn(min = Grid.x240, max = Grid.x240),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                Text(
                    text = stringResource(Res.string.settings_signature_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SignatureEditorTabs(
                    mode = state.mode,
                    isBuilderEnabled = state.isBuilderCompatible,
                    onModeChange = onModeChange,
                )

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(Grid.x6),
                ) {
                    when (state.mode) {
                        SignatureEditorMode.Builder -> {
                            SignatureBuilderPanel(
                                builder = state.builder,
                                onSetFont = onSetFont,
                                onSetFontSize = onSetFontSize,
                                onToggleItalic = onToggleItalic,
                                onToggleBold = onToggleBold,
                                onSetLineHeight = onSetLineHeight,
                                onSetColorHex = onSetColorHex,
                                onAddLine = onAddLine,
                                onRemoveLine = onRemoveLine,
                                onMoveLineUp = onMoveLineUp,
                                onMoveLineDown = onMoveLineDown,
                                onLineTextChange = onLineTextChange,
                            )
                        }

                        SignatureEditorMode.Html -> {
                            SignatureHtmlPanel(
                                draftHtml = state.draftHtml,
                                isBuilderCompatible = state.isBuilderCompatible,
                                validationError = state.validationError,
                                validationMessage = state.validationError?.let {
                                    validationMessage(it, state.validationErrorDetails)
                                },
                                onDraftHtmlChange = onDraftHtmlChange,
                                onValidate = onValidate,
                                onConvertToBuilder = onConvertToBuilder,
                            )
                        }

                        SignatureEditorMode.Preview -> {
                            SignaturePreviewPanel(
                                builder = state.builder,
                                isBuilderCompatible = state.isBuilderCompatible,
                            )
                        }
                    }
                }

                state.validationError?.let { error ->
                    Text(
                        text = validationMessage(error, state.validationErrorDetails),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.settings_signature_action_cancel))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
                        OutlinedButton(onClick = onReset) {
                            Text(text = stringResource(Res.string.settings_signature_action_reset))
                        }
                        Button(onClick = onSave) {
                            Text(text = stringResource(Res.string.settings_signature_action_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureEditorTabs(
    mode: SignatureEditorMode,
    isBuilderEnabled: Boolean,
    onModeChange: (SignatureEditorMode) -> Unit,
) {
    val tabs = listOf(
        SignatureEditorMode.Builder to Res.string.settings_signature_tab_builder,
        SignatureEditorMode.Html to Res.string.settings_signature_tab_html,
        SignatureEditorMode.Preview to Res.string.settings_signature_tab_preview,
    )
    val selectedIndex = tabs.indexOfFirst { it.first == mode }.coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, entry ->
            val enabled = entry.first != SignatureEditorMode.Builder || isBuilderEnabled
            Tab(
                selected = index == selectedIndex,
                onClick = { if (enabled) onModeChange(entry.first) },
                enabled = enabled,
                text = { Text(text = stringResource(entry.second)) },
            )
        }
    }
}

@Composable
private fun SignatureBuilderPanel(
    builder: SignatureBuilderState,
    onSetFont: (SignatureFont) -> Unit,
    onSetFontSize: (String) -> Unit,
    onToggleItalic: () -> Unit,
    onToggleBold: () -> Unit,
    onSetLineHeight: (String) -> Unit,
    onSetColorHex: (String) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onMoveLineUp: (Int) -> Unit,
    onMoveLineDown: (Int) -> Unit,
    onLineTextChange: (Int, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x6)) {
        SignatureStylePanel(
            builder = builder,
            onSetFont = onSetFont,
            onSetFontSize = onSetFontSize,
            onToggleItalic = onToggleItalic,
            onToggleBold = onToggleBold,
            onSetLineHeight = onSetLineHeight,
            onSetColorHex = onSetColorHex,
        )

        Text(
            text = stringResource(Res.string.settings_signature_builder_lines),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        builder.lines.forEachIndexed { index, line ->
            Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
                OutlinedTextField(
                    value = line,
                    onValueChange = { onLineTextChange(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Grid.x2),
                ) {
                    TextButton(onClick = { onMoveLineUp(index) }) {
                        Text(text = stringResource(Res.string.settings_signature_builder_move_up))
                    }
                    TextButton(onClick = { onMoveLineDown(index) }) {
                        Text(text = stringResource(Res.string.settings_signature_builder_move_down))
                    }
                    TextButton(onClick = { onRemoveLine(index) }) {
                        Text(text = stringResource(Res.string.settings_signature_builder_remove))
                    }
                }
            }
        }

        OutlinedButton(onClick = onAddLine) {
            Text(text = stringResource(Res.string.settings_signature_builder_add_line))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignatureStylePanel(
    builder: SignatureBuilderState,
    onSetFont: (SignatureFont) -> Unit,
    onSetFontSize: (String) -> Unit,
    onToggleItalic: () -> Unit,
    onToggleBold: () -> Unit,
    onSetLineHeight: (String) -> Unit,
    onSetColorHex: (String) -> Unit,
) {
    var fontExpanded by remember { mutableStateOf(false) }

    val style = builder.style
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            ExposedDropdownMenuBox(
                expanded = fontExpanded,
                onExpandedChange = { fontExpanded = !fontExpanded },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = style.fontFamily.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = stringResource(Res.string.settings_signature_builder_font)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = fontExpanded,
                    onDismissRequest = { fontExpanded = false },
                ) {
                    SignatureFont.entries.forEach { font ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(font.displayName) },
                            onClick = {
                                onSetFont(font)
                                fontExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = builder.fontSizeInput,
                onValueChange = onSetFontSize,
                label = { Text(text = stringResource(Res.string.settings_signature_builder_size)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            ToggleChip(
                selected = style.italic,
                label = stringResource(Res.string.settings_signature_builder_italic),
                onClick = onToggleItalic,
            )
            ToggleChip(
                selected = style.bold,
                label = stringResource(Res.string.settings_signature_builder_bold),
                onClick = onToggleBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            OutlinedTextField(
                value = builder.lineHeightInput,
                onValueChange = onSetLineHeight,
                label = { Text(text = stringResource(Res.string.settings_signature_builder_line_height)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = builder.colorInput,
                onValueChange = onSetColorHex,
                label = { Text(text = stringResource(Res.string.settings_signature_builder_color)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ToggleChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = if (selected) colors.secondaryContainer else colors.surface
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface

    OutlinedButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = content,
        ),
        border = BorderStroke(Stroke.thin, colors.outlineVariant),
    ) {
        Text(
            text = label
        )
    }
}

@Composable
private fun SignatureHtmlPanel(
    draftHtml: String,
    isBuilderCompatible: Boolean,
    validationError: SignatureValidationError?,
    validationMessage: String?,
    onDraftHtmlChange: (String) -> Unit,
    onValidate: () -> Unit,
    onConvertToBuilder: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x4)) {
        if (!isBuilderCompatible) {
            Text(
                text = stringResource(Res.string.settings_signature_custom_html_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = draftHtml,
            onValueChange = onDraftHtmlChange,
            label = { Text(text = stringResource(Res.string.settings_signature_html_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            isError = validationError != null,
            supportingText = {
                if (validationError != null && !validationMessage.isNullOrBlank()) {
                    Text(
                        text = validationMessage,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
            OutlinedButton(onClick = onValidate) {
                Text(text = stringResource(Res.string.settings_signature_action_validate))
            }
            if (isBuilderCompatible) {
                TextButton(onClick = onConvertToBuilder) {
                    Text(text = stringResource(Res.string.settings_signature_html_convert))
                }
            }
        }
    }
}

@Composable
private fun SignaturePreviewPanel(
    builder: SignatureBuilderState,
    isBuilderCompatible: Boolean,
) {
    val previewAvailable = isBuilderCompatible
    if (!previewAvailable) {
        Text(
            text = stringResource(Res.string.settings_signature_preview_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SignatureBuilderPreview(builder = builder)
}

@Composable
private fun SignatureBuilderPreview(builder: SignatureBuilderState) {
    val style = builder.style
    val colors = MaterialTheme.colorScheme
    val fontFamily = when (style.fontFamily) {
        SignatureFont.TimesNewRoman -> FontFamily.Serif
        SignatureFont.Arial, SignatureFont.Calibri -> FontFamily.SansSerif
    }
    val fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal
    val fontSize = style.fontSizePt.sp
    val lineHeight = (style.fontSizePt * style.lineHeight).sp
    val textColor = parseHexColor(style.colorHex, colors.onSurface)

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, colors.outlineVariant),
        color = colors.surface,
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Grid.x6),
            verticalArrangement = Arrangement.spacedBy(Grid.x2),
        ) {
            builder.lines.forEach { line ->
                Text(
                    text = line,
                    color = textColor,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                )
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    val sanitized = hex.removePrefix("#").trim()
    val value = when (sanitized.length) {
        3 -> sanitized.map { "$it$it" }.joinToString("")
        6 -> sanitized
        else -> return fallback
    }
    return runCatching { Color(0xFF000000 or value.toLong(16)) }.getOrDefault(fallback)
}

@Composable
private fun validationMessage(error: SignatureValidationError, detail: String?): String {
    return when (error) {
        SignatureValidationError.MissingRoot -> stringResource(Res.string.settings_signature_error_missing_root)
        SignatureValidationError.ForbiddenTag -> stringResource(
            Res.string.settings_signature_error_forbidden_tag,
            detail.orEmpty(),
        )

        SignatureValidationError.ForbiddenAttribute -> stringResource(Res.string.settings_signature_error_forbidden_attr)
        SignatureValidationError.TooLong -> stringResource(Res.string.settings_signature_error_too_long)
        SignatureValidationError.TooManyLines -> stringResource(Res.string.settings_signature_error_too_many_lines)
        SignatureValidationError.InvalidHtml -> stringResource(Res.string.settings_signature_error_invalid_html)
    }
}
