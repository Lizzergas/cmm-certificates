package com.cmm.certificates.di

import com.cmm.certificates.data.config.ActiveCertificateConfigStore
import com.cmm.certificates.data.config.CertificateConfigurationRepository
import com.cmm.certificates.presentation.CertificateConfigViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val certificateConfigModule = module {
    singleOf(::ActiveCertificateConfigStore)
    singleOf(::CertificateConfigurationRepository)
    viewModelOf(::CertificateConfigViewModel)
}
