package com.cmm.certificates.feature.settings

import com.cmm.certificates.data.email.SmtpSettings
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.data.email.SmtpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

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
}

class SmtpSettingsStore {
    private val _state = MutableStateFlow(SmtpSettingsState())
    val state: StateFlow<SmtpSettingsState> = _state

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

    suspend fun authenticate(): Boolean {
        val snapshot = _state.value
        val settings = snapshot.toSettings()
        if (settings == null || !snapshot.canAuthenticate) {
            _state.update { it.copy(errorMessage = "SMTP details are incomplete.") }
            return false
        }
        _state.update { it.copy(isAuthenticating = true, errorMessage = null) }
        return try {
            withContext(Dispatchers.Default) {
                SmtpClient.testConnection(settings)
            }
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
}
