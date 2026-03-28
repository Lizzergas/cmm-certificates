package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildEmailRequestsUseCaseTest {

    private val useCase = BuildEmailRequestsUseCase()

    @Test
    fun buildsSequentialAttachmentNamesAndFullNames() {
        val requests = useCase(
            entries = listOf(
                registrationEntry("ada@example.com", "Ada", "Lovelace"),
                registrationEntry("grace@example.com", "Grace", "Hopper"),
            ),
            docIdStart = 120,
            outputDir = "/tmp/output",
            certificateName = "Masterclass",
            subject = "Subject",
            body = "Body",
            htmlBody = "<p>Body</p>",
        )

        assertEquals("Ada Lovelace", requests[0].toName)
        assertEquals("Masterclass", requests[0].certificateName)
        assertEquals("/tmp/output/120.pdf", requests[0].attachmentPath)
        assertEquals("120.pdf", requests[0].attachmentName)
        assertEquals("Grace Hopper", requests[1].toName)
        assertEquals("/tmp/output/121.pdf", requests[1].attachmentPath)
    }

    @Test
    fun fallsBackToEmailWhenNameIsBlank() {
        val requests = useCase(
            entries = listOf(registrationEntry("anon@example.com", "", "")),
            docIdStart = 9,
            outputDir = "/tmp/output",
            certificateName = "Masterclass",
            subject = "Subject",
            body = "Body",
            htmlBody = null,
        )

        assertEquals("anon@example.com", requests.single().toName)
    }

    @Test
    fun trimsEmailsAndPreservesPayloadFields() {
        val requests = useCase(
            entries = listOf(registrationEntry("  trim@example.com  ", "Trim", "User")),
            docIdStart = 42,
            outputDir = "/tmp/output",
            certificateName = "Masterclass",
            subject = "Subject line",
            body = "Body line",
            htmlBody = "<p>Body line</p>",
        )

        val request = requests.single()
        assertEquals("trim@example.com", request.toEmail)
        assertEquals("Masterclass", request.certificateName)
        assertEquals("Subject line", request.subject)
        assertEquals("Body line", request.body)
        assertEquals("<p>Body line</p>", request.htmlBody)
    }

    private fun registrationEntry(email: String, name: String, surname: String): RegistrationEntry {
        return RegistrationEntry(
            date = LocalDateTime(2026, 3, 26, 10, 0),
            formattedDate = "2026-03-26",
            primaryEmail = email,
            name = name,
            surname = surname,
            institution = "CMM",
            forEvent = "Workshop",
            publicityApproval = "yes",
        )
    }
}
