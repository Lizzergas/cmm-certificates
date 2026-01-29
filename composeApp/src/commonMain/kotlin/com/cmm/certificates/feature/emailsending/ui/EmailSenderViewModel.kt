package com.cmm.certificates.feature.emailsending.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.data.email.buildEmailHtmlBody
import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailSenderViewModel(
    private val emailProgressRepository: EmailProgressRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private var sendJob: Job? = null
    val uiState: StateFlow<EmailProgressUiState> =
        emailProgressRepository.state.map { progressState ->
            val total = progressState.total.coerceAtLeast(0)
            val current = progressState.current.coerceAtLeast(0)
            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            val mode = when {
                progressState.errorMessage != null -> EmailProgress.Error(
                    message = progressState.errorMessage
                )

                progressState.completed -> EmailProgress.Success(total = total)
                else -> EmailProgress.Running(
                    current = current,
                    total = total,
                    progress = progress,
                    currentRecipient = progressState.currentRecipient,
                    isInProgress = progressState.inProgress,
                )
            }
            EmailProgressUiState(mode = mode)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EmailProgressUiState(),
        )

    fun startSendingIfIdle() {
        if (sendJob?.isActive == true) return
        val snapshot = emailProgressRepository.state.value
        if (snapshot.inProgress) return
        if (snapshot.completed || snapshot.errorMessage != null) {
            emailProgressRepository.clear()
        }
        sendJob = viewModelScope.launch {
            try {
                sendEmails()
            } finally {
                sendJob = null
            }
        }
    }

    fun cancelSending() {
        emailProgressRepository.requestCancel()
        sendJob?.cancel()
    }

    private suspend fun sendEmails() {
        val settingsState = settingsRepository.state.value
        val smtpState = settingsState.smtp
        val settings = smtpState.toSmtpSettings()
        if (settings == null || !smtpState.isAuthenticated) {
            emailProgressRepository.fail("SMTP authentication is required.")
            return
        }

        val conversionState = pdfConversionProgressRepository.state.value
        val docIdStart = conversionState.docIdStart
        if (docIdStart == null) {
            emailProgressRepository.fail("Document ID start is missing.")
            return
        }
        if (conversionState.outputDir.isBlank()) {
            emailProgressRepository.fail("Output folder is missing.")
            return
        }
        if (conversionState.entries.isEmpty()) {
            emailProgressRepository.fail("No entries to send.")
            return
        }

        try {
            val subject = settingsState.email.subject
            val body = settingsState.email.body
            val htmlBody = buildEmailHtmlBody(
                body = body,
                signatureHtml = settingsState.email.signatureHtml,
            )
            val missingEmailIndexes = conversionState.entries.mapIndexedNotNull { index, entry ->
                if (entry.primaryEmail.isBlank()) index + 1 else null
            }
            if (missingEmailIndexes.isNotEmpty()) {
                val preview = missingEmailIndexes.take(3).joinToString(", ")
                val suffix = if (missingEmailIndexes.size > 3) "..." else ""
                emailProgressRepository.fail("Missing email address for entries: $preview$suffix")
                return
            }
            val requests = buildRequests(
                conversionState.entries,
                docIdStart,
                conversionState.outputDir,
                subject,
                body,
                htmlBody,
            )
            if (requests.isEmpty()) {
                emailProgressRepository.fail("No emails to send.")
                return
            }

            emailProgressRepository.start(requests.size)
            val parentContext = currentCoroutineContext()
            withContext(Dispatchers.IO) {
                SmtpClient.sendBatch(
                    settings = settings,
                    requests = requests,
                    onSending = { request ->
                        val recipient = "${request.toName} <${request.toEmail}>"
                        emailProgressRepository.setCurrentRecipient(recipient)
                    },
                    onProgress = { emailProgressRepository.update(it) },
                    isCancelRequested = {
                        !parentContext.isActive || emailProgressRepository.isCancelRequested()
                    },
                )
            }
            currentCoroutineContext().ensureActive()
            if (!emailProgressRepository.isCancelRequested()) {
                emailProgressRepository.finish()
            }
        } catch (e: CancellationException) {
            emailProgressRepository.requestCancel()
        } catch (e: Exception) {
            emailProgressRepository.fail(e.message ?: "Failed to send emails.")
        }
    }

    private fun buildRequests(
        entries: List<RegistrationEntry>,
        docIdStart: Long,
        outputDir: String,
        subject: String,
        body: String,
        htmlBody: String?,
    ): List<EmailSendRequest> {
        return entries.mapIndexed { index, entry ->
            val email = entry.primaryEmail.trim()
            val fullName = listOf(entry.name, entry.surname)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { email }
            val docId = docIdStart + index
            EmailSendRequest(
                toEmail = email,
                toName = fullName,
                subject = subject,
                body = body,
                htmlBody = htmlBody,
                attachmentPath = joinPath(outputDir, "${docId}.pdf"),
                attachmentName = "${docId}.pdf",
            )
        }
    }

}

data class EmailProgressUiState(
    val mode: EmailProgress = EmailProgress.Running(),
)

sealed interface EmailProgress {
    data class Running(
        val current: Int = 0,
        val total: Int = 0,
        val progress: Float = 0f,
        val currentRecipient: String? = null,
        val isInProgress: Boolean = false,
    ) : EmailProgress

    data class Success(val total: Int) : EmailProgress

    data class Error(val message: String) : EmailProgress
}
