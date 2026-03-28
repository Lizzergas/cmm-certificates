package com.cmm.certificates.feature.emailsending.domain

import kotlinx.serialization.Serializable

@Serializable
data class SentEmailRecord(
    val id: String,
    val sentAt: Long,
    val toEmail: String,
    val toName: String,
    val certificateName: String,
    val subject: String,
    val attachmentName: String? = null,
    val attachmentPath: String? = null,
)
