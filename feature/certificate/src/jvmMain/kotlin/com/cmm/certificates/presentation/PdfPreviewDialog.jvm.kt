package com.cmm.certificates.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_preview_error
import com.cmm.certificates.core.theme.Grid
import org.icepdf.ri.common.MyAnnotationCallback
import org.icepdf.ri.common.SwingController
import org.icepdf.ri.common.SwingViewBuilder
import org.icepdf.ri.common.views.DocumentViewController
import org.icepdf.ri.common.views.DocumentViewControllerImpl
import org.icepdf.ri.common.views.DocumentViewModelImpl
import org.icepdf.ri.util.FontPropertiesManager
import org.icepdf.ri.util.ViewerPropertiesManager
import org.jetbrains.compose.resources.stringResource
import java.awt.BorderLayout
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Composable
internal actual fun PlatformPdfPreviewContent(
    pdfPath: String,
    modifier: Modifier,
) {
    val viewerHandle = remember(pdfPath) {
        runCatching {
            createIcePdfViewerHandle(pdfPath)
        }.getOrNull()
    }

    DisposableEffect(viewerHandle) {
        onDispose {
            viewerHandle?.dispose()
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (viewerHandle == null) {
            Text(
                text = stringResource(Res.string.conversion_preview_error),
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            SwingPanel(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxSize(),
                factory = { viewerHandle.panel },
            )
        }
    }
}

private fun createIcePdfViewerHandle(pdfPath: String): IcePdfViewerHandle {
    configureIcePdfLogging()
    FontPropertiesManager.getInstance().loadOrReadSystemFonts()

    val controller = SwingController().apply {
        setIsEmbeddedComponent(true)
    }
    val properties = ViewerPropertiesManager.getInstance().apply {
        preferences.putFloat(ViewerPropertiesManager.PROPERTY_DEFAULT_ZOOM_LEVEL, 1.0f)
        preferences.putInt(
            ViewerPropertiesManager.PROPERTY_DEFAULT_PAGEFIT,
            DocumentViewController.PAGE_FIT_WINDOW_WIDTH,
        )
        preferences.putInt(
            ViewerPropertiesManager.PROPERTY_DEFAULT_VIEW_TYPE,
            DocumentViewControllerImpl.ONE_COLUMN_VIEW,
        )
        preferences.putInt(
            ViewerPropertiesManager.PROPERTY_DEFAULT_DISPLAY_TOOL,
            DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION,
        )
    }
    SwingViewBuilder(
        controller,
        properties,
        null,
        false,
        SwingViewBuilder.TOOL_BAR_STYLE_FIXED,
        null,
        DocumentViewControllerImpl.ONE_COLUMN_VIEW,
        DocumentViewController.PAGE_FIT_WINDOW_WIDTH,
        0f,
    )
    val panel = JPanel(BorderLayout()).apply {
        border = javax.swing.BorderFactory.createEmptyBorder(
            Grid.x2.value.toInt(),
            Grid.x2.value.toInt(),
            Grid.x2.value.toInt(),
            Grid.x2.value.toInt(),
        )
    }
    controller.documentViewController.annotationCallback =
        MyAnnotationCallback(controller.documentViewController)
    panel.add(controller.documentViewController.viewContainer, BorderLayout.CENTER)
    controller.openDocument(File(pdfPath).absolutePath)
    controller.setPageFitMode(DocumentViewController.PAGE_FIT_WINDOW_WIDTH, true)
    controller.setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION)
    controller.setUtilityPaneVisible(false)
    return IcePdfViewerHandle(controller, panel)
}

private fun configureIcePdfLogging() {
    Logger.getLogger("org.icepdf.core.pobjects.fonts.zfont.fontFiles.ZFontTrueType").level = Level.SEVERE
    Logger.getLogger("org.icepdf.core.pobjects.fonts.FontFactory").level = Level.SEVERE
    Logger.getLogger("org.icepdf.core.pobjects.fonts.FontManager").level = Level.SEVERE
}

private class IcePdfViewerHandle(
    private val controller: SwingController,
    val panel: JPanel,
) {
    fun dispose() {
        SwingUtilities.invokeLater {
            runCatching { controller.closeDocument() }
            runCatching { controller.dispose() }
        }
    }
}
