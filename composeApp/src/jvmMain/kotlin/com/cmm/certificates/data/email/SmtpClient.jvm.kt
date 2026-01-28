package com.cmm.certificates.data.email

import jakarta.activation.FileDataSource
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.io.File

actual object SmtpClient {
    actual suspend fun testConnection(settings: SmtpSettings) {
        buildMailer(settings).testConnection()
    }

    actual suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onProgress: (Int) -> Unit,
        isCancelRequested: () -> Boolean,
    ) {
        val mailer = buildMailer(settings)
        requests.forEachIndexed { index, request ->
            if (isCancelRequested()) return
            onSending(request)
            val attachmentFile = File(request.attachmentPath)
            require(attachmentFile.exists()) {
                "Attachment not found: ${request.attachmentPath}"
            }
            val emailBuilder = EmailBuilder.startingBlank()
                .from(settings.username, settings.username)
                .to(request.toName, request.toEmail)
                .withSubject(request.subject)
                .withPlainText(request.body)
            val htmlBody = request.htmlBody?.trim().orEmpty()
            if (htmlBody.isNotBlank()) {
                emailBuilder.withHTMLText(htmlBody)
            }
            val email = emailBuilder
                .withAttachment(request.attachmentName, FileDataSource(attachmentFile))
                .buildEmail()
            mailer.sendMail(email)
            if (isCancelRequested()) return
            onProgress(index + 1)
        }
    }
}

private fun buildMailer(settings: SmtpSettings) =
    MailerBuilder
        .withSMTPServer(settings.host, settings.port, settings.username, settings.password)
        .withTransportStrategy(settings.transport.toTransportStrategy())
        .buildMailer()

private fun SmtpTransport.toTransportStrategy(): TransportStrategy = when (this) {
    SmtpTransport.SMTP -> TransportStrategy.SMTP
    SmtpTransport.SMTPS -> TransportStrategy.SMTPS
    SmtpTransport.SMTP_TLS -> TransportStrategy.SMTP_TLS
}
