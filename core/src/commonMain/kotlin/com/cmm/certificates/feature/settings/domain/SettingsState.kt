package com.cmm.certificates.feature.settings.domain

import com.cmm.certificates.core.presentation.UiMessage

enum class AppThemeMode {
    LIGHT,
    DARK,
}

data class SmtpSettingsState(
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val transport: SmtpTransport = SmtpTransport.SMTPS,
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: UiMessage? = null,
) {
    val canAuthenticate: Boolean
        get() = host.isNotBlank() &&
            port.isNotBlank() &&
            username.isNotBlank() &&
            password.isNotBlank()

    fun toSmtpSettings(): SmtpSettings? {
        val portNumber = port.toIntOrNull() ?: return null
        return SmtpSettings(
            host = host.trim(),
            port = portNumber,
            username = username.trim(),
            password = password,
            transport = transport,
        )
    }
}

data class EmailTemplateSettingsState(
    val subject: String = "",
    val body: String = "",
    val signatureHtml: String = "",
    val previewEmail: String = "",
    val dailyLimit: Int = 450,
)

data class CertificateSettingsState(
    val accreditedTypeOptions: String = "",
    val outputDirectory: String = "",
)

data class AppearanceSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val useInAppPdfPreview: Boolean = true,
)

data class SettingsState(
    val smtp: SmtpSettingsState = SmtpSettingsState(),
    val email: EmailTemplateSettingsState = EmailTemplateSettingsState(),
    val certificate: CertificateSettingsState = CertificateSettingsState(),
    val appearance: AppearanceSettingsState = AppearanceSettingsState(),
)
