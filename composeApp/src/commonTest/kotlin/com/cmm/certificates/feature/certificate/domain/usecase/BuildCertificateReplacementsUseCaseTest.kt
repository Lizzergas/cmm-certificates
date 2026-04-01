package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.domain.BuildCertificateReplacementsUseCase
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildCertificateReplacementsUseCaseTest {

    private val useCase = BuildCertificateReplacementsUseCase()

    @Test
    fun buildsExpectedTemplateReplacementMap() {
        val request = GenerateCertificatesRequest(
            templatePath = "template.docx",
            entries = emptyList(),
            certificateDate = "2026 m. kovo 26 d.",
            accreditedId = "IVP-10",
            docIdStart = "100",
            accreditedType = "seminar",
            accreditedHours = "8",
            certificateName = "Music Workshop",
            feedbackUrl = "",
            lector = "Jane Doe",
            lectorGender = "Lecturer:",
            outputDirectory = "",
        )
        val entry = RegistrationEntry(
            primaryEmail = "ada@example.com",
            name = "Ada",
            surname = "Lovelace",
            institution = "CMM",
            forEvent = "Workshop",
            publicityApproval = "yes",
        )

        val replacements = useCase(request, entry, docId = 123)

        assertEquals("Ada Lovelace", replacements["{{vardas_pavarde}}"])
        assertEquals("2026 m. kovo 26 d.", replacements["{{data}}"])
        assertEquals("123", replacements["{{dokumento_id}}"])
        assertEquals("Music Workshop", replacements["{{sertifikato_pavadinimas}}"])
    }
}
