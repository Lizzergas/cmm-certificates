package com.cmm.certificates.feature.settings.di

import com.cmm.certificates.core.signature.SignatureEditorController
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.feature.settings.data.defaultSignatureHtml
import com.cmm.certificates.feature.settings.data.SettingsRepositoryImpl
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get(), get()) }
    singleOf(::ClearAllDataUseCase)
    factory { SignatureEditorController(defaultSignatureHtml()) }
    viewModelOf(::SettingsViewModel)
}
