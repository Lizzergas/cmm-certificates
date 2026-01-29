package com.cmm.certificates.feature.pdfconversion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.usecase.SendPreviewEmailUseCase
import com.cmm.certificates.data.network.NETWORK_UNAVAILABLE_MESSAGE
import com.cmm.certificates.data.network.NetworkService
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class PdfConversionProgressViewModel(
    private val progressRepository: PdfConversionProgressRepository,
    private val settingsRepository: SettingsRepository,
    networkService: NetworkService,
    private val sendPreviewEmailUseCase: SendPreviewEmailUseCase,
) : ViewModel() {
    private val previewState = MutableStateFlow(
        PdfPreviewUiState(email = settingsRepository.state.value.email.previewEmail)
    )
    val uiState: StateFlow<PdfConversionProgressUiState> = combine(
        progressRepository.state,
        settingsRepository.state,
        networkService.isNetworkAvailable,
        previewState,
    ) { progressState, settingsState, networkAvailable, preview ->
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
            preview = preview,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PdfConversionProgressUiState(),
    )

    fun requestCancel() {
        progressRepository.requestCancel()
    }

    fun preparePreviewDialog() {
        previewState.value = PdfPreviewUiState(
            email = settingsRepository.state.value.email.previewEmail
        )
    }

    fun setPreviewEmail(value: String) {
        previewState.update { current ->
            current.copy(email = value, errorMessage = null, sent = false)
        }
    }

    fun sendPreviewEmail(attachFirstPdf: Boolean) {
        val email = previewState.value.email
        viewModelScope.launch {
            previewState.update { it.copy(isSending = true, errorMessage = null, sent = false) }
            val result = sendPreviewEmailUseCase.sendPreviewEmail(
                toEmail = email,
                attachFirstPdf = attachFirstPdf,
            )
            previewState.update { current ->
                if (result.isSuccess) {
                    current.copy(isSending = false, sent = true)
                } else {
                    current.copy(
                        isSending = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Failed to send preview email.",
                    )
                }
            }
        }
    }

    fun clearPreviewStatus() {
        previewState.update { current ->
            current.copy(errorMessage = null, sent = false, isSending = false)
        }
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
    val preview: PdfPreviewUiState = PdfPreviewUiState(),
)

data class PdfPreviewUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val sent: Boolean = false,
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
