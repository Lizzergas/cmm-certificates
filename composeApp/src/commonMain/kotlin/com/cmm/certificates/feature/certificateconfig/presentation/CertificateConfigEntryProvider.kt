package com.cmm.certificates.feature.certificateconfig.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import com.cmm.certificates.presentation.CertificateConfigScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object CertificateConfigScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureCertificateConfigEntryProvider(navigator: Navigator) {
    entry<CertificateConfigScreenRoute> {
        CertificateConfigScreen(
            onBack = { navigator.back() },
        )
    }
}

val certificateConfigNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CertificateConfigScreenRoute::class)
    }
}
