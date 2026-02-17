package com.cmm.certificates.feature.emailsending.data

import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class EmailProgressRepositoryImpl(
    private val store: EmailProgressStore,
    private val cachedEmailStore: CachedEmailStore,
    private val historyStore: SentEmailHistoryStore,
) : EmailProgressRepository {
    override val state: StateFlow<EmailProgressState> = store.state
    override val cachedEmails: Flow<CachedEmailBatch?> = cachedEmailStore.cachedEmails
    override val sentCountInLast24Hours: Flow<Int> = historyStore.history.map { it.timestamps.size }

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

    override suspend fun clearCachedEmails() {
        cachedEmailStore.clear()
    }

    override suspend fun getSentCountInLast24Hours(): Int {
        return historyStore.getCountInLast24Hours()
    }

    override suspend fun recordSuccessfulSend() {
        historyStore.addSend(Clock.System.now().toEpochMilliseconds())
    }
}
