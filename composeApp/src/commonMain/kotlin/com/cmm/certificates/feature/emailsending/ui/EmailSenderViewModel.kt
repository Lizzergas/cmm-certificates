package com.cmm.certificates.feature.emailsending.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.email.buildEmailHtmlBody
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.feature.emailsending.data.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailSenderViewModel(
    private val emailProgressRepository: EmailProgressRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private var sendJob: Job? = null
    val uiState: StateFlow<EmailProgressUiState> =
        combine(
            emailProgressRepository.state,
            emailProgressRepository.cachedEmails,
            emailProgressRepository.sentCountInLast24Hours,
            settingsRepository.state
        ) { progressState, cachedEmails, sentCount, settings ->
            val total = progressState.total.coerceAtLeast(0)
            val current = progressState.current.coerceAtLeast(0)
            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            val mode = when {
                progressState.stopReason != null -> EmailProgress.Error(
                    reason = progressState.stopReason
                )

                progressState.completed -> EmailProgress.Success(total = total)
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
                cachedCount = cachedEmails?.requests?.size ?: 0,
                sentToday = sentCount,
                dailyLimit = settings.email.dailyLimit
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EmailProgressUiState(),
        )

    fun startSendingIfIdle() {
        if (sendJob?.isActive == true) return
        val snapshot = emailProgressRepository.state.value
        if (snapshot.inProgress) return
        if (snapshot.completed || snapshot.stopReason != null) {
            emailProgressRepository.clear()
        }
        sendJob = viewModelScope.launch {
            try {
                sendEmails()
            } finally {
                sendJob = null
            }
        }
    }

    fun retryCachedEmails() {
        if (sendJob?.isActive == true) return
        emailProgressRepository.clear()
        sendJob = viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    emailProgressRepository.cachedEmails.firstOrNull()
                }
                if (cached != null && cached.requests.isNotEmpty()) {
                    sendEmailsInternal(cached.requests)
                } else {
                    emailProgressRepository.fail(EmailStopReason.NoCachedEmails)
                }
            } finally {
                sendJob = null
            }
        }
    }

    fun cancelSending() {
        emailProgressRepository.requestCancel()
        sendJob?.cancel()
    }

    private suspend fun sendEmails() {
        val settingsState = settingsRepository.state.value
        val smtpState = settingsState.smtp
        val settings = smtpState.toSmtpSettings()
        if (settings == null || !smtpState.isAuthenticated) {
            emailProgressRepository.fail(EmailStopReason.SmtpAuthRequired)
            return
        }

        val conversionState = pdfConversionProgressRepository.state.value
        val docIdStart = conversionState.docIdStart
        if (docIdStart == null) {
            emailProgressRepository.fail(EmailStopReason.DocumentIdMissing)
            return
        }
        if (conversionState.outputDir.isBlank()) {
            emailProgressRepository.fail(EmailStopReason.OutputDirMissing)
            return
        }
        if (conversionState.entries.isEmpty()) {
            emailProgressRepository.fail(EmailStopReason.NoEntries)
            return
        }

        try {
            val subject = settingsState.email.subject
            val body = settingsState.email.body
            val htmlBody = buildEmailHtmlBody(
                body = body,
                signatureHtml = settingsState.email.signatureHtml,
            )
            val missingEmailIndexes = conversionState.entries.mapIndexedNotNull { index, entry ->
                if (entry.primaryEmail.isBlank()) index + 1 else null
            }
            if (missingEmailIndexes.isNotEmpty()) {
                val preview = missingEmailIndexes.take(3).joinToString(", ")
                val suffix = if (missingEmailIndexes.size > 3) "..." else ""
                emailProgressRepository.fail(EmailStopReason.MissingEmailAddresses("$preview$suffix"))
                return
            }
            val requests = buildRequests(
                conversionState.entries,
                docIdStart,
                conversionState.outputDir,
                subject,
                body,
                htmlBody,
            )
            if (requests.isEmpty()) {
                emailProgressRepository.fail(EmailStopReason.NoEmailsToSend)
                return
            }

            sendEmailsInternal(requests)
        } catch (e: CancellationException) {
            emailProgressRepository.requestCancel()
        } catch (e: Exception) {
            emailProgressRepository.fail(EmailStopReason.Raw(e.message ?: "Failed to send emails."))
        }
    }

    private suspend fun sendEmailsInternal(requests: List<EmailSendRequest>) {
        val settingsState = settingsRepository.state.value
        val settings = settingsState.smtp.toSmtpSettings()
        if (settings == null) {
            emailProgressRepository.fail(EmailStopReason.SmtpSettingsMissing)
            return
        }

        val dailyLimit = settingsState.email.dailyLimit
        var sentInWindow = emailProgressRepository.getSentCountInLast24Hours()

        var consecutiveErrors = 0
        val errorThreshold = 3
        var shouldStopDueToErrors = false
        var stopReason: EmailStopReason? = null
        val sentIndices = mutableSetOf<Int>()

        emailProgressRepository.start(requests.size)
        val parentContext = currentCoroutineContext()

        try {
            withContext(Dispatchers.IO) {
                SmtpClient.sendBatch(
                    settings = settings,
                    requests = requests,
                    onSending = { request ->
                        val recipient = "${request.toName} <${request.toEmail}>"
                        emailProgressRepository.setCurrentRecipient(recipient)
                    },
                    onSuccess = { index ->
                        consecutiveErrors = 0
                        sentIndices.add(index)
                        emailProgressRepository.update(sentIndices.size)
                        viewModelScope.launch {
                            emailProgressRepository.recordSuccessfulSend()
                        }
                        sentInWindow++
                    },
                    onFailure = { index, exception ->
                        consecutiveErrors++
                        if (isGmailQuotaError(exception)) {
                            shouldStopDueToErrors = true
                            stopReason = EmailStopReason.GmailQuotaExceeded
                        } else if (consecutiveErrors >= errorThreshold) {
                            shouldStopDueToErrors = true
                            stopReason = EmailStopReason.ConsecutiveErrors(errorThreshold)
                        }
                    },
                    isCancelRequested = {
                        val limitReached = sentInWindow >= dailyLimit
                        if (limitReached && stopReason == null) {
                            stopReason = EmailStopReason.DailyLimitReached(dailyLimit)
                        }
                        !parentContext.isActive ||
                                emailProgressRepository.isCancelRequested() ||
                                shouldStopDueToErrors ||
                                limitReached
                    },
                )
            }
            currentCoroutineContext().ensureActive()

            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            if (unsentRequests.isNotEmpty()) {
                val reason = stopReason
                    ?: if (emailProgressRepository.isCancelRequested()) {
                        EmailStopReason.Cancelled
                    } else {
                        EmailStopReason.GenericFailure
                    }

                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = reason
                    )
                )

                if (shouldStopDueToErrors || stopReason != null || !emailProgressRepository.isCancelRequested()) {
                    emailProgressRepository.fail(EmailStopReason.Cached(reason, unsentRequests.size))
                }
            } else {
                emailProgressRepository.clearCachedEmails()
                if (!emailProgressRepository.isCancelRequested()) {
                    emailProgressRepository.finish()
                }
            }
        } catch (e: CancellationException) {
            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            if (unsentRequests.isNotEmpty()) {
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = EmailStopReason.Cancelled
                    )
                )
            }
            emailProgressRepository.requestCancel()
        } catch (e: Exception) {
            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            val reason = EmailStopReason.Raw(e.message ?: "Unknown error")
            if (unsentRequests.isNotEmpty()) {
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = reason
                    )
                )
            }
            emailProgressRepository.fail(reason)
        }
    }

    private fun isGmailQuotaError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        val quotaKeywords = listOf(
            "quota",
            "rate limit",
            "too many messages",
            "daily user sending",
            "try again later",
            "temporarily blocked"
        )
        return quotaKeywords.any { it in message }
    }

    private fun buildRequests(
        entries: List<RegistrationEntry>,
        docIdStart: Long,
        outputDir: String,
        subject: String,
        body: String,
        htmlBody: String?,
    ): List<EmailSendRequest> {
        return entries.mapIndexed { index, entry ->
            val email = entry.primaryEmail.trim()
            val fullName = listOf(entry.name, entry.surname)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { email }
            val docId = docIdStart + index
            EmailSendRequest(
                toEmail = email,
                toName = fullName,
                subject = subject,
                body = body,
                htmlBody = htmlBody,
                attachmentPath = joinPath(outputDir, "${docId}.pdf"),
                attachmentName = "${docId}.pdf",
            )
        }
    }

}

data class EmailProgressUiState(
    val mode: EmailProgress = EmailProgress.Running(),
    val cachedCount: Int = 0,
    val sentToday: Int = 0,
    val dailyLimit: Int = 0,
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
