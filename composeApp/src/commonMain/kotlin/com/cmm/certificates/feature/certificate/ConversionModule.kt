package com.cmm.certificates.feature.certificate

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val conversionModule = module {
    viewModelOf(::ConversionViewModel)
}
