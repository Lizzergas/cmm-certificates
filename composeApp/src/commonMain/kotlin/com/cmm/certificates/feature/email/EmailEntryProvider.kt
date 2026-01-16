package com.cmm.certificates.feature.email

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object EmailProgressScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureEmailEntryProvider(navigator: Navigator) {
    entry<EmailProgressScreenRoute> {
        EmailProgressScreen(
            onBack = { navigator.back() },
            onCancel = { navigator.back() },
        )
    }
}

val emailNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(EmailProgressScreenRoute::class)
    }
}
