package com.cmm.certificates.core.usecase

import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.email.buildEmailHtmlBody
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SendPreviewEmailUseCase(
    private val settingsRepository: SettingsRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
) {
    suspend fun sendPreviewEmail(
        toEmail: String,
        attachFirstPdf: Boolean,
    ): Result<Unit> {
        val trimmedEmail = toEmail.trim()
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email address is required."))
        }

        val settingsState = settingsRepository.state.value
        val smtpState = settingsState.smtp
        val smtpSettings = smtpState.toSmtpSettings()
            ?: return Result.failure(IllegalStateException("SMTP details are incomplete."))
        if (!smtpState.isAuthenticated) {
            return Result.failure(IllegalStateException("SMTP authentication is required."))
        }

        val (attachmentPath, attachmentName) = if (attachFirstPdf) {
            val conversionState = pdfConversionProgressRepository.state.value
            val outputDir = conversionState.outputDir
            val docIdStart = conversionState.docIdStart
            if (outputDir.isBlank()) {
                return Result.failure(IllegalStateException("Output folder is missing."))
            }
            if (docIdStart == null) {
                return Result.failure(IllegalStateException("Document ID start is missing."))
            }
            val filename = "${docIdStart}.pdf"
            joinPath(outputDir, filename) to filename
        } else {
            null to null
        }

        settingsRepository.setPreviewEmail(trimmedEmail)
        settingsRepository.save()

        val htmlBody = buildEmailHtmlBody(
            body = settingsState.email.body,
            signatureHtml = settingsState.email.signatureHtml,
        )
        val request = EmailSendRequest(
            toEmail = trimmedEmail,
            toName = trimmedEmail,
            subject = settingsState.email.subject,
            body = settingsState.email.body,
            htmlBody = htmlBody,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName,
        )

        return runCatching {
            withContext(Dispatchers.IO) {
                SmtpClient.sendBatch(
                    settings = smtpSettings,
                    requests = listOf(request),
                    onSending = {},
                    onSuccess = {},
                    onFailure = { _, _ -> },
                    isCancelRequested = { false },
                )
            }
        }
    }
}
