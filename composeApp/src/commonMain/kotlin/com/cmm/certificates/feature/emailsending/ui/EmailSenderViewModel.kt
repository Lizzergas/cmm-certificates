package com.cmm.certificates.feature.emailsending.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val uiState: StateFlow<EmailProgressUiState> = emailProgressRepository.state
        .map { state ->
            val total = state.total.coerceAtLeast(0)
            val current = state.current.coerceAtLeast(0)
            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            EmailProgressUiState(
                current = current,
                total = total,
                progress = progress,
                inProgress = state.inProgress,
                completed = state.completed,
                errorMessage = state.errorMessage,
                currentRecipient = state.currentRecipient,
            )
        }
        .stateIn(
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
            val htmlBody = buildHtmlBody(
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

    private fun buildHtmlBody(
        body: String,
        signatureHtml: String,
    ): String? {
        val trimmedSignature = signatureHtml.trim()
        if (trimmedSignature.isBlank()) return null
        val escapedBody = escapeHtml(body)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "<br>")
        val baseHtml = if (escapedBody.isBlank()) "" else "$escapedBody<br><br>"
        return baseHtml + trimmedSignature
    }

    private fun escapeHtml(text: String): String {
        return buildString(text.length) {
            text.forEach { ch ->
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(ch)
                }
            }
        }
    }
}

data class EmailProgressUiState(
    val current: Int = 0,
    val total: Int = 0,
    val progress: Float = 0f,
    val inProgress: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: String? = null,
    val currentRecipient: String? = null,
)
