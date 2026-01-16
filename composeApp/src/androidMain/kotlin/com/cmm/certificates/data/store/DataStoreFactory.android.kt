package com.cmm.certificates.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

private var appContext: Context? = null

fun initDataStore(context: Context) {
    appContext = context.applicationContext
}

actual fun createPlatformDataStore(): DataStore<Preferences> {
    val context = requireNotNull(appContext) { "DataStore context is not initialized." }
    return createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath },
    )
}
