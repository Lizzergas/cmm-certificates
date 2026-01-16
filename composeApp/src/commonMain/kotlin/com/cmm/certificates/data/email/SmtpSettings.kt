package com.cmm.certificates.data.email

data class SmtpSettings(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val transport: SmtpTransport,
)
