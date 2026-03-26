package com.cmm.certificates.feature.certificate.di

import com.cmm.certificates.feature.certificate.data.DefaultOutputDirectoryResolver
import com.cmm.certificates.feature.certificate.data.DocxCertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.data.XlsxRegistrationParser
import com.cmm.certificates.feature.certificate.domain.usecase.BuildCertificateReplacementsUseCase
import com.cmm.certificates.feature.certificate.domain.usecase.GenerateCertificatesUseCase
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.feature.certificate.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.domain.port.OutputDirectoryResolver
import com.cmm.certificates.feature.certificate.domain.port.RegistrationParser
import com.cmm.certificates.feature.certificate.presentation.ConversionViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val conversionModule = module {
    single<RegistrationParser> { XlsxRegistrationParser() }
    single<CertificateDocumentGenerator> { DocxCertificateDocumentGenerator() }
    single<OutputDirectoryResolver> { DefaultOutputDirectoryResolver() }
    singleOf(::ParseRegistrationsUseCase)
    singleOf(::BuildCertificateReplacementsUseCase)
    singleOf(::GenerateCertificatesUseCase)
    viewModelOf(::ConversionViewModel)
}
