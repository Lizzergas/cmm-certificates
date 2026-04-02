package com.cmm.certificates.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_cancel
import certificates.composeapp.generated.resources.settings_signature_action_reset
import certificates.composeapp.generated.resources.settings_signature_action_save
import certificates.composeapp.generated.resources.settings_signature_dialog_title
import com.cmm.certificates.core.signature.SignatureEditorMode
import com.cmm.certificates.core.signature.SignatureEditorUiState
import com.cmm.certificates.core.signature.SignatureFont
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.AnimatedDialog
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

    AnimatedDialog(onDismiss = onDismiss) { requestDismiss ->
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

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
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

                SignatureEditorFooter(
                    onDismiss = requestDismiss,
                    onReset = onReset,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun SignatureEditorFooter(
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
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
