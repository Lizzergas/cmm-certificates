package com.cmm.certificates.presentation.actions

import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.ConversionFormState
import com.cmm.certificates.presentation.ConversionUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class GenerateCertificatesRequestBuilderTest {

    @Test
    fun build_usesCurrentUiStateValuesAndDefaultOutputDirectory() {
        val configuration = defaultCertificateConfiguration()
        val defaultOutputDirectory = Files.createTempDirectory("conversion-default-output").toString()
        val builder = GenerateCertificatesRequestBuilder(
            defaultOutputDirectory = defaultOutputDirectory,
            installationDirectoryPath = null,
        )
        val entry = RegistrationEntry(
            primaryEmail = "user@example.com",
            name = "Jonas",
            surname = "Jonaitis",
            institution = "CMM",
            forEvent = "Mokymai",
            publicityApproval = "yes",
        )

        val request = builder.build(
            snapshot = ConversionUiState(
                configuration = configuration,
                files = ConversionFilesState(
                    templatePath = "/tmp/template.docx",
                ),
                form = ConversionFormState(
                    manualValues = mapOf(
                        configuration.documentNumberTag to "100",
                        CertificateNameFieldId to "Akreditacija",
                        "papildomas" to "reiksme",
                    ),
                    feedbackUrl = "https://example.com/feedback",
                ),
                entries = listOf(entry),
            ),
            settings = SettingsState(),
        )

        assertEquals(configuration, request.configuration)
        assertEquals("/tmp/template.docx", request.templatePath)
        assertEquals(listOf(entry), request.entries)
        assertEquals("100", request.docIdStart)
        assertEquals("Akreditacija", request.certificateName)
        assertEquals("https://example.com/feedback", request.feedbackUrl)
        assertEquals("reiksme", request.manualValues["papildomas"])
        assertEquals(defaultOutputDirectory, request.outputDirectory)
    }

    @Test
    fun effectiveOutputDirectory_resetsLegacyInstallationPathToDefault() {
        val defaultOutputDirectory = Files.createTempDirectory("conversion-fallback-output").toString()
        val installationFile = Files.createTempFile("legacy-install-path", ".tmp").toFile().absolutePath
        val builder = GenerateCertificatesRequestBuilder(
            defaultOutputDirectory = defaultOutputDirectory,
            installationDirectoryPath = installationFile,
        )

        val resolved = builder.effectiveOutputDirectory(
            settings = SettingsState(
                certificate = CertificateSettingsState(outputDirectory = installationFile),
            ),
        )

        assertEquals(defaultOutputDirectory, resolved)
    }
}
