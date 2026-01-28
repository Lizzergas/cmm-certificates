package com.cmm.certificates.feature.settings

import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::SmtpSettingsStore)
    singleOf(::ClearAllDataUseCase)
}
