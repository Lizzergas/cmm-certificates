package com.cmm.certificates.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.feature.settings.data.CertificateSettingsState
import com.cmm.certificates.feature.settings.data.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.data.SmtpSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val clearAllDataUseCase: ClearAllDataUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = settingsRepository.state
        .map { state -> state.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(),
        )

    fun setHost(value: String) = settingsRepository.setHost(value)

    fun setPort(value: String) = settingsRepository.setPort(value)

    fun setUsername(value: String) = settingsRepository.setUsername(value)

    fun setPassword(value: String) = settingsRepository.setPassword(value)

    fun setTransport(value: SmtpTransport) = settingsRepository.setTransport(value)

    fun setSubject(value: String) = settingsRepository.setSubject(value)

    fun setBody(value: String) = settingsRepository.setBody(value)

    fun setSignatureHtml(value: String) = settingsRepository.setSignatureHtml(value)

    fun setAccreditedTypeOptions(value: String) = settingsRepository.setAccreditedTypeOptions(value)

    fun save() {
        viewModelScope.launch { settingsRepository.save() }
    }

    fun authenticate() {
        viewModelScope.launch { settingsRepository.authenticate() }
    }

    fun clearAll() {
        viewModelScope.launch { clearAllDataUseCase.clearAll() }
    }
}

data class SettingsUiState(
    val smtp: SmtpSettingsState = SmtpSettingsState(),
    val email: EmailTemplateSettingsState = EmailTemplateSettingsState(),
    val certificate: CertificateSettingsState = CertificateSettingsState(),
)
