package com.cmm.certificates.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.cmm.certificates.core.theme.Grid
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isError = !errorMessage.isNullOrBlank()
    val successColor = MaterialTheme.colorScheme.primary
    val indicatorColor by animateColorAsState(
        targetValue = if (isSuccess) successColor else MaterialTheme.colorScheme.onPrimary,
        label = "PreviewEmailIndicatorColor",
    )

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1_500)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onSend,
                enabled = email.isNotBlank() && !isSending && !isSuccess,
            ) {
                if (isSending || isSuccess) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = isSuccess,
                            label = "PreviewEmailIndicator"
                        ) { success ->
                            if (success) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(Grid.x8),
                                    tint = indicatorColor,
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = indicatorColor,
                                    strokeWidth = Grid.x1,
                                    modifier = Modifier.size(Grid.x8),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Grid.x4))
                        Text(
                            text = stringResource(if (isSuccess) successText else confirmText),
                            color = if (isSuccess) successColor else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Text(text = stringResource(confirmText))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
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
                    enabled = !isSending,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = errorMessage,
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
