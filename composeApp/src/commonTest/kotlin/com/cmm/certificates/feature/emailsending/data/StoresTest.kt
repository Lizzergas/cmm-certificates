package com.cmm.certificates.feature.emailsending.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.cmm.certificates.data.CachedEmailStore
import com.cmm.certificates.data.SentEmailHistoryStore
import com.cmm.certificates.data.store.createDataStore
import com.cmm.certificates.test.createTestPreferencesFilePath
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class StoresTest {

    @Test
    fun cachedEmailStore_roundTripsBatch() = runBlocking {
        val store = CachedEmailStore(testDataStore("cached-email-store"))
        val batch = CachedEmailBatch(
            requests = listOf(
                EmailSendRequest(
                    toEmail = "ada@example.com",
                    toName = "Ada",
                    subject = "Subject",
                    body = "Body",
                )
            ),
            lastReason = EmailStopReason.GenericFailure,
        )

        store.save(batch)

        assertEquals(batch, store.cachedEmails.first())
    }

    @Test
    fun sentEmailHistoryStore_prunesOldEntriesAndCountsRecentOnes() = runBlocking {
        val store = SentEmailHistoryStore(testDataStore("sent-history-store"))
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val old = now - (25L * 60L * 60L * 1000L)

        store.addSend(old)
        store.addSend(now)

        assertEquals(1, store.getCountInLast24Hours())
        assertEquals(1, store.history.first().timestamps.size)
    }

    private fun testDataStore(name: String): DataStore<Preferences> {
        return createDataStore { createTestPreferencesFilePath(name) }
    }
}
