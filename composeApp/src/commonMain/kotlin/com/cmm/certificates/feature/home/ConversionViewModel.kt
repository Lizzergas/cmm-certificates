package com.cmm.certificates.feature.home

import androidx.lifecycle.ViewModel
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.feature.progress.ConversionProgressStore
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val DEFAULT_OUTPUT_PATH = "pdf/"

class ConversionViewModel(
    private val progressStore: ConversionProgressStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConversionUiState())
    val uiState = _uiState.asStateFlow()

    fun setTemplatePath(path: String) {
        _uiState.update { it.copy(templatePath = path) }
    }

    fun setAccreditedId(value: String) {
        _uiState.update { it.copy(accreditedId = value) }
    }

    fun setDocIdStart(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        _uiState.update { it.copy(docIdStart = sanitized) }
    }

    fun setAccreditedType(value: String) {
        _uiState.update { it.copy(accreditedType = value) }
    }

    fun setAccreditedHours(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        _uiState.update { it.copy(accreditedHours = sanitized) }
    }

    fun setCertificateName(value: String) {
        _uiState.update { it.copy(certificateName = value) }
    }

    fun setLector(value: String) {
        _uiState.update { it.copy(lector = value) }
    }

    fun setLectorGender(value: String) {
        _uiState.update { it.copy(lectorGender = value) }
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
        if (snapshot.templatePath.isBlank()) {
            println("Template path is blank; cannot generate documents.")
            _uiState.update { it.copy(parseError = "Template is required.") }
            progressStore.fail("Template is required.")
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

        val outputDir = OutputDirectory.resolve(DEFAULT_OUTPUT_PATH)
        if (!OutputDirectory.ensureExists(outputDir)) {
            _uiState.update { it.copy(parseError = "Failed to create output folder: $outputDir") }
            progressStore.fail("Failed to create output folder: $outputDir")
            return
        }

        withContext(Dispatchers.IO) {
            progressStore.start(
                total = snapshot.entries.size,
                outputDir = outputDir,
                docIdStart = docIdStart,
                entries = snapshot.entries,
            )
            for ((index, entry) in snapshot.entries.withIndex()) {
                if (progressStore.isCancelRequested()) return@withContext
                println("Generating document for entry #${index + 1}: $entry")
                val fullName = listOf(entry.name, entry.surname)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val docId = docIdStart + index
                progressStore.setCurrentDocId(docId)
                val replacements = mapOf(
                    "{{full_name}}" to fullName,
                    "{{date}}" to entry.formattedDate,
                    "{{accredited_id}}" to snapshot.accreditedId,
                    "{{doc_id}}" to docId.toString(),
                    "{{accredited_type}}" to snapshot.accreditedType,
                    "{{accredited_hours}}" to snapshot.accreditedHours,
                    "{{certificate_name}}" to snapshot.certificateName,
                    "{{lector}}" to snapshot.lector,
                    "{{lector_gender}}" to snapshot.lectorGender,
                )
                val outputPath = joinPath(outputDir, "${docId}.pdf")
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
    val xlsxPath: String = "",
    val templatePath: String = "",
    val accreditedId: String = "IVP-10",
    val docIdStart: String = "",
    val accreditedType: String = "paskaitoje",
    val accreditedHours: String = "",
    val certificateName: String = "",
    val lector: String = "",
    val lectorGender: String = "Lektorius:",
    val entries: List<RegistrationEntry> = emptyList(),
    val parseError: String? = null,
) {
    val isConversionEnabled: Boolean
        get() = xlsxPath.isNotBlank() &&
                templatePath.isNotBlank() &&
                accreditedId.isNotBlank() &&
                docIdStart.isNotBlank() &&
                accreditedHours.isNotBlank() &&
                certificateName.isNotBlank() &&
                lector.isNotBlank() &&
                entries.isNotEmpty()
}
