package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.domain.BuildCertificateReplacementsUseCase
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.PreviewCertificateUseCase
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
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewCertificateUseCaseTest {

    @Test
    fun returnsPreviewPathForFirstEntry() = runTest {
        val documentGenerator = FakeCertificateDocumentGenerator()
        val useCase = PreviewCertificateUseCase(
            buildCertificateReplacements = BuildCertificateReplacementsUseCase(),
            documentGenerator = documentGenerator,
        )

        val previewPath = useCase(
            GenerateCertificatesRequest(
                configuration = defaultCertificateConfiguration(),
                templatePath = "template.docx",
                entries = listOf(
                    sampleEntry(name = "Ada", surname = "Lovelace"),
                    sampleEntry(name = "Grace", surname = "Hopper"),
                ),
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
        )

        assertEquals("/tmp/preview.pdf", previewPath)
        assertEquals("template.docx", documentGenerator.loadedTemplatePath)
        assertEquals("Ada", documentGenerator.previewReplacements["{{vardas}}"])
        assertEquals("Lovelace", documentGenerator.previewReplacements["{{pavarde}}"])
        assertEquals("100", documentGenerator.previewReplacements["{{dokumento_id}}"])
        assertTrue(documentGenerator.fillTemplateCalled.not())
    }

    private fun sampleEntry(name: String, surname: String) = RegistrationEntry(
        primaryEmail = "preview@example.com",
        name = name,
        surname = surname,
        institution = "CMM",
        forEvent = "Workshop",
        publicityApproval = "yes",
        fieldValues = mapOf(
            NameFieldId to name,
            SurnameFieldId to surname,
        ),
    )
}

private class FakeCertificateDocumentGenerator : CertificateDocumentGenerator {
    var loadedTemplatePath: String = ""
    var previewReplacements: Map<String, String> = emptyMap()
    var fillTemplateCalled: Boolean = false

    override fun loadTemplate(path: String): ByteArray {
        loadedTemplatePath = path
        return byteArrayOf(1, 2, 3)
    }

    override fun inspectTemplatePlaceholders(path: String): Set<String> = emptySet()

    override fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        fillTemplateCalled = true
    }

    override fun createPreviewPdf(
        templateBytes: ByteArray,
        replacements: Map<String, String>,
    ): String {
        previewReplacements = replacements
        return "/tmp/preview.pdf"
    }
}
