package com.cmm.certificates.feature.emailsending.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.feature.certificate.ConversionScreenRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class EmailProgressScreenRoute(val retryCached: Boolean = false) : NavKey

fun EntryProviderScope<NavKey>.featureEmailEntryProvider(navigator: Navigator) {
    entry<EmailProgressScreenRoute> { route ->
        EmailProgressScreen(
            retryCached = route.retryCached,
            onFinish = { navigator.clearAndPush(ConversionScreenRoute) },
            onCancel = { navigator.back() },
        )
    }
}

val emailNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(EmailProgressScreenRoute::class)
    }
}
