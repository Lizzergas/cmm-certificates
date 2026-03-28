package com.cmm.certificates.feature.certificate.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.feature.emailsending.presentation.EmailProgressScreenRoute
import com.cmm.certificates.feature.pdfconversion.presentation.ProgressScreenRoute
import com.cmm.certificates.feature.settings.presentation.SettingsScreenRoute
import com.cmm.certificates.presentation.ConversionScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


@Serializable
data object ConversionScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureConversionEntryProvider(navigator: Navigator) {
    entry<ConversionScreenRoute> {
        ConversionScreen(
            onProfileClick = { navigator.push(SettingsScreenRoute) },
            onStartConversion = { navigator.push(ProgressScreenRoute) },
            onRetryCachedEmails = { navigator.push(EmailProgressScreenRoute(retryCached = true)) },
        )
    }
}

val conversionNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ConversionScreenRoute::class)
    }
}
