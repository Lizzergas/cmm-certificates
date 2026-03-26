package com.cmm.certificates.core.i18n

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
}
