package com.cmm.certificates.feature.settings.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.feature.certificateconfig.presentation.CertificateConfigScreenRoute
import com.cmm.certificates.presentation.SettingsScreen
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
            onOpenCertificateConfig = { navigator.push(CertificateConfigScreenRoute) },
        )
    }
}

val settingsNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsScreenRoute::class)
    }
}
