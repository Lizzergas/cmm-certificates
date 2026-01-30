package com.cmm.certificates.feature.certificate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.data.docx.DocxTemplate
import com.cmm.certificates.data.network.NETWORK_UNAVAILABLE_MESSAGE
import com.cmm.certificates.data.network.NetworkService
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_OUTPUT_PATH = "pdf/"

class ConversionViewModel(
    private val progressRepository: PdfConversionProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val networkService: NetworkService,
) : ViewModel() {
    private val defaultAccreditedTypeOptions = parseAccreditedTypeOptions(
        settingsRepository.state.value.certificate.accreditedTypeOptions,
    )
    private val formState = MutableStateFlow(
        ConversionFormState(
            accreditedType = defaultAccreditedTypeOptions.firstOrNull().orEmpty(),
        )
    )
    private val filesState = MutableStateFlow(ConversionFilesState())
    private val entriesState = MutableStateFlow<List<RegistrationEntry>>(emptyList())

    val uiState: StateFlow<ConversionUiState> = combine(
        formState,
        filesState,
        entriesState,
        settingsRepository.state,
        networkService.isNetworkAvailable,
    ) { form, files, entries, settings, networkAvailable ->
        val options = parseAccreditedTypeOptions(settings.certificate.accreditedTypeOptions)
            .ifEmpty { defaultAccreditedTypeOptions }
        val resolvedType = if (form.accreditedType.isNotBlank() && form.accreditedType in options) {
            form.accreditedType
        } else {
            options.firstOrNull().orEmpty()
        }
        val resolvedForm = if (resolvedType == form.accreditedType) {
            form
        } else {
            form.copy(accreditedType = resolvedType)
        }
        ConversionUiState(
            files = files,
            form = resolvedForm,
            accreditedTypeOptions = options,
            isNetworkAvailable = networkAvailable,
            entries = entries,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConversionUiState(
            files = ConversionFilesState(),
            form = ConversionFormState(
                accreditedType = defaultAccreditedTypeOptions.firstOrNull().orEmpty(),
            ),
            accreditedTypeOptions = defaultAccreditedTypeOptions,
        ),
    )

    fun setTemplatePath(path: String) {
        filesState.update { it.copy(templatePath = path) }
    }

    fun setAccreditedId(value: String) {
        formState.update { it.copy(accreditedId = value) }
    }

    fun setDocIdStart(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        formState.update { it.copy(docIdStart = sanitized) }
    }

    fun setAccreditedType(value: String) {
        formState.update { it.copy(accreditedType = value) }
    }

    fun setAccreditedHours(value: String) {
        val sanitized = value.filter { it in '0'..'9' }
        formState.update { it.copy(accreditedHours = sanitized) }
    }

    fun setCertificateName(value: String) {
        formState.update { it.copy(certificateName = value) }
    }

    fun setLector(value: String) {
        formState.update { it.copy(lector = value) }
    }

    fun setLectorGender(value: String) {
        formState.update { it.copy(lectorGender = value) }
    }

    fun selectXlsx(path: String) {
        filesState.update { it.copy(xlsxPath = path) }
        if (path.isBlank()) {
            entriesState.value = emptyList()
            return
        }
        viewModelScope.launch {
            val parsed = runCatching {
                withContext(Dispatchers.IO) { XlsxParser.parse(path) }
            }
            entriesState.value = parsed.getOrElse { emptyList() }
        }
    }

    fun generateDocuments() {
        viewModelScope.launch {
            generateDocumentsInternal()
        }
    }

    private suspend fun generateDocumentsInternal() {
        networkService.refresh()
        if (!networkService.isNetworkAvailable.value) {
            progressRepository.fail(NETWORK_UNAVAILABLE_MESSAGE)
            return
        }
        val snapshot = uiState.value
        if (snapshot.files.templatePath.isBlank()) {
            progressRepository.fail("Template is required.")
            return
        }
        if (snapshot.entries.isEmpty()) {
            progressRepository.fail("No XLSX entries to generate.")
            return
        }
        val form = snapshot.form
        if (form.accreditedId.isBlank() ||
            form.docIdStart.isBlank() ||
            form.accreditedHours.isBlank() ||
            form.certificateName.isBlank() ||
            form.lector.isBlank()
        ) {
            progressRepository.fail("All certificate fields are required.")
            return
        }

        val templateBytes = try {
            DocxTemplate.loadTemplate(snapshot.files.templatePath)
        } catch (e: Exception) {
            progressRepository.fail(e.message ?: "Failed to load template.")
            return
        }

        val docIdStart = form.docIdStart.trim().toLongOrNull()
        if (docIdStart == null) {
            progressRepository.fail("Document ID start must be a number.")
            return
        }

        val baseOutputDir = OutputDirectory.resolve(DEFAULT_OUTPUT_PATH)
        val sanitizedFolder = sanitizeFolderName(form.certificateName)
        val outputDir = joinPath(baseOutputDir, sanitizedFolder)
        if (!OutputDirectory.ensureExists(outputDir)) {
            progressRepository.fail("Failed to create output folder: $outputDir")
            return
        }

        withContext(Dispatchers.IO) {
            progressRepository.start(
                total = snapshot.entries.size,
                outputDir = outputDir,
                docIdStart = docIdStart,
                entries = snapshot.entries,
            )
            for ((index, entry) in snapshot.entries.withIndex()) {
                if (progressRepository.isCancelRequested()) return@withContext
                val fullName = listOf(entry.name, entry.surname)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val docId = docIdStart + index
                progressRepository.setCurrentDocId(docId)
                val replacements = mapOf(
                    "{{vardas_pavarde}}" to fullName,
                    "{{data}}" to entry.formattedDate,
                    "{{akreditacijos_id}}" to form.accreditedId,
                    "{{dokumento_id}}" to docId.toString(),
                    "{{akreditacijos_tipas}}" to form.accreditedType,
                    "{{akreditacijos_valandos}}" to form.accreditedHours,
                    "{{sertifikato_pavadinimas}}" to form.certificateName,
                    "{{destytojas}}" to form.lector,
                    "{{destytojo_tipas}}" to form.lectorGender,
                )
                val outputPath = joinPath(outputDir, "${docId}.pdf")
                try {
                    DocxTemplate.fillTemplateToPdf(
                        templateBytes = templateBytes,
                        outputPath = outputPath,
                        replacements = replacements,
                    )
                    if (progressRepository.isCancelRequested()) return@withContext
                    progressRepository.update(index + 1)
                } catch (e: Exception) {
                    progressRepository.fail(e.message ?: "Failed to write $outputPath")
                    return@withContext
                }
            }
            if (!progressRepository.isCancelRequested()) {
                progressRepository.finish()
            }
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
    return cleaned.ifBlank { "certificate" }
}

data class ConversionUiState(
    val files: ConversionFilesState = ConversionFilesState(),
    val form: ConversionFormState = ConversionFormState(),
    val accreditedTypeOptions: List<String> = emptyList(),
    val isNetworkAvailable: Boolean = true,
    val entries: List<RegistrationEntry> = emptyList(),
) {
    val isConversionEnabled: Boolean
        get() = files.xlsxPath.isNotBlank() &&
                files.templatePath.isNotBlank() &&
                form.accreditedId.isNotBlank() &&
                form.docIdStart.isNotBlank() &&
                form.accreditedHours.isNotBlank() &&
                form.certificateName.isNotBlank() &&
                form.lector.isNotBlank() &&
                isNetworkAvailable &&
                entries.isNotEmpty()
}

data class ConversionFilesState(
    val xlsxPath: String = "",
    val templatePath: String = "",
) {
    val hasXlsx: Boolean
        get() = xlsxPath.isNotBlank()

    val hasTemplate: Boolean
        get() = templatePath.isNotBlank()
}

data class ConversionFormState(
    val accreditedId: String = "IVP-10",
    val docIdStart: String = "",
    val accreditedType: String = "",
    val accreditedHours: String = "",
    val certificateName: String = "",
    val lector: String = "",
    val lectorGender: String = "Lektorius:",
)

private fun parseAccreditedTypeOptions(raw: String): List<String> {
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
