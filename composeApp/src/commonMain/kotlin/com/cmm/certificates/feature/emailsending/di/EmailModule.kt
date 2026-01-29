package com.cmm.certificates.feature.emailsending.di

import com.cmm.certificates.feature.emailsending.data.EmailProgressRepositoryImpl
import com.cmm.certificates.feature.emailsending.data.EmailProgressStore
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.ui.EmailSenderViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val emailModule = module {
    singleOf(::EmailProgressStore)
    single<EmailProgressRepository> { EmailProgressRepositoryImpl(get()) }
    viewModelOf(::EmailSenderViewModel)
}
