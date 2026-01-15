package com.cmm.certificates.di

import org.koin.dsl.module

val appModule = module {
    includes(viewModelModule)
}
