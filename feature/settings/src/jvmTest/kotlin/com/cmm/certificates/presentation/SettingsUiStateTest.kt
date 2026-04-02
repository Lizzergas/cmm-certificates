package com.cmm.certificates.presentation

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_send_logs_success
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SettingsUiStateTest {

    @Test
    fun buildSettingsUiState_treatsBlankDirectoriesAsWritableWithoutFilesystemProbe() {
        val uiState = buildSettingsUiState(
            settings = SettingsState(),
            sentToday = 5,
            supportsEmailSending = true,
            supportsLogSubmission = true,
            defaultOutputDirectory = "",
            sentHistory = emptyList(),
            cachedEmails = CachedEmailBatch(lastReason = EmailStopReason.NetworkUnavailable),
            installationDirectoryPath = null,
            legalResourcesDirectoryPath = null,
            appVersionName = null,
            appCommitHash = null,
            isNetworkAvailable = true,
            isSendingLogs = false,
            logSubmissionMessage = UiMessage(Res.string.settings_send_logs_success),
            isLogSubmissionSuccess = true,
            notification = null,
        )

        assertTrue(uiState.isOutputDirectoryWritable)
        assertEquals("", uiState.resolvedOutputDirectory)
        assertEquals(EmailStopReason.NetworkUnavailable, uiState.cachedLastReason)
        assertTrue(uiState.canSendLogs)
    }

    @Test
    fun buildSettingsUiState_marksInstallationDefaultOnlyWhenNoCustomOutputDirectoryIsSet() {
        val writableDirectory = Files.createTempDirectory("settings-ui-state").toFile().absolutePath

        val defaultUiState = buildSettingsUiState(
            settings = SettingsState(),
            sentToday = 0,
            supportsEmailSending = true,
            supportsLogSubmission = false,
            defaultOutputDirectory = writableDirectory,
            sentHistory = emptyList(),
            cachedEmails = CachedEmailBatch(),
            installationDirectoryPath = writableDirectory,
            legalResourcesDirectoryPath = writableDirectory,
            appVersionName = "1.0.0",
            appCommitHash = "abc123",
            isNetworkAvailable = false,
            isSendingLogs = false,
            logSubmissionMessage = null,
            isLogSubmissionSuccess = false,
            notification = null,
        )

        val customUiState = buildSettingsUiState(
            settings = SettingsState(
                certificate = CertificateSettingsState(outputDirectory = writableDirectory),
            ),
            sentToday = 0,
            supportsEmailSending = true,
            supportsLogSubmission = false,
            defaultOutputDirectory = writableDirectory,
            sentHistory = emptyList(),
            cachedEmails = CachedEmailBatch(),
            installationDirectoryPath = writableDirectory,
            legalResourcesDirectoryPath = writableDirectory,
            appVersionName = null,
            appCommitHash = null,
            isNetworkAvailable = false,
            isSendingLogs = false,
            logSubmissionMessage = null,
            isLogSubmissionSuccess = false,
            notification = null,
        )

        assertTrue(defaultUiState.outputDirectoryUsesInstallationDefault)
        assertTrue(defaultUiState.canOpenInstallationDirectory)
        assertTrue(defaultUiState.canOpenLegalResourcesDirectory)
        assertFalse(customUiState.outputDirectoryUsesInstallationDefault)
        assertTrue(customUiState.hasCustomOutputDirectory)
        assertTrue(customUiState.isOutputDirectoryWritable)
    }
}
