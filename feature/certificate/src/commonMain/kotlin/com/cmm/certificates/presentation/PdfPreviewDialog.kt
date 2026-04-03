package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.focusable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_preview_dialog_hint
import certificates.composeapp.generated.resources.conversion_preview_dialog_title
import certificates.composeapp.generated.resources.settings_history_cache_close
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AnimatedDialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.jetbrains.compose.resources.stringResource

private val PreviewDialogMinWidth = Grid.x(160)
private val PreviewDialogMinHeight = Grid.x(220)

@Composable
fun PdfPreviewDialog(
    pdfPath: String,
    onDismiss: () -> Unit,
) {
    AnimatedDialog(
        onDismiss = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) { requestDismiss ->
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = Grid.x3,
                shadowElevation = Grid.x3,
                border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.94f)
                    .widthIn(min = PreviewDialogMinWidth)
                    .heightIn(min = PreviewDialogMinHeight)
                    .align(Alignment.Center)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val hasShortcutModifier = event.isMetaPressed || event.isCtrlPressed
                        when {
                            event.key == Key.Escape -> {
                                requestDismiss()
                                true
                            }

                            hasShortcutModifier && event.key == Key.W -> {
                                requestDismiss()
                                true
                            }

                            else -> false
                        }
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Grid.x8, end = Grid.x4, top = Grid.x6, bottom = Grid.x4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Grid.x1)) {
                            Text(
                                text = stringResource(Res.string.conversion_preview_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = pdfPath.previewFileName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = requestDismiss) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = stringResource(Res.string.settings_history_cache_close),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    ) {
                        PlatformPdfPreviewContent(
                            pdfPath = pdfPath,
                            modifier = Modifier.fillMaxSize(),
                            onRequestClose = requestDismiss,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Grid.x8, vertical = Grid.x4),
                        horizontalArrangement = Arrangement.spacedBy(Grid.x6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.conversion_preview_dialog_hint),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = requestDismiss) {
                            Text(stringResource(Res.string.settings_history_cache_close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal expect fun PlatformPdfPreviewContent(
    pdfPath: String,
    modifier: Modifier,
    onRequestClose: () -> Unit,
)

private fun String.previewFileName(): String = substringAfterLast('/').substringAfterLast('\\')
