package com.cmm.certificates.data.config

import com.cmm.certificates.AppInstallation
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.ManualTagField
import com.cmm.certificates.domain.config.XlsxTagField
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.domain.config.validateCertificateConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val EditableConfigFileName = "config.json"
private const val DefaultConfigFileName = "default.config.json"

data class CertificateConfigurationState(
    val configuration: CertificateConfiguration = defaultCertificateConfiguration(),
    val source: CertificateConfigurationSource = CertificateConfigurationSource.CodeDefault,
    val externalPath: String? = null,
    val loadFailureMessage: String? = null,
)

enum class CertificateConfigurationSource {
    InstalledFile,
    DefaultFile,
    CodeDefault,
}

class CertificateConfigurationRepository(
    private val activeStore: ActiveCertificateConfigStore,
) {
    private val logTag = "CertificateConfigRepo"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(CertificateConfigurationState())

    val state: StateFlow<CertificateConfigurationState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        scope.launch {
            _state.value = loadCurrentState()
        }
    }

    suspend fun save(configuration: CertificateConfiguration): Result<Unit> {
        return runCatching {
            val validated = validateCertificateConfiguration(configuration).getOrThrow()
            val rawJson = json.encodeToString(validated)
            val editablePath = editableConfigPath()
                ?: error("Editable config.json path is unavailable")
            InstalledConfigFileAccess.write(editablePath, rawJson)
            activeStore.save(rawJson)
            _state.value = CertificateConfigurationState(
                configuration = validated,
                source = CertificateConfigurationSource.InstalledFile,
                externalPath = editablePath,
                loadFailureMessage = null,
            )
        }
    }

    suspend fun resetToDefault(): Result<CertificateConfiguration> {
        return runCatching {
            val configuration = loadDefaultConfiguration().configuration
            save(configuration).getOrThrow()
            configuration
        }
    }

    suspend fun clearActiveConfig() {
        activeStore.clear()
        _state.value = loadCurrentState()
    }

    suspend fun inspectXlsxHeaders(path: String): Result<List<String>> {
        return runCatching {
            withContext(Dispatchers.IO) {
                XlsxParser.readFirstSheet(path).headers
            }
        }
    }

    private suspend fun loadCurrentState(): CertificateConfigurationState {
        val editablePath = editableConfigPath()
        decode(editablePath?.let(InstalledConfigFileAccess::readSafely))?.let { decoded ->
            logInfo(logTag, "Loaded editable certificate config from $editablePath")
            activeStore.save(json.encodeToString(decoded.configuration))
            return CertificateConfigurationState(
                configuration = decoded.configuration,
                source = CertificateConfigurationSource.InstalledFile,
                externalPath = editablePath,
                loadFailureMessage = decoded.message,
            )
        }

        val defaultState = loadDefaultConfiguration()
        activeStore.save(json.encodeToString(defaultState.configuration))
        return defaultState
    }

    private fun loadDefaultConfiguration(): CertificateConfigurationState {
        val defaultPath = AppInstallation.installedResourcePath(DefaultConfigFileName)
        decode(defaultPath?.let(InstalledConfigFileAccess::readSafely))?.let { decoded ->
            logInfo(logTag, "Loaded default certificate config from $defaultPath")
            return CertificateConfigurationState(
                configuration = decoded.configuration,
                source = CertificateConfigurationSource.DefaultFile,
                externalPath = defaultPath,
                loadFailureMessage = decoded.message,
            )
        }
        logWarn(logTag, "Falling back to hardcoded default certificate configuration")
        return CertificateConfigurationState(
            configuration = defaultCertificateConfiguration(),
            source = CertificateConfigurationSource.CodeDefault,
            externalPath = null,
            loadFailureMessage = null,
        )
    }

    private fun editableConfigPath(): String? {
        return AppInstallation.installedResourcePath(EditableConfigFileName)
            ?: AppInstallation.installationDirectoryPath()?.trimEnd('/', '\\')
                ?.plus("/$EditableConfigFileName")
    }

    private fun decode(rawJson: String?): DecodedConfiguration? {
        if (rawJson.isNullOrBlank()) return null
        runCatching {
            val decoded = json.decodeFromString<CertificateConfiguration>(rawJson)
            validateCertificateConfiguration(decoded).getOrThrow()
        }.onSuccess {
            return DecodedConfiguration(it, null)
        }

        return runCatching {
            val legacy = json.decodeFromString<LegacyCertificateConfiguration>(rawJson)
            val migrated = legacy.toCurrentConfiguration()
            validateCertificateConfiguration(migrated).getOrThrow()
        }.fold(
            onSuccess = {
                DecodedConfiguration(
                    configuration = it,
                    message = "Aptikta sena konfigūracija. Ji automatiškai perkelta į naują formatą.",
                )
            },
            onFailure = {
                logError(logTag, "Failed to decode certificate config", it)
                null
            },
        )
    }
}

private data class DecodedConfiguration(
    val configuration: CertificateConfiguration,
    val message: String?,
)

@Serializable
private data class LegacyCertificateConfiguration(
    val version: Int = 1,
    val id: String,
    val documentNumberTag: String,
    val manualFields: List<LegacyManualField> = emptyList(),
    val xlsxFields: List<LegacyXlsxField> = emptyList(),
)

@Serializable
private data class LegacyManualField(
    val id: String,
    val type: CertificateFieldType,
    val label: String? = null,
    val options: List<String> = emptyList(),
    val defaultValue: String? = null,
)

@Serializable
private data class LegacyXlsxField(
    val id: String,
    val label: String? = null,
)

private fun LegacyCertificateConfiguration.toCurrentConfiguration(): CertificateConfiguration {
    return CertificateConfiguration(
        version = version,
        id = id,
        documentNumberTag = documentNumberTag,
        manualFields = manualFields.map {
            ManualTagField(
                tag = it.id,
                label = it.label,
                type = it.type,
                defaultValue = it.defaultValue,
                options = it.options,
            )
        },
        xlsxFields = xlsxFields.map {
            XlsxTagField(
                tag = it.id,
                label = it.label,
                headerName = it.label?.trim().takeUnless { label -> label.isNullOrBlank() } ?: it.id,
            )
        },
    )
}

private fun InstalledConfigFileAccess.readSafely(path: String): String? {
    return runCatching { read(path) }
        .getOrNull()
}
