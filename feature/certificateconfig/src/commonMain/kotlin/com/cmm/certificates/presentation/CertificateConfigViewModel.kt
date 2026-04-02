package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.configeditor.ManualTagFieldDraft
import com.cmm.certificates.configeditor.removeItem
import com.cmm.certificates.configeditor.toDraft
import com.cmm.certificates.configeditor.updateItem
import com.cmm.certificates.data.config.CertificateConfigurationRepository
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CertificateConfigViewModel(
    private val repository: CertificateConfigurationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CertificateConfigUiState())
    val uiState: StateFlow<CertificateConfigUiState> = _uiState.asStateFlow()
    private var hasLocalChanges = false

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                if (hasLocalChanges) return@collect
                _uiState.value = state.toUiState()
            }
        }
    }

    fun setSampleXlsxPath(path: String) {
        _uiState.update { it.copy(sampleXlsxPath = path, message = null) }
        if (path.isBlank()) {
            _uiState.update { it.copy(sampleHeaders = emptyList()) }
            return
        }
        viewModelScope.launch {
            repository.inspectXlsxHeaders(path)
                .onSuccess { headers ->
                    _uiState.update { current ->
                        current.copy(
                            sampleHeaders = headers,
                            xlsxFields = current.xlsxFields.map { field ->
                                if (!field.headerName.isNullOrBlank()) {
                                    field
                                } else {
                                    val exactMatch = headers.firstOrNull {
                                        it.trim().equals(field.tag.trim(), ignoreCase = true)
                                    }
                                    field.copy(headerName = exactMatch.orEmpty())
                                }
                            },
                            message = null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            message = it.message ?: "Nepavyko nuskaityti XLSX antraščių."
                        )
                    }
                }
        }
    }

    fun setDocumentNumberTag(tag: String) = mutate { it.copy(documentNumberTag = tag) }

    fun addXlsxField() = mutate {
        it.copy(xlsxFields = it.xlsxFields + XlsxTagFieldDraft())
    }

    fun updateXlsxField(index: Int, update: (XlsxTagFieldDraft) -> XlsxTagFieldDraft) = mutate {
        it.copy(xlsxFields = it.xlsxFields.updateItem(index, update))
    }

    fun removeXlsxField(index: Int) = mutate {
        it.copy(xlsxFields = it.xlsxFields.removeItem(index))
    }

    fun addManualField() = mutate {
        it.copy(manualFields = it.manualFields + ManualTagFieldDraft())
    }

    fun updateManualField(index: Int, update: (ManualTagFieldDraft) -> ManualTagFieldDraft) =
        mutate {
            val currentField = it.manualFields.getOrNull(index) ?: return@mutate it
            val updatedField = update(currentField)
            val updatedDocumentNumberTag = if (it.documentNumberTag == currentField.tag) {
                updatedField.tag
            } else {
                it.documentNumberTag
            }
            it.copy(
                manualFields = it.manualFields.updateItem(index) { updatedField },
                documentNumberTag = updatedDocumentNumberTag,
            )
        }

    fun removeManualField(index: Int) = mutate {
        val removed = it.manualFields.getOrNull(index)
        val updatedFields = it.manualFields.removeItem(index)
        val updatedDocumentNumberTag = when {
            removed == null -> it.documentNumberTag
            removed.tag != it.documentNumberTag -> it.documentNumberTag
            else -> updatedFields.firstOrNull()?.tag.orEmpty()
        }
        it.copy(
            manualFields = updatedFields,
            documentNumberTag = updatedDocumentNumberTag,
        )
    }

    fun resetToDefault() {
        hasLocalChanges = true
        _uiState.value =
            repository.state.value.copy(configuration = defaultCertificateConfiguration())
                .toUiState(
                    message = null,
                )
    }

    fun save(onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val configuration = _uiState.value.toConfiguration()
            repository.save(configuration)
                .onSuccess {
                    hasLocalChanges = false
                    _uiState.value =
                        repository.state.value.toUiState(message = "Konfigūracija išsaugota.")
                    onFinished(true)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            message = error.message ?: "Nepavyko išsaugoti konfigūracijos."
                        )
                    }
                    onFinished(false)
                }
        }
    }

    private fun mutate(update: (CertificateConfigUiState) -> CertificateConfigUiState) {
        hasLocalChanges = true
        _uiState.update { current -> update(current).copy(message = null) }
    }
}

private fun com.cmm.certificates.data.config.CertificateConfigurationState.toUiState(
    message: String? = null,
): CertificateConfigUiState {
    return CertificateConfigUiState(
        source = source,
        externalPath = externalPath,
        loadFailureMessage = loadFailureMessage,
        sampleXlsxPath = "",
        sampleHeaders = emptyList(),
        documentNumberTag = configuration.documentNumberTag,
        xlsxFields = configuration.xlsxFields.map { it.toDraft() },
        manualFields = configuration.manualFields.map { it.toDraft() },
        message = message,
    )
}
