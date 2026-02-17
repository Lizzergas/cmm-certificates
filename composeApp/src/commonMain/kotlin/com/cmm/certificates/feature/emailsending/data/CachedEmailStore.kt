package com.cmm.certificates.feature.emailsending.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.email.EmailSendRequest
import com.cmm.certificates.data.store.safeData
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedEmailBatch(
    val requests: List<EmailSendRequest>,
    val lastReason: EmailStopReason? = null,
)

class CachedEmailStore(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val cachedEmails = stringPreferencesKey("cached_emails")
    }

    val cachedEmails: Flow<CachedEmailBatch?> = dataStore
        .safeData()
        .map { prefs ->
            prefs[Keys.cachedEmails]?.let { json ->
                runCatching { Json.decodeFromString<CachedEmailBatch>(json) }.getOrNull()
            }
        }

    suspend fun save(batch: CachedEmailBatch) {
        dataStore.edit { prefs ->
            prefs[Keys.cachedEmails] = Json.encodeToString(batch)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.cachedEmails)
        }
    }
}
