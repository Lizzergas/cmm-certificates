package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_preview_dialog_title
import certificates.composeapp.generated.resources.settings_history_cache_close
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AnimatedDialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.jetbrains.compose.resources.stringResource

private val PreviewDialogMinWidth = Grid.x120
private val PreviewDialogMaxWidth = Grid.x(280)
private val PreviewDialogMaxHeight = Grid.x(360)

@Composable
fun PdfPreviewDialog(
    pdfPath: String,
    onDismiss: () -> Unit,
) {
    AnimatedDialog(onDismiss = onDismiss) { requestDismiss ->
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Grid.x3,
            shadowElevation = Grid.x3,
            border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(min = PreviewDialogMinWidth, max = PreviewDialogMaxWidth),
        ) {
            Column(
                modifier = Modifier.padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.conversion_preview_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = requestDismiss) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = stringResource(Res.string.settings_history_cache_close),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Grid.x80, max = PreviewDialogMaxHeight),
                ) {
                    PlatformPdfPreviewContent(
                        pdfPath = pdfPath,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = requestDismiss) {
                        Text(stringResource(Res.string.settings_history_cache_close))
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
)
