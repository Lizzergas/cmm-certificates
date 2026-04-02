package com.cmm.certificates.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cmm.certificates.data.store.safeData
import kotlinx.coroutines.flow.first

class ActiveCertificateConfigStore(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val configJson = stringPreferencesKey("certificate_active_config_json")
    }

    suspend fun load(): String? {
        return dataStore.safeData().first()[Keys.configJson]
    }

    suspend fun save(rawJson: String) {
        dataStore.edit { prefs ->
            prefs[Keys.configJson] = rawJson
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.configJson)
        }
    }
}
