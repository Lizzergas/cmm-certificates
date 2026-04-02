package com.cmm.certificates.feature.emailsending.domain.usecase

import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.EmailProgressState
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettings
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SendEmailRequestsUseCaseTest {

    @Test
    fun failsImmediatelyWhenNetworkIsUnavailable() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            connectivityMonitor = FakeConnectivityMonitor(false),
            emailGateway = FakeSendGateway(),
        )

        useCase(listOf(sampleRequest()))

        assertEquals(EmailStopReason.NetworkUnavailable, repository.failedReason)
    }

    @Test
    fun failsWhenSmtpSettingsAreMissing() = runBlocking {
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = FakeEmailProgressRepository(),
            settingsRepository = FakeSettingsRepository(
                FakeSettingsRepository.defaultState().copy(
                    smtp = FakeSettingsRepository.defaultState().smtp.copy(port = ""),
                )
            ),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(),
        )

        val repository = FakeEmailProgressRepository()
        val target = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(
                FakeSettingsRepository.defaultState().copy(
                    smtp = FakeSettingsRepository.defaultState().smtp.copy(port = ""),
                )
            ),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(),
        )

        target(listOf(sampleRequest()))

        assertEquals(EmailStopReason.SmtpSettingsMissing, repository.failedReason)
    }

    @Test
    fun failsWhenSmtpIsNotAuthenticated() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(
                FakeSettingsRepository.defaultState().copy(
                    smtp = FakeSettingsRepository.defaultState().smtp.copy(isAuthenticated = false),
                )
            ),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(),
        )

        useCase(listOf(sampleRequest()))

        assertEquals(EmailStopReason.SmtpAuthRequired, repository.failedReason)
    }

    @Test
    fun failsWhenAttachmentIsMissing() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val missingPath = "/tmp/missing-${Random.nextLong().toString().replace('-', '0')}.pdf"
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(),
        )

        useCase(listOf(sampleRequest().copy(attachmentPath = missingPath, attachmentName = "missing.pdf")))

        assertEquals(EmailStopReason.MissingAttachments("missing.pdf"), repository.failedReason)
    }

    @Test
    fun cachesRemainingRequestsWhenDailyLimitIsReached() = runBlocking {
        val repository = FakeEmailProgressRepository(initialSentCountInLast24Hours = 0)
        val settingsRepository = FakeSettingsRepository(
            FakeSettingsRepository.defaultState().copy(
                email = FakeSettingsRepository.defaultState().email.copy(dailyLimit = 1)
            )
        )
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = settingsRepository,
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(),
        )

        useCase(listOf(sampleRequest("1"), sampleRequest("2")))

        val failed = assertIs<EmailStopReason.Cached>(repository.failedReason)
        assertEquals(1, failed.count)
        assertEquals(1, repository.cachedBatch?.requests?.size)
        assertEquals(1, repository.recordedSends)
    }

    @Test
    fun cachesRequestsWhenGatewayReportsQuotaFailure() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(failure = IllegalStateException("quota exceeded")),
        )

        useCase(listOf(sampleRequest("1"), sampleRequest("2")))

        val failed = assertIs<EmailStopReason.Cached>(repository.failedReason)
        assertEquals(EmailStopReason.GmailQuotaExceeded, failed.reason)
        assertEquals(2, repository.cachedBatch?.requests?.size)
    }

    @Test
    fun cachesRequestsWhenGatewayStopsAfterConsecutiveErrors() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(failure = IllegalStateException("temporary failure")),
        )

        useCase(listOf(sampleRequest("1"), sampleRequest("2"), sampleRequest("3"), sampleRequest("4")))

        val failed = assertIs<EmailStopReason.Cached>(repository.failedReason)
        assertEquals(EmailStopReason.ConsecutiveErrors(3), failed.reason)
        assertEquals(4, repository.cachedBatch?.requests?.size)
    }

    @Test
    fun cachesRemainingRequestsWhenGatewayCancellationOccurs() = runBlocking {
        val repository = FakeEmailProgressRepository()
        val useCase = SendEmailRequestsUseCase(
            emailProgressRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true),
            emailGateway = FakeSendGateway(cancelAfterFirstSuccess = true),
        )

        useCase(listOf(sampleRequest("1"), sampleRequest("2")))

        assertEquals(EmailStopReason.Cancelled, repository.cachedBatch?.lastReason)
        assertEquals(1, repository.cachedBatch?.requests?.size)
    }

    private fun sampleRequest(id: String = "1"): EmailSendRequest {
        return EmailSendRequest(
            toEmail = "user$id@example.com",
            toName = "User $id",
            certificateName = "Certificate $id",
            subject = "Subject",
            body = "Body",
        )
    }
}

private class FakeConnectivityMonitor(isAvailable: Boolean) : ConnectivityMonitor {
    override val isNetworkAvailable: StateFlow<Boolean> = MutableStateFlow(isAvailable)
    override fun refresh() = Unit
}

private class FakeSendGateway(
    private val failure: Exception? = null,
    private val cancelAfterFirstSuccess: Boolean = false,
) : EmailGateway {
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
            if (isCancelRequested()) return
            onSending(request)
            val exception = failure
            if (exception == null) {
                onSuccess(index)
                if (cancelAfterFirstSuccess) throw CancellationException("cancelled")
            } else {
                onFailure(index, exception)
            }
        }
    }
}

private class FakeEmailProgressRepository(
    initialSentCountInLast24Hours: Int = 0,
) : EmailProgressRepository {
    override val state: StateFlow<EmailProgressState> = MutableStateFlow(EmailProgressState())
    override val cachedEmails: Flow<CachedEmailBatch> = flowOf(CachedEmailBatch())
    override val sentHistory: Flow<List<SentEmailRecord>> = flowOf(emptyList())
    override val sentCountInLast24Hours: Flow<Int> = flowOf(initialSentCountInLast24Hours)
    var failedReason: EmailStopReason? = null
    var cachedBatch: CachedEmailBatch? = null
    var recordedSends: Int = 0

    override fun start(total: Int) = Unit
    override fun update(current: Int) = Unit
    override fun setCurrentRecipient(recipient: String?) = Unit
    override fun finish() = Unit
    override fun fail(reason: EmailStopReason) {
        failedReason = reason
    }

    override fun requestCancel() = Unit
    override fun isCancelRequested(): Boolean = false
    override fun clear() = Unit
    override suspend fun cacheEmails(batch: CachedEmailBatch) {
        cachedBatch = batch
    }

    override suspend fun clearCachedEmails() {
        cachedBatch = null
    }

    override suspend fun getSentCountInLast24Hours(): Int = this.sentCountInLast24Hours.first()
    override suspend fun recordSuccessfulSend(request: EmailSendRequest) {
        recordedSends++
    }

    override suspend fun removeCachedEmail(id: String) = Unit

    override suspend fun clearSentHistory() = Unit
}

private class FakeSettingsRepository(
    initialState: SettingsState = defaultState(),
) : SettingsRepository {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<SettingsState> = _state

    override fun setHost(value: String) = Unit
    override fun setPort(value: String) = Unit
    override fun setUsername(value: String) = Unit
    override fun setPassword(value: String) = Unit
    override fun setTransport(value: SmtpTransport) = Unit
    override fun setSubject(value: String) = Unit
    override fun setBody(value: String) = Unit
    override fun setSignatureHtml(value: String) = Unit
    override fun setOutputDirectory(value: String) = Unit
    override fun setPreviewEmail(value: String) = Unit
    override fun setDailyLimit(value: Int) = Unit
    override fun setThemeMode(value: AppThemeMode) = Unit
    override fun setUseInAppPdfPreview(value: Boolean) = Unit
    override suspend fun save() = Unit
    override suspend fun authenticate(): Boolean = true
    override suspend fun resetAndClear() = Unit

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
                    body = "Hello",
                    signatureHtml = "<div>Best regards</div>",
                    dailyLimit = 450,
                ),
                certificate = CertificateSettingsState(),
            )
        }
    }
}
