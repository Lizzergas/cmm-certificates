package com.cmm.certificates.data.email

actual object SmtpClient {
    actual suspend fun testConnection(settings: SmtpSettings) {
        throw UnsupportedOperationException("SMTP is not supported on Android yet.")
    }

    actual suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onProgress: (Int) -> Unit,
        isCancelRequested: () -> Boolean,
    ) {
        throw UnsupportedOperationException("SMTP is not supported on Android yet.")
    }
}
