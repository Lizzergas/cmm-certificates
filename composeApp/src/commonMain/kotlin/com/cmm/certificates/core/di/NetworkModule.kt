package com.cmm.certificates.core.di

import com.cmm.certificates.core.data.DefaultPlatformCapabilityProvider
import com.cmm.certificates.core.data.NetworkConnectivityMonitor
import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.data.network.NetworkService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    singleOf(::NetworkService)
    single<ConnectivityMonitor> { NetworkConnectivityMonitor(get()) }
    single<PlatformCapabilityProvider> { DefaultPlatformCapabilityProvider() }
}
