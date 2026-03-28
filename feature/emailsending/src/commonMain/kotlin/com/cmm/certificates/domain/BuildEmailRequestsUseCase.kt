package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.joinPath

class BuildEmailRequestsUseCase {
    operator fun invoke(
        entries: List<RegistrationEntry>,
        docIdStart: Long,
        outputDir: String,
        subject: String,
        body: String,
        htmlBody: String?,
    ): List<EmailSendRequest> {
        return entries.mapIndexed { index, entry ->
            val email = entry.primaryEmail.trim()
            val fullName = listOf(entry.name, entry.surname)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { email }
            val docId = docIdStart + index

            EmailSendRequest(
                toEmail = email,
                toName = fullName,
                subject = subject,
                body = body,
                htmlBody = htmlBody,
                attachmentPath = joinPath(outputDir, "$docId.pdf"),
                attachmentName = "$docId.pdf",
            )
        }
    }
}
