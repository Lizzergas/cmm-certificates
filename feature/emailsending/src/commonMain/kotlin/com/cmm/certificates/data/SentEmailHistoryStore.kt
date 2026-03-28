package com.cmm.certificates.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.store.safeData
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@Serializable
data class SentEmailHistory(
    val records: List<SentEmailRecord> = emptyList(),
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

    suspend fun addSend(record: SentEmailRecord) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.history]?.let { json ->
                runCatching { Json.decodeFromString<SentEmailHistory>(json) }.getOrNull()
            } ?: SentEmailHistory()

            prefs[Keys.history] = Json.encodeToString(
                current.copy(records = current.records + record)
            )
        }
    }

    suspend fun getCountInLast24Hours(): Int {
        return history
            .map { it.records.countInLast24Hours() }
            .first()
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.history)
        }
    }
}

private fun List<SentEmailRecord>.countInLast24Hours(
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Int {
    val cutoff = nowMillis - 24.hours.inWholeMilliseconds
    return count { it.sentAt > cutoff }
}
