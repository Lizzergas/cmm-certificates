package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_legal_commit
import certificates.composeapp.generated.resources.settings_legal_open_folder_hint
import certificates.composeapp.generated.resources.settings_legal_summary
import certificates.composeapp.generated.resources.settings_legal_title
import certificates.composeapp.generated.resources.settings_legal_version
import certificates.composeapp.generated.resources.settings_ownership_notice
import certificates.composeapp.generated.resources.settings_theme_dark
import certificates.composeapp.generated.resources.settings_theme_light
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.AppVerticalScrollbar
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.AppearanceSettingsState
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import org.jetbrains.compose.resources.stringResource

private val MaxWidth = Grid.x240
private val PreviewPaddingHorizontal = Grid.x8
private val PreviewPaddingVertical = Grid.x6

@Composable
internal fun BoxScope.SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpenCertificateConfig: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val themeOptions = remember {
        linkedMapOf(
            AppThemeMode.LIGHT to Res.string.settings_theme_light,
            AppThemeMode.DARK to Res.string.settings_theme_dark,
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .widthIn(max = MaxWidth),
        verticalArrangement = Arrangement.spacedBy(Grid.x8),
    ) {
        SettingsGeneralSection(
            state = state,
            actions = actions,
            themeOptions = themeOptions,
            onOpenCertificateConfig = onOpenCertificateConfig,
        )
        SettingsLegalSection(
            state = state,
            onOpenLegalResourcesDirectory = actions.onOpenLegalResourcesDirectory,
        )
    }

    AppVerticalScrollbar(
        scrollState = scrollState,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(end = Grid.x2),
    )
}

@Composable
private fun SettingsLegalSection(
    state: SettingsUiState,
    onOpenLegalResourcesDirectory: () -> Unit,
) {
    SettingsCard(
        title = Res.string.settings_legal_title,
        onClick = if (state.canOpenLegalResourcesDirectory) onOpenLegalResourcesDirectory else null,
    ) {
        Text(
            text = stringResource(Res.string.settings_legal_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.settings_ownership_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.appVersionName?.let { versionName ->
            Text(
                text = stringResource(Res.string.settings_legal_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.appCommitHash?.let { commitHash ->
            Text(
                text = stringResource(Res.string.settings_legal_commit, commitHash),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.canOpenLegalResourcesDirectory) {
            Text(
                text = stringResource(Res.string.settings_legal_open_folder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    AppTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PreviewPaddingHorizontal, vertical = PreviewPaddingVertical),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsContent(
                state = SettingsUiState(
                    smtp = SmtpSettingsState(
                        host = "smtp.example.com",
                        port = "465",
                        username = "team@example.com",
                        password = "secret",
                        isAuthenticated = true,
                    ),
                    email = EmailTemplateSettingsState(
                        subject = "Certificate",
                        body = "Hello,\n\nAttached is your certificate.",
                        signatureHtml = "<div>Best regards</div>",
                        dailyLimit = 120,
                    ),
                    certificate = CertificateSettingsState(
                        outputDirectory = "/Users/tester/pdf",
                    ),
                    appearance = AppearanceSettingsState(
                        themeMode = AppThemeMode.LIGHT,
                    ),
                    installationDirectoryPath = "/Applications/CMM Sertifikatai",
                    defaultOutputDirectory = "/Users/tester/pdf",
                    sentToday = 32,
                    supportsEmailSending = true,
                    supportsLogSubmission = true,
                ),
                actions = SettingsActions(
                    onHostChange = {},
                    onPortChange = {},
                    onTransportChange = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubjectChange = {},
                    onBodyChange = {},
                    onDailyLimitChange = {},
                    onThemeModeChange = {},
                    onUseInAppPdfPreviewChange = {},
                    onOutputDirectoryReset = {},
                    onChooseOutputDirectory = {},
                    onOpenHistoryCache = {},
                    onOpenInstallationDirectory = {},
                    onOpenLegalResourcesDirectory = {},
                    onEditSignature = {},
                    onAuthenticate = {},
                    onOpenEmailConfiguration = {},
                    onSendLogs = {},
                ),
                onOpenCertificateConfig = {},
            )
        }
    }
}
