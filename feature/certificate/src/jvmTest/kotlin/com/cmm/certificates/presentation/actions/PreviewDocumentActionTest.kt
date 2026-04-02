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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PreviewDocumentActionTest {

    private val configuration = defaultCertificateConfiguration()
    private val builder = GenerateCertificatesRequestBuilder(
        defaultOutputDirectory = Files.createTempDirectory("preview-action").toString(),
        installationDirectoryPath = null,
    )

    @Test
    fun execute_returnsInAppPreviewWhenConfigured() = runTest {
        var openCalled = false
        val action = PreviewDocumentAction(
            requestBuilder = builder,
            previewDocument = { "/tmp/preview.pdf" },
            openFile = {
                openCalled = true
                true
            },
        )

        val result = action.execute(
            snapshot = sampleState(),
            settings = SettingsState(),
            useInAppPdfPreview = true,
        )

        assertEquals(PreviewDocumentResult.ShowInApp("/tmp/preview.pdf"), result)
        assertFalse(openCalled)
    }

    @Test
    fun execute_attemptsExternalOpenWhenInAppPreviewDisabled() = runTest {
        var openedPath: String? = null
        var capturedRequest: GenerateCertificatesRequest? = null
        val action = PreviewDocumentAction(
            requestBuilder = builder,
            previewDocument = { request ->
                capturedRequest = request
                "/tmp/preview.pdf"
            },
            openFile = {
                openedPath = it
                true
            },
        )

        val result = action.execute(
            snapshot = sampleState(),
            settings = SettingsState(),
            useInAppPdfPreview = false,
        )

        assertEquals(PreviewDocumentResult.NoPreview, result)
        assertEquals("/tmp/preview.pdf", openedPath)
        assertEquals("template.docx", capturedRequest?.templatePath)
    }

    @Test
    fun execute_returnsFailureWhenExternalOpenFails() = runTest {
        val action = PreviewDocumentAction(
            requestBuilder = builder,
            previewDocument = { "/tmp/preview.pdf" },
            openFile = { false },
        )

        val result = action.execute(
            snapshot = sampleState(),
            settings = SettingsState(),
            useInAppPdfPreview = false,
        )

        assertTrue(result is PreviewDocumentResult.ExternalOpenFailed)
        result as PreviewDocumentResult.ExternalOpenFailed
        assertEquals("/tmp/preview.pdf", result.path)
    }

    private fun sampleState() = ConversionUiState(
        configuration = configuration,
        files = ConversionFilesState(templatePath = "template.docx"),
        form = ConversionFormState(
            manualValues = mapOf(
                configuration.documentNumberTag to "42",
                CertificateNameFieldId to "Sertifikatas",
            ),
        ),
        entries = listOf(
            RegistrationEntry(
                primaryEmail = "user@example.com",
                name = "Jonas",
                surname = "Jonaitis",
                institution = "CMM",
                forEvent = "Renginys",
                publicityApproval = "yes",
            ),
        ),
    )
}
