package com.cmm.certificates.core.di

import com.cmm.certificates.feature.settings.data.SettingsStore
import com.cmm.certificates.data.store.createPlatformDataStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataStoreModule = module {
    single { createPlatformDataStore() }
    singleOf(::SettingsStore)
}
