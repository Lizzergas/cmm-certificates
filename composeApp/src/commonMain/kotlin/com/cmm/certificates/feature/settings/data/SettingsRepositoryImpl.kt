package com.cmm.certificates.feature.settings.data

import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.email.SmtpSettings
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.ui.SettingsUiState
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
) : SettingsRepository {
    private val _state = MutableStateFlow(SettingsState())
    override val state: StateFlow<SettingsState> = _state
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
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

    override fun setAccreditedTypeOptions(value: String) {
        _state.update { current ->
            current.copy(
                smtp = current.smtp.copy(errorMessage = null),
                certificate = current.certificate.copy(accreditedTypeOptions = value),
            )
        }
    }

    override suspend fun resetAndClear() {
        _state.value = SettingsState()
        settingsStore.clear()
    }

    override suspend fun save() {
        settingsStore.save(_state.value.toStoredSettings())
    }

    override suspend fun authenticate(): Boolean {
        val snapshot = _state.value
        val smtpSnapshot = snapshot.smtp
        val settings = smtpSnapshot.toSmtpSettings()
        if (settings == null || !smtpSnapshot.canAuthenticate) {
            updateSmtp { it.copy(errorMessage = "SMTP details are incomplete.") }
            return false
        }
        updateSmtp { it.copy(isAuthenticating = true, errorMessage = null) }
        return try {
            withContext(Dispatchers.IO) {
                SmtpClient.testConnection(settings)
            }
            settingsStore.save(_state.value.toStoredSettings())
            updateSmtp { it.copy(isAuthenticated = true, isAuthenticating = false) }
            true
        } catch (e: Exception) {
            updateSmtp {
                it.copy(
                    isAuthenticated = false,
                    isAuthenticating = false,
                    errorMessage = e.message ?: "SMTP authentication failed.",
                )
            }
            false
        }
    }

    private suspend fun loadFromStore() {
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
                ),
                certificate = current.certificate.copy(
                    accreditedTypeOptions = stored.accreditedTypeOptions,
                ),
            )
        }
        if (_state.value.smtp.canAuthenticate) {
            authenticate()
        }
    }

    private fun updateSmtp(block: (SmtpSettingsState) -> SmtpSettingsState) {
        _state.update { current -> current.copy(smtp = block(current.smtp)) }
    }
}


data class SmtpSettingsState(
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val transport: SmtpTransport = SmtpTransport.SMTPS,
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
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
    val subject: String = SettingsStore.DEFAULT_EMAIL_SUBJECT,
    val body: String = SettingsStore.DEFAULT_EMAIL_BODY,
    val signatureHtml: String = SettingsStore.DEFAULT_SIGNATURE_HTML,
)

data class CertificateSettingsState(
    val accreditedTypeOptions: String = SettingsStore.DEFAULT_ACCREDITED_TYPE_OPTIONS,
)

data class SettingsState(
    val smtp: SmtpSettingsState = SmtpSettingsState(),
    val email: EmailTemplateSettingsState = EmailTemplateSettingsState(),
    val certificate: CertificateSettingsState = CertificateSettingsState(),
) {
    fun toStoredSettings(): SettingsStore.StoredSettings {
        return SettingsStore.StoredSettings(
            host = smtp.host.trim(),
            port = smtp.port.trim(),
            username = smtp.username.trim(),
            password = smtp.password,
            transport = smtp.transport,
            subject = email.subject,
            body = email.body,
            accreditedTypeOptions = certificate.accreditedTypeOptions,
            signatureHtml = email.signatureHtml,
        )
    }

    fun toUiState(): SettingsUiState {
        return SettingsUiState(
            smtp = smtp,
            email = email,
            certificate = certificate,
        )
    }
}
