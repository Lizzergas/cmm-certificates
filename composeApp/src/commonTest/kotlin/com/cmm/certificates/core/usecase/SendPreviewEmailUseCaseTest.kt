package com.cmm.certificates.core.usecase

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_error_doc_id_missing
import certificates.composeapp.generated.resources.common_error_email_required
import certificates.composeapp.generated.resources.common_error_output_dir_missing
import certificates.composeapp.generated.resources.common_error_smtp_incomplete
import certificates.composeapp.generated.resources.common_error_smtp_auth_required
import certificates.composeapp.generated.resources.email_preview_error_send_failed
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressState
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettings
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SendPreviewEmailUseCaseTest {

    @Test
    fun returnsFailureWhenEmailIsBlank() = runBlocking {
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = FakeSettingsRepository(),
            pdfConversionProgressRepository = FakePdfProgressRepository(),
            emailGateway = FakeEmailGateway(),
        )

        val result = useCase.sendPreviewEmail("   ", attachFirstPdf = false)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.common_error_email_required, failure.message.resource)
    }

    @Test
    fun returnsFailureWhenSmtpIsNotAuthenticated() = runBlocking {
        val settingsRepository = FakeSettingsRepository(
            initialState = FakeSettingsRepository.defaultState().copy(
                smtp = FakeSettingsRepository.defaultState().smtp.copy(isAuthenticated = false),
            )
        )
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = settingsRepository,
            pdfConversionProgressRepository = FakePdfProgressRepository(),
            emailGateway = FakeEmailGateway(),
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = false)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.common_error_smtp_auth_required, failure.message.resource)
    }

    @Test
    fun returnsFailureWhenSmtpSettingsAreIncomplete() = runBlocking {
        val settingsRepository = FakeSettingsRepository(
            initialState = FakeSettingsRepository.defaultState().copy(
                smtp = FakeSettingsRepository.defaultState().smtp.copy(port = ""),
            )
        )
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = settingsRepository,
            pdfConversionProgressRepository = FakePdfProgressRepository(),
            emailGateway = FakeEmailGateway(),
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = false)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.common_error_smtp_incomplete, failure.message.resource)
    }

    @Test
    fun returnsFailureWhenAttachmentOutputDirectoryIsMissing() = runBlocking {
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = FakeSettingsRepository(),
            pdfConversionProgressRepository = FakePdfProgressRepository(
                initialState = PdfConversionProgressState(outputDir = "", docIdStart = 500)
            ),
            emailGateway = FakeEmailGateway(),
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = true)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.common_error_output_dir_missing, failure.message.resource)
    }

    @Test
    fun returnsFailureWhenAttachmentDocIdIsMissing() = runBlocking {
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = FakeSettingsRepository(),
            pdfConversionProgressRepository = FakePdfProgressRepository(
                initialState = PdfConversionProgressState(outputDir = "/tmp/output", docIdStart = null)
            ),
            emailGateway = FakeEmailGateway(),
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = true)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.common_error_doc_id_missing, failure.message.resource)
    }

    @Test
    fun returnsFailureWhenGatewaySendFails() = runBlocking {
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = FakeSettingsRepository(),
            pdfConversionProgressRepository = FakePdfProgressRepository(),
            emailGateway = FakeEmailGateway(failure = IllegalStateException("boom")),
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = false)

        val failure = assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Failure>(result)
        assertEquals(Res.string.email_preview_error_send_failed, failure.message.resource)
    }

    @Test
    fun sendsPreviewEmailAndPersistsPreviewAddress() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val emailGateway = FakeEmailGateway()
        val pdfRepository = FakePdfProgressRepository(
            initialState = PdfConversionProgressState(
                outputDir = "/tmp/output",
                docIdStart = 500,
                feedbackUrl = "https://example.com/form",
                entries = listOf(sampleEntry()),
            )
        )
        val useCase = SendPreviewEmailUseCase(
            settingsRepository = settingsRepository,
            pdfConversionProgressRepository = pdfRepository,
            emailGateway = emailGateway,
        )

        val result = useCase.sendPreviewEmail("preview@example.com", attachFirstPdf = true)

        assertIs<SendPreviewEmailUseCase.PreviewEmailResult.Success>(result)
        assertEquals("preview@example.com", settingsRepository.state.value.email.previewEmail)
        assertEquals(true, settingsRepository.saved)
        assertEquals("/tmp/output/500.pdf", emailGateway.sentRequests.single().attachmentPath)
        assertEquals(
            "Hello https://example.com/form",
            emailGateway.sentRequests.single().body,
        )
    }

    private fun sampleEntry(): RegistrationEntry {
        return RegistrationEntry(
            primaryEmail = "ada@example.com",
            name = "Ada",
            surname = "Lovelace",
            institution = "CMM",
            forEvent = "Workshop",
            publicityApproval = "yes",
        )
    }
}

private class FakeSettingsRepository(
    initialState: SettingsState = defaultState(),
) : SettingsRepository {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<SettingsState> = _state
    var saved: Boolean = false

    override fun setHost(value: String) {}
    override fun setPort(value: String) {}
    override fun setUsername(value: String) {}
    override fun setPassword(value: String) {}
    override fun setTransport(value: SmtpTransport) {}
    override fun setSubject(value: String) {}
    override fun setBody(value: String) {}
    override fun setSignatureHtml(value: String) {}
    override fun setAccreditedTypeOptions(value: String) {}
    override fun setOutputDirectory(value: String) {}
    override fun setPreviewEmail(value: String) {
        _state.value = _state.value.copy(email = _state.value.email.copy(previewEmail = value))
    }

    override fun setDailyLimit(value: Int) {}
    override fun setThemeMode(value: AppThemeMode) {}
    override fun setUseInAppPdfPreview(value: Boolean) {}

    override suspend fun save() {
        saved = true
    }

    override suspend fun authenticate(): Boolean = true

    override suspend fun resetAndClear() {}

    companion object {
        fun defaultState(): SettingsState {
            return SettingsState(
                smtp = SmtpSettingsState(
                    host = "smtp.example.com",
                    port = "465",
                    username = "user@example.com",
                    password = "secret",
                    transport = SmtpTransport.SMTPS,
                    isAuthenticated = true,
                ),
                email = EmailTemplateSettingsState(
                    subject = "Preview",
                    body = "Hello {{feedback_url}}",
                    signatureHtml = "<div>Best regards</div>",
                ),
                certificate = CertificateSettingsState(),
            )
        }
    }
}

private class FakePdfProgressRepository(
    initialState: PdfConversionProgressState = PdfConversionProgressState(),
) : PdfConversionProgressRepository {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<PdfConversionProgressState> = _state

    override fun start(
        total: Int,
        outputDir: String,
        certificateName: String,
        docIdStart: Long,
        feedbackUrl: String,
        entries: List<RegistrationEntry>,
    ) {}
    override fun update(current: Int) {}
    override fun setCurrentDocId(docId: Long?) {}
    override fun finish() {}
    override fun fail(message: UiMessage) {}
    override fun requestCancel() {}
    override fun isCancelRequested(): Boolean = false
    override fun clear() {}
}

private class FakeEmailGateway(
    private val failure: Exception? = null,
) : EmailGateway {
    val sentRequests = mutableListOf<EmailSendRequest>()

    override suspend fun testConnection(settings: SmtpSettings) = Unit

    override suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, exception: Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    ) {
        requests.forEachIndexed { index, request ->
            onSending(request)
            sentRequests += request
            val exception = failure
            if (exception == null) {
                onSuccess(index)
            } else {
                onFailure(index, exception)
            }
        }
    }
}
