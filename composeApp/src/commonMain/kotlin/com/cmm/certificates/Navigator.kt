package com.cmm.certificates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.cmm.certificates.feature.certificate.presentation.ConversionScreenRoute
import com.cmm.certificates.feature.certificate.presentation.conversionNavSerializerModule
import com.cmm.certificates.feature.certificate.presentation.featureConversionEntryProvider
import com.cmm.certificates.feature.emailsending.presentation.emailNavSerializerModule
import com.cmm.certificates.feature.emailsending.presentation.featureEmailEntryProvider
import com.cmm.certificates.feature.pdfconversion.presentation.featureProgressEntryProvider
import com.cmm.certificates.feature.pdfconversion.presentation.progressNavSerializerModule
import com.cmm.certificates.feature.settings.presentation.featureSettingsEntryProvider
import com.cmm.certificates.feature.settings.presentation.settingsNavSerializerModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Stable
class Navigator internal constructor(
    val backStack: NavBackStack<NavKey>,
) {
    val entries = entryProvider {
        featureConversionEntryProvider(this@Navigator)
        featureProgressEntryProvider(this@Navigator)
        featureSettingsEntryProvider(this@Navigator)
        featureEmailEntryProvider(this@Navigator)
    }

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        backStack.removeLastOrNull()
    }

    fun clearAndPush(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }
}

@Composable
fun rememberNavigator(
    startDestination: NavKey = ConversionScreenRoute,
): Navigator {
    val navSerializers = remember {
        SerializersModule {
            polymorphic(NavKey::class) {
                include(conversionNavSerializerModule)
                include(progressNavSerializerModule)
                include(settingsNavSerializerModule)
                include(emailNavSerializerModule)
            }
        }
    }
    val savedStateConfiguration = remember {
        SavedStateConfiguration {
            serializersModule = navSerializers
        }
    }
    val backStack = rememberNavBackStack(
        configuration = savedStateConfiguration,
        startDestination
    )

    return remember { Navigator(backStack) }
}
