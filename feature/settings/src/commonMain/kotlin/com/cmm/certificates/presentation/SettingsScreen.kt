package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_back
import certificates.composeapp.generated.resources.settings_clear_all_cancel
import certificates.composeapp.generated.resources.settings_clear_all_confirm
import certificates.composeapp.generated.resources.settings_clear_all_message
import certificates.composeapp.generated.resources.settings_clear_all_title
import certificates.composeapp.generated.resources.settings_subtitle
import certificates.composeapp.generated.resources.settings_title
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.rememberDirectoryPickerLauncher
import com.cmm.certificates.presentation.components.HistoryCacheDialog
import com.cmm.certificates.presentation.components.SignatureEditorDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PaddingHorizontal = Grid.x8
private val PaddingVertical = Grid.x6

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCertificateConfig: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signatureEditorState by viewModel.signatureEditorState.collectAsStateWithLifecycle()
    val launchDirectoryPicker = rememberDirectoryPickerLauncher()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSmtpDialog by remember { mutableStateOf(false) }
    val notification = state.notification
    val notificationText = notification?.message?.asString()

    LaunchedEffect(notification?.id) {
        val currentNotification = notification ?: return@LaunchedEffect
        val message = notificationText ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeNotification(currentNotification.id)
    }

    val actions = SettingsActions(
        onHostChange = viewModel::setHost,
        onPortChange = viewModel::setPort,
        onTransportChange = viewModel::setTransport,
        onUsernameChange = viewModel::setUsername,
        onPasswordChange = viewModel::setPassword,
        onSubjectChange = viewModel::setSubject,
        onBodyChange = viewModel::setBody,
        onDailyLimitChange = viewModel::setDailyLimit,
        onThemeModeChange = viewModel::setThemeMode,
        onUseInAppPdfPreviewChange = viewModel::setUseInAppPdfPreview,
        onOutputDirectoryReset = viewModel::resetOutputDirectory,
        onChooseOutputDirectory = {
            launchDirectoryPicker(state.resolvedOutputDirectory, viewModel::setOutputDirectory)
        },
        onOpenHistoryCache = { showHistoryDialog = true },
        onOpenInstallationDirectory = viewModel::openInstallationDirectory,
        onOpenLegalResourcesDirectory = viewModel::openLegalResourcesDirectory,
        onEditSignature = viewModel::openSignatureEditor,
        onAuthenticate = viewModel::authenticate,
        onOpenEmailConfiguration = { showSmtpDialog = true },
        onSendLogs = viewModel::sendLogs,
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(Res.string.settings_clear_all_title)) },
            text = { Text(stringResource(Res.string.settings_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.settings_clear_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(Res.string.settings_clear_all_cancel))
                }
            },
        )
    }

    HistoryCacheDialog(
        isOpen = showHistoryDialog,
        sentHistory = state.sentHistory,
        cachedEmails = state.cachedEmails,
        cachedLastReason = state.cachedLastReason,
        onRemoveCachedEmail = viewModel::removeCachedEmail,
        onDismiss = { showHistoryDialog = false },
    )

    if (showSmtpDialog) {
        SmtpSettingsDialog(
            state = state,
            actions = actions,
            onDismiss = { showSmtpDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SettingsTopBar(
                title = Res.string.settings_title,
                subtitle = Res.string.settings_subtitle,
                actionText = Res.string.settings_back,
                onAction = {
                    viewModel.save()
                    onBack()
                },
            )
        },
        bottomBar = {
            SettingsBottomBar(
                onClearAll = { showClearDialog = true },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsContent(
                state = state,
                actions = actions,
                onOpenCertificateConfig = onOpenCertificateConfig,
            )
        }
    }

    SignatureEditorDialog(
        state = signatureEditorState,
        onDismiss = viewModel::closeSignatureEditor,
        onSave = viewModel::saveSignatureDraft,
        onReset = viewModel::resetSignatureToDefault,
        onModeChange = viewModel::setSignatureEditorMode,
        onDraftHtmlChange = viewModel::setSignatureDraftHtml,
        onValidate = viewModel::validateSignatureDraft,
        onConvertToBuilder = viewModel::convertSignatureToBuilder,
        onSetFont = viewModel::setSignatureFont,
        onSetFontSize = viewModel::setSignatureFontSize,
        onToggleItalic = viewModel::toggleSignatureItalic,
        onToggleBold = viewModel::toggleSignatureBold,
        onSetLineHeight = viewModel::setSignatureLineHeight,
        onSetColorHex = viewModel::setSignatureColorHex,
        onAddLine = viewModel::addSignatureLine,
        onRemoveLine = viewModel::removeSignatureLine,
        onMoveLineUp = viewModel::moveSignatureLineUp,
        onMoveLineDown = viewModel::moveSignatureLineDown,
        onLineTextChange = viewModel::setSignatureLineText,
    )
}
