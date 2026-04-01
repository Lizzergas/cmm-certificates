package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.domain.BuildCertificateReplacementsUseCase
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.PreviewCertificateUseCase
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
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
                templatePath = "template.docx",
                entries = listOf(
                    sampleEntry(name = "Ada", surname = "Lovelace"),
                    sampleEntry(name = "Grace", surname = "Hopper"),
                ),
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
        )

        assertEquals("/tmp/preview.pdf", previewPath)
        assertEquals("template.docx", documentGenerator.loadedTemplatePath)
        assertEquals("Ada Lovelace", documentGenerator.previewReplacements["{{vardas_pavarde}}"])
        assertEquals("100", documentGenerator.previewReplacements["{{dokumento_id}}"])
        assertTrue(documentGenerator.fillTemplateCalled.not())
    }

    private fun sampleEntry(name: String, surname: String) = RegistrationEntry(
        date = LocalDateTime(2026, 3, 26, 10, 0),
        formattedDate = "2026-03-26",
        primaryEmail = "preview@example.com",
        name = name,
        surname = surname,
        institution = "CMM",
        forEvent = "Workshop",
        publicityApproval = "yes",
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
