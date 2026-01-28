package com.cmm.certificates.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )

internal suspend fun clearDataStore(dataStore: DataStore<Preferences>) {
    dataStore.edit { it.clear() }
}

internal const val dataStoreFileName = "cmm.preferences_pb"

expect fun createPlatformDataStore(): DataStore<Preferences>
