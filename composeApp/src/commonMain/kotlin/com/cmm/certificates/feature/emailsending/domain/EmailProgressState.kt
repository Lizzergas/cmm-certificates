package com.cmm.certificates.feature.emailsending.domain

data class EmailProgressState(
    val current: Int = 0,
    val total: Int = 0,
    val inProgress: Boolean = false,
    val completed: Boolean = false,
    val stopReason: EmailStopReason? = null,
    val currentRecipient: String? = null,
    val cancelRequested: Boolean = false,
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
)
