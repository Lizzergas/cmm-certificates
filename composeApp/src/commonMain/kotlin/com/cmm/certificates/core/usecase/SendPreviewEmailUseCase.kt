package com.cmm.certificates.core.usecase

import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_error_doc_id_missing
import certificates.composeapp.generated.resources.common_error_email_required
import certificates.composeapp.generated.resources.common_error_output_dir_missing
import certificates.composeapp.generated.resources.common_error_smtp_auth_required
import certificates.composeapp.generated.resources.common_error_smtp_incomplete
import certificates.composeapp.generated.resources.email_preview_error_send_failed
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.buildEmailHtmlBody
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SendPreviewEmailUseCase(
    private val settingsRepository: SettingsRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val emailGateway: EmailGateway,
) {
    private val logTag = "PreviewEmail"

    sealed interface PreviewEmailResult {
        data object Success : PreviewEmailResult
        data class Failure(val message: UiMessage) : PreviewEmailResult
    }

    suspend fun sendPreviewEmail(
        toEmail: String,
        attachFirstPdf: Boolean,
    ): PreviewEmailResult {
        val trimmedEmail = toEmail.trim()
        if (trimmedEmail.isBlank()) {
            logWarn(logTag, "Preview email aborted: missing recipient")
            return PreviewEmailResult.Failure(UiMessage(Res.string.common_error_email_required))
        }

        val settingsState = settingsRepository.state.value
        val smtpState = settingsState.smtp
        val smtpSettings = smtpState.toSmtpSettings()
            ?: run {
                logWarn(logTag, "Preview email aborted: incomplete SMTP settings")
                return PreviewEmailResult.Failure(UiMessage(Res.string.common_error_smtp_incomplete))
            }
        if (!smtpState.isAuthenticated) {
            logWarn(logTag, "Preview email aborted: SMTP not authenticated")
            return PreviewEmailResult.Failure(UiMessage(Res.string.common_error_smtp_auth_required))
        }

        val (attachmentPath, attachmentName) = if (attachFirstPdf) {
            val conversionState = pdfConversionProgressRepository.state.value
            val outputDir = conversionState.outputDir
            val docIdStart = conversionState.docIdStart
            if (outputDir.isBlank()) {
                logWarn(logTag, "Preview email aborted: missing output directory")
                return PreviewEmailResult.Failure(UiMessage(Res.string.common_error_output_dir_missing))
            }
            if (docIdStart == null) {
                logWarn(logTag, "Preview email aborted: missing doc id start")
                return PreviewEmailResult.Failure(UiMessage(Res.string.common_error_doc_id_missing))
            }
            val filename = "${docIdStart}.pdf"
            joinPath(outputDir, filename) to filename
        } else {
            null to null
        }

        settingsRepository.setPreviewEmail(trimmedEmail)
        settingsRepository.save()
        logInfo(logTag, "Saved preview email recipient")

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
            logInfo(logTag, "Sending preview email to $trimmedEmail attachFirstPdf=$attachFirstPdf")
            var failure: Exception? = null
            var successCount = 0
            withContext(Dispatchers.IO) {
                emailGateway.sendBatch(
                    settings = smtpSettings,
                    requests = listOf(request),
                    onSending = {},
                    onSuccess = { successCount++ },
                    onFailure = { _, exception -> failure = exception },
                    isCancelRequested = { false },
                )
            }
            failure?.let { throw it }
            check(successCount == 1)
            logInfo(logTag, "Preview email sent successfully")
            PreviewEmailResult.Success
        }.getOrElse {
            logWarn(logTag, "Preview email failed")
            PreviewEmailResult.Failure(UiMessage(Res.string.email_preview_error_send_failed))
        }
    }
}
