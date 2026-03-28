package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.domain.SmtpSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class SendEmailRequestsUseCase(
    private val emailProgressRepository: EmailProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val emailGateway: EmailGateway,
) {
    private val logTag = "SendEmailRequests"

    suspend operator fun invoke(requests: List<EmailSendRequest>) {
        if (!connectivityMonitor.isNetworkAvailable.value) {
            logWarn(logTag, "Email send aborted: network unavailable")
            emailProgressRepository.fail(EmailStopReason.NetworkUnavailable)
            return
        }

        val settings = authenticatedSmtpSettingsOrFail() ?: return
        val settingsState = settingsRepository.state.value
        val missingAttachments = requests.findMissingAttachments()
        if (missingAttachments.isNotEmpty()) {
            val preview = missingAttachments.take(3).joinToString(", ")
            val suffix = if (missingAttachments.size > 3) "..." else ""
            logWarn(logTag, "Email send aborted: missing attachments $preview$suffix")
            emailProgressRepository.fail(EmailStopReason.MissingAttachments("$preview$suffix"))
            return
        }

        val dailyLimit = settingsState.email.dailyLimit.takeIf { it > 0 }
        var sentInWindow = emailProgressRepository.getSentCountInLast24Hours()
        var consecutiveErrors = 0
        val errorThreshold = 3
        var shouldStopDueToErrors = false
        var stopReason: EmailStopReason? = null
        val sentIndices = mutableSetOf<Int>()
        var successfulSendCount = 0

        logInfo(logTag, "Starting send for ${requests.size} email requests")
        emailProgressRepository.start(requests.size)
        val parentContext = currentCoroutineContext()

        try {
            withContext(Dispatchers.IO) {
                emailGateway.sendBatch(
                    settings = settings,
                    requests = requests,
                    onSending = { request ->
                        val recipient = "${request.toName} <${request.toEmail}>"
                        logInfo(logTag, "Sending email to $recipient")
                        emailProgressRepository.setCurrentRecipient(recipient)
                    },
                    onSuccess = { index ->
                        consecutiveErrors = 0
                        sentIndices.add(index)
                        emailProgressRepository.update(sentIndices.size)
                        successfulSendCount++
                        sentInWindow++
                        logInfo(
                            logTag,
                            "Email sent successfully at index=$index sentInWindow=$sentInWindow"
                        )
                    },
                    onFailure = { index, exception ->
                        consecutiveErrors++
                        logWarn(
                            logTag,
                            "Email failed at index=$index consecutiveErrors=$consecutiveErrors"
                        )
                        if (isGmailQuotaError(exception)) {
                            shouldStopDueToErrors = true
                            stopReason = EmailStopReason.GmailQuotaExceeded
                            logWarn(logTag, "Detected Gmail quota stop condition")
                        } else if (consecutiveErrors >= errorThreshold) {
                            shouldStopDueToErrors = true
                            stopReason = EmailStopReason.ConsecutiveErrors(errorThreshold)
                            logWarn(
                                logTag,
                                "Stopping after $errorThreshold consecutive send errors"
                            )
                        }
                    },
                    isCancelRequested = {
                        val limitReached = dailyLimit != null && sentInWindow >= dailyLimit
                        if (limitReached && stopReason == null) {
                            stopReason = EmailStopReason.DailyLimitReached(dailyLimit)
                            logWarn(logTag, "Stopping because daily limit $dailyLimit was reached")
                        }
                        !parentContext.isActive ||
                                emailProgressRepository.isCancelRequested() ||
                                shouldStopDueToErrors ||
                                limitReached
                    },
                )
            }
            persistSuccessfulSends(successfulSendCount)
            currentCoroutineContext().ensureActive()

            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            if (unsentRequests.isNotEmpty()) {
                val reason = stopReason
                    ?: if (emailProgressRepository.isCancelRequested()) {
                        EmailStopReason.Cancelled
                    } else {
                        EmailStopReason.GenericFailure
                    }

                logWarn(logTag, "Caching ${unsentRequests.size} unsent emails because of $reason")
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = reason,
                    )
                )

                if (shouldStopDueToErrors || stopReason != null || !emailProgressRepository.isCancelRequested()) {
                    emailProgressRepository.fail(
                        EmailStopReason.Cached(
                            reason,
                            unsentRequests.size
                        )
                    )
                }
            } else {
                logInfo(logTag, "All emails sent successfully")
                emailProgressRepository.clearCachedEmails()
                if (!emailProgressRepository.isCancelRequested()) {
                    emailProgressRepository.finish()
                }
            }
        } catch (e: CancellationException) {
            persistSuccessfulSends(successfulSendCount)
            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            if (unsentRequests.isNotEmpty()) {
                logWarn(logTag, "Send job cancelled; caching ${unsentRequests.size} unsent emails")
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = EmailStopReason.Cancelled,
                    )
                )
            }
            emailProgressRepository.requestCancel()
        } catch (e: Exception) {
            logError(logTag, "Unexpected email send failure", e)
            persistSuccessfulSends(successfulSendCount)
            val unsentRequests = requests.filterIndexed { index, _ -> index !in sentIndices }
            val reason = EmailStopReason.Raw(e.message ?: "Unknown error")
            if (unsentRequests.isNotEmpty()) {
                logWarn(
                    logTag,
                    "Caching ${unsentRequests.size} unsent emails after unexpected failure"
                )
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        requests = unsentRequests,
                        lastReason = reason,
                    )
                )
            }
            emailProgressRepository.fail(reason)
        }
    }

    private fun authenticatedSmtpSettingsOrFail(): SmtpSettings? {
        val smtpState = settingsRepository.state.value.smtp
        val settings = smtpState.toSmtpSettings()
        return when {
            settings == null -> {
                logWarn(logTag, "Email send aborted: SMTP settings missing or invalid")
                emailProgressRepository.fail(EmailStopReason.SmtpSettingsMissing)
                null
            }

            !smtpState.isAuthenticated -> {
                logWarn(logTag, "Email send aborted: SMTP authentication required")
                emailProgressRepository.fail(EmailStopReason.SmtpAuthRequired)
                null
            }

            else -> settings
        }
    }

    private fun isGmailQuotaError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        val quotaKeywords = listOf(
            "quota",
            "rate limit",
            "too many messages",
            "daily user sending",
            "try again later",
            "temporarily blocked",
        )
        return quotaKeywords.any { it in message }
    }

    private suspend fun persistSuccessfulSends(count: Int) {
        repeat(count) {
            emailProgressRepository.recordSuccessfulSend()
        }
    }

    private fun List<EmailSendRequest>.findMissingAttachments(): List<String> {
        return mapNotNull { request ->
            val path = request.attachmentPath?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (FileSystem.SYSTEM.exists(path.toPath())) {
                null
            } else {
                request.attachmentName ?: path
            }
        }
    }
}
