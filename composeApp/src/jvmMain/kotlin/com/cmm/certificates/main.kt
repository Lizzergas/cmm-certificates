package com.cmm.certificates

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.app_name
import certificates.composeapp.generated.resources.cmm_logo
import com.cmm.certificates.core.initializeSentry
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun main() {
    initializeSentry()
    FileKit.init(appId = "com.cmm.certificates")
    application {
        val state = rememberWindowState(
            width = 600.dp,
            height = 900.dp,
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            state = state,
            icon = painterResource(Res.drawable.cmm_logo)
        ) {
            App()
        }
    }
}
