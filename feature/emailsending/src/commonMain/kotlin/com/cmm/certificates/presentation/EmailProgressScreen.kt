package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_progress_daily_limit_status
import certificates.composeapp.generated.resources.email_progress_daily_limit_unlimited_status
import certificates.composeapp.generated.resources.email_progress_error_title
import certificates.composeapp.generated.resources.email_progress_recipient_label
import certificates.composeapp.generated.resources.email_progress_title
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
import com.cmm.certificates.core.ui.resolveEmailStopReason
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
                onRetry = viewModel::retryCachedEmails,
                onOverview = { showOverviewDialog = true },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeContentPadding()
                .padding(horizontal = Grid.x12, vertical = Grid.x8),
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
                    val infoText = buildRunningInfoText(
                        currentRecipient = mode.currentRecipient,
                        sentToday = uiState.sentToday,
                        dailyLimit = uiState.dailyLimit,
                    )
                    ProgressIndicatorContent(
                        modifier = Modifier.fillMaxSize(),
                        current = mode.current,
                        total = mode.total,
                        progress = mode.progress,
                        title = stringResource(Res.string.email_progress_title),
                        infoText = infoText,
                    )
                }
            }
        }
    }
}

@Composable
private fun buildRunningInfoText(
    currentRecipient: String?,
    sentToday: Int,
    dailyLimit: Int,
): String {
    val infoText = currentRecipient?.let {
        stringResource(Res.string.email_progress_recipient_label, it)
    }.orEmpty()
    val limitText = if (dailyLimit > 0) {
        stringResource(
            Res.string.email_progress_daily_limit_status,
            sentToday,
            dailyLimit,
        )
    } else {
        stringResource(
            Res.string.email_progress_daily_limit_unlimited_status,
            sentToday,
        )
    }
    return infoText + if (limitText.isNotBlank()) "\n$limitText" else ""
}
