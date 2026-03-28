package com.cmm.certificates.di

import com.cmm.certificates.core.usecase.SendPreviewEmailUseCase
import com.cmm.certificates.data.PdfConversionProgressRepositoryImpl
import com.cmm.certificates.data.PdfConversionProgressStore
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.presentation.PdfConversionProgressViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val progressModule = module {
    singleOf(::PdfConversionProgressStore)
    single<PdfConversionProgressRepository> { PdfConversionProgressRepositoryImpl(get()) }
    singleOf(::SendPreviewEmailUseCase)
    viewModelOf(::PdfConversionProgressViewModel)
}
