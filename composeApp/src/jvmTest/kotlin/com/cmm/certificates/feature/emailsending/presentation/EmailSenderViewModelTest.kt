package com.cmm.certificates.feature.emailsending.presentation

import com.cmm.certificates.core.domain.AppCapabilities
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailProgressState
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.emailsending.domain.usecase.BuildEmailRequestsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.RetryCachedEmailsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.SendEmailRequestsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.SendGeneratedEmailsUseCase
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressState
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettings
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EmailSenderViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cancelSending_requestsRepositoryCancellation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = PreviewEmailProgressRepository(cachedCount = 1)
        val viewModel = createViewModel(
            repository = repository,
            cachedCount = 1,
            authenticated = true,
            canSendEmails = true,
            networkAvailable = true
        )

        viewModel.cancelSending()

        assertEquals(true, repository.cancelRequested)
    }

    @Test
    fun startSendingIfIdle_doesNothingWhenEmailSendingIsUnsupported() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = PreviewEmailProgressRepository(cachedCount = 1)
        val viewModel = createViewModel(
            repository = repository,
            cachedCount = 1,
            authenticated = true,
            canSendEmails = false,
            networkAvailable = true
        )

        viewModel.startSendingIfIdle()

        assertEquals(0, repository.clearCalls)
    }

    @Test
    fun retryCachedEmails_doesNothingWhenEmailSendingIsUnsupported() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = PreviewEmailProgressRepository(cachedCount = 1)
        val viewModel = createViewModel(
            repository = repository,
            cachedCount = 1,
            authenticated = true,
            canSendEmails = false,
            networkAvailable = true
        )

        viewModel.retryCachedEmails()

        assertEquals(0, repository.clearCalls)
    }

    private fun createViewModel(
        repository: PreviewEmailProgressRepository = PreviewEmailProgressRepository(cachedCount = 0),
        cachedCount: Int,
        authenticated: Boolean,
        canSendEmails: Boolean,
        networkAvailable: Boolean,
    ): EmailSenderViewModel {
        val settingsRepository = PreviewSettingsRepository(authenticated)
        val connectivityMonitor = PreviewConnectivityMonitor(networkAvailable)
        val sendEmailRequests = SendEmailRequestsUseCase(
            repository,
            settingsRepository,
            connectivityMonitor,
            PreviewEmailGateway()
        )
        val sendGeneratedEmails = SendGeneratedEmailsUseCase(
            emailProgressRepository = repository,
            pdfConversionProgressRepository = PreviewPdfProgressRepository(),
            settingsRepository = settingsRepository,
            buildEmailRequests = BuildEmailRequestsUseCase(),
            sendEmailRequests = sendEmailRequests,
        )
        val retryCachedEmails = RetryCachedEmailsUseCase(repository, sendEmailRequests)

        return EmailSenderViewModel(
            emailProgressRepository = repository,
            settingsRepository = settingsRepository,
            connectivityMonitor = connectivityMonitor,
            capabilityProvider = PreviewCapabilityProvider(canSendEmails),
            sendGeneratedEmails = sendGeneratedEmails,
            retryCachedEmailsUseCase = retryCachedEmails,
        )
    }
}

private class PreviewEmailProgressRepository(cachedCount: Int) : EmailProgressRepository {
    override val state: StateFlow<EmailProgressState> = MutableStateFlow(EmailProgressState())
    override val cachedEmails: Flow<CachedEmailBatch?> = MutableStateFlow(
        CachedEmailBatch(List(cachedCount) { index ->
            EmailSendRequest("user$index@example.com", "User $index", "Subject", "Body")
        })
    )
    override val sentCountInLast24Hours: Flow<Int> = MutableStateFlow(0)
    var cancelRequested: Boolean = false
    var clearCalls: Int = 0
    override fun start(total: Int) = Unit
    override fun update(current: Int) = Unit
    override fun setCurrentRecipient(recipient: String?) = Unit
    override fun finish() = Unit
    override fun fail(reason: EmailStopReason) = Unit
    override fun requestCancel() {
        cancelRequested = true
    }

    override fun isCancelRequested(): Boolean = cancelRequested
    override fun clear() {
        clearCalls++
    }

    override suspend fun cacheEmails(batch: CachedEmailBatch) = Unit
    override suspend fun clearCachedEmails() = Unit
    override suspend fun getSentCountInLast24Hours(): Int = 0
    override suspend fun recordSuccessfulSend() = Unit
    override suspend fun clearSentHistory() = Unit
}

private class PreviewSettingsRepository(authenticated: Boolean) : SettingsRepository {
    override val state: StateFlow<SettingsState> = MutableStateFlow(
        SettingsState(
            smtp = SmtpSettingsState(
                host = "smtp.example.com",
                port = "465",
                username = "user@example.com",
                password = "secret",
                transport = SmtpTransport.SMTPS,
                isAuthenticated = authenticated,
            ),
            email = EmailTemplateSettingsState(
                subject = "Subject",
                body = "Body",
                signatureHtml = "<div/>"
            ),
            certificate = CertificateSettingsState(),
        )
    )

    override fun setHost(value: String) = Unit
    override fun setPort(value: String) = Unit
    override fun setUsername(value: String) = Unit
    override fun setPassword(value: String) = Unit
    override fun setTransport(value: SmtpTransport) = Unit
    override fun setSubject(value: String) = Unit
    override fun setBody(value: String) = Unit
    override fun setSignatureHtml(value: String) = Unit
    override fun setAccreditedTypeOptions(value: String) = Unit
    override fun setPreviewEmail(value: String) = Unit
    override fun setDailyLimit(value: Int) = Unit
    override suspend fun save() = Unit
    override suspend fun authenticate(): Boolean = true
    override suspend fun resetAndClear() = Unit
}

private class PreviewConnectivityMonitor(isAvailable: Boolean) : ConnectivityMonitor {
    override val isNetworkAvailable: StateFlow<Boolean> = MutableStateFlow(isAvailable)
    override fun refresh() = Unit
}

private class PreviewCapabilityProvider(canSendEmails: Boolean) : PlatformCapabilityProvider {
    override val capabilities = AppCapabilities(true, true, canSendEmails, true, true)
}

private class PreviewPdfProgressRepository : PdfConversionProgressRepository {
    override val state: StateFlow<PdfConversionProgressState> =
        MutableStateFlow(PdfConversionProgressState())

    override fun start(
        total: Int,
        outputDir: String,
        docIdStart: Long,
        entries: List<RegistrationEntry>,
    ) = Unit

    override fun update(current: Int) = Unit
    override fun setCurrentDocId(docId: Long?) = Unit
    override fun finish() = Unit
    override fun fail(message: UiMessage) = Unit
    override fun requestCancel() = Unit
    override fun isCancelRequested(): Boolean = false
    override fun clear() = Unit
}

private class PreviewEmailGateway : EmailGateway {
    override suspend fun testConnection(settings: SmtpSettings) = Unit
    override suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, exception: Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    ) = Unit
}
