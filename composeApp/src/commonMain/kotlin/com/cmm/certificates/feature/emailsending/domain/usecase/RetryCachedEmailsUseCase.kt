package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import kotlinx.coroutines.flow.firstOrNull

class RetryCachedEmailsUseCase(
    private val emailProgressRepository: EmailProgressRepository,
    private val sendEmailRequests: SendEmailRequestsUseCase,
) {
    private val logTag = "RetryCachedEmails"

    suspend operator fun invoke() {
        val cached = emailProgressRepository.cachedEmails.firstOrNull()
        if (cached == null || cached.requests.isEmpty()) {
            logWarn(logTag, "Retry aborted: no cached email batch found")
            emailProgressRepository.fail(EmailStopReason.NoCachedEmails)
            return
        }

        logInfo(logTag, "Retrying cached batch with ${cached.requests.size} emails")
        sendEmailRequests(cached.requests)
    }
}
