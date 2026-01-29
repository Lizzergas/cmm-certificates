package com.cmm.certificates.core.di

import com.cmm.certificates.data.network.NetworkService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    singleOf(::NetworkService)
}
