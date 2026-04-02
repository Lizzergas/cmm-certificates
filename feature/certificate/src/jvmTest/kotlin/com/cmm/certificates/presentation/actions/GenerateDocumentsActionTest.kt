package com.cmm.certificates.presentation.actions

import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.ConversionFormState
import com.cmm.certificates.presentation.ConversionUiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.file.Files

class GenerateDocumentsActionTest {

    @Test
    fun execute_buildsAndForwardsGenerateRequest() = runTest {
        val configuration = defaultCertificateConfiguration()
        val builder = GenerateCertificatesRequestBuilder(
            defaultOutputDirectory = Files.createTempDirectory("generate-action").toString(),
            installationDirectoryPath = null,
        )
        var capturedRequest: GenerateCertificatesRequest? = null
        val action = GenerateDocumentsAction(
            requestBuilder = builder,
            generateDocuments = { request -> capturedRequest = request },
        )

        action.execute(
            snapshot = ConversionUiState(
                configuration = configuration,
                files = ConversionFilesState(templatePath = "template.docx"),
                form = ConversionFormState(
                    manualValues = mapOf(
                        configuration.documentNumberTag to "300",
                        CertificateNameFieldId to "Preview Name",
                    ),
                    feedbackUrl = "https://example.com/feedback",
                ),
                entries = listOf(sampleEntry()),
            ),
            settings = SettingsState(),
        )

        assertNotNull(capturedRequest)
        assertEquals("template.docx", capturedRequest?.templatePath)
        assertEquals("300", capturedRequest?.docIdStart)
        assertEquals("Preview Name", capturedRequest?.certificateName)
        assertEquals("https://example.com/feedback", capturedRequest?.feedbackUrl)
    }

    private fun sampleEntry() = RegistrationEntry(
        primaryEmail = "user@example.com",
        name = "Jonas",
        surname = "Jonaitis",
        institution = "CMM",
        forEvent = "Renginys",
        publicityApproval = "yes",
    )
}
