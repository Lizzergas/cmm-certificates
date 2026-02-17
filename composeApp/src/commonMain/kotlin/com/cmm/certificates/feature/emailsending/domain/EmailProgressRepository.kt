package com.cmm.certificates.feature.emailsending.domain

import com.cmm.certificates.feature.emailsending.data.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.data.EmailProgressState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface EmailProgressRepository {
    val state: StateFlow<EmailProgressState>
    val cachedEmails: Flow<CachedEmailBatch?>
    val sentCountInLast24Hours: Flow<Int>

    fun start(total: Int)

    fun update(current: Int)

    fun setCurrentRecipient(recipient: String?)

    fun finish()

    fun fail(reason: EmailStopReason)

    fun requestCancel()

    fun isCancelRequested(): Boolean

    fun clear()

    suspend fun cacheEmails(batch: CachedEmailBatch)

    suspend fun clearCachedEmails()

    suspend fun getSentCountInLast24Hours(): Int
    suspend fun recordSuccessfulSend()
}
