package com.cmm.certificates.presentation

import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import com.cmm.certificates.feature.settings.domain.AppearanceSettingsState
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState

data class SettingsUiState(
    val smtp: SmtpSettingsState = SmtpSettingsState(),
    val email: EmailTemplateSettingsState = EmailTemplateSettingsState(),
    val certificate: CertificateSettingsState = CertificateSettingsState(),
    val appearance: AppearanceSettingsState = AppearanceSettingsState(),
    val defaultOutputDirectory: String = "",
    val sentToday: Int = 0,
    val sentHistory: List<SentEmailRecord> = emptyList(),
    val cachedEmails: List<CachedEmailEntry> = emptyList(),
    val cachedLastReason: EmailStopReason? = null,
    val installationDirectoryPath: String? = null,
    val legalResourcesDirectoryPath: String? = null,
    val appVersionName: String? = null,
    val appCommitHash: String? = null,
    val isOutputDirectoryWritable: Boolean = true,
    val outputDirectoryUsesInstallationDefault: Boolean = false,
    val supportsEmailSending: Boolean = true,
    val supportsLogSubmission: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val isSendingLogs: Boolean = false,
    val logSubmissionMessage: UiMessage? = null,
    val isLogSubmissionSuccess: Boolean = false,
    val notification: SettingsNotificationState? = null,
) {
    val resolvedOutputDirectory: String
        get() = certificate.outputDirectory.ifBlank { defaultOutputDirectory }

    val hasCustomOutputDirectory: Boolean
        get() = certificate.outputDirectory.isNotBlank()

    val canOpenInstallationDirectory: Boolean
        get() = !installationDirectoryPath.isNullOrBlank()

    val canOpenLegalResourcesDirectory: Boolean
        get() = !legalResourcesDirectoryPath.isNullOrBlank()

    val shouldShowOutputDirectoryWarning: Boolean
        get() = hasCustomOutputDirectory && !isOutputDirectoryWritable

    val canSendLogs: Boolean
        get() = supportsLogSubmission && isNetworkAvailable && !isSendingLogs
}

data class SettingsNotificationState(
    val id: Long,
    val message: UiMessage,
)

internal fun buildSettingsUiState(
    settings: SettingsState,
    sentToday: Int,
    supportsEmailSending: Boolean,
    supportsLogSubmission: Boolean,
    defaultOutputDirectory: String,
    sentHistory: List<SentEmailRecord>,
    cachedEmails: CachedEmailBatch,
    installationDirectoryPath: String?,
    legalResourcesDirectoryPath: String?,
    appVersionName: String?,
    appCommitHash: String?,
    isNetworkAvailable: Boolean,
    isSendingLogs: Boolean,
    logSubmissionMessage: UiMessage?,
    isLogSubmissionSuccess: Boolean,
    notification: SettingsNotificationState?,
): SettingsUiState {
    return SettingsUiState(
        smtp = settings.smtp,
        email = settings.email,
        certificate = settings.certificate,
        appearance = settings.appearance,
        defaultOutputDirectory = defaultOutputDirectory,
        sentToday = sentToday,
        sentHistory = sentHistory,
        cachedEmails = cachedEmails.entries,
        cachedLastReason = cachedEmails.lastReason,
        installationDirectoryPath = installationDirectoryPath,
        legalResourcesDirectoryPath = legalResourcesDirectoryPath,
        appVersionName = appVersionName,
        appCommitHash = appCommitHash,
        isOutputDirectoryWritable = if (defaultOutputDirectory.isBlank() && settings.certificate.outputDirectory.isBlank()) {
            true
        } else {
            OutputDirectory.canWrite(settings.certificate.outputDirectory.ifBlank { defaultOutputDirectory })
        },
        outputDirectoryUsesInstallationDefault = settings.certificate.outputDirectory.isBlank() &&
                installationDirectoryPath == defaultOutputDirectory,
        supportsEmailSending = supportsEmailSending,
        supportsLogSubmission = supportsLogSubmission,
        isNetworkAvailable = isNetworkAvailable,
        isSendingLogs = isSendingLogs,
        logSubmissionMessage = logSubmissionMessage,
        isLogSubmissionSuccess = isLogSubmissionSuccess,
        notification = notification,
    )
}
