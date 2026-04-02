package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_pick_color
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
import com.cmm.certificates.core.signature.SignatureBuilderState
import com.cmm.certificates.core.signature.SignatureFont
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pipette
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignatureBuilderPanel(
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
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        builder.lines.forEachIndexed { index, line ->
            SignatureLineEditor(
                line = line,
                onValueChange = { onLineTextChange(index, it) },
                onMoveUp = { onMoveLineUp(index) },
                onMoveDown = { onMoveLineDown(index) },
                onRemove = { onRemoveLine(index) },
            )
        }

        OutlinedButton(onClick = onAddLine) {
            Text(text = stringResource(Res.string.settings_signature_builder_add_line))
        }
    }
}

@Composable
private fun SignatureLineEditor(
    line: String,
    onValueChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
        OutlinedTextField(
            value = line,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x2),
        ) {
            TextButtonAction(
                label = stringResource(Res.string.settings_signature_builder_move_up),
                onClick = onMoveUp,
            )
            TextButtonAction(
                label = stringResource(Res.string.settings_signature_builder_move_down),
                onClick = onMoveDown,
            )
            TextButtonAction(
                label = stringResource(Res.string.settings_signature_builder_remove),
                onClick = onRemove,
            )
        }
    }
}

@Composable
private fun TextButtonAction(
    label: String,
    onClick: () -> Unit,
) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text = label)
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
    var isColorPickerOpen by remember { mutableStateOf(false) }
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
                        DropdownMenuItem(
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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
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
                fontStyle = FontStyle.Italic,
            )
            ToggleChip(
                selected = style.bold,
                label = stringResource(Res.string.settings_signature_builder_bold),
                onClick = onToggleBold,
                fontWeight = FontWeight.Bold,
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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Grid.x2),
            ) {
                OutlinedTextField(
                    value = builder.colorInput,
                    onValueChange = onSetColorHex,
                    label = { Text(text = stringResource(Res.string.settings_signature_builder_color)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(onClick = { isColorPickerOpen = true }) {
                    Icon(
                        imageVector = Lucide.Pipette,
                        contentDescription = stringResource(Res.string.settings_signature_action_pick_color),
                    )
                }
            }
        }
    }

    if (isColorPickerOpen) {
        SignatureColorPickerDialog(
            initialHex = builder.colorInput,
            onDismiss = { isColorPickerOpen = false },
            onConfirm = {
                onSetColorHex(it)
                isColorPickerOpen = false
            },
        )
    }
}

@Composable
private fun ToggleChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    val background = if (selected) colors.secondaryContainer else colors.surface
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface

    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = content,
        ),
        border = BorderStroke(Stroke.thin, colors.outlineVariant),
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontWeight = fontWeight,
                fontStyle = fontStyle,
            ),
        )
    }
}
