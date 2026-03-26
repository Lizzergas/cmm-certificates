package com.cmm.certificates.feature.emailsending.di

import com.cmm.certificates.feature.emailsending.data.EmailProgressRepositoryImpl
import com.cmm.certificates.feature.emailsending.data.EmailProgressStore
import com.cmm.certificates.feature.emailsending.data.SmtpEmailGateway
import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.emailsending.domain.port.EmailGateway
import com.cmm.certificates.feature.emailsending.domain.usecase.BuildEmailRequestsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.RetryCachedEmailsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.SendEmailRequestsUseCase
import com.cmm.certificates.feature.emailsending.domain.usecase.SendGeneratedEmailsUseCase
import com.cmm.certificates.feature.emailsending.presentation.EmailSenderViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val emailModule = module {
    singleOf(::EmailProgressStore)
    single<EmailProgressRepository> { EmailProgressRepositoryImpl(get(), get(), get()) }
    single<EmailGateway> { SmtpEmailGateway() }
    singleOf(::BuildEmailRequestsUseCase)
    singleOf(::SendEmailRequestsUseCase)
    singleOf(::SendGeneratedEmailsUseCase)
    singleOf(::RetryCachedEmailsUseCase)
    viewModelOf(::EmailSenderViewModel)
}
