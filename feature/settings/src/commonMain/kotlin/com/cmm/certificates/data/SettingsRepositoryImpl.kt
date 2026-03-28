package com.cmm.certificates.data

import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.core.presentation.UiMessage
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_error_smtp_incomplete
import certificates.composeapp.generated.resources.settings_error_auth_failed
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val settingsStore: SettingsStore,
    private val emailGateway: EmailGateway,
    capabilityProvider: PlatformCapabilityProvider,
) : SettingsRepository {
    private val logTag = "SettingsRepo"
    private val _state = MutableStateFlow(defaultSettingsState())
    override val state: StateFlow<SettingsState> = _state
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supportsEmailSending = capabilityProvider.capabilities.canSendEmails

    init {
        logInfo(logTag, "Initializing settings repository")
        scope.launch { loadFromStore() }
    }

    override fun setHost(value: String) {
        updateSmtp { it.copy(host = value, isAuthenticated = false, errorMessage = null) }
    }

    override fun setPort(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        updateSmtp { it.copy(port = sanitized, isAuthenticated = false, errorMessage = null) }
    }

    override fun setUsername(value: String) {
        updateSmtp { it.copy(username = value, isAuthenticated = false, errorMessage = null) }
    }

    override fun setPassword(value: String) {
        updateSmtp { it.copy(password = value, isAuthenticated = false, errorMessage = null) }
    }

    override fun setTransport(value: SmtpTransport) {
        updateSmtp { it.copy(transport = value, isAuthenticated = false, errorMessage = null) }
    }

    override fun setSubject(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                email = current.email.copy(subject = value),
            )
        }
    }

    override fun setBody(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                email = current.email.copy(body = value),
            )
        }
    }

    override fun setSignatureHtml(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                email = current.email.copy(signatureHtml = value),
            )
        }
    }

    override fun setPreviewEmail(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                email = current.email.copy(previewEmail = value),
            )
        }
    }

    override fun setAccreditedTypeOptions(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                certificate = current.certificate.copy(accreditedTypeOptions = value),
            )
        }
    }

    override fun setOutputDirectory(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                certificate = current.certificate.copy(outputDirectory = value),
            )
        }
    }

    override fun setDailyLimit(value: Int) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                email = current.email.copy(dailyLimit = value),
            )
        }
    }

    override fun setThemeMode(value: AppThemeMode) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                appearance = current.appearance.copy(themeMode = value),
            )
        }
    }

    override suspend fun resetAndClear() {
        logWarn(logTag, "Resetting settings state and clearing persistent store")
        _state.value = defaultSettingsState()
        settingsStore.clear()
    }

    override suspend fun save() {
        logInfo(logTag, "Saving settings state")
        settingsStore.save(_state.value.toStoredSettings())
    }

    override suspend fun authenticate(): Boolean {
        if (!supportsEmailSending) {
            logWarn(logTag, "Skipping SMTP authentication because email sending is unsupported")
            updateSmtp { it.copy(isAuthenticated = false, isAuthenticating = false) }
            return false
        }
        val snapshot = _state.value
        val smtpSnapshot = snapshot.smtp
        val settings = smtpSnapshot.toSmtpSettings()
        if (settings == null || !smtpSnapshot.canAuthenticate) {
            logWarn(logTag, "SMTP authentication aborted because settings are incomplete")
            updateSmtp { it.copy(errorMessage = UiMessage(Res.string.common_error_smtp_incomplete)) }
            return false
        }
        updateSmtp { it.copy(isAuthenticating = true, errorMessage = null) }
        return try {
            logInfo(logTag, "Testing SMTP connection for host=${settings.host} port=${settings.port}")
            withContext(Dispatchers.IO) {
                emailGateway.testConnection(settings)
            }
            settingsStore.save(_state.value.toStoredSettings())
            updateSmtp { it.copy(isAuthenticated = true, isAuthenticating = false) }
            logInfo(logTag, "SMTP authentication succeeded")
            true
        } catch (e: Exception) {
            logError(logTag, "SMTP authentication failed", e)
            updateSmtp {
                it.copy(
                    isAuthenticated = false,
                    isAuthenticating = false,
                    errorMessage = UiMessage(Res.string.settings_error_auth_failed),
                )
            }
            false
        }
    }

    private suspend fun loadFromStore() {
        logInfo(logTag, "Loading settings from store")
        val stored = settingsStore.loadOrDefault()
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(
                    host = stored.host,
                    port = stored.port,
                    username = stored.username,
                    password = stored.password,
                    transport = stored.transport,
                    isAuthenticated = false,
                    isAuthenticating = false,
                    errorMessage = null,
                ),
                email = current.email.copy(
                    subject = stored.subject,
                    body = stored.body,
                    signatureHtml = stored.signatureHtml,
                    previewEmail = stored.previewEmail,
                    dailyLimit = stored.dailyLimit,
                ),
                certificate = current.certificate.copy(
                    accreditedTypeOptions = stored.accreditedTypeOptions,
                    outputDirectory = stored.outputDirectory,
                ),
                appearance = current.appearance.copy(
                    themeMode = stored.themeMode,
                ),
            )
        }
        if (supportsEmailSending && _state.value.smtp.canAuthenticate) {
            logInfo(logTag, "Attempting automatic SMTP authentication from persisted settings")
            authenticate()
        } else {
            logInfo(logTag, "Loaded settings without automatic SMTP authentication")
        }
    }

    private fun updateSmtp(block: (SmtpSettingsState) -> SmtpSettingsState) {
        _state.update { current -> current.copy(smtp = block(current.smtp)) }
    }
}

private fun SettingsState.toStoredSettings(): SettingsStore.StoredSettings {
    return SettingsStore.StoredSettings(
        host = smtp.host.trim(),
        port = smtp.port.trim(),
        username = smtp.username.trim(),
        password = smtp.password,
        transport = smtp.transport,
        subject = email.subject,
        body = email.body,
        accreditedTypeOptions = certificate.accreditedTypeOptions,
        outputDirectory = certificate.outputDirectory,
        signatureHtml = email.signatureHtml,
        previewEmail = email.previewEmail,
        dailyLimit = email.dailyLimit,
        themeMode = appearance.themeMode,
    )
}
