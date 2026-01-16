package com.cmm.certificates.core.di

import com.cmm.certificates.feature.home.conversionModule
import com.cmm.certificates.feature.progress.progressModule
import org.koin.dsl.module

val featuresModule = module {
    includes(conversionModule)
    includes(progressModule)
}
