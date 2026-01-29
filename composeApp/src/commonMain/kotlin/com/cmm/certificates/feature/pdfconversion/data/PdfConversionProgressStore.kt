package com.cmm.certificates.feature.pdfconversion.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import com.cmm.certificates.data.xlsx.RegistrationEntry

data class PdfConversionProgressState(
    val current: Int = 0,
    val total: Int = 0,
    val inProgress: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: String? = null,
    val outputDir: String = "",
    val docIdStart: Long? = null,
    val entries: List<RegistrationEntry> = emptyList(),
    val currentDocId: Long? = null,
    val cancelRequested: Boolean = false,
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
)

class PdfConversionProgressStore {
    private val _state = MutableStateFlow(PdfConversionProgressState())
    val state: StateFlow<PdfConversionProgressState> = _state

    fun start(
        total: Int,
        outputDir: String,
        docIdStart: Long,
        entries: List<RegistrationEntry>,
    ) {
        _state.value = PdfConversionProgressState(
            current = 0,
            total = total,
            inProgress = true,
            outputDir = outputDir,
            docIdStart = docIdStart,
            entries = entries,
            cancelRequested = false,
            startedAtMillis = nowMillis(),
        )
    }

    fun update(current: Int) {
        _state.update {
            it.copy(
                current = current,
                inProgress = true,
            )
        }
    }

    fun setCurrentDocId(docId: Long?) {
        _state.update { it.copy(currentDocId = docId) }
    }

    fun finish() {
        _state.update {
            it.copy(
                current = it.total,
                inProgress = false,
                completed = true,
                currentDocId = null,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun fail(message: String) {
        _state.update {
            it.copy(
                inProgress = false,
                errorMessage = message,
                currentDocId = null,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun requestCancel() {
        _state.update {
            it.copy(
                inProgress = false,
                currentDocId = null,
                cancelRequested = true,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun isCancelRequested(): Boolean = _state.value.cancelRequested

    fun clear() {
        _state.value = PdfConversionProgressState()
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
