package com.cmm.certificates.feature.emailsending.data

import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.settings.domain.SmtpSettings

class SmtpEmailGateway : EmailGateway {
    override suspend fun testConnection(settings: SmtpSettings) {
        SmtpClient.testConnection(settings)
    }

    override suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, exception: Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    ) {
        SmtpClient.sendBatch(
            settings = settings,
            requests = requests,
            onSending = onSending,
            onSuccess = onSuccess,
            onFailure = onFailure,
            isCancelRequested = isCancelRequested,
        )
    }
}
