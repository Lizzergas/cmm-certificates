package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.usecase.RetryCachedEmailsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.SendGeneratedEmailsUseCase
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmailSenderViewModel(
    private val emailProgressRepository: EmailProgressRepository,
    private val settingsRepository: SettingsRepository,
    connectivityMonitor: ConnectivityMonitor,
    capabilityProvider: PlatformCapabilityProvider,
    private val sendGeneratedEmails: SendGeneratedEmailsUseCase,
    private val retryCachedEmailsUseCase: RetryCachedEmailsUseCase,
) : ViewModel() {
    private val logTag = "EmailSenderVM"
    private var sendJob: Job? = null
    private val capabilities = capabilityProvider.capabilities
    private val supportsEmailSending = capabilities.canSendEmails

    val uiState: StateFlow<EmailProgressUiState> = combine(
        emailProgressRepository.state,
        emailProgressRepository.cachedEmails,
        emailProgressRepository.sentCountInLast24Hours,
        settingsRepository.state,
        connectivityMonitor.isNetworkAvailable,
    ) { progressState, cachedEmails, sentCount, settings, networkAvailable ->
        val total = progressState.total.coerceAtLeast(0)
        val current = progressState.current.coerceAtLeast(0)
        val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
        val cachedCount = cachedEmails.entries.size
        val isSmtpAuthenticated = settings.smtp.isAuthenticated
        val stopReason = progressState.stopReason

        val mode = when {
            stopReason != null -> EmailProgress.Error(stopReason)
            progressState.completed -> EmailProgress.Success(total)
            else -> EmailProgress.Running(
                current = current,
                total = total,
                progress = progress,
                currentRecipient = progressState.currentRecipient,
                isInProgress = progressState.inProgress,
            )
        }

        EmailProgressUiState(
            mode = mode,
            cachedCount = cachedCount,
            sentToday = sentCount,
            dailyLimit = settings.email.dailyLimit,
            isNetworkAvailable = networkAvailable,
            isSmtpAuthenticated = isSmtpAuthenticated,
            supportsEmailSending = capabilities.canSendEmails,
            canRetryCachedEmails = cachedCount > 0 && capabilities.canSendEmails && networkAvailable && isSmtpAuthenticated,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EmailProgressUiState(),
    )

    fun startSendingIfIdle() {
        if (sendJob?.isActive == true) {
            logWarn(logTag, "Ignored send request because a send job is already active")
            return
        }
        if (!supportsEmailSending) {
            logWarn(logTag, "Ignored send request because email sending is unsupported")
            return
        }
        val snapshot = emailProgressRepository.state.value
        if (snapshot.inProgress) {
            logWarn(logTag, "Ignored send request because repository is already in progress")
            return
        }
        if (snapshot.completed || snapshot.stopReason != null) {
            logInfo(logTag, "Clearing previous email progress state before a new send")
            emailProgressRepository.clear()
        }

        sendJob = viewModelScope.launch {
            try {
                logInfo(logTag, "Starting bulk email send")
                sendGeneratedEmails()
            } catch (e: CancellationException) {
                logWarn(logTag, "Bulk email send cancelled")
                emailProgressRepository.requestCancel()
            } catch (e: Exception) {
                logWarn(logTag, "Bulk email send failed with an unexpected error")
                emailProgressRepository.fail(EmailStopReason.GenericFailure)
            } finally {
                logInfo(logTag, "Bulk email send job finished")
                sendJob = null
            }
        }
    }

    fun retryCachedEmails() {
        if (sendJob?.isActive == true) {
            logWarn(logTag, "Ignored retry request because a send job is already active")
            return
        }
        if (!supportsEmailSending) {
            logWarn(logTag, "Ignored retry request because email sending is unsupported")
            return
        }
        logInfo(logTag, "Retrying cached emails")
        emailProgressRepository.clear()

        sendJob = viewModelScope.launch {
            try {
                retryCachedEmailsUseCase()
            } catch (e: CancellationException) {
                logWarn(logTag, "Cached email retry cancelled")
                emailProgressRepository.requestCancel()
            } catch (e: Exception) {
                logWarn(logTag, "Cached email retry failed with an unexpected error")
                emailProgressRepository.fail(EmailStopReason.GenericFailure)
            } finally {
                logInfo(logTag, "Cached email retry job finished")
                sendJob = null
            }
        }
    }

    fun cancelSending() {
        logWarn(logTag, "Cancelling email send job")
        emailProgressRepository.requestCancel()
        sendJob?.cancel()
    }
}

data class EmailProgressUiState(
    val mode: EmailProgress = EmailProgress.Running(),
    val cachedCount: Int = 0,
    val sentToday: Int = 0,
    val dailyLimit: Int = 0,
    val isNetworkAvailable: Boolean = true,
    val isSmtpAuthenticated: Boolean = false,
    val supportsEmailSending: Boolean = true,
    val canRetryCachedEmails: Boolean = false,
)

sealed interface EmailProgress {
    data class Running(
        val current: Int = 0,
        val total: Int = 0,
        val progress: Float = 0f,
        val currentRecipient: String? = null,
        val isInProgress: Boolean = false,
    ) : EmailProgress

    data class Success(val total: Int) : EmailProgress

    data class Error(val reason: EmailStopReason) : EmailProgress
}
