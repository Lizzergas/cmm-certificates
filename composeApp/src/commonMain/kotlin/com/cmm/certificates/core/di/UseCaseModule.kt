package com.cmm.certificates.core.di

import com.cmm.certificates.core.usecase.SendPreviewEmailUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::SendPreviewEmailUseCase)
}
