package com.cmm.certificates.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.data.email.SmtpSettingsRepository
import com.cmm.certificates.data.network.NetworkService
import com.cmm.certificates.data.network.NETWORK_UNAVAILABLE_MESSAGE
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.feature.progress.ConversionProgressStore
import com.cmm.certificates.feature.settings.SmtpSettingsStore
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_OUTPUT_PATH = "pdf/"
private val DEFAULT_ACCREDITED_TYPE_OPTIONS =
    parseAccreditedTypeOptions(SmtpSettingsRepository.DEFAULT_ACCREDITED_TYPE_OPTIONS)

class ConversionViewModel(
    private val progressStore: ConversionProgressStore,
    private val smtpSettingsStore: SmtpSettingsStore,
    private val networkService: NetworkService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConversionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkService.isNetworkAvailable.collect { available ->
                updateNetworkAvailability(available)
            }
        }
        viewModelScope.launch {
            smtpSettingsStore.state.collect { settings ->
                val parsedOptions = parseAccreditedTypeOptions(settings.accreditedTypeOptions)
                val options = parsedOptions.ifEmpty {
                    DEFAULT_ACCREDITED_TYPE_OPTIONS
                }
                _uiState.update { current ->
                    val resolvedType =
                        if (current.accreditedType.isBlank() || current.accreditedType !in options) {
                            options.firstOrNull().orEmpty()
                        } else {
                            current.accreditedType
                        }
                    current.copy(
                        accreditedTypeOptions = options,
                        accreditedType = resolvedType,
                    )
                }
            }
        }
    }

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
        networkService.refresh()
        if (!networkService.isNetworkAvailable.value) {
            updateNetworkAvailability(false)
            _uiState.update { it.copy(parseError = NETWORK_UNAVAILABLE_MESSAGE) }
            progressStore.fail(NETWORK_UNAVAILABLE_MESSAGE)
            return
        }
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

        val baseOutputDir = OutputDirectory.resolve(DEFAULT_OUTPUT_PATH)
        val sanitizedFolder = sanitizeFolderName(snapshot.certificateName)
        val outputDir = joinPath(baseOutputDir, sanitizedFolder)
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
                    "{{vardas_pavarde}}" to fullName,
                    "{{data}}" to entry.formattedDate,
                    "{{akreditacijos_id}}" to snapshot.accreditedId,
                    "{{dokumento_id}}" to docId.toString(),
                    "{{akreditacijos_tipas}}" to snapshot.accreditedType,
                    "{{akreditacijos_valandos}}" to snapshot.accreditedHours,
                    "{{sertifikato_pavadinimas}}" to snapshot.certificateName,
                    "{{destytojas}}" to snapshot.lector,
                    "{{destytojo_tipas}}" to snapshot.lectorGender,
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

    private fun updateNetworkAvailability(available: Boolean) {
        _uiState.update { current ->
            current.copy(
                isNetworkAvailable = available,
                parseError = if (available && current.parseError == NETWORK_UNAVAILABLE_MESSAGE) null else current.parseError,
            )
        }
    }
}

private fun sanitizeFolderName(rawName: String): String {
    val trimmed = rawName.trim()
    if (trimmed.isBlank()) return "certificate"
    val cleaned = trimmed
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('.')
    return if (cleaned.isBlank()) "certificate" else cleaned
}

data class ConversionUiState(
    val xlsxPath: String = "",
    val templatePath: String = "",
    val accreditedId: String = "IVP-10",
    val docIdStart: String = "",
    val accreditedType: String = DEFAULT_ACCREDITED_TYPE_OPTIONS.firstOrNull().orEmpty(),
    val accreditedTypeOptions: List<String> = DEFAULT_ACCREDITED_TYPE_OPTIONS,
    val accreditedHours: String = "",
    val certificateName: String = "",
    val lector: String = "",
    val lectorGender: String = "Lektorius:",
    val isNetworkAvailable: Boolean = true,
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
                isNetworkAvailable &&
                entries.isNotEmpty()
}

private fun parseAccreditedTypeOptions(raw: String): List<String> {
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
