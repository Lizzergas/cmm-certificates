package com.cmm.certificates.feature.emailsending.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_progress_cached_send_button
import certificates.composeapp.generated.resources.email_progress_cancel
import certificates.composeapp.generated.resources.email_progress_daily_limit_status
import certificates.composeapp.generated.resources.email_progress_error_title
import certificates.composeapp.generated.resources.email_progress_finish
import certificates.composeapp.generated.resources.email_progress_recipient_label
import certificates.composeapp.generated.resources.email_progress_success_title
import certificates.composeapp.generated.resources.email_progress_title
import certificates.composeapp.generated.resources.email_stop_reason_cached_suffix
import certificates.composeapp.generated.resources.email_stop_reason_cancelled
import certificates.composeapp.generated.resources.email_stop_reason_consecutive_errors
import certificates.composeapp.generated.resources.email_stop_reason_daily_limit
import certificates.composeapp.generated.resources.email_stop_reason_doc_id_missing
import certificates.composeapp.generated.resources.email_stop_reason_generic_fail
import certificates.composeapp.generated.resources.email_stop_reason_gmail_quota
import certificates.composeapp.generated.resources.email_stop_reason_missing_emails
import certificates.composeapp.generated.resources.email_stop_reason_no_cached
import certificates.composeapp.generated.resources.email_stop_reason_no_emails
import certificates.composeapp.generated.resources.email_stop_reason_no_entries
import certificates.composeapp.generated.resources.email_stop_reason_output_dir_missing
import certificates.composeapp.generated.resources.email_stop_reason_smtp_auth
import certificates.composeapp.generated.resources.email_stop_reason_smtp_settings_missing
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun EmailProgressScreen(
    retryCached: Boolean = false,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EmailSenderViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (retryCached) {
            viewModel.retryCachedEmails()
        } else {
            viewModel.startSendingIfIdle()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            EmailProgressBottomBar(
                isInProgress = (uiState.mode as? EmailProgress.Running)?.isInProgress == true,
                cachedCount = uiState.cachedCount,
                onFinish = onFinish,
                onCancel = {
                    viewModel.cancelSending()
                    onCancel()
                },
                onRetry = {
                    viewModel.retryCachedEmails()
                }
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .safeContentPadding()
            .padding(horizontal = Grid.x12, vertical = Grid.x8)
        Box(
            modifier = contentModifier,
            contentAlignment = Alignment.Center,
        ) {
            when (val mode = uiState.mode) {
                is EmailProgress.Error -> {
                    ProgressErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(Res.string.email_progress_error_title),
                        message = resolveStopReason(mode.reason),
                    )
                }

                is EmailProgress.Success -> {
                    EmailSuccessContent(
                        modifier = Modifier.fillMaxSize(),
                        total = mode.total,
                    )
                }

                is EmailProgress.Running -> {
                    val infoText = mode.currentRecipient?.let {
                        stringResource(Res.string.email_progress_recipient_label, it)
                    }.orEmpty()
                    val limitText = if (uiState.dailyLimit > 0) {
                        stringResource(
                            Res.string.email_progress_daily_limit_status,
                            uiState.sentToday,
                            uiState.dailyLimit
                        )
                    } else ""

                    ProgressIndicatorContent(
                        modifier = Modifier.fillMaxSize(),
                        current = mode.current,
                        total = mode.total,
                        progress = mode.progress,
                        title = stringResource(Res.string.email_progress_title),
                        infoText = infoText + if (limitText.isNotBlank()) "\n$limitText" else "",
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailProgressBottomBar(
    isInProgress: Boolean,
    cachedCount: Int,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Grid.x3,
        shadowElevation = Grid.x3,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Grid.x8, vertical = Grid.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Grid.x4)
        ) {
            if (isInProgress) {
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
                    Text(text = stringResource(Res.string.email_progress_cancel))
                }
            } else {
                if (cachedCount > 0) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        )
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.email_progress_cached_send_button,
                                cachedCount
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.email_progress_finish),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailSuccessContent(
    modifier: Modifier,
    total: Int,
) {
    val successTitle = stringResource(Res.string.email_progress_success_title, total)
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
                    text = "OK",
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
    }
}

@Composable
private fun resolveStopReason(reason: EmailStopReason): String {
    return when (reason) {
        EmailStopReason.SmtpAuthRequired -> stringResource(Res.string.email_stop_reason_smtp_auth)
        EmailStopReason.DocumentIdMissing -> stringResource(Res.string.email_stop_reason_doc_id_missing)
        EmailStopReason.OutputDirMissing -> stringResource(Res.string.email_stop_reason_output_dir_missing)
        EmailStopReason.NoEntries -> stringResource(Res.string.email_stop_reason_no_entries)
        is EmailStopReason.MissingEmailAddresses -> stringResource(
            Res.string.email_stop_reason_missing_emails,
            reason.preview
        )

        EmailStopReason.NoEmailsToSend -> stringResource(Res.string.email_stop_reason_no_emails)
        EmailStopReason.SmtpSettingsMissing -> stringResource(Res.string.email_stop_reason_smtp_settings_missing)
        EmailStopReason.GmailQuotaExceeded -> stringResource(Res.string.email_stop_reason_gmail_quota)
        is EmailStopReason.ConsecutiveErrors -> stringResource(
            Res.string.email_stop_reason_consecutive_errors,
            reason.threshold
        )

        is EmailStopReason.DailyLimitReached -> stringResource(
            Res.string.email_stop_reason_daily_limit,
            reason.limit
        )

        EmailStopReason.Cancelled -> stringResource(Res.string.email_stop_reason_cancelled)
        EmailStopReason.GenericFailure -> stringResource(Res.string.email_stop_reason_generic_fail)
        EmailStopReason.NoCachedEmails -> stringResource(Res.string.email_stop_reason_no_cached)
        is EmailStopReason.Cached -> {
            val base = resolveStopReason(reason.reason)
            val suffix = stringResource(Res.string.email_stop_reason_cached_suffix, reason.count)
            "$base $suffix"
        }

        is EmailStopReason.Raw -> reason.message
    }
}
