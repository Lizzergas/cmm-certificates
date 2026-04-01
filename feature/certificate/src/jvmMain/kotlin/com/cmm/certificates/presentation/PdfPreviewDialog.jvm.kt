package com.cmm.certificates.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_preview_error
import certificates.composeapp.generated.resources.conversion_preview_loading
import com.cmm.certificates.core.theme.Grid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.system.measureTimeMillis

private const val PreviewLoadingMinDurationMillis = 1_000L

@Composable
internal actual fun PlatformPdfPreviewContent(
    pdfPath: String,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    val renderedPageState = produceState<PdfPreviewRenderState>(
        initialValue = PdfPreviewRenderState.Loading,
        key1 = pdfPath,
    ) {
        var resolvedState: PdfPreviewRenderState = PdfPreviewRenderState.Error
        val elapsedMillis = measureTimeMillis {
            resolvedState = runCatching {
            withContext(Dispatchers.IO) {
                Loader.loadPDF(File(pdfPath)).use { document ->
                    PDFRenderer(document)
                        .renderImageWithDPI(0, 144f)
                        .toComposeImageBitmap()
                }
            }
        }.fold(
            onSuccess = PdfPreviewRenderState::Ready,
            onFailure = { PdfPreviewRenderState.Error },
        )
        }
        val remainingLoadingMillis = PreviewLoadingMinDurationMillis - elapsedMillis
        if (remainingLoadingMillis > 0) {
            delay(remainingLoadingMillis)
        }
        value = resolvedState
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Grid.x4),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = renderedPageState.value,
            animationSpec = tween(durationMillis = 220),
            label = "pdf-preview-content",
        ) { state ->
            when (state) {
                PdfPreviewRenderState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Grid.x4),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.conversion_preview_loading))
                    }
                }

                PdfPreviewRenderState.Error -> {
                    Text(
                        text = stringResource(Res.string.conversion_preview_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is PdfPreviewRenderState.Ready -> {
                    Image(
                        bitmap = state.image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                    )
                }
            }
        }
    }
}

private sealed interface PdfPreviewRenderState {
    data object Loading : PdfPreviewRenderState
    data object Error : PdfPreviewRenderState
    data class Ready(val image: ImageBitmap) : PdfPreviewRenderState
}
