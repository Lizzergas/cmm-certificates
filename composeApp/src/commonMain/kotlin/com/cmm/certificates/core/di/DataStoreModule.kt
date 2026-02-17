package com.cmm.certificates.core.di

import com.cmm.certificates.data.store.createPlatformDataStore
import com.cmm.certificates.feature.emailsending.data.CachedEmailStore
import com.cmm.certificates.feature.emailsending.data.SentEmailHistoryStore
import com.cmm.certificates.feature.settings.data.SettingsStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataStoreModule = module {
    single { createPlatformDataStore() }
    singleOf(::SettingsStore)
    singleOf(::CachedEmailStore)
    singleOf(::SentEmailHistoryStore)
}
