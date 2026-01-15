package com.cmm.certificates.features.progress

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val progressModule = module {
    singleOf(::ConversionProgressStore)
}
