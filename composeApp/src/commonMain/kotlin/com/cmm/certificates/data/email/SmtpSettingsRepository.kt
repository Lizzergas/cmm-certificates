package com.cmm.certificates.data.email

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

data class StoredSmtpSettings(
    val host: String,
    val port: String,
    val username: String,
    val password: String,
    val transport: SmtpTransport,
    val subject: String,
    val body: String,
)

class SmtpSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val hostKey = stringPreferencesKey("smtp_host")
    private val portKey = stringPreferencesKey("smtp_port")
    private val usernameKey = stringPreferencesKey("smtp_username")
    private val passwordKey = stringPreferencesKey("smtp_password")
    private val transportKey = stringPreferencesKey("smtp_transport")
    private val subjectKey = stringPreferencesKey("smtp_subject")
    private val bodyKey = stringPreferencesKey("smtp_body")

    suspend fun load(): StoredSmtpSettings? {
        val prefs = dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()
        val hasAny = prefs.contains(hostKey) ||
            prefs.contains(portKey) ||
            prefs.contains(usernameKey) ||
            prefs.contains(passwordKey) ||
            prefs.contains(transportKey) ||
            prefs.contains(subjectKey) ||
            prefs.contains(bodyKey)
        if (!hasAny) return null

        val transportValue = prefs[transportKey]
        val transport = runCatching {
            if (transportValue.isNullOrBlank()) null else SmtpTransport.valueOf(transportValue)
        }.getOrNull() ?: SmtpTransport.SMTPS
        val subject = if (prefs.contains(subjectKey)) {
            prefs[subjectKey].orEmpty()
        } else {
            DEFAULT_EMAIL_SUBJECT
        }
        val body = if (prefs.contains(bodyKey)) {
            prefs[bodyKey].orEmpty()
        } else {
            DEFAULT_EMAIL_BODY
        }
        return StoredSmtpSettings(
            host = prefs[hostKey].orEmpty(),
            port = prefs[portKey].orEmpty(),
            username = prefs[usernameKey].orEmpty(),
            password = prefs[passwordKey].orEmpty(),
            transport = transport,
            subject = subject,
            body = body,
        )
    }

    suspend fun save(settings: StoredSmtpSettings) {
        dataStore.edit { prefs ->
            prefs[hostKey] = settings.host
            prefs[portKey] = settings.port
            prefs[usernameKey] = settings.username
            prefs[passwordKey] = settings.password
            prefs[transportKey] = settings.transport.name
            prefs[subjectKey] = settings.subject
            prefs[bodyKey] = settings.body
        }
    }

    companion object {
        const val DEFAULT_EMAIL_SUBJECT = "Pa\u017Eyma"
        const val DEFAULT_EMAIL_BODY = "Certificate attached."
    }
}
