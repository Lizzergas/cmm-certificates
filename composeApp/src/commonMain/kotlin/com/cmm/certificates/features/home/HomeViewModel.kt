package com.cmm.certificates.features.home

import androidx.lifecycle.ViewModel
import com.cmm.certificates.getPlatform
import com.cmm.certificates.xlsx.RegistrationEntry
import com.cmm.certificates.xlsx.XlsxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            xlsxPath = "",
            outputDir = "",
            entries = emptyList(),
            parseError = null,
        )
    )
    val uiState = _uiState.asStateFlow()

    fun setOutputDir(path: String) {
        _uiState.update { it.copy(outputDir = path) }
    }

    fun selectXlsx(path: String) {
        _uiState.update { it.copy(xlsxPath = path, parseError = null) }
        if (path.isBlank()) {
            _uiState.update { it.copy(entries = emptyList()) }
            return
        }
        try {
            val entries = XlsxParser.parse(path)
            _uiState.update { it.copy(entries = entries, parseError = null) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    entries = emptyList(),
                    parseError = e.message ?: "Failed to parse XLSX",
                )
            }
        }
    }
}

data class HomeUiState(
    val xlsxPath: String,
    val outputDir: String,
    val entries: List<RegistrationEntry>,
    val parseError: String?,
)
