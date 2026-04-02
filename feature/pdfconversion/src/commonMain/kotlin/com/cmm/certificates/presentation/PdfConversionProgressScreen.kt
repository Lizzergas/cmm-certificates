package com.cmm.certificates.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.conversion_offline_hint
import certificates.composeapp.generated.resources.conversion_error_load_template
import certificates.composeapp.generated.resources.email_preview_button
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import certificates.composeapp.generated.resources.email_preview_description
import certificates.composeapp.generated.resources.email_preview_email_label
import certificates.composeapp.generated.resources.email_preview_send
import certificates.composeapp.generated.resources.email_preview_success
import certificates.composeapp.generated.resources.email_preview_title
import certificates.composeapp.generated.resources.email_progress_cancel
import certificates.composeapp.generated.resources.network_unavailable_message
import certificates.composeapp.generated.resources.progress_cancel
import certificates.composeapp.generated.resources.progress_convert_another
import certificates.composeapp.generated.resources.progress_current_doc_label
import certificates.composeapp.generated.resources.progress_duration_minutes_seconds
import certificates.composeapp.generated.resources.progress_duration_seconds
import certificates.composeapp.generated.resources.progress_error_title
import certificates.composeapp.generated.resources.progress_open_folder
import certificates.composeapp.generated.resources.progress_open_folder_unsupported_hint
import certificates.composeapp.generated.resources.progress_output_label
import certificates.composeapp.generated.resources.progress_send_emails
import certificates.composeapp.generated.resources.progress_send_emails_hint
import certificates.composeapp.generated.resources.progress_success_title
import certificates.composeapp.generated.resources.progress_time_label
import certificates.composeapp.generated.resources.progress_title
import com.cmm.certificates.core.openFolder
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.presentation.components.PreviewEmailDialog
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
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
                }
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .safeContentPadding()
            .padding(horizontal = Grid.x12, vertical = Grid.x8)
        AnimatedContent(
            targetState = mode,
            modifier = contentModifier,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = FADE_IN_DURATION_MS)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = FADE_OUT_DURATION_MS)) using
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(durationMillis = SIZE_ANIMATION_DURATION_MS) },
                        )
            },
        ) { mode ->
            when (mode) {
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
                        } else null,
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

@Composable
private fun ProgressBottomBar(
    isCompleted: Boolean,
    outputDir: String,
    isSendPreviewEnabled: Boolean,
    isSendEmailsEnabled: Boolean,
    isNetworkAvailable: Boolean,
    isSmtpAuthenticated: Boolean,
    supportsEmailSending: Boolean,
    canOpenGeneratedFolders: Boolean,
    isPreviewSending: Boolean,
    onSendPreview: () -> Unit,
    onSendEmails: () -> Unit,
    onCancel: () -> Unit,
    onConvertAnother: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Grid.x3,
        shadowElevation = Grid.x3,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Grid.x8, vertical = Grid.x6),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Grid.x4),
                ) {
                    OutlinedButton(
                        onClick = onSendPreview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPreviewSending && isSendPreviewEnabled,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            Stroke.thin,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.email_preview_button))
                    }
                    if (!supportsEmailSending || !isNetworkAvailable || !isSmtpAuthenticated) {
                        Text(
                            text = if (!supportsEmailSending) {
                                stringResource(Res.string.email_sending_unsupported_hint)
                            } else if (!isNetworkAvailable) {
                                stringResource(Res.string.network_unavailable_message)
                            } else {
                                stringResource(Res.string.progress_send_emails_hint)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = Grid.x6, vertical = Grid.x3),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = onSendEmails,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSendEmailsEnabled,
                    ) {
                        Text(
                            text = stringResource(Res.string.progress_send_emails),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = { openFolder(outputDir) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canOpenGeneratedFolders,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            Stroke.thin,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.progress_open_folder))
                    }
                    if (!canOpenGeneratedFolders) {
                        Text(
                            text = stringResource(Res.string.progress_open_folder_unsupported_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = Grid.x6, vertical = Grid.x3),
                            textAlign = TextAlign.Center,
                        )
                    }
                    TextButton(
                        onClick = {
                            onConvertAnother()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.progress_convert_another))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = BorderStroke(
                        Stroke.thin,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(text = stringResource(Res.string.progress_cancel))
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    modifier: Modifier,
    total: Int,
    outputDir: String,
    elapsedSeconds: Long,
    warningMessage: String?,
) {
    val successTitle = stringResource(Res.string.progress_success_title, total)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Grid.x64)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(Grid.x48)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.common_action_ok),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(Grid.x8))
        Text(
            text = successTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!warningMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Grid.x4))
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
        }
        Spacer(modifier = Modifier.height(Grid.x10))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = Grid.x1,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
                    Text(
                        text = stringResource(Res.string.progress_output_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = outputDir.ifBlank { "-" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.thin)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
                    Text(
                        text = stringResource(Res.string.progress_time_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(elapsedSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PdfSuccessContentPreview() {
    AppTheme {
        SuccessContent(
            modifier = Modifier.fillMaxSize(),
            total = 24,
            outputDir = "/tmp/certificates",
            elapsedSeconds = 12,
            warningMessage = null,
        )
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

@Composable
private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        stringResource(Res.string.progress_duration_minutes_seconds, minutes, seconds)
    } else {
        stringResource(Res.string.progress_duration_seconds, seconds)
    }
}
