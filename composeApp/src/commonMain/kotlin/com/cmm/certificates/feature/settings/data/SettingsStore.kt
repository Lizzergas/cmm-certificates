package com.cmm.certificates.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.email.SmtpTransport
import com.cmm.certificates.data.store.clearDataStore
import com.cmm.certificates.data.store.enumOrDefault
import com.cmm.certificates.data.store.safeData
import com.cmm.certificates.data.store.stringOrDefault
import kotlinx.coroutines.flow.first

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val host = stringPreferencesKey("smtp_host")
        val port = stringPreferencesKey("smtp_port")
        val username = stringPreferencesKey("smtp_username")
        val password = stringPreferencesKey("smtp_password")
        val transport = stringPreferencesKey("smtp_transport")
        val subject = stringPreferencesKey("smtp_subject")
        val body = stringPreferencesKey("smtp_body")
        val accreditedTypeOptions = stringPreferencesKey("accredited_type_options")
        val signatureHtml = stringPreferencesKey("email_signature_html")
    }

    suspend fun loadOrDefault(): StoredSettings {
        val prefs = dataStore
            .safeData()
            .first()

        return StoredSettings(
            host = prefs[Keys.host].orEmpty(),
            port = prefs[Keys.port].orEmpty(),
            username = prefs[Keys.username].orEmpty(),
            password = prefs[Keys.password].orEmpty(),
            transport = prefs.enumOrDefault(Keys.transport, SmtpTransport.SMTPS),
            subject = prefs.stringOrDefault(Keys.subject, DEFAULT_EMAIL_SUBJECT),
            body = prefs.stringOrDefault(Keys.body, DEFAULT_EMAIL_BODY),
            accreditedTypeOptions = prefs.stringOrDefault(
                Keys.accreditedTypeOptions,
                DEFAULT_ACCREDITED_TYPE_OPTIONS
            ),
            signatureHtml = prefs.stringOrDefault(Keys.signatureHtml, DEFAULT_SIGNATURE_HTML),
        )
    }

    suspend fun save(settings: StoredSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.host] = settings.host
            prefs[Keys.port] = settings.port
            prefs[Keys.username] = settings.username
            prefs[Keys.password] = settings.password
            prefs[Keys.transport] = settings.transport.name
            prefs[Keys.subject] = settings.subject
            prefs[Keys.body] = settings.body
            prefs[Keys.accreditedTypeOptions] = settings.accreditedTypeOptions
            prefs[Keys.signatureHtml] = settings.signatureHtml
        }
    }

    suspend fun clear() {
        clearDataStore(dataStore)
    }

    data class StoredSettings(
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

    companion object {
        const val DEFAULT_EMAIL_SUBJECT = "Pa\u017Eyma"
        const val DEFAULT_EMAIL_BODY =
            "Laba diena,\n\n" +
                    "Siunčiame parengtą dalyvio pažymėjimą ir maloniai kviečiame įvertinti renginio kokybę ir pakomentuoti, kas Jums buvo naudingiausia.\n\n" +
                    "Prašome anketą užpildyti šiuo adresu:\n" +
                    "anketos_nuoroda\n\n" +
                    "Dėkojame už bendradarbiavimą.\n"

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

