package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_authenticate
import certificates.composeapp.generated.resources.settings_authenticated
import certificates.composeapp.generated.resources.settings_email_configuration_title
import certificates.composeapp.generated.resources.settings_history_cache_close
import certificates.composeapp.generated.resources.settings_password_label
import certificates.composeapp.generated.resources.settings_port_label
import certificates.composeapp.generated.resources.settings_server_label
import certificates.composeapp.generated.resources.settings_transport_smtp
import certificates.composeapp.generated.resources.settings_transport_smtps
import certificates.composeapp.generated.resources.settings_transport_tls
import certificates.composeapp.generated.resources.settings_username_label
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SmtpSettingsDialog(
    state: SettingsUiState,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val transportOptions = remember {
        linkedMapOf(
            SmtpTransport.SMTP to Res.string.settings_transport_smtp,
            SmtpTransport.SMTPS to Res.string.settings_transport_smtps,
            SmtpTransport.SMTP_TLS to Res.string.settings_transport_tls,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_email_configuration_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Grid.x5),
            ) {
                StatusLines(
                    error = state.smtp.errorMessage,
                    authenticated = state.smtp.isAuthenticated,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Grid.x5),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsField(
                        label = Res.string.settings_server_label,
                        value = state.smtp.host,
                        onValueChange = actions.onHostChange,
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    SettingsField(
                        label = Res.string.settings_port_label,
                        value = state.smtp.port,
                        onValueChange = actions.onPortChange,
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                SmtpTransportPicker(
                    selected = state.smtp.transport,
                    options = transportOptions,
                    onSelect = actions.onTransportChange,
                )
                SettingsField(
                    label = Res.string.settings_username_label,
                    value = state.smtp.username,
                    onValueChange = actions.onUsernameChange,
                    singleLine = true,
                )
                SettingsField(
                    label = Res.string.settings_password_label,
                    value = state.smtp.password,
                    onValueChange = actions.onPasswordChange,
                    singleLine = true,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = actions.onAuthenticate,
                enabled = state.supportsEmailSending && state.smtp.canAuthenticate && !state.smtp.isAuthenticating,
            ) {
                Text(stringResource(Res.string.settings_authenticate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_history_cache_close))
            }
        },
    )
}

@Composable
private fun StatusLines(
    error: UiMessage?,
    authenticated: Boolean,
) {
    if (error != null) {
        Text(
            text = error.asString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (authenticated) {
        Text(
            text = stringResource(Res.string.settings_authenticated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
