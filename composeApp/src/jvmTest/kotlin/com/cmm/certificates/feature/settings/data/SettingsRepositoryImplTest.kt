package com.cmm.certificates.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_error_smtp_incomplete
import certificates.composeapp.generated.resources.settings_error_auth_failed
import com.cmm.certificates.core.domain.AppCapabilities
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.data.SettingsRepositoryImpl
import com.cmm.certificates.data.SettingsStore
import com.cmm.certificates.data.store.createDataStore
import com.cmm.certificates.data.defaultEmailSubject
import com.cmm.certificates.test.createTestPreferencesFilePath
import com.cmm.certificates.feature.emailsending.domain.EmailSendRequest
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.settings.domain.SmtpSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {

    @Test
    fun authenticate_marksStateAsAuthenticatedWhenGatewaySucceeds() = runBlocking {
        val repository = SettingsRepositoryImpl(
            settingsStore = SettingsStore(testDataStore("settings-auth-success")),
            emailGateway = FakeGateway(),
            capabilityProvider = FakeCapabilityProvider(canSendEmails = true),
        )

        repository.setHost("smtp.example.com")
        repository.setPort("465")
        repository.setUsername("user@example.com")
        repository.setPassword("secret")

        val authenticated = repository.authenticate()

        assertEquals(true, authenticated)
        assertEquals(true, repository.state.value.smtp.isAuthenticated)
    }

    @Test
    fun authenticate_setsLocalizedErrorWhenDetailsAreIncomplete() = runBlocking {
        val repository = SettingsRepositoryImpl(
            settingsStore = SettingsStore(testDataStore("settings-auth-incomplete")),
            emailGateway = FakeGateway(),
            capabilityProvider = FakeCapabilityProvider(canSendEmails = true),
        )

        val authenticated = repository.authenticate()

        assertEquals(false, authenticated)
        assertEquals(Res.string.common_error_smtp_incomplete, repository.state.value.smtp.errorMessage?.resource)
    }

    @Test
    fun save_persistsUpdatedValuesToStore() = runBlocking {
        val dataStore = testDataStore("settings-save")
        val repository = SettingsRepositoryImpl(
            settingsStore = SettingsStore(dataStore),
            emailGateway = FakeGateway(),
            capabilityProvider = FakeCapabilityProvider(canSendEmails = true),
        )
        repository.setHost("smtp.saved.example.com")
        repository.setSubject("Saved Subject")
        repository.setBody("Saved Body")
        repository.setAccreditedTypeOptions("lecture\nseminar")
        repository.setSignatureHtml("<div>Saved signature</div>")
        repository.save()

        val stored = SettingsStore(dataStore).loadOrDefault()

        assertEquals("smtp.saved.example.com", stored.host)
        assertEquals("Saved Subject", stored.subject)
    }

    @Test
    fun authenticate_returnsFalseWhenGatewayThrows() = runBlocking {
        val repository = SettingsRepositoryImpl(
            settingsStore = SettingsStore(testDataStore("settings-auth-failure")),
            emailGateway = FakeGateway(testConnectionFailure = IllegalStateException("boom")),
            capabilityProvider = FakeCapabilityProvider(canSendEmails = true),
        )
        repository.setHost("smtp.example.com")
        repository.setPort("465")
        repository.setUsername("user@example.com")
        repository.setPassword("secret")

        val authenticated = repository.authenticate()

        assertEquals(false, authenticated)
        assertEquals(Res.string.settings_error_auth_failed, repository.state.value.smtp.errorMessage?.resource)
    }

    @Test
    fun resetAndClear_resetsState() = runBlocking {
        val repository = SettingsRepositoryImpl(
            settingsStore = SettingsStore(testDataStore("settings-reset")),
            emailGateway = FakeGateway(),
            capabilityProvider = FakeCapabilityProvider(canSendEmails = true),
        )
        repository.setHost("smtp.example.com")
        repository.setSubject("Saved Subject")

        repository.resetAndClear()

        assertEquals("", repository.state.value.smtp.host)
        assertEquals(defaultEmailSubject(), repository.state.value.email.subject)
    }

    private fun testDataStore(name: String): DataStore<Preferences> {
        return createDataStore { createTestPreferencesFilePath(name) }
    }
}

private class FakeGateway(
    private val testConnectionFailure: Exception? = null,
) : EmailGateway {
    var testConnectionCalls: Int = 0

    override suspend fun testConnection(settings: SmtpSettings) {
        testConnectionCalls++
        testConnectionFailure?.let { throw it }
    }

    override suspend fun sendBatch(
        settings: SmtpSettings,
        requests: List<EmailSendRequest>,
        onSending: (EmailSendRequest) -> Unit,
        onSuccess: (index: Int) -> Unit,
        onFailure: (index: Int, exception: Exception) -> Unit,
        isCancelRequested: () -> Boolean,
    ) = Unit
}

private class FakeCapabilityProvider(canSendEmails: Boolean) : PlatformCapabilityProvider {
    override val capabilities = AppCapabilities(
        canParseXlsx = true,
        canGeneratePdf = true,
        canSendEmails = canSendEmails,
        canResolveOutputDirectory = true,
        canOpenGeneratedFolders = true,
    )
}
