package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.GenerateCertificatesRequest
import com.cmm.certificates.feature.certificate.domain.usecase.GenerateCertificatesUseCase
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.data.defaultLectorLabel
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversionViewModel(
    emailProgressRepository: EmailProgressRepository,
    settingsRepository: SettingsRepository,
    connectivityMonitor: ConnectivityMonitor,
    capabilityProvider: PlatformCapabilityProvider,
    private val parseRegistrations: ParseRegistrationsUseCase,
    private val generateCertificates: GenerateCertificatesUseCase,
) : ViewModel() {
    private val logTag = "ConversionVM"
    private val capabilities = capabilityProvider.capabilities
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
        combine(
            formState,
            filesState,
            entriesState,
            settingsRepository.state,
            connectivityMonitor.isNetworkAvailable,
        ) { form, files, entries, settings, networkAvailable ->
            val options = parseAccreditedTypeOptions(settings.certificate.accreditedTypeOptions)
                .ifEmpty { defaultAccreditedTypeOptions }
            val resolvedType =
                if (form.accreditedType.isNotBlank() && form.accreditedType in options) {
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
                isSmtpAuthenticated = settings.smtp.isAuthenticated,
                supportsConversion = capabilities.canRunConversion,
                supportsEmailSending = capabilities.canSendEmails,
                entries = entries,
            )
        },
        emailProgressRepository.cachedEmails,
    ) { baseState, cachedEmails ->
        baseState.copy(
            cachedEmailsCount = cachedEmails?.requests?.size ?: 0
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
        if (!capabilities.canRunConversion) return
        logInfo(logTag, "Template selected: ${path.ifBlank { "<empty>" }}")
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
        if (!capabilities.canRunConversion) {
            entriesState.value = emptyList()
            logWarn(logTag, "Ignored XLSX selection because conversion is unsupported")
            return
        }
        filesState.update { it.copy(xlsxPath = path) }
        if (path.isBlank()) {
            entriesState.value = emptyList()
            logWarn(logTag, "Cleared XLSX selection")
            return
        }
        viewModelScope.launch {
            logInfo(logTag, "Parsing XLSX: $path")
            val parsed = runCatching {
                withContext(Dispatchers.IO) { parseRegistrations(path).getOrThrow() }
            }
            entriesState.value = parsed.getOrElse {
                logError(logTag, "Failed to parse XLSX: $path", it)
                emptyList()
            }
            logInfo(logTag, "Parsed ${entriesState.value.size} XLSX entries")
        }
    }

    fun generateDocuments() {
        if (!capabilities.canRunConversion) {
            logWarn(logTag, "Ignored conversion request because conversion is unsupported")
            return
        }
        viewModelScope.launch {
            logInfo(logTag, "Starting conversion request")
            generateDocumentsInternal()
        }
    }

    private suspend fun generateDocumentsInternal() {
        val snapshot = uiState.value
        generateCertificates(
            GenerateCertificatesRequest(
                templatePath = snapshot.files.templatePath,
                entries = snapshot.entries,
                accreditedId = snapshot.form.accreditedId,
                docIdStart = snapshot.form.docIdStart,
                accreditedType = snapshot.form.accreditedType,
                accreditedHours = snapshot.form.accreditedHours,
                certificateName = snapshot.form.certificateName,
                lector = snapshot.form.lector,
                lectorGender = snapshot.form.lectorGender,
            )
        )
    }
}

data class ConversionUiState(
    val files: ConversionFilesState = ConversionFilesState(),
    val form: ConversionFormState = ConversionFormState(),
    val accreditedTypeOptions: List<String> = emptyList(),
    val isNetworkAvailable: Boolean = true,
    val isSmtpAuthenticated: Boolean = false,
    val supportsConversion: Boolean = true,
    val supportsEmailSending: Boolean = true,
    val entries: List<RegistrationEntry> = emptyList(),
    val cachedEmailsCount: Int = 0,
) {
    val isConversionEnabled: Boolean
        get() = supportsConversion &&
                files.xlsxPath.isNotBlank() &&
                files.templatePath.isNotBlank() &&
                form.accreditedId.isNotBlank() &&
                form.docIdStart.isNotBlank() &&
                form.accreditedHours.isNotBlank() &&
                form.certificateName.isNotBlank() &&
                form.lector.isNotBlank() &&
                entries.isNotEmpty()

    val canRetryCachedEmails: Boolean
        get() = cachedEmailsCount > 0 && supportsEmailSending && isNetworkAvailable && isSmtpAuthenticated
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
    val lectorGender: String = defaultLectorLabel(),
)

private fun parseAccreditedTypeOptions(raw: String): List<String> {
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
