package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildCertificateReplacementsUseCaseTest {

    private val useCase = BuildCertificateReplacementsUseCase()

    @Test
    fun buildsExpectedTemplateReplacementMap() {
        val request = GenerateCertificatesRequest(
            templatePath = "template.docx",
            entries = emptyList(),
            accreditedId = "IVP-10",
            docIdStart = "100",
            accreditedType = "seminar",
            accreditedHours = "8",
            certificateName = "Music Workshop",
            lector = "Jane Doe",
            lectorGender = "Lecturer:",
            outputDirectory = "",
        )
        val entry = RegistrationEntry(
            date = LocalDateTime(2026, 3, 26, 10, 0),
            formattedDate = "2026-03-26",
            primaryEmail = "ada@example.com",
            name = "Ada",
            surname = "Lovelace",
            institution = "CMM",
            forEvent = "Workshop",
            publicityApproval = "yes",
        )

        val replacements = useCase(request, entry, docId = 123)

        assertEquals("Ada Lovelace", replacements["{{vardas_pavarde}}"])
        assertEquals("2026-03-26", replacements["{{data}}"])
        assertEquals("123", replacements["{{dokumento_id}}"])
        assertEquals("Music Workshop", replacements["{{sertifikato_pavadinimas}}"])
    }
}
