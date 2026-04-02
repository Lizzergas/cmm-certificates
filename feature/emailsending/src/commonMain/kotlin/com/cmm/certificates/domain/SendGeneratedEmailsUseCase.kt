package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.emailsending.domain.EmailTemplateVariables
import com.cmm.certificates.feature.emailsending.domain.buildEmailHtmlBody
import com.cmm.certificates.feature.emailsending.domain.renderEmailTemplate
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository

class SendGeneratedEmailsUseCase(
    private val emailProgressRepository: EmailProgressRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val buildEmailRequests: BuildEmailRequestsUseCase,
    private val sendEmailRequests: SendEmailRequestsUseCase,
) {
    private val logTag = "SendGeneratedEmails"

    suspend operator fun invoke() {
        val conversionState = pdfConversionProgressRepository.state.value
        val docIdStart = conversionState.docIdStart
        if (docIdStart == null) {
            logWarn(logTag, "Email send aborted: missing document id start")
            emailProgressRepository.fail(EmailStopReason.DocumentIdMissing)
            return
        }
        if (conversionState.outputDir.isBlank()) {
            logWarn(logTag, "Email send aborted: missing output directory")
            emailProgressRepository.fail(EmailStopReason.OutputDirMissing)
            return
        }
        if (conversionState.entries.isEmpty()) {
            logWarn(logTag, "Email send aborted: no conversion entries available")
            emailProgressRepository.fail(EmailStopReason.NoEntries)
            return
        }

        val settingsState = settingsRepository.state.value
        val templateVariables = EmailTemplateVariables(
            feedbackUrl = conversionState.feedbackUrl,
        )
        val resolvedBody = renderEmailTemplate(
            text = settingsState.email.body,
            variables = templateVariables,
        )
        val htmlBody = buildEmailHtmlBody(
            body = resolvedBody,
            signatureHtml = settingsState.email.signatureHtml,
        )
        val requests = buildEmailRequests(
            entries = conversionState.entries,
            docIdStart = docIdStart,
            outputDir = conversionState.outputDir,
            certificateName = conversionState.certificateName,
            subject = settingsState.email.subject,
            body = resolvedBody,
            htmlBody = htmlBody,
        )
        if (requests.isEmpty()) {
            logWarn(logTag, "Email send aborted: no email requests were built")
            emailProgressRepository.fail(EmailStopReason.NoEmailsToSend)
            return
        }

        logInfo(logTag, "Built ${requests.size} email requests for outputDir=${conversionState.outputDir}")
        sendEmailRequests(requests)
    }
}
