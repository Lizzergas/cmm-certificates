package com.cmm.certificates.presentation.refresh

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_no_entries
import certificates.composeapp.generated.resources.conversion_refresh_docx_failed
import certificates.composeapp.generated.resources.conversion_refresh_docx_succeeded
import certificates.composeapp.generated.resources.conversion_refresh_xlsx_failed
import certificates.composeapp.generated.resources.conversion_refresh_xlsx_succeeded
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.xlsx.XlsxMissingValuesException
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.domain.port.FileChangeObserver
import com.cmm.certificates.domain.port.FileChangeSubscription
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.docxInspectErrorMessage
import com.cmm.certificates.presentation.xlsxMissingHeadersErrorMessage
import com.cmm.certificates.presentation.xlsxMissingValuesErrorMessage
import com.cmm.certificates.presentation.xlsxParseErrorMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ConversionRefreshCoordinator(
    private val scope: CoroutineScope,
    private val fileChangeObserver: FileChangeObserver,
    private val parseRegistrations: ParseRegistrationsUseCase,
    private val documentGenerator: CertificateDocumentGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val refreshDebounceMillis: Long,
    private val currentConfiguration: () -> CertificateConfiguration,
    private val currentFiles: () -> ConversionFilesState,
    private val currentEntries: () -> List<RegistrationEntry>,
    private val updateFiles: (((ConversionFilesState) -> ConversionFilesState)) -> Unit,
    private val setParsedEntries: (String, List<RegistrationEntry>) -> Unit,
    private val postNotification: (UiMessage, Boolean) -> Unit,
) {
    private val logTag = "ConversionRefresh"
    private var xlsxWatcher: FileChangeSubscription? = null
    private var templateWatcher: FileChangeSubscription? = null
    private var xlsxRefreshJob: Job? = null
    private var templateRefreshJob: Job? = null

    fun observeConfigurationChanges(configurations: Flow<CertificateConfiguration>) {
        scope.launch {
            var previousConfiguration: CertificateConfiguration? = null
            configurations.collect { configuration ->
                if (previousConfiguration == null) {
                    previousConfiguration = configuration
                    return@collect
                }
                if (previousConfiguration == configuration) return@collect

                previousConfiguration = configuration
                val selectedXlsx = currentFiles().xlsxPath
                if (selectedXlsx.isNotBlank()) {
                    refreshXlsx(selectedXlsx, isAutoRefresh = false)
                }
            }
        }
    }

    fun refreshTemplate(path: String, isAutoRefresh: Boolean) {
        updateFiles {
            it.copy(
                templatePath = path,
                templateLoadError = if (isAutoRefresh) it.templateLoadError else null,
                templateAvailableTags = if (isAutoRefresh) it.templateAvailableTags else null,
                isTemplateInspectionInProgress = path.isNotBlank(),
            )
        }
        if (path.isBlank()) return

        scope.launch {
            val placeholders = runCatching {
                withContext(ioDispatcher) { documentGenerator.inspectTemplatePlaceholders(path) }
            }
            updateFiles { current ->
                if (current.templatePath != path) return@updateFiles current
                placeholders.fold(
                    onSuccess = {
                        if (isAutoRefresh) {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_docx_succeeded),
                                false
                            )
                        }
                        current.copy(
                            templateLoadError = null,
                            templateAvailableTags = it,
                            isTemplateInspectionInProgress = false,
                        )
                    },
                    onFailure = { error ->
                        logError(
                            logTag,
                            "Failed to inspect DOCX template placeholders: $path",
                            error
                        )
                        if (isAutoRefresh) {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_docx_failed),
                                true
                            )
                        }
                        current.copy(
                            templateLoadError = docxInspectErrorMessage(),
                            isTemplateInspectionInProgress = false,
                        )
                    },
                )
            }
        }
    }

    fun refreshXlsx(path: String, isAutoRefresh: Boolean) {
        scope.launch {
            val configuration = currentConfiguration()
            val inspected = runCatching {
                withContext(ioDispatcher) { parseRegistrations.inspect(path).getOrThrow() }
            }
            inspected.fold(
                onSuccess = { sheet ->
                    val missingHeaders = missingHeaders(configuration, sheet.headers)
                    updateFiles { current ->
                        if (current.xlsxPath != path) return@updateFiles current
                        current.copy(
                            xlsxHeaders = sheet.headers,
                            xlsxLoadError = if (missingHeaders.isNotEmpty()) {
                                xlsxMissingHeadersErrorMessage(missingHeaders.joinToString(", "))
                            } else {
                                null
                            },
                        )
                    }
                    if (missingHeaders.isNotEmpty()) {
                        if (!isAutoRefresh && currentFiles().xlsxPath == path) {
                            setParsedEntries(path, emptyList())
                        } else if (isAutoRefresh && currentFiles().xlsxPath == path) {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_xlsx_failed),
                                true
                            )
                        }
                        return@fold
                    }

                    val parsed = runCatching {
                        withContext(ioDispatcher) {
                            parseRegistrations(
                                path,
                                configuration
                            ).getOrThrow()
                        }
                    }
                    val entries = parsed.getOrElse { error ->
                        logError(logTag, "Failed to parse XLSX: $path", error)
                        if (!isAutoRefresh) emptyList() else currentEntries()
                    }
                    if (parsed.isSuccess && currentFiles().xlsxPath == path) {
                        setParsedEntries(path, entries)
                        if (isAutoRefresh) {
                            postNotification(
                                UiMessage(Res.string.conversion_refresh_xlsx_succeeded),
                                false
                            )
                        }
                    } else if (isAutoRefresh && currentFiles().xlsxPath == path) {
                        postNotification(UiMessage(Res.string.conversion_refresh_xlsx_failed), true)
                    }
                    updateFiles { current ->
                        if (current.xlsxPath != path) return@updateFiles current
                        current.copy(
                            xlsxLoadError = when {
                                parsed.exceptionOrNull() is XlsxMissingValuesException -> {
                                    xlsxMissingValuesErrorMessage(
                                        parsed.exceptionOrNull()?.message.orEmpty(),
                                    )
                                }

                                parsed.isFailure -> xlsxParseErrorMessage()
                                entries.isEmpty() -> UiMessage(Res.string.conversion_error_no_entries)
                                else -> null
                            },
                        )
                    }
                },
                onFailure = { error ->
                    logError(logTag, "Failed to inspect XLSX: $path", error)
                    if (!isAutoRefresh && currentFiles().xlsxPath == path) {
                        setParsedEntries(path, emptyList())
                    }
                    updateFiles { current ->
                        if (current.xlsxPath != path) return@updateFiles current
                        current.copy(xlsxLoadError = xlsxParseErrorMessage())
                    }
                    if (isAutoRefresh && currentFiles().xlsxPath == path) {
                        postNotification(UiMessage(Res.string.conversion_refresh_xlsx_failed), true)
                    }
                },
            )
        }
    }

    fun restartXlsxWatcher(path: String) {
        xlsxWatcher?.cancel()
        xlsxRefreshJob?.cancel()
        xlsxWatcher = if (path.isBlank()) {
            null
        } else {
            fileChangeObserver.watch(path) { scheduleXlsxAutoRefresh(path) }
        }
    }

    fun restartTemplateWatcher(path: String) {
        templateWatcher?.cancel()
        templateRefreshJob?.cancel()
        templateWatcher = if (path.isBlank()) {
            null
        } else {
            fileChangeObserver.watch(path) { scheduleTemplateAutoRefresh(path) }
        }
    }

    fun cancel() {
        xlsxWatcher?.cancel()
        templateWatcher?.cancel()
        xlsxRefreshJob?.cancel()
        templateRefreshJob?.cancel()
    }

    private fun scheduleXlsxAutoRefresh(path: String) {
        xlsxRefreshJob?.cancel()
        xlsxRefreshJob = scope.launch {
            delay(refreshDebounceMillis)
            if (currentFiles().xlsxPath == path) {
                logInfo(logTag, "Detected XLSX file change: $path")
                refreshXlsx(path, isAutoRefresh = true)
            }
        }
    }

    private fun scheduleTemplateAutoRefresh(path: String) {
        templateRefreshJob?.cancel()
        templateRefreshJob = scope.launch {
            delay(refreshDebounceMillis)
            if (currentFiles().templatePath == path) {
                logInfo(logTag, "Detected DOCX file change: $path")
                refreshTemplate(path, isAutoRefresh = true)
            }
        }
    }
}

internal fun missingHeaders(
    configuration: CertificateConfiguration,
    actualHeaders: List<String>,
): List<String> {
    return configuration.xlsxFields.mapNotNull { field ->
        val expectedHeader = field.headerName?.trim().takeUnless { it.isNullOrBlank() }
        if (expectedHeader == null || expectedHeader !in actualHeaders) {
            val descriptor = field.label?.takeIf { it.isNotBlank() } ?: field.tag
            "$descriptor -> ${expectedHeader ?: "?"}"
        } else {
            null
        }
    }
}
