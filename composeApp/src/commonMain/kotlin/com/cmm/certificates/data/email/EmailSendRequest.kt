package com.cmm.certificates.data.email

import kotlinx.serialization.Serializable

@Serializable
data class EmailSendRequest(
    val toEmail: String,
    val toName: String,
    val subject: String,
    val body: String,
    val htmlBody: String? = null,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
)
