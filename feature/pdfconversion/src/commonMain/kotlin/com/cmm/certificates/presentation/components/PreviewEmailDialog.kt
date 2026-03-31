package com.cmm.certificates.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_preview_description
import certificates.composeapp.generated.resources.email_preview_email_label
import certificates.composeapp.generated.resources.email_preview_send
import certificates.composeapp.generated.resources.email_preview_success
import certificates.composeapp.generated.resources.email_preview_title
import certificates.composeapp.generated.resources.email_progress_cancel
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val SuccessDismissDelayMillis = 1_100L
private const val DialogFadeDurationMillis = 220L
private val PreviewEmailSuccessGreen = Color(0xFF2E7D32)
private val PreviewEmailSuccessContent = Color(0xFFFFFFFF)

private enum class PreviewEmailButtonState {
    Idle,
    Sending,
    Success,
}

@Composable
fun PreviewEmailDialog(
    title: StringResource,
    description: StringResource,
    emailLabel: StringResource,
    confirmText: StringResource,
    cancelText: StringResource,
    successText: StringResource,
    email: String,
    isSending: Boolean,
    isSuccess: Boolean,
    errorMessage: UiMessage?,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isError = errorMessage != null
    var isDialogVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val buttonState = when {
        isSuccess -> PreviewEmailButtonState.Success
        isSending -> PreviewEmailButtonState.Sending
        else -> PreviewEmailButtonState.Idle
    }
    val buttonContainerColor by animateColorAsState(
        targetValue = if (isSuccess) PreviewEmailSuccessGreen else MaterialTheme.colorScheme.primary,
        label = "PreviewEmailButtonContainerColor",
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (isSuccess) PreviewEmailSuccessContent else MaterialTheme.colorScheme.onPrimary,
        label = "PreviewEmailButtonContentColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = buttonContentColor,
        label = "PreviewEmailIndicatorColor",
    )

    fun dismissWithFade() {
        if (!isDialogVisible) return
        isDialogVisible = false
        scope.launch {
            delay(DialogFadeDurationMillis)
            onDismiss()
        }
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(SuccessDismissDelayMillis)
            dismissWithFade()
        }
    }

    AnimatedVisibility(
        visible = isDialogVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        AlertDialog(
            onDismissRequest = { if (!isSending && !isSuccess) dismissWithFade() },
            confirmButton = {
                Button(
                    onClick = onSend,
                    enabled = email.isNotBlank() && !isSending && !isSuccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonContentColor,
                        disabledContainerColor = buttonContainerColor,
                        disabledContentColor = buttonContentColor,
                    ),
                ) {
                    AnimatedContent(
                        targetState = buttonState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "PreviewEmailButtonContent",
                    ) { state ->
                        when (state) {
                            PreviewEmailButtonState.Idle -> {
                                Text(text = stringResource(confirmText))
                            }

                            PreviewEmailButtonState.Sending -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = indicatorColor,
                                        strokeWidth = Grid.x1,
                                        modifier = Modifier.size(Grid.x8),
                                    )
                                    Spacer(modifier = Modifier.width(Grid.x4))
                                    Text(text = stringResource(confirmText))
                                }
                            }

                            PreviewEmailButtonState.Success -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(Grid.x8),
                                        tint = indicatorColor,
                                    )
                                    Spacer(modifier = Modifier.width(Grid.x4))
                                    Text(text = stringResource(successText))
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { dismissWithFade() },
                    enabled = !isSending && !isSuccess,
                ) {
                    Text(text = stringResource(cancelText))
                }
            },
            title = { Text(text = stringResource(title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Grid.x6)) {
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(emailLabel)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending && !isSuccess,
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(
                                    text = errorMessage.asString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            },
        )
    }
}

@Preview
@Composable
private fun PreviewEmailDialogPreviewLight() {
    AppTheme(darkTheme = false) {
        PreviewEmailDialog(
            title = Res.string.email_preview_title,
            description = Res.string.email_preview_description,
            emailLabel = Res.string.email_preview_email_label,
            confirmText = Res.string.email_preview_send,
            cancelText = Res.string.email_progress_cancel,
            successText = Res.string.email_preview_success,
            email = "preview@example.com",
            isSending = false,
            isSuccess = false,
            errorMessage = null,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PreviewEmailDialogPreviewDarkSuccess() {
    AppTheme(darkTheme = true) {
        PreviewEmailDialog(
            title = Res.string.email_preview_title,
            description = Res.string.email_preview_description,
            emailLabel = Res.string.email_preview_email_label,
            confirmText = Res.string.email_preview_send,
            cancelText = Res.string.email_progress_cancel,
            successText = Res.string.email_preview_success,
            email = "preview@example.com",
            isSending = false,
            isSuccess = true,
            errorMessage = null,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}
