package com.cmm.certificates.core.di

import com.cmm.certificates.di.emailModule
import com.cmm.certificates.di.conversionModule
import com.cmm.certificates.di.progressModule
import com.cmm.certificates.di.settingsModule
import org.koin.dsl.module

val featuresModule = module {
    includes(conversionModule)
    includes(progressModule)
    includes(settingsModule)
    includes(emailModule)
}
