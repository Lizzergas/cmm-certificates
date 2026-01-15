package com.cmm.certificates.features.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.features.test.TestScreenRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


@Serializable
data object HomeScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureHomeEntryProvider(navigator: Navigator) {
    entry<HomeScreenRoute> {
        HomeScreen(
            onProfileClick = { navigator.push(TestScreenRoute("Hello from Home Screen")) }
        )
    }
}

val homeNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HomeScreenRoute::class)
    }
}
