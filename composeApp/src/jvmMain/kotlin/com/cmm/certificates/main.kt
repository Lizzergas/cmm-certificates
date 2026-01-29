package com.cmm.certificates

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream

fun main() {
    FileKit.init(appId = "com.cmm.certificates")
    val version = System.getProperty("app.version") ?: "Development"
    application {
        val appIcon = remember {
            System.getProperty("app.dir")
                ?.let { Paths.get(it, "icon-512.png") }
                ?.takeIf { it.exists() }
                ?.inputStream()
                ?.buffered()
                ?.use { BitmapPainter(loadImageBitmap(it)) }
        }

        val state = rememberWindowState(
            width = 600.dp,
            height = 900.dp,
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Pažymos konverteris",
            state = state,
        ) {
            App()
        }
    }
}
