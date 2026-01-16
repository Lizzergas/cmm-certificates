package com.cmm.certificates.core.di

import com.cmm.certificates.data.email.SmtpSettingsRepository
import com.cmm.certificates.data.store.createPlatformDataStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataStoreModule = module {
    single { createPlatformDataStore() }
    singleOf(::SmtpSettingsRepository)
}
