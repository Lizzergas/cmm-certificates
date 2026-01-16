package com.cmm.certificates

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init(appId = "com.cmm.certificates")
    application {
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
