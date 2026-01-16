package com.cmm.certificates.feature.home

import androidx.lifecycle.ViewModel
import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.feature.progress.ConversionProgressStore
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.data.xlsx.XlsxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class ConversionViewModel(
    private val progressStore: ConversionProgressStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ConversionUiState(
            xlsxPath = "",
            templatePath = "",
            outputDir = "",
            accreditedId = "IVP-10",
            docIdStart = "",
            accreditedType = "paskaitoje",
            accreditedHours = "",
            certificateName = "",
            lector = "",
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

    fun setAccreditedId(value: String) {
        _uiState.update { it.copy(accreditedId = value) }
    }

    fun setDocIdStart(value: String) {
        _uiState.update { it.copy(docIdStart = value) }
    }

    fun setAccreditedType(value: String) {
        _uiState.update { it.copy(accreditedType = value) }
    }

    fun setAccreditedHours(value: String) {
        _uiState.update { it.copy(accreditedHours = value) }
    }

    fun setCertificateName(value: String) {
        _uiState.update { it.copy(certificateName = value) }
    }

    fun setLector(value: String) {
        _uiState.update { it.copy(lector = value) }
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

    suspend fun generateDocuments() {
        val snapshot = _uiState.value
        if (snapshot.templatePath.isBlank() || snapshot.outputDir.isBlank()) {
            println("Template path or output directory is blank; cannot generate documents.")
            _uiState.update { it.copy(parseError = "Template and output folder are required.") }
            progressStore.fail("Template and output folder are required.")
            return
        }
        if (snapshot.entries.isEmpty()) {
            println("No entries to process; nothing to generate.")
            _uiState.update { it.copy(parseError = "No XLSX entries to generate.") }
            progressStore.fail("No XLSX entries to generate.")
            return
        }
        if (snapshot.accreditedId.isBlank() ||
            snapshot.docIdStart.isBlank() ||
            snapshot.accreditedHours.isBlank() ||
            snapshot.certificateName.isBlank() ||
            snapshot.lector.isBlank()
        ) {
            _uiState.update { it.copy(parseError = "All certificate fields are required.") }
            progressStore.fail("All certificate fields are required.")
            return
        }

        val templateBytes = try {
            DocxTemplate.loadTemplate(snapshot.templatePath)
        } catch (e: Exception) {
            println("Failed to load template: ${e.message}")
            _uiState.update { it.copy(parseError = e.message ?: "Failed to load template.") }
            progressStore.fail(e.message ?: "Failed to load template.")
            return
        }

        val docIdStart = snapshot.docIdStart.trim().toLongOrNull()
        if (docIdStart == null) {
            _uiState.update { it.copy(parseError = "Document ID start must be a number.") }
            progressStore.fail("Document ID start must be a number.")
            return
        }

        withContext(Dispatchers.Default) {
            progressStore.start(snapshot.entries.size, snapshot.outputDir)
            for ((index, entry) in snapshot.entries.withIndex()) {
                if (progressStore.isCancelRequested()) return@withContext
                println("Generating document for entry #${index + 1}: $entry")
                val fullName = listOf(entry.name, entry.surname)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val docId = docIdStart + index
                val replacements = mapOf(
                    "{{full_name}}" to fullName,
                    "{{date}}" to entry.formattedDate,
                    "{{accredited_id}}" to snapshot.accreditedId,
                    "{{doc_id}}" to docId.toString(),
                    "{{accredited_type}}" to snapshot.accreditedType,
                    "{{accredited_hours}}" to snapshot.accreditedHours,
                    "{{certificate_name}}" to snapshot.certificateName,
                    "{{lector}}" to snapshot.lector,
                )
                val outputPath = joinPath(snapshot.outputDir, "${docId}.pdf")
                try {
                    println("Writing output: $outputPath")
                    DocxTemplate.fillTemplateToPdf(
                        templateBytes = templateBytes,
                        outputPath = outputPath,
                        replacements = replacements,
                    )
                    if (progressStore.isCancelRequested()) return@withContext
                    progressStore.update(index + 1)
                } catch (e: Exception) {
                    println("Failed to generate document for $fullName: ${e.message}")
                    _uiState.update {
                        it.copy(parseError = e.message ?: "Failed to write $outputPath")
                    }
                    progressStore.fail(e.message ?: "Failed to write $outputPath")
                    return@withContext
                }
            }
            if (!progressStore.isCancelRequested()) {
                progressStore.finish()
            }
        }
    }
}

data class ConversionUiState(
    val xlsxPath: String,
    val templatePath: String,
    val outputDir: String,
    val accreditedId: String,
    val docIdStart: String,
    val accreditedType: String,
    val accreditedHours: String,
    val certificateName: String,
    val lector: String,
    val entries: List<RegistrationEntry>,
    val parseError: String?,
) {
    val isConversionEnabled: Boolean
        get() = xlsxPath.isNotBlank() &&
                templatePath.isNotBlank() &&
                outputDir.isNotBlank() &&
                accreditedId.isNotBlank() &&
                docIdStart.isNotBlank() &&
                accreditedHours.isNotBlank() &&
                certificateName.isNotBlank() &&
                lector.isNotBlank() &&
                entries.isNotEmpty()
}

private fun joinPath(directory: String, fileName: String): String {
    val trimmed = directory.trimEnd('/', '\\')
    return if (trimmed.isEmpty()) fileName else "$trimmed/$fileName"
}
