package com.cmm.certificates.feature.progress.di

import com.cmm.certificates.feature.progress.data.PdfConversionProgressRepositoryImpl
import com.cmm.certificates.feature.progress.data.PdfConversionProgressStore
import com.cmm.certificates.feature.progress.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.progress.ui.PdfConversionProgressViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val progressModule = module {
    singleOf(::PdfConversionProgressStore)
    single<PdfConversionProgressRepository> { PdfConversionProgressRepositoryImpl(get()) }
    viewModelOf(::PdfConversionProgressViewModel)
}
