package com.cmm.certificates.presentation

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.email_progress_cached_retry_requirements_hint
import certificates.composeapp.generated.resources.email_progress_cached_send_button
import certificates.composeapp.generated.resources.email_progress_cancel
import certificates.composeapp.generated.resources.email_progress_daily_limit_status
import certificates.composeapp.generated.resources.email_progress_daily_limit_unlimited_status
import certificates.composeapp.generated.resources.email_progress_error_title
import certificates.composeapp.generated.resources.email_progress_finish
import certificates.composeapp.generated.resources.email_progress_overview_button
import certificates.composeapp.generated.resources.email_progress_recipient_label
import certificates.composeapp.generated.resources.email_progress_success_title
import certificates.composeapp.generated.resources.email_progress_title
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
import com.cmm.certificates.core.ui.resolveEmailStopReason
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
    var showOverviewDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (retryCached) {
            viewModel.retryCachedEmails()
        } else {
            viewModel.startSendingIfIdle()
        }
    }

    EmailSessionOverviewDialog(
        isOpen = showOverviewDialog && uiState.canShowOverview,
        sentHistory = uiState.currentSessionSentHistory,
        cachedEmails = uiState.currentSessionCachedEmails,
        cachedLastReason = uiState.currentSessionCachedLastReason,
        onDismiss = { showOverviewDialog = false },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            EmailProgressBottomBar(
                isInProgress = (uiState.mode as? EmailProgress.Running)?.isInProgress == true,
                canShowOverview = uiState.canShowOverview,
                cachedCount = uiState.cachedCount,
                canRetryCachedEmails = uiState.canRetryCachedEmails,
                supportsEmailSending = uiState.supportsEmailSending,
                onFinish = onFinish,
                onCancel = {
                    viewModel.cancelSending()
                    onCancel()
                },
                onRetry = {
                    viewModel.retryCachedEmails()
                },
                onOverview = {
                    showOverviewDialog = true
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
                        message = resolveEmailStopReason(mode.reason),
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
                    } else {
                        stringResource(
                            Res.string.email_progress_daily_limit_unlimited_status,
                            uiState.sentToday,
                        )
                    }

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
    canShowOverview: Boolean,
    cachedCount: Int,
    canRetryCachedEmails: Boolean,
    supportsEmailSending: Boolean,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOverview: () -> Unit,
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
                if (canShowOverview) {
                    OutlinedButton(
                        onClick = onOverview,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            Stroke.thin,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.email_progress_overview_button))
                    }
                }

                if (cachedCount > 0) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canRetryCachedEmails,
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
                    if (!canRetryCachedEmails) {
                        Text(
                            text = if (!supportsEmailSending) {
                                stringResource(Res.string.email_sending_unsupported_hint)
                            } else {
                                stringResource(Res.string.email_progress_cached_retry_requirements_hint)
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
    }
}

@Preview
@Composable
private fun EmailSuccessContentPreview() {
    AppTheme {
        EmailSuccessContent(modifier = Modifier.fillMaxSize(), total = 42)
    }
}

@Preview
@Composable
private fun EmailRunningContentPreview() {
    AppTheme(darkTheme = true) {
        ProgressIndicatorContent(
            modifier = Modifier.fillMaxSize(),
            current = 12,
            total = 42,
            progress = 12f / 42f,
            title = stringResource(Res.string.email_progress_title),
            infoText = stringResource(
                Res.string.email_progress_recipient_label,
                "Ada <ada@example.com>"
            ),
        )
    }
}

@Preview
@Composable
private fun EmailErrorContentPreview() {
    AppTheme(darkTheme = false) {
        ProgressErrorContent(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(Res.string.email_progress_error_title),
            message = resolveEmailStopReason(EmailStopReason.NetworkUnavailable),
        )
    }
}
