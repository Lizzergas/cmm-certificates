package com.cmm.certificates.core.di

import com.cmm.certificates.feature.emailsending.di.emailModule
import com.cmm.certificates.feature.certificate.conversionModule
import com.cmm.certificates.feature.pdfconversion.di.progressModule
import com.cmm.certificates.feature.settings.di.settingsModule
import org.koin.dsl.module

val featuresModule = module {
    includes(conversionModule)
    includes(progressModule)
    includes(settingsModule)
    includes(emailModule)
}
