package com.cmm.certificates.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.signature.SignatureEditorController
import com.cmm.certificates.core.signature.SignatureEditorMode
import com.cmm.certificates.core.signature.SignatureEditorUiState
import com.cmm.certificates.core.signature.SignatureFont
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.settings.data.CertificateSettingsState
import com.cmm.certificates.feature.settings.data.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.data.SmtpSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val emailProgressRepository: EmailProgressRepository,
    private val clearAllDataUseCase: ClearAllDataUseCase,
    private val signatureEditor: SignatureEditorController,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.state,
        emailProgressRepository.sentCountInLast24Hours
    ) { settings, sentCount ->
        settings.toUiState().copy(sentToday = sentCount)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(),
    )

    val signatureEditorState: StateFlow<SignatureEditorUiState> = signatureEditor.state

    fun setHost(value: String) = settingsRepository.setHost(value)

    fun setPort(value: String) = settingsRepository.setPort(value)

    fun setUsername(value: String) = settingsRepository.setUsername(value)

    fun setPassword(value: String) = settingsRepository.setPassword(value)

    fun setTransport(value: SmtpTransport) = settingsRepository.setTransport(value)

    fun setSubject(value: String) = settingsRepository.setSubject(value)

    fun setBody(value: String) = settingsRepository.setBody(value)


    fun setAccreditedTypeOptions(value: String) = settingsRepository.setAccreditedTypeOptions(value)

    fun setDailyLimit(value: String) {
        val limit = value.filter { it in '0'..'9' }.toIntOrNull() ?: 0
        settingsRepository.setDailyLimit(limit)
    }

    fun save() {
        viewModelScope.launch { settingsRepository.save() }
    }

    fun authenticate() {
        viewModelScope.launch { settingsRepository.authenticate() }
    }

    fun clearAll() {
        viewModelScope.launch { clearAllDataUseCase.clearAll() }
    }

    fun openSignatureEditor() {
        signatureEditor.open(settingsRepository.state.value.email.signatureHtml)
    }

    fun closeSignatureEditor() {
        signatureEditor.close()
    }

    fun setSignatureEditorMode(mode: SignatureEditorMode) {
        signatureEditor.setMode(mode)
    }

    fun setSignatureFont(font: SignatureFont) {
        signatureEditor.setFont(font)
    }

    fun setSignatureFontSize(value: String) {
        signatureEditor.setFontSize(value)
    }

    fun toggleSignatureItalic() {
        signatureEditor.toggleItalic()
    }

    fun toggleSignatureBold() {
        signatureEditor.toggleBold()
    }

    fun setSignatureLineHeight(value: String) {
        signatureEditor.setLineHeight(value)
    }

    fun setSignatureColorHex(value: String) {
        signatureEditor.setColorHex(value)
    }

    fun addSignatureLine() {
        signatureEditor.addLine()
    }

    fun removeSignatureLine(index: Int) {
        signatureEditor.removeLine(index)
    }

    fun moveSignatureLineUp(index: Int) {
        signatureEditor.moveLineUp(index)
    }

    fun moveSignatureLineDown(index: Int) {
        signatureEditor.moveLineDown(index)
    }

    fun setSignatureLineText(index: Int, text: String) {
        signatureEditor.setLineText(index, text)
    }

    fun setSignatureDraftHtml(html: String) {
        signatureEditor.setDraftHtml(html)
    }

    fun validateSignatureDraft(): Boolean {
        return signatureEditor.validateDraft().isValid
    }

    fun convertSignatureToBuilder() {
        signatureEditor.convertToBuilder()
    }

    fun resetSignatureToDefault() {
        signatureEditor.resetToDefault()
    }

    fun saveSignatureDraft() {
        if (!signatureEditor.validateDraft().isValid) return
        val html = signatureEditor.state.value.draftHtml
        settingsRepository.setSignatureHtml(html)
        viewModelScope.launch { settingsRepository.save() }
        signatureEditor.close()
    }
}

data class SettingsUiState(
    val smtp: SmtpSettingsState = SmtpSettingsState(),
    val email: EmailTemplateSettingsState = EmailTemplateSettingsState(),
    val certificate: CertificateSettingsState = CertificateSettingsState(),
    val sentToday: Int = 0,
)
