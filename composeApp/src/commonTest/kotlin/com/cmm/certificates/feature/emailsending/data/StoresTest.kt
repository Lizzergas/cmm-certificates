package com.cmm.certificates.feature.emailsending.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.cmm.certificates.data.CachedEmailStore
import com.cmm.certificates.data.SentEmailHistoryStore
import com.cmm.certificates.data.store.createDataStore
import com.cmm.certificates.test.createTestPreferencesFilePath
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.test.Test
import kotlin.test.assertEquals

class StoresTest {

    @Test
    fun cachedEmailStore_roundTripsBatch() = runBlocking {
        val store = CachedEmailStore(testDataStore("cached-email-store"))
        val batch = CachedEmailBatch(
            entries = listOf(
                CachedEmailEntry(
                    id = "1",
                    cachedAt = 123L,
                    request = EmailSendRequest(
                        toEmail = "ada@example.com",
                        toName = "Ada",
                        certificateName = "Certificate",
                        subject = "Subject",
                        body = "Body",
                    ),
                )
            ),
            lastReason = EmailStopReason.GenericFailure,
        )

        store.save(batch)

        assertEquals(batch, store.cachedEmails.first())
    }

    @Test
    fun cachedEmailStore_removesSingleEntry() = runBlocking {
        val store = CachedEmailStore(testDataStore("cached-email-remove"))
        val first = CachedEmailEntry(
            id = "1",
            cachedAt = 123L,
            request = EmailSendRequest("ada@example.com", "Ada", "Certificate", "Subject", "Body"),
        )
        val second = CachedEmailEntry(
            id = "2",
            cachedAt = 124L,
            request = EmailSendRequest("grace@example.com", "Grace", "Certificate", "Subject", "Body"),
        )

        store.save(CachedEmailBatch(entries = listOf(first, second), lastReason = EmailStopReason.GenericFailure))
        store.removeEntry("1")

        assertEquals(listOf(second), store.cachedEmails.first().entries)
    }

    @Test
    fun sentEmailHistoryStore_roundTripsDetailedRecords() = runBlocking {
        val store = SentEmailHistoryStore(testDataStore("sent-history-store"))
        val record = SentEmailRecord(
            id = "sent-1",
            sentAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            toEmail = "ada@example.com",
            toName = "Ada",
            certificateName = "Certificate",
            subject = "Certificate",
            attachmentName = "certificate.pdf",
            attachmentPath = "/tmp/certificate.pdf",
        )

        store.addSend(record)

        assertEquals(listOf(record), store.history.first().records)
    }

    @Test
    fun sentEmailHistoryStore_countsRecentDetailedRecords() = runBlocking {
        val store = SentEmailHistoryStore(testDataStore("sent-history-count"))
        val now = Clock.System.now().toEpochMilliseconds()
        val old = now - 25.hours.inWholeMilliseconds

        store.addSend(
            SentEmailRecord(
                id = "old",
                sentAt = old,
                toEmail = "old@example.com",
                toName = "Old",
                certificateName = "Old certificate",
                subject = "Old",
            )
        )
        store.addSend(
            SentEmailRecord(
                id = "new",
                sentAt = now,
                toEmail = "new@example.com",
                toName = "New",
                certificateName = "New certificate",
                subject = "New",
            )
        )

        assertEquals(1, store.getCountInLast24Hours())
    }

    private fun testDataStore(name: String): DataStore<Preferences> {
        return createDataStore { createTestPreferencesFilePath(name) }
    }
}
