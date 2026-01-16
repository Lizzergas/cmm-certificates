package com.cmm.certificates.feature.progress

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object ProgressScreenRoute : NavKey

fun EntryProviderScope<NavKey>.featureProgressEntryProvider(navigator: Navigator) {
    entry<ProgressScreenRoute> {
        ProgressScreen(
            onCancel = {
                navigator.back()
            },
            onConvertAnother = {
                navigator.back()
            },
        )
    }
}

val progressNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ProgressScreenRoute::class)
    }
}
