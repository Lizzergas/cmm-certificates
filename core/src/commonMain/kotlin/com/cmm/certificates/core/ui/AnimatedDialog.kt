package com.cmm.certificates.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

private const val DialogEnterDurationMillis = 120
private const val DialogExitDurationMillis = 100

@Composable
fun AnimatedDialog(
    onDismiss: () -> Unit,
    content: @Composable (requestDismiss: () -> Unit) -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    var isDismissRequested by remember { mutableStateOf(false) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isDismissRequested) {
        if (!isDismissRequested) return@LaunchedEffect
        isVisible = false
        delay(DialogExitDurationMillis.toLong())
        latestOnDismiss()
    }

    val requestDismiss: () -> Unit = {
        if (!isDismissRequested) {
            isDismissRequested = true
        }
    }

    Dialog(onDismissRequest = requestDismiss) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(DialogEnterDurationMillis)),
            exit = fadeOut(animationSpec = tween(DialogExitDurationMillis)),
        ) {
            content(requestDismiss)
        }
    }
}
