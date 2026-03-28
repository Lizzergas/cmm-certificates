package com.cmm.certificates.data

import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailProgressState
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class EmailProgressRepositoryImpl(
    private val store: EmailProgressStore,
    private val cachedEmailStore: CachedEmailStore,
    private val historyStore: SentEmailHistoryStore,
) : EmailProgressRepository {
    override val state: StateFlow<EmailProgressState> = store.state
    override val cachedEmails: Flow<CachedEmailBatch> = cachedEmailStore.cachedEmails
    override val sentHistory: Flow<List<SentEmailRecord>> = historyStore.history
        .map { it.records }
    override val sentCountInLast24Hours: Flow<Int> = combine(
        sentHistory,
        refreshTickerFlow(),
    ) { history, _ ->
        history.countInLast24Hours()
    }

    override fun start(total: Int) {
        store.start(total)
    }

    override fun update(current: Int) {
        store.update(current)
    }

    override fun setCurrentRecipient(recipient: String?) {
        store.setCurrentRecipient(recipient)
    }

    override fun finish() {
        store.finish()
    }

    override fun fail(reason: EmailStopReason) {
        store.fail(reason)
    }

    override fun requestCancel() {
        store.requestCancel()
    }

    override fun isCancelRequested(): Boolean = store.isCancelRequested()

    override fun clear() {
        store.clear()
    }

    override suspend fun cacheEmails(batch: CachedEmailBatch) {
        cachedEmailStore.save(batch)
    }

    override suspend fun removeCachedEmail(id: String) {
        cachedEmailStore.removeEntry(id)
    }

    override suspend fun clearCachedEmails() {
        cachedEmailStore.clear()
    }

    override suspend fun getSentCountInLast24Hours(): Int {
        return historyStore.getCountInLast24Hours()
    }

    override suspend fun recordSuccessfulSend(request: EmailSendRequest) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        historyStore.addSend(
            SentEmailRecord(
                id = buildRecordId(request, timestamp),
                sentAt = timestamp,
                toEmail = request.toEmail,
                toName = request.toName,
                certificateName = request.certificateName,
                subject = request.subject,
                attachmentName = request.attachmentName,
                attachmentPath = request.attachmentPath,
            )
        )
    }

    override suspend fun clearSentHistory() {
        historyStore.clear()
    }

    private fun refreshTickerFlow(intervalMillis: Long = 60_000L): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            delay(intervalMillis)
            emit(Unit)
        }
    }

    private fun List<SentEmailRecord>.countInLast24Hours(
        nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): Int {
        val cutoff = nowMillis - 24.hours.inWholeMilliseconds
        return count { it.sentAt > cutoff }
    }

    private fun buildRecordId(request: EmailSendRequest, timestamp: Long): String {
        val seed = listOf(request.toEmail, request.attachmentName, request.subject)
            .joinToString("|")
            .hashCode()
            .absoluteValue
        return "$timestamp-$seed"
    }
}
