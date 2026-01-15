package com.cmm.certificates

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init(appId = "com.cmm.certificates")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Certificates",
        ) {
            App()
        }
    }
}
