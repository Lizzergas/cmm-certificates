package com.cmm.certificates.data.email

data class EmailSendRequest(
    val toEmail: String,
    val toName: String,
    val subject: String,
    val body: String,
    val htmlBody: String? = null,
    val attachmentPath: String,
    val attachmentName: String,
)
