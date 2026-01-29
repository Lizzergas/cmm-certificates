package com.cmm.certificates.feature.settings.di

import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.feature.settings.data.SettingsRepositoryImpl
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.ui.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    singleOf(::SettingsRepositoryImpl)
    singleOf(::ClearAllDataUseCase)
    viewModelOf(::SettingsViewModel)
}
