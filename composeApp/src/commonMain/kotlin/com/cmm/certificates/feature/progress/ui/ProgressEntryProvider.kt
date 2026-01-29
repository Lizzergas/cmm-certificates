package com.cmm.certificates.feature.progress.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.feature.email.EmailProgressScreenRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object ProgressScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureProgressEntryProvider(navigator: Navigator) {
    entry<ProgressScreenRoute> {
        PdfConversionProgressScreen(
            onCancel = {
                navigator.back()
            },
            onConvertAnother = {
                navigator.back()
            },
            onSendEmails = {
                navigator.push(EmailProgressScreenRoute)
            },
        )
    }
}

val progressNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ProgressScreenRoute::class)
    }
}
