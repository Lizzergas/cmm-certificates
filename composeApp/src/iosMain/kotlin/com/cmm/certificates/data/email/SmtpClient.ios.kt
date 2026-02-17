package com.cmm.certificates.data.email

actual object SmtpClient {
    actual suspend fun testConnection(settings: SmtpSettings) {
        throw UnsupportedOperationException("SMTP is not supported on iOS yet.")
    }

    actual suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (Int) -> Unit,
        onFailure: (Int, Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    ) {
        throw UnsupportedOperationException("SMTP is not supported on iOS yet.")
    }
}
