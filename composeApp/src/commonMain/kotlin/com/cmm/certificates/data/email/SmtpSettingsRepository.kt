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
    val accreditedTypeOptions: String,
    val signatureHtml: String,
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
    private val accreditedTypeOptionsKey = stringPreferencesKey("accredited_type_options")
    private val signatureHtmlKey = stringPreferencesKey("email_signature_html")

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
                prefs.contains(bodyKey) ||
                prefs.contains(accreditedTypeOptionsKey) ||
                prefs.contains(signatureHtmlKey)
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
        val accreditedTypeOptions = if (prefs.contains(accreditedTypeOptionsKey)) {
            prefs[accreditedTypeOptionsKey].orEmpty()
        } else {
            DEFAULT_ACCREDITED_TYPE_OPTIONS
        }
        val signatureHtml = if (prefs.contains(signatureHtmlKey)) {
            prefs[signatureHtmlKey].orEmpty()
        } else {
            DEFAULT_SIGNATURE_HTML
        }
        return StoredSmtpSettings(
            host = prefs[hostKey].orEmpty(),
            port = prefs[portKey].orEmpty(),
            username = prefs[usernameKey].orEmpty(),
            password = prefs[passwordKey].orEmpty(),
            transport = transport,
            subject = subject,
            body = body,
            accreditedTypeOptions = accreditedTypeOptions,
            signatureHtml = signatureHtml,
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
            prefs[accreditedTypeOptionsKey] = settings.accreditedTypeOptions
            prefs[signatureHtmlKey] = settings.signatureHtml
        }
    }

    companion object {
        const val DEFAULT_EMAIL_SUBJECT = "Pa\u017Eyma"
        const val DEFAULT_EMAIL_BODY = "Certificate attached."
        const val DEFAULT_ACCREDITED_TYPE_OPTIONS =
            "paskaitoje\nseminare\nkonferencijoje\nmokymuose"
        const val DEFAULT_SIGNATURE_HTML = """
            <div style="font-family:'Times New Roman', Times, serif; font-size:8pt; font-style:italic; line-height:1.25; color:#000;">
              <div>Pagarbiai</div>
              <div>Raminta Čyplytė</div>
              <div>Meninio ugdymo pedagogų kvalifikacijos tobulinimo centro metodininkė</div>
              <div>Nacionalinė M. K. Čiurlionio menų mokykla</div>
              <div>Tel. +370 67357212</div>
              <div>El. p. raminta.cyplyte@cmm.lt</div>
              <div>T. Kosciuškos g. 11 LT-01100, Vilnius</div>
            </div>
        """
    }
}
