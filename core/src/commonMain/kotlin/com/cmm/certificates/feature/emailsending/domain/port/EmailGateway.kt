package com.cmm.certificates.feature.emailsending.domain.port

import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.settings.domain.SmtpSettings

interface EmailGateway {
    suspend fun testConnection(settings: SmtpSettings)

    suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, exception: Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    )
}
