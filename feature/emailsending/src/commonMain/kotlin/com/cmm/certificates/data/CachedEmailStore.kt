package com.cmm.certificates.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.store.safeData
import com.cmm.certificates.feature.emailsending.domain.CachedEmailBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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
