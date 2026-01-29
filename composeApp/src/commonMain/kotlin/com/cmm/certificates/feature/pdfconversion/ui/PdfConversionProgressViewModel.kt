package com.cmm.certificates.feature.pdfconversion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.data.network.NETWORK_UNAVAILABLE_MESSAGE
import com.cmm.certificates.data.network.NetworkService
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max

class PdfConversionProgressViewModel(
    private val progressRepository: PdfConversionProgressRepository,
    settingsRepository: SettingsRepository,
    networkService: NetworkService,
) : ViewModel() {
    val uiState: StateFlow<PdfConversionProgressUiState> = combine(
        progressRepository.state,
        settingsRepository.state,
        networkService.isNetworkAvailable,
    ) { progressState, settingsState, networkAvailable ->
        val total = max(progressState.total, 0)
        val current = progressState.current.coerceAtLeast(0)
        val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
        val isNetworkError = progressState.errorMessage == NETWORK_UNAVAILABLE_MESSAGE
        PdfConversionProgressUiState(
            current = current,
            total = total,
            progress = progress,
            completed = progressState.completed,
            errorMessage = progressState.errorMessage,
            outputDir = progressState.outputDir,
            durationText = formatDuration(
                progressState.startedAtMillis,
                progressState.endedAtMillis,
            ),
            currentDocId = progressState.currentDocId,
            isNetworkAvailable = networkAvailable,
            isNetworkError = isNetworkError,
            isSmtpAuthenticated = settingsState.smtp.isAuthenticated,
            isSendEmailsEnabled = settingsState.smtp.isAuthenticated && networkAvailable,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PdfConversionProgressUiState(),
    )

    fun requestCancel() {
        progressRepository.requestCancel()
    }
}

data class PdfConversionProgressUiState(
    val current: Int = 0,
    val total: Int = 0,
    val progress: Float = 0f,
    val completed: Boolean = false,
    val errorMessage: String? = null,
    val outputDir: String = "",
    val durationText: String = "0s",
    val currentDocId: Long? = null,
    val isNetworkAvailable: Boolean = true,
    val isNetworkError: Boolean = false,
    val isSmtpAuthenticated: Boolean = false,
    val isSendEmailsEnabled: Boolean = false,
)

private fun formatDuration(startedAtMillis: Long?, endedAtMillis: Long?): String {
    if (startedAtMillis == null) return "0s"
    val end = endedAtMillis ?: return "0s"
    val totalSeconds = ((end - startedAtMillis) / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
