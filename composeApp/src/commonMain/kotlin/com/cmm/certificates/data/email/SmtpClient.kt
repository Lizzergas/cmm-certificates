package com.cmm.certificates.data.email

expect object SmtpClient {
    suspend fun testConnection(settings: SmtpSettings)

    suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    )
}
