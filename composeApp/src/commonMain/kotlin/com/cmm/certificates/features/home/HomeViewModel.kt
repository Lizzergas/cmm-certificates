package com.cmm.certificates.features.home

import androidx.lifecycle.ViewModel
import com.cmm.certificates.docx.DocxTemplate
import com.cmm.certificates.xlsx.RegistrationEntry
import com.cmm.certificates.xlsx.XlsxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            xlsxPath = "",
            templatePath = "",
            outputDir = "",
            entries = emptyList(),
            parseError = null,
        )
    )
    val uiState = _uiState.asStateFlow()

    fun setOutputDir(path: String) {
        _uiState.update { it.copy(outputDir = path) }
    }

    fun setTemplatePath(path: String) {
        _uiState.update { it.copy(templatePath = path) }
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
            println("Failed to parse XLSX: ${e.message}")
            _uiState.update {
                it.copy(
                    entries = emptyList(),
                    parseError = e.message ?: "Failed to parse XLSX",
                )
            }
        }
    }

    fun generateDocuments() {
        val snapshot = _uiState.value
        if (snapshot.templatePath.isBlank() || snapshot.outputDir.isBlank()) {
            println("Template path or output directory is blank; cannot generate documents.")
            _uiState.update { it.copy(parseError = "Template and output folder are required.") }
            return
        }
        if (snapshot.entries.isEmpty()) {
            println("No entries to process; nothing to generate.")
            _uiState.update { it.copy(parseError = "No XLSX entries to generate.") }
            return
        }

        val templateBytes = try {
            DocxTemplate.loadTemplate(snapshot.templatePath)
        } catch (e: Exception) {
            println("Failed to load template: ${e.message}")
            _uiState.update { it.copy(parseError = e.message ?: "Failed to load template.") }
            return
        }

        snapshot.entries.forEachIndexed { index, entry ->
            println("Generating document for entry #${index + 1}: $entry")
            val fullName = listOf(entry.name, entry.surname)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val replacements = mapOf("{{full_name}}" to fullName)
            val outputPath = joinPath(snapshot.outputDir, "entry_${index + 1}.docx")
            try {
                println("Writing output: $outputPath")
                DocxTemplate.fillTemplate(
                    templateBytes = templateBytes,
                    outputPath = outputPath,
                    replacements = replacements,
                )
            } catch (e: Exception) {
                println("Failed to generate document for $fullName: ${e.message}")
                _uiState.update {
                    it.copy(parseError = e.message ?: "Failed to write $outputPath")
                }
            }
        }
    }
}

data class HomeUiState(
    val xlsxPath: String,
    val templatePath: String,
    val outputDir: String,
    val entries: List<RegistrationEntry>,
    val parseError: String?,
)

private fun joinPath(directory: String, fileName: String): String {
    val trimmed = directory.trimEnd('/', '\\')
    return if (trimmed.isEmpty()) fileName else "$trimmed/$fileName"
}
