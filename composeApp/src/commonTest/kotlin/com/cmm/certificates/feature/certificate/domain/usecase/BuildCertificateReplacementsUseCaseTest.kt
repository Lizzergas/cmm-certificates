package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.domain.BuildCertificateReplacementsUseCase
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.config.AccreditedHoursFieldId
import com.cmm.certificates.domain.config.AccreditedIdFieldId
import com.cmm.certificates.domain.config.AccreditedTypeFieldId
import com.cmm.certificates.domain.config.CertificateDateFieldId
import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.domain.config.DocumentIdFieldId
import com.cmm.certificates.domain.config.LectorFieldId
import com.cmm.certificates.domain.config.LectorLabelFieldId
import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildCertificateReplacementsUseCaseTest {

    private val useCase = BuildCertificateReplacementsUseCase()

    @Test
    fun buildsExpectedTemplateReplacementMap() {
        val request = GenerateCertificatesRequest(
            configuration = defaultCertificateConfiguration(),
            templatePath = "template.docx",
            entries = emptyList(),
            manualValues = mapOf(
                CertificateDateFieldId to "2026-03-26",
                AccreditedIdFieldId to "IVP-10",
                DocumentIdFieldId to "100",
                AccreditedTypeFieldId to "seminar",
                AccreditedHoursFieldId to "8",
                CertificateNameFieldId to "Music Workshop",
                LectorFieldId to "Jane Doe",
                LectorLabelFieldId to "Lecturer:",
            ),
            docIdStart = "100",
            certificateName = "Music Workshop",
            feedbackUrl = "",
            outputDirectory = "",
        )
        val entry = RegistrationEntry(
            primaryEmail = "ada@example.com",
            name = "Ada",
            surname = "Lovelace",
            institution = "CMM",
            forEvent = "Workshop",
            publicityApproval = "yes",
            fieldValues = mapOf(
                NameFieldId to "Ada",
                SurnameFieldId to "Lovelace",
            ),
        )

        val replacements = useCase(request, entry, docId = 123)

        assertEquals("Ada", replacements["{{vardas}}"])
        assertEquals("Lovelace", replacements["{{pavarde}}"])
        assertEquals("2026 m. kovo 26 d.", replacements["{{data}}"])
        assertEquals("123", replacements["{{dokumento_id}}"])
        assertEquals("Music Workshop", replacements["{{sertifikato_pavadinimas}}"])
    }
}
