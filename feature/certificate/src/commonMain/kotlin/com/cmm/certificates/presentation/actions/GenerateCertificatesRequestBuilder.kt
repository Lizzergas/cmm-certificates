package com.cmm.certificates.presentation.actions

import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.presentation.ConversionUiState
import com.cmm.certificates.shouldResetLegacyInstallOutputDirectory

internal class GenerateCertificatesRequestBuilder(
    private val defaultOutputDirectory: String,
    private val installationDirectoryPath: String?,
) {
    fun build(snapshot: ConversionUiState, settings: SettingsState): GenerateCertificatesRequest {
        return GenerateCertificatesRequest(
            configuration = snapshot.configuration,
            templatePath = snapshot.files.templatePath,
            entries = snapshot.entries,
            manualValues = snapshot.form.manualValues,
            docIdStart = snapshot.form.valueFor(snapshot.configuration.documentNumberTag),
            certificateName = snapshot.form.valueFor(CertificateNameFieldId),
            feedbackUrl = snapshot.form.feedbackUrl,
            outputDirectory = effectiveOutputDirectory(settings),
        )
    }

    internal fun effectiveOutputDirectory(settings: SettingsState): String {
        val configuredOutputDirectory = settings.certificate.outputDirectory.trim()
        return when {
            configuredOutputDirectory.isBlank() -> defaultOutputDirectory
            shouldResetLegacyInstallOutputDirectory(
                configuredOutputDirectory = configuredOutputDirectory,
                installationDirectoryPath = installationDirectoryPath,
            ) -> defaultOutputDirectory

            OutputDirectory.canWrite(configuredOutputDirectory) -> configuredOutputDirectory
            else -> configuredOutputDirectory
        }
    }
}
