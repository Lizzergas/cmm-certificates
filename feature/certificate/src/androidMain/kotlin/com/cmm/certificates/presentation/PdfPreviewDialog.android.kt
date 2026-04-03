package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_preview_unavailable_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun PlatformPdfPreviewContent(
    pdfPath: String,
    modifier: Modifier,
    onRequestClose: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.conversion_preview_unavailable_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
