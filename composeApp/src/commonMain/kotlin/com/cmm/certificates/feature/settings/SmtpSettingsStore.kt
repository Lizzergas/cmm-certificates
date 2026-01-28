package com.cmm.certificates.feature.settings

import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.email.SmtpSettings
import com.cmm.certificates.data.email.SmtpSettingsRepository
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.data.email.StoredSmtpSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SmtpSettingsState(
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val transport: SmtpTransport = SmtpTransport.SMTPS,
    val subject: String = SmtpSettingsRepository.DEFAULT_EMAIL_SUBJECT,
    val body: String = SmtpSettingsRepository.DEFAULT_EMAIL_BODY,
    val accreditedTypeOptions: String = SmtpSettingsRepository.DEFAULT_ACCREDITED_TYPE_OPTIONS,
    val signatureHtml: String = SmtpSettingsRepository.DEFAULT_SIGNATURE_HTML,
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
) {
    val canAuthenticate: Boolean
        get() = host.isNotBlank() &&
                port.isNotBlank() &&
                username.isNotBlank() &&
                password.isNotBlank()

    fun toSettings(): SmtpSettings? {
        val portNumber = port.toIntOrNull() ?: return null
        return SmtpSettings(
            host = host.trim(),
            port = portNumber,
            username = username.trim(),
            password = password,
            transport = transport,
        )
    }

    fun toStoredSettings(): StoredSmtpSettings {
        return StoredSmtpSettings(
            host = host.trim(),
            port = port.trim(),
            username = username.trim(),
            password = password,
            transport = transport,
            subject = subject,
            body = body,
            accreditedTypeOptions = accreditedTypeOptions,
            signatureHtml = signatureHtml,
        )
    }
}

class SmtpSettingsStore(
    private val repository: SmtpSettingsRepository,
) {
    private val _state = MutableStateFlow(SmtpSettingsState())
    val state: StateFlow<SmtpSettingsState> = _state
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { loadFromStore() }
    }

    fun setHost(value: String) {
        _state.update { it.copy(host = value, isAuthenticated = false, errorMessage = null) }
    }

    fun setPort(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        _state.update { it.copy(port = sanitized, isAuthenticated = false, errorMessage = null) }
    }

    fun setUsername(value: String) {
        _state.update { it.copy(username = value, isAuthenticated = false, errorMessage = null) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, isAuthenticated = false, errorMessage = null) }
    }

    fun setTransport(value: SmtpTransport) {
        _state.update { it.copy(transport = value, isAuthenticated = false, errorMessage = null) }
    }

    fun setSubject(value: String) {
        _state.update { it.copy(subject = value, errorMessage = null) }
        persistIfAuthenticated()
    }

    fun setBody(value: String) {
        _state.update { it.copy(body = value, errorMessage = null) }
        persistIfAuthenticated()
    }

    fun setAccreditedTypeOptions(value: String) {
        _state.update { it.copy(accreditedTypeOptions = value, errorMessage = null) }
        persistIfAuthenticated()
    }

    fun setSignatureHtml(value: String) {
        _state.update { it.copy(signatureHtml = value, errorMessage = null) }
        persistIfAuthenticated()
    }

    suspend fun save() {
        repository.save(_state.value.toStoredSettings())
    }

    suspend fun authenticate(): Boolean {
        val snapshot = _state.value
        val settings = snapshot.toSettings()
        if (settings == null || !snapshot.canAuthenticate) {
            _state.update { it.copy(errorMessage = "SMTP details are incomplete.") }
            return false
        }
        _state.update { it.copy(isAuthenticating = true, errorMessage = null) }
        return try {
            withContext(Dispatchers.IO) {
                SmtpClient.testConnection(settings)
            }
            repository.save(_state.value.toStoredSettings())
            _state.update { it.copy(isAuthenticated = true, isAuthenticating = false) }
            true
        } catch (e: Exception) {
            _state.update {
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
        val stored = repository.load() ?: return
        _state.update {
            it.copy(
                host = stored.host,
                port = stored.port,
                username = stored.username,
                password = stored.password,
                transport = stored.transport,
                subject = stored.subject,
                body = stored.body,
                accreditedTypeOptions = stored.accreditedTypeOptions,
                signatureHtml = stored.signatureHtml,
                isAuthenticated = false,
                isAuthenticating = false,
                errorMessage = null,
            )
        }
        if (_state.value.canAuthenticate) {
            authenticate()
        }
    }

    private fun persistIfAuthenticated() {
        val snapshot = _state.value
        if (!snapshot.isAuthenticated) return
        scope.launch {
            repository.save(snapshot.toStoredSettings())
        }
    }
}
