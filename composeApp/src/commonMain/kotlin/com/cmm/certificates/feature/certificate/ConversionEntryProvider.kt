package com.cmm.certificates.feature.certificate

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.feature.pdfconversion.ui.ProgressScreenRoute
import com.cmm.certificates.feature.settings.ui.SettingsScreenRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


@Serializable
data object ConversionScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureHomeEntryProvider(navigator: Navigator) {
    entry<ConversionScreenRoute> {
        ConversionScreen(
            onProfileClick = { navigator.push(SettingsScreenRoute) },
            onStartConversion = { navigator.push(ProgressScreenRoute) },
        )
    }
}

val conversionNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ConversionScreenRoute::class)
    }
}
