package com.cmm.certificates.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.data.email.SmtpTransport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SmtpSettingsStore,
    private val clearAllDataUseCase: ClearAllDataUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = store.state
        .map { state ->
            SettingsUiState(
                host = state.host,
                port = state.port,
                username = state.username,
                password = state.password,
                transport = state.transport,
                subject = state.subject,
                body = state.body,
                accreditedTypeOptions = state.accreditedTypeOptions,
                signatureHtml = state.signatureHtml,
                isAuthenticated = state.isAuthenticated,
                isAuthenticating = state.isAuthenticating,
                errorMessage = state.errorMessage,
                canAuthenticate = state.canAuthenticate,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(),
        )

    fun setHost(value: String) = store.setHost(value)

    fun setPort(value: String) = store.setPort(value)

    fun setUsername(value: String) = store.setUsername(value)

    fun setPassword(value: String) = store.setPassword(value)

    fun setTransport(value: SmtpTransport) = store.setTransport(value)

    fun setSubject(value: String) = store.setSubject(value)

    fun setBody(value: String) = store.setBody(value)

    fun setSignatureHtml(value: String) = store.setSignatureHtml(value)

    fun setAccreditedTypeOptions(value: String) = store.setAccreditedTypeOptions(value)

    fun save() {
        viewModelScope.launch { store.save() }
    }

    fun authenticate() {
        viewModelScope.launch { store.authenticate() }
    }

    fun clearAll() {
        viewModelScope.launch { clearAllDataUseCase.clearAll() }
    }
}

data class SettingsUiState(
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val transport: SmtpTransport = SmtpTransport.SMTPS,
    val subject: String = "",
    val body: String = "",
    val accreditedTypeOptions: String = "",
    val signatureHtml: String = "",
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
    val canAuthenticate: Boolean = false,
)
