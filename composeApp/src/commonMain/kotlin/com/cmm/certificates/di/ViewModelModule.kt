package com.cmm.certificates.di

import com.cmm.certificates.features.home.homeModule
import com.cmm.certificates.features.progress.progressModule
import org.koin.dsl.module

val featuresModule = module {
    includes(homeModule)
    includes(progressModule)
}
