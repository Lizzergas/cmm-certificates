package com.cmm.certificates.feature.pdfconversion.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_preview_button
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
import certificates.composeapp.generated.resources.progress_error_title
import certificates.composeapp.generated.resources.progress_open_folder
import certificates.composeapp.generated.resources.progress_output_label
import certificates.composeapp.generated.resources.progress_send_emails
import certificates.composeapp.generated.resources.progress_send_emails_hint
import certificates.composeapp.generated.resources.progress_success_title
import certificates.composeapp.generated.resources.progress_time_label
import certificates.composeapp.generated.resources.progress_title
import com.cmm.certificates.core.openFolder
import com.cmm.certificates.core.ui.PreviewEmailDialog
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
                isSendEmailsEnabled = uiState.isSendEmailsEnabled,
                isSmtpAuthenticated = uiState.isSmtpAuthenticated,
                isPreviewSending = uiState.preview.isSending,
                onSendPreview = {
                    viewModel.preparePreviewDialog()
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
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
                        durationText = uiState.durationText,
                        warningMessage = if (!uiState.isNetworkAvailable) {
                            stringResource(Res.string.network_unavailable_message)
                        } else null,
                    )
                }

                ProgressMode.Error -> {
                    val resolvedMessage = if (uiState.isNetworkError) {
                        stringResource(Res.string.network_unavailable_message)
                    } else {
                        uiState.errorMessage.orEmpty()
                    }
                    ProgressErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(Res.string.progress_error_title),
                        message = resolvedMessage,
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
    isSendEmailsEnabled: Boolean,
    isSmtpAuthenticated: Boolean,
    isPreviewSending: Boolean,
    onSendPreview: () -> Unit,
    onSendEmails: () -> Unit,
    onCancel: () -> Unit,
    onConvertAnother: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSendPreview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPreviewSending,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.email_preview_button))
                    }
                    if (!isSmtpAuthenticated) {
                        Text(
                            text = stringResource(Res.string.progress_send_emails_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = onSendEmails,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSendEmailsEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8),
                        ),
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
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.progress_open_folder))
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
                        1.dp,
                        Color(0xFFE2E8F0),
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
    durationText: String,
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
                .size(128.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = successTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!warningMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warningMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.progress_time_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
