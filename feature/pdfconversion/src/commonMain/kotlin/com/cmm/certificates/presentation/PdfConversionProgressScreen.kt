package com.cmm.certificates.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_load_template
import certificates.composeapp.generated.resources.conversion_offline_hint
import certificates.composeapp.generated.resources.email_preview_description
import certificates.composeapp.generated.resources.email_preview_email_label
import certificates.composeapp.generated.resources.email_preview_send
import certificates.composeapp.generated.resources.email_preview_success
import certificates.composeapp.generated.resources.email_preview_title
import certificates.composeapp.generated.resources.email_progress_cancel
import certificates.composeapp.generated.resources.progress_current_doc_label
import certificates.composeapp.generated.resources.progress_error_title
import certificates.composeapp.generated.resources.progress_title
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
import com.cmm.certificates.presentation.components.PreviewEmailDialog
import com.cmm.certificates.presentation.components.ProgressBottomBar
import com.cmm.certificates.presentation.components.SuccessContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val FADE_IN_DURATION_MS = 420
private const val FADE_OUT_DURATION_MS = 300
private const val SIZE_ANIMATION_DURATION_MS = 360

private enum class ProgressMode { Running, Success, Error }

@Composable
fun PdfConversionProgressScreen(
    onCancel: () -> Unit,
    onConvertAnother: () -> Unit,
    onSendEmails: () -> Unit,
    viewModel: PdfConversionProgressViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPreviewDialogVisible by remember { mutableStateOf(false) }
    val mode = when {
        uiState.errorMessage != null -> ProgressMode.Error
        uiState.completed -> ProgressMode.Success
        else -> ProgressMode.Running
    }

    if (isPreviewDialogVisible) {
        PreviewEmailDialog(
            title = Res.string.email_preview_title,
            description = Res.string.email_preview_description,
            emailLabel = Res.string.email_preview_email_label,
            confirmText = Res.string.email_preview_send,
            cancelText = Res.string.email_progress_cancel,
            successText = Res.string.email_preview_success,
            email = uiState.preview.email,
            isSending = uiState.preview.isSending,
            isSuccess = uiState.preview.sent,
            errorMessage = uiState.preview.errorMessage,
            onEmailChange = viewModel::setPreviewEmail,
            onSend = { viewModel.sendPreviewEmail(attachFirstPdf = true) },
            onDismiss = {
                if (!uiState.preview.isSending) {
                    viewModel.clearPreviewStatus()
                    isPreviewDialogVisible = false
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ProgressBottomBar(
                isCompleted = uiState.completed,
                outputDir = uiState.outputDir,
                isSendPreviewEnabled = uiState.isSendPreviewEnabled,
                isSendEmailsEnabled = uiState.isSendEmailsEnabled,
                isNetworkAvailable = uiState.isNetworkAvailable,
                isSmtpAuthenticated = uiState.isSmtpAuthenticated,
                supportsEmailSending = uiState.supportsEmailSending,
                canOpenGeneratedFolders = uiState.canOpenGeneratedFolders,
                isPreviewSending = uiState.preview.isSending,
                onSendPreview = {
                    viewModel.preparePreviewDialog()
                    isPreviewDialogVisible = true
                },
                onSendEmails = onSendEmails,
                onConvertAnother = onConvertAnother,
                onCancel = {
                    viewModel.requestCancel()
                    onCancel()
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = mode,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeContentPadding()
                .padding(horizontal = Grid.x12, vertical = Grid.x8),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = FADE_IN_DURATION_MS)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = FADE_OUT_DURATION_MS)) using
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> tween(durationMillis = SIZE_ANIMATION_DURATION_MS) },
                    )
            },
        ) { currentMode ->
            when (currentMode) {
                ProgressMode.Running -> {
                    val infoText = uiState.currentDocId?.let {
                        stringResource(Res.string.progress_current_doc_label, it)
                    }
                    ProgressIndicatorContent(
                        modifier = Modifier.fillMaxSize(),
                        current = uiState.current,
                        total = uiState.total,
                        progress = uiState.progress,
                        title = stringResource(Res.string.progress_title),
                        infoText = infoText,
                    )
                }

                ProgressMode.Success -> {
                    SuccessContent(
                        modifier = Modifier.fillMaxSize(),
                        total = uiState.total,
                        outputDir = uiState.outputDir,
                        elapsedSeconds = uiState.elapsedSeconds,
                        warningMessage = if (!uiState.isNetworkAvailable) {
                            stringResource(Res.string.conversion_offline_hint)
                        } else {
                            null
                        },
                    )
                }

                ProgressMode.Error -> {
                    ProgressErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(Res.string.progress_error_title),
                        message = uiState.errorMessage?.asString().orEmpty(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PdfRunningContentPreview() {
    AppTheme(darkTheme = false) {
        ProgressIndicatorContent(
            modifier = Modifier.fillMaxSize(),
            current = 8,
            total = 24,
            progress = 8f / 24f,
            title = stringResource(Res.string.progress_title),
            infoText = stringResource(Res.string.progress_current_doc_label, 108),
        )
    }
}

@Preview
@Composable
private fun PdfErrorContentPreview() {
    AppTheme(darkTheme = true) {
        ProgressErrorContent(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(Res.string.progress_error_title),
            message = stringResource(Res.string.conversion_error_load_template),
        )
    }
}
