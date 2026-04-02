package com.cmm.certificates.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_output_dir_label
import certificates.composeapp.generated.resources.email_progress_daily_limit_status
import certificates.composeapp.generated.resources.email_progress_daily_limit_unlimited_status
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import certificates.composeapp.generated.resources.network_unavailable_message
import certificates.composeapp.generated.resources.settings_body_label
import certificates.composeapp.generated.resources.settings_body_placeholder_hint
import certificates.composeapp.generated.resources.settings_certificate_config_button
import certificates.composeapp.generated.resources.settings_daily_limit_label
import certificates.composeapp.generated.resources.settings_email_configuration_button
import certificates.composeapp.generated.resources.settings_history_cache_button
import certificates.composeapp.generated.resources.settings_open_installation_directory
import certificates.composeapp.generated.resources.settings_output_directory_default_fallback_hint
import certificates.composeapp.generated.resources.settings_output_directory_default_install_hint
import certificates.composeapp.generated.resources.settings_output_directory_invalid_hint
import certificates.composeapp.generated.resources.settings_pdf_preview_hint
import certificates.composeapp.generated.resources.settings_pdf_preview_toggle
import certificates.composeapp.generated.resources.settings_section_title
import certificates.composeapp.generated.resources.settings_send_logs
import certificates.composeapp.generated.resources.settings_send_logs_unsupported_hint
import certificates.composeapp.generated.resources.settings_signature_action_edit
import certificates.composeapp.generated.resources.settings_subject_label
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsGeneralSection(
    state: SettingsUiState,
    actions: SettingsActions,
    themeOptions: Map<AppThemeMode, StringResource>,
    onOpenCertificateConfig: () -> Unit,
) {
    SettingsCard(title = Res.string.settings_section_title) {
        SettingsPrimaryActions(
            supportsEmailSending = state.supportsEmailSending,
            onOpenEmailConfiguration = actions.onOpenEmailConfiguration,
            onOpenCertificateConfig = onOpenCertificateConfig,
        )
        SettingsEmailTemplateFields(
            state = state,
            actions = actions,
        )
        ThemeModePicker(
            selected = state.appearance.themeMode,
            options = themeOptions,
            onSelect = actions.onThemeModeChange,
        )
        PreviewPreferenceField(
            useInAppPreview = state.appearance.useInAppPdfPreview,
            onCheckedChange = actions.onUseInAppPdfPreviewChange,
        )
        OutputDirectorySettingsField(
            outputDirectory = state.resolvedOutputDirectory,
            hasCustomOutputDirectory = state.hasCustomOutputDirectory,
            isWritable = state.isOutputDirectoryWritable,
            usesInstallationDefault = state.outputDirectoryUsesInstallationDefault,
            onChoose = actions.onChooseOutputDirectory,
        )
        SettingsUtilityActions(
            state = state,
            actions = actions,
        )
        SettingsLogSubmissionStatus(state = state)
    }
}

@Composable
private fun SettingsPrimaryActions(
    supportsEmailSending: Boolean,
    onOpenEmailConfiguration: () -> Unit,
    onOpenCertificateConfig: () -> Unit,
) {
    OutlinedButton(
        onClick = onOpenEmailConfiguration,
        modifier = Modifier.fillMaxWidth(),
        enabled = supportsEmailSending,
    ) {
        Text(stringResource(Res.string.settings_email_configuration_button))
    }
    OutlinedButton(
        onClick = onOpenCertificateConfig,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.settings_certificate_config_button))
    }
    if (!supportsEmailSending) {
        Text(
            text = stringResource(Res.string.email_sending_unsupported_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.small,
                )
                .padding(horizontal = Grid.x6, vertical = Grid.x3),
        )
    }
}

@Composable
private fun SettingsEmailTemplateFields(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SettingsField(
        label = Res.string.settings_subject_label,
        value = state.email.subject,
        onValueChange = actions.onSubjectChange,
        singleLine = true,
    )
    SettingsField(
        label = Res.string.settings_body_label,
        value = state.email.body,
        onValueChange = actions.onBodyChange,
        minLines = 5,
        maxLines = 10,
        showClearIcon = false,
        supportingText = {
            Text(stringResource(Res.string.settings_body_placeholder_hint))
        },
    )
    SettingsField(
        label = Res.string.settings_daily_limit_label,
        value = state.email.dailyLimit.toString(),
        onValueChange = actions.onDailyLimitChange,
        singleLine = true,
        keyboardType = KeyboardType.Number,
        supportingText = {
            val statusText = if (state.email.dailyLimit > 0) {
                stringResource(
                    Res.string.email_progress_daily_limit_status,
                    state.sentToday,
                    state.email.dailyLimit,
                )
            } else {
                stringResource(
                    Res.string.email_progress_daily_limit_unlimited_status,
                    state.sentToday,
                )
            }
            Text(statusText)
        },
    )
}

@Composable
private fun SettingsUtilityActions(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    OutlinedButton(
        onClick = actions.onOpenHistoryCache,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.settings_history_cache_button))
    }
    if (state.canOpenInstallationDirectory) {
        OutlinedButton(
            onClick = actions.onOpenInstallationDirectory,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.settings_open_installation_directory))
        }
    }
    OutlinedButton(
        onClick = actions.onEditSignature,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.settings_signature_action_edit))
    }
    OutlinedButton(
        onClick = actions.onSendLogs,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canSendLogs,
    ) {
        Text(stringResource(Res.string.settings_send_logs))
    }
}

@Composable
private fun SettingsLogSubmissionStatus(state: SettingsUiState) {
    when {
        !state.supportsLogSubmission -> {
            Text(
                text = stringResource(Res.string.settings_send_logs_unsupported_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        !state.isNetworkAvailable -> {
            Text(
                text = stringResource(Res.string.network_unavailable_message),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.logSubmissionMessage != null -> {
            Text(
                text = state.logSubmissionMessage.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isLogSubmissionSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
private fun OutputDirectorySettingsField(
    outputDirectory: String,
    hasCustomOutputDirectory: Boolean,
    isWritable: Boolean,
    usesInstallationDefault: Boolean,
    onChoose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Grid.x2),
    ) {
        Text(
            text = stringResource(Res.string.conversion_output_dir_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = outputDirectory,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChoose)
                .padding(vertical = Grid.x3),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWritable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textDecoration = TextDecoration.Underline,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Grid.x1)) {
            if (!isWritable) {
                Text(
                    text = stringResource(Res.string.settings_output_directory_invalid_hint),
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (hasCustomOutputDirectory) {
                Unit
            } else if (usesInstallationDefault) {
                Text(stringResource(Res.string.settings_output_directory_default_install_hint))
            } else {
                Text(stringResource(Res.string.settings_output_directory_default_fallback_hint))
            }
        }
    }
}

@Composable
private fun PreviewPreferenceField(
    useInAppPreview: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!useInAppPreview) }
            .padding(vertical = Grid.x1),
        horizontalArrangement = Arrangement.spacedBy(Grid.x4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = useInAppPreview,
            onCheckedChange = onCheckedChange,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Grid.x1),
        ) {
            Text(
                text = stringResource(Res.string.settings_pdf_preview_toggle),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.settings_pdf_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
