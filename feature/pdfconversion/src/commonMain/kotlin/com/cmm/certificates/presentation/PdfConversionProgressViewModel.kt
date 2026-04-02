package com.cmm.certificates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.usecase.SendPreviewEmailUseCase
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
    connectivityMonitor: ConnectivityMonitor,
    capabilityProvider: PlatformCapabilityProvider,
    private val sendPreviewEmailUseCase: SendPreviewEmailUseCase,
) : ViewModel() {
    private val logTag = "PdfProgressVM"
    private val capabilities = capabilityProvider.capabilities
    private val previewState = MutableStateFlow(
        PdfPreviewUiState(email = settingsRepository.state.value.email.previewEmail)
    )
    val uiState: StateFlow<PdfConversionProgressUiState> = combine(
        progressRepository.state,
        settingsRepository.state,
        connectivityMonitor.isNetworkAvailable,
        previewState,
    ) { progressState, settingsState, networkAvailable, preview ->
        val total = max(progressState.total, 0)
        val current = progressState.current.coerceAtLeast(0)
        val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
        PdfConversionProgressUiState(
            current = current,
            total = total,
            progress = progress,
            completed = progressState.completed,
            errorMessage = progressState.errorMessage,
            outputDir = progressState.outputDir,
            elapsedSeconds = elapsedSeconds(
                progressState.startedAtMillis,
                progressState.endedAtMillis,
            ),
            currentDocId = progressState.currentDocId,
            isNetworkAvailable = networkAvailable,
            isNetworkError = false,
            isSmtpAuthenticated = settingsState.smtp.isAuthenticated,
            supportsEmailSending = capabilities.canSendEmails,
            canOpenGeneratedFolders = capabilities.canOpenGeneratedFolders,
            isSendPreviewEnabled = capabilities.canSendEmails && settingsState.smtp.isAuthenticated && networkAvailable,
            isSendEmailsEnabled = capabilities.canSendEmails &&
                settingsState.smtp.isAuthenticated &&
                networkAvailable,
            preview = preview,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PdfConversionProgressUiState(),
    )

    init {
        val snapshot = progressRepository.state.value
        logInfo(
            logTag,
            "Opened progress screen: inProgress=${snapshot.inProgress}, completed=${snapshot.completed}, total=${snapshot.total}, current=${snapshot.current}",
        )
    }

    fun requestCancel() {
        logWarn(logTag, "User requested conversion cancellation")
        progressRepository.requestCancel()
    }

    fun preparePreviewDialog() {
        logInfo(logTag, "Opening preview email dialog")
        previewState.value = PdfPreviewUiState(
            email = settingsRepository.state.value.email.previewEmail
        )
    }

    fun setPreviewEmail(value: String) {
        logInfo(logTag, "Preview email updated")
        previewState.update { current ->
            current.copy(email = value, errorMessage = null, sent = false)
        }
    }

    fun sendPreviewEmail(attachFirstPdf: Boolean) {
        val email = previewState.value.email
        viewModelScope.launch {
            logInfo(logTag, "Sending preview email to ${email.trim().ifBlank { "<empty>" }} attachFirstPdf=$attachFirstPdf")
            previewState.update { it.copy(isSending = true, errorMessage = null, sent = false) }
            val result = sendPreviewEmailUseCase.sendPreviewEmail(
                toEmail = email,
                attachFirstPdf = attachFirstPdf,
            )
            previewState.update { current ->
                when (result) {
                    SendPreviewEmailUseCase.PreviewEmailResult.Success -> {
                        logInfo(logTag, "Preview email sent successfully")
                        current.copy(isSending = false, sent = true)
                    }

                    is SendPreviewEmailUseCase.PreviewEmailResult.Failure -> {
                        logWarn(logTag, "Preview email failed")
                        current.copy(isSending = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearPreviewStatus() {
        logInfo(logTag, "Clearing preview email dialog status")
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
    val errorMessage: UiMessage? = null,
    val outputDir: String = "",
    val elapsedSeconds: Long = 0,
    val currentDocId: Long? = null,
    val isNetworkAvailable: Boolean = true,
    val isNetworkError: Boolean = false,
    val isSmtpAuthenticated: Boolean = false,
    val supportsEmailSending: Boolean = true,
    val canOpenGeneratedFolders: Boolean = true,
    val isSendPreviewEnabled: Boolean = false,
    val isSendEmailsEnabled: Boolean = false,
    val preview: PdfPreviewUiState = PdfPreviewUiState(),
)

data class PdfPreviewUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val errorMessage: UiMessage? = null,
    val sent: Boolean = false,
)

private fun elapsedSeconds(startedAtMillis: Long?, endedAtMillis: Long?): Long {
    if (startedAtMillis == null || endedAtMillis == null) return 0
    return ((endedAtMillis - startedAtMillis) / 1000).coerceAtLeast(0)
}
