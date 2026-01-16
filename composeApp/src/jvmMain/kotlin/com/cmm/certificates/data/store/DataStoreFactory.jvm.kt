package com.cmm.certificates.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

actual fun createPlatformDataStore(): DataStore<Preferences> {
    val baseDir = File(System.getProperty("user.home"), ".cmmcertificates")
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    val file = File(baseDir, dataStoreFileName)
    return createDataStore(producePath = { file.absolutePath })
}
