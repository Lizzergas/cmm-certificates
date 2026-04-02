package com.cmm.certificates.feature.emailsending.domain

import kotlinx.serialization.Serializable

@Serializable
data class CachedEmailEntry(
    val id: String,
    val request: EmailSendRequest,
    val cachedAt: Long,
    val failureReason: EmailStopReason? = null,
)

@Serializable
data class CachedEmailBatch(
    val entries: List<CachedEmailEntry> = emptyList(),
    val lastReason: EmailStopReason? = null,
) {
    val requests: List<EmailSendRequest>
        get() = entries.map(CachedEmailEntry::request)
}
