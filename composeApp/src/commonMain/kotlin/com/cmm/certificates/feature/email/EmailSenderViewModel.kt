package com.cmm.certificates.feature.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.email.SmtpClient
import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.feature.progress.ConversionProgressStore
import com.cmm.certificates.feature.settings.SmtpSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EMAIL_SUBJECT = "Pa\u017Eyma"
private const val EMAIL_BODY = "Certificate attached."

class EmailSenderViewModel(
    private val emailProgressStore: EmailProgressStore,
    private val conversionProgressStore: ConversionProgressStore,
    private val smtpSettingsStore: SmtpSettingsStore,
) : ViewModel() {
    fun startSendingIfIdle() {
        val snapshot = emailProgressStore.state.value
        if (snapshot.inProgress) return
        viewModelScope.launch { sendEmails() }
    }

    private suspend fun sendEmails() {
        val smtpState = smtpSettingsStore.state.value
        val settings = smtpState.toSettings()
        if (settings == null || !smtpState.isAuthenticated) {
            emailProgressStore.fail("SMTP authentication is required.")
            return
        }

        val conversionState = conversionProgressStore.state.value
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
            val requests = buildRequests(
                conversionState.entries,
                docIdStart,
                conversionState.outputDir,
            )
            if (requests.isEmpty()) {
                emailProgressStore.fail("No emails to send.")
                return
            }

            emailProgressStore.start(requests.size)
            withContext(Dispatchers.IO) {
                SmtpClient.sendBatch(
                    settings = settings,
                    requests = requests,
                    onProgress = { emailProgressStore.update(it) },
                    isCancelRequested = { emailProgressStore.isCancelRequested() },
                )
            }
            if (!emailProgressStore.isCancelRequested()) {
                emailProgressStore.finish()
            }
        } catch (e: Exception) {
            emailProgressStore.fail(e.message ?: "Failed to send emails.")
        }
    }

    private fun buildRequests(
        entries: List<RegistrationEntry>,
        docIdStart: Long,
        outputDir: String,
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
                subject = EMAIL_SUBJECT,
                body = EMAIL_BODY,
                attachmentPath = joinPath(outputDir, "${docId}.pdf"),
                attachmentName = "${docId}.pdf",
            )
        }
    }
}

private fun joinPath(directory: String, fileName: String): String {
    val trimmed = directory.trimEnd('/', '\\')
    return if (trimmed.isEmpty()) fileName else "$trimmed/$fileName"
}
