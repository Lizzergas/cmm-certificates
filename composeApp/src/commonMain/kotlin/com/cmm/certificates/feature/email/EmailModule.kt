package com.cmm.certificates.feature.email

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val emailModule = module {
    singleOf(::EmailProgressStore)
    viewModelOf(::EmailSenderViewModel)
}
