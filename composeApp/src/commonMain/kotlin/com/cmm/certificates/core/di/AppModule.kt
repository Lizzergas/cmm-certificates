package com.cmm.certificates.core.di

import org.koin.dsl.module

val appModule = module {
    includes(dataStoreModule)
    includes(networkModule)
    includes(useCaseModule)
    includes(featuresModule)
}
