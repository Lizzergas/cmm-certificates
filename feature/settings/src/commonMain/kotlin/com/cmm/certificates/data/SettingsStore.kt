package com.cmm.certificates.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.store.clearDataStore
import com.cmm.certificates.data.store.enumOrDefault
import com.cmm.certificates.data.store.intOrDefault
import com.cmm.certificates.data.store.safeData
import com.cmm.certificates.data.store.stringOrDefault
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.SmtpTransport
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
        val outputDirectory = stringPreferencesKey("output_directory")
        val signatureHtml = stringPreferencesKey("email_signature_html")
        val previewEmail = stringPreferencesKey("email_preview_address")
        val dailyLimit = intPreferencesKey("email_daily_limit")
        val themeMode = stringPreferencesKey("app_theme_mode")
        val useInAppPdfPreview = booleanPreferencesKey("use_in_app_pdf_preview")
    }

    suspend fun loadOrDefault(): StoredSettings {
        val prefs = dataStore.safeData().first()

        return StoredSettings(
            host = prefs[Keys.host].orEmpty(),
            port = prefs[Keys.port].orEmpty(),
            username = prefs[Keys.username].orEmpty(),
            password = prefs[Keys.password].orEmpty(),
            transport = prefs.enumOrDefault(Keys.transport, SmtpTransport.SMTPS),
            subject = prefs.stringOrDefault(Keys.subject, defaultEmailSubject()),
            body = prefs.stringOrDefault(Keys.body, defaultEmailBody()),
            outputDirectory = prefs[Keys.outputDirectory].orEmpty(),
            signatureHtml = prefs.stringOrDefault(Keys.signatureHtml, defaultSignatureHtml()),
            previewEmail = prefs.stringOrDefault(Keys.previewEmail, DEFAULT_PREVIEW_EMAIL),
            dailyLimit = prefs.intOrDefault(Keys.dailyLimit, DEFAULT_DAILY_LIMIT),
            themeMode = prefs.enumOrDefault(Keys.themeMode, AppThemeMode.LIGHT),
            useInAppPdfPreview = prefs[Keys.useInAppPdfPreview] ?: true,
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
            prefs[Keys.outputDirectory] = settings.outputDirectory
            prefs[Keys.signatureHtml] = settings.signatureHtml
            prefs[Keys.previewEmail] = settings.previewEmail
            prefs[Keys.dailyLimit] = settings.dailyLimit
            prefs[Keys.themeMode] = settings.themeMode.name
            prefs[Keys.useInAppPdfPreview] = settings.useInAppPdfPreview
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
        val outputDirectory: String,
        val signatureHtml: String,
        val previewEmail: String,
        val dailyLimit: Int,
        val themeMode: AppThemeMode,
        val useInAppPdfPreview: Boolean,
    )
}
