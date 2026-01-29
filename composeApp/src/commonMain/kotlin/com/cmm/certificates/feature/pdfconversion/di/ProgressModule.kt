package com.cmm.certificates.feature.pdfconversion.di

import com.cmm.certificates.feature.pdfconversion.data.PdfConversionProgressRepositoryImpl
import com.cmm.certificates.feature.pdfconversion.data.PdfConversionProgressStore
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.pdfconversion.ui.PdfConversionProgressViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val progressModule = module {
    singleOf(::PdfConversionProgressStore)
    single<PdfConversionProgressRepository> { PdfConversionProgressRepositoryImpl(get()) }
    viewModelOf(::PdfConversionProgressViewModel)
}
