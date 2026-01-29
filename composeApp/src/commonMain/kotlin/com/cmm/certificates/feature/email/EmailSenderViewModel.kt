package com.cmm.certificates.feature.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.email.SmtpSettingsRepository
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.feature.progress.PdfConversionProgressStore
import com.cmm.certificates.feature.settings.SmtpSettingsStore
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

private const val DEFAULT_SUBJECT = SmtpSettingsRepository.DEFAULT_EMAIL_SUBJECT
private const val DEFAULT_BODY = SmtpSettingsRepository.DEFAULT_EMAIL_BODY

class EmailSenderViewModel(
    private val emailProgressStore: EmailProgressStore,
    private val pdfConversionProgressStore: PdfConversionProgressStore,
    private val smtpSettingsStore: SmtpSettingsStore,
) : ViewModel() {
    private var sendJob: Job? = null
    val uiState: StateFlow<EmailProgressUiState> = emailProgressStore.state
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
        val snapshot = emailProgressStore.state.value
        if (snapshot.inProgress) return
        sendJob = viewModelScope.launch {
            try {
                sendEmails()
            } finally {
                sendJob = null
            }
        }
    }

    fun cancelSending() {
        emailProgressStore.requestCancel()
        sendJob?.cancel()
    }

    private suspend fun sendEmails() {
        val smtpState = smtpSettingsStore.state.value
        val settings = smtpState.toSettings()
        if (settings == null || !smtpState.isAuthenticated) {
            emailProgressStore.fail("SMTP authentication is required.")
            return
        }

        val conversionState = pdfConversionProgressStore.state.value
        val docIdStart = conversionState.docIdStart
        if (docIdStart == null) {
            emailProgressStore.fail("Document ID start is missing.")
            return
        }
        if (conversionState.outputDir.isBlank()) {
            emailProgressStore.fail("Output folder is missing.")
            return
        }
        if (conversionState.entries.isEmpty()) {
            emailProgressStore.fail("No entries to send.")
            return
        }

        try {
            val subject = smtpState.subject.ifBlank { DEFAULT_SUBJECT }
            val body = smtpState.body.ifBlank { DEFAULT_BODY }
            val htmlBody = buildHtmlBody(
                body = body,
                signatureHtml = smtpState.signatureHtml,
            )
            val requests = buildRequests(
                conversionState.entries,
                docIdStart,
                conversionState.outputDir,
                subject,
                body,
                htmlBody,
            )
            if (requests.isEmpty()) {
                emailProgressStore.fail("No emails to send.")
                return
            }

            emailProgressStore.start(requests.size)
            val context = currentCoroutineContext()
            withContext(Dispatchers.IO) {
                SmtpClient.sendBatch(
                    settings = settings,
                    requests = requests,
                    onSending = { request ->
                        val recipient = "${request.toName} <${request.toEmail}>"
                        emailProgressStore.setCurrentRecipient(recipient)
                    },
                    onProgress = { emailProgressStore.update(it) },
                    isCancelRequested = {
                        !context.isActive || emailProgressStore.isCancelRequested()
                    },
                )
            }
            currentCoroutineContext().ensureActive()
            if (!emailProgressStore.isCancelRequested()) {
                emailProgressStore.finish()
            }
        } catch (e: CancellationException) {
            emailProgressStore.requestCancel()
        } catch (e: Exception) {
            emailProgressStore.fail(e.message ?: "Failed to send emails.")
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
            if (email.isBlank()) {
                throw IllegalStateException("Missing email address for entry ${index + 1}.")
            }
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
        val escapedBody = escapeHtml(body).replace("\n", "<br>")
        val baseHtml = if (escapedBody.isBlank()) "" else escapedBody + "<br><br>"
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
