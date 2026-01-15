package com.cmm.certificates.di

import com.cmm.certificates.TestViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::TestViewModel)
}
