package com.cmm.certificates.feature.emailsending.domain

import kotlinx.serialization.Serializable

@Serializable
data class CachedEmailBatch(
    val requests: List<EmailSendRequest>,
    val lastReason: EmailStopReason? = null,
)
