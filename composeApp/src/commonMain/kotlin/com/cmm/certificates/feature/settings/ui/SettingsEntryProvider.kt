package com.cmm.certificates.feature.settings.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object SettingsScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureSettingsEntryProvider(navigator: Navigator) {
    entry<SettingsScreenRoute> {
        SettingsScreen(
            onBack = { navigator.back() },
        )
    }
}

val settingsNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsScreenRoute::class)
    }
}
