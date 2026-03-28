package com.cmm.certificates.feature.settings.domain

data class SmtpSettings(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val transport: SmtpTransport,
)

enum class SmtpTransport {
    SMTP,
    SMTPS,
    SMTP_TLS,
}
