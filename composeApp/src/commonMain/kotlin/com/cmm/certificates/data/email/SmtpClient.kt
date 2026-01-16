package com.cmm.certificates.data.email

expect object SmtpClient {
    suspend fun testConnection(settings: SmtpSettings)

    suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onProgress: (Int) -> Unit,
        isCancelRequested: () -> Boolean,
    )
}
