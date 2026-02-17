package com.cmm.certificates.feature.emailsending.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.store.safeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@Serializable
data class SentEmailHistory(
    val timestamps: List<Long> = emptyList(),
)

class SentEmailHistoryStore(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val history = stringPreferencesKey("sent_email_history")
    }

    val history: Flow<SentEmailHistory> = dataStore.safeData()
        .map { prefs ->
            prefs[Keys.history]?.let { json ->
                runCatching { Json.decodeFromString<SentEmailHistory>(json) }.getOrNull()
            } ?: SentEmailHistory()
        }

    suspend fun addSend(timestamp: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.history]?.let { json ->
                runCatching { Json.decodeFromString<SentEmailHistory>(json) }.getOrNull()
            } ?: SentEmailHistory()

            val now = Clock.System.now().toEpochMilliseconds()
            val cutoff = now - 24.hours.inWholeMilliseconds

            val updatedTimestamps = (current.timestamps + timestamp)
                .filter { it > cutoff }

            prefs[Keys.history] = Json.encodeToString(SentEmailHistory(updatedTimestamps))
        }
    }

    suspend fun getCountInLast24Hours(): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val cutoff = now - 24.hours.inWholeMilliseconds

        var count = 0
        dataStore.edit { prefs ->
            val current = prefs[Keys.history]?.let { json ->
                runCatching { Json.decodeFromString<SentEmailHistory>(json) }.getOrNull()
            } ?: SentEmailHistory()
            val updatedTimestamps = current.timestamps.filter { it > cutoff }
            count = updatedTimestamps.size
            if (updatedTimestamps.size != current.timestamps.size) {
                prefs[Keys.history] = Json.encodeToString(SentEmailHistory(updatedTimestamps))
            }
        }
        return count
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.history)
        }
    }
}
