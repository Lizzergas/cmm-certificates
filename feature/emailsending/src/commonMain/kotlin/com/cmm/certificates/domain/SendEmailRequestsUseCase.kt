package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
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
import kotlin.math.absoluteValue
import kotlin.time.Clock

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
        val preparedRequests = requests.prepareForSending()
        val skippedEntries = preparedRequests.skippedEntries
        val sendableRequests = preparedRequests.sendableRequests

        val dailyLimit = settingsState.email.dailyLimit.takeIf { it > 0 }
        var sentInWindow = emailProgressRepository.getSentCountInLast24Hours()
        var consecutiveErrors = 0
        val errorThreshold = 3
        var shouldStopDueToErrors = false
        var stopReason: EmailStopReason? = null
        val sentIndices = mutableSetOf<Int>()
        val skippedCount = skippedEntries.size

        logInfo(logTag, "Starting send for ${requests.size} email requests")
        emailProgressRepository.start(requests.size)
        if (skippedCount > 0) {
            emailProgressRepository.update(skippedCount)
        }

        if (sendableRequests.isEmpty()) {
            val skippedSummary = preparedRequests.skippedSummaryReason ?: EmailStopReason.NoEmailsToSend
            emailProgressRepository.cacheEmails(
                CachedEmailBatch(
                    entries = skippedEntries,
                    lastReason = skippedSummary,
                )
            )
            emailProgressRepository.fail(EmailStopReason.Cached(skippedSummary, skippedEntries.size))
            return
        }

        val parentContext = currentCoroutineContext()

        try {
            withContext(Dispatchers.IO) {
                emailGateway.sendBatch(
                    settings = settings,
                    requests = sendableRequests,
                    onSending = { request ->
                        val recipient = "${request.toName} <${request.toEmail}>"
                        logInfo(logTag, "Sending email to $recipient")
                        emailProgressRepository.setCurrentRecipient(recipient)
                    },
                    onSuccess = { index ->
                        consecutiveErrors = 0
                        sentIndices.add(index)
                        emailProgressRepository.update(skippedCount + sentIndices.size)
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
            persistSuccessfulSends(sendableRequests, sentIndices)
            currentCoroutineContext().ensureActive()

            val unsentRequests = sendableRequests.filterIndexed { index, _ -> index !in sentIndices }
            val cachedEntries = skippedEntries + unsentRequests.toCachedEntries()
            if (cachedEntries.isNotEmpty()) {
                val sendStopReason = stopReason
                    ?: if (emailProgressRepository.isCancelRequested()) {
                        EmailStopReason.Cancelled
                    } else {
                        EmailStopReason.GenericFailure
                    }
                val reason = buildFinalFailureReason(
                    skippedSummary = preparedRequests.skippedSummaryReason,
                    sendStopReason = sendStopReason.takeIf { unsentRequests.isNotEmpty() },
                )

                logWarn(logTag, "Caching ${cachedEntries.size} unsent emails because of $reason")
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        entries = cachedEntries,
                        lastReason = reason,
                    )
                )

                if (shouldStopDueToErrors || stopReason != null || !emailProgressRepository.isCancelRequested() || skippedEntries.isNotEmpty()) {
                    emailProgressRepository.fail(
                        EmailStopReason.Cached(
                            reason,
                            cachedEntries.size
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
            persistSuccessfulSends(sendableRequests, sentIndices)
            val unsentRequests = sendableRequests.filterIndexed { index, _ -> index !in sentIndices }
            val cachedEntries = skippedEntries + unsentRequests.toCachedEntries()
            if (cachedEntries.isNotEmpty()) {
                val reason = buildFinalFailureReason(
                    skippedSummary = preparedRequests.skippedSummaryReason,
                    sendStopReason = EmailStopReason.Cancelled,
                )
                logWarn(logTag, "Send job cancelled; caching ${cachedEntries.size} unsent emails")
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        entries = cachedEntries,
                        lastReason = reason,
                    )
                )
            }
            emailProgressRepository.requestCancel()
        } catch (e: Exception) {
            logError(logTag, "Unexpected email send failure", e)
            persistSuccessfulSends(sendableRequests, sentIndices)
            val unsentRequests = sendableRequests.filterIndexed { index, _ -> index !in sentIndices }
            val reason = buildFinalFailureReason(
                skippedSummary = preparedRequests.skippedSummaryReason,
                sendStopReason = EmailStopReason.Raw(e.message ?: "Unknown error"),
            )
            val cachedEntries = skippedEntries + unsentRequests.toCachedEntries()
            if (cachedEntries.isNotEmpty()) {
                logWarn(
                    logTag,
                    "Caching ${cachedEntries.size} unsent emails after unexpected failure"
                )
                emailProgressRepository.cacheEmails(
                    CachedEmailBatch(
                        entries = cachedEntries,
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

    private suspend fun persistSuccessfulSends(requests: List<EmailSendRequest>, sentIndices: Set<Int>) {
        requests.forEachIndexed { index, request ->
            if (index in sentIndices) {
                emailProgressRepository.recordSuccessfulSend(request)
            }
        }
    }

    private fun List<EmailSendRequest>.prepareForSending(): PreparedRequests {
        val cachedAt = Clock.System.now().toEpochMilliseconds()
        val sendableRequests = mutableListOf<EmailSendRequest>()
        val skippedEntries = mutableListOf<CachedEmailEntry>()
        val missingEmailRows = mutableListOf<Int>()
        val missingAttachments = mutableListOf<String>()

        forEachIndexed { index, request ->
            val attachmentPath = request.attachmentPath?.takeIf { it.isNotBlank() }
            when {
                request.toEmail.isBlank() -> {
                    val rowNumber = index + 1
                    missingEmailRows += rowNumber
                    skippedEntries += CachedEmailEntry(
                        id = buildCachedEntryId(request, cachedAt, index),
                        request = request,
                        cachedAt = cachedAt,
                        failureReason = EmailStopReason.MissingEmailAddresses(rowNumber.toString()),
                    )
                }

                attachmentPath != null && !FileSystem.SYSTEM.exists(attachmentPath.toPath()) -> {
                    val attachmentName = request.attachmentName ?: attachmentPath
                    missingAttachments += attachmentName
                    skippedEntries += CachedEmailEntry(
                        id = buildCachedEntryId(request, cachedAt, index),
                        request = request,
                        cachedAt = cachedAt,
                        failureReason = EmailStopReason.MissingAttachments(attachmentName),
                    )
                }

                else -> sendableRequests += request
            }
        }

        return PreparedRequests(
            sendableRequests = sendableRequests,
            skippedEntries = skippedEntries,
            skippedSummaryReason = buildSkippedSummaryReason(missingEmailRows, missingAttachments),
        )
    }

    private fun buildSkippedSummaryReason(
        missingEmailRows: List<Int>,
        missingAttachments: List<String>,
    ): EmailStopReason? {
        val emailPreview = missingEmailRows.preview()
        val attachmentPreview = missingAttachments.preview()
        return when {
            emailPreview != null && attachmentPreview != null -> {
                EmailStopReason.Raw(
                    "Missing recipient emails for entries: $emailPreview. Missing PDF attachments: $attachmentPreview.",
                )
            }

            emailPreview != null -> EmailStopReason.MissingEmailAddresses(emailPreview)
            attachmentPreview != null -> EmailStopReason.MissingAttachments(attachmentPreview)
            else -> null
        }
    }

    private fun buildFinalFailureReason(
        skippedSummary: EmailStopReason?,
        sendStopReason: EmailStopReason?,
    ): EmailStopReason {
        return when {
            skippedSummary == null && sendStopReason != null -> sendStopReason
            skippedSummary != null && sendStopReason == null -> skippedSummary
            skippedSummary != null && sendStopReason != null -> {
                EmailStopReason.Raw("${reasonText(skippedSummary)} ${reasonText(sendStopReason)}")
            }

            else -> EmailStopReason.GenericFailure
        }
    }

    private fun reasonText(reason: EmailStopReason): String {
        return when (reason) {
            is EmailStopReason.Raw -> reason.message
            is EmailStopReason.MissingEmailAddresses ->
                "Missing recipient emails for entries: ${reason.preview}."
            is EmailStopReason.MissingAttachments ->
                "Missing PDF attachments: ${reason.preview}."
            EmailStopReason.Cancelled -> "Sending cancelled."
            EmailStopReason.GenericFailure -> "Some emails failed to send."
            EmailStopReason.GmailQuotaExceeded -> "Gmail daily sending limit reached or account blocked."
            EmailStopReason.NetworkUnavailable -> "No network connection."
            is EmailStopReason.ConsecutiveErrors ->
                "Stopped after ${reason.threshold} consecutive errors."
            is EmailStopReason.DailyLimitReached ->
                "Daily limit of ${reason.limit} emails reached."
            EmailStopReason.SmtpAuthRequired -> "SMTP authentication is required."
            EmailStopReason.SmtpSettingsMissing -> "SMTP settings are missing."
            EmailStopReason.DocumentIdMissing -> "Document ID start is missing."
            EmailStopReason.OutputDirMissing -> "Output folder is missing."
            EmailStopReason.NoEntries -> "No entries to send."
            EmailStopReason.NoEmailsToSend -> "No emails to send."
            EmailStopReason.NoCachedEmails -> "No cached emails found."
            is EmailStopReason.Cached -> reasonText(reason.reason)
        }
    }

    private fun List<EmailSendRequest>.toCachedEntries(): List<CachedEmailEntry> {
        val cachedAt = Clock.System.now().toEpochMilliseconds()
        return mapIndexed { index, request ->
            CachedEmailEntry(
                id = buildCachedEntryId(request, cachedAt, index),
                request = request,
                cachedAt = cachedAt,
            )
        }
    }

    private fun buildCachedEntryId(request: EmailSendRequest, cachedAt: Long, index: Int): String {
        val seed = listOf(request.toEmail, request.attachmentName, request.subject, index.toString())
            .joinToString("|")
            .hashCode()
            .absoluteValue
        return "$cachedAt-$seed"
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

    private fun <T> List<T>.preview(): String? {
        if (isEmpty()) return null
        val preview = take(3).joinToString(", ")
        val suffix = if (size > 3) "..." else ""
        return "$preview$suffix"
    }

    private data class PreparedRequests(
        val sendableRequests: List<EmailSendRequest>,
        val skippedEntries: List<CachedEmailEntry>,
        val skippedSummaryReason: EmailStopReason?,
    )
}
