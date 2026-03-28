package com.cmm.certificates.di

import com.cmm.certificates.core.signature.SignatureEditorController
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.data.defaultSignatureHtml
import com.cmm.certificates.data.SettingsRepositoryImpl
import com.cmm.certificates.data.SettingsStore
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import com.cmm.certificates.presentation.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::SettingsStore)
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get(), get()) }
    singleOf(::ClearAllDataUseCase)
    factory { SignatureEditorController(defaultSignatureHtml()) }
    viewModelOf(::SettingsViewModel)
}
