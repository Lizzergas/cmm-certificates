package com.cmm.certificates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.cmm.certificates.features.home.HomeScreenRoute
import com.cmm.certificates.features.home.featureHomeEntryProvider
import com.cmm.certificates.features.home.homeNavSerializerModule
import com.cmm.certificates.features.progress.featureProgressEntryProvider
import com.cmm.certificates.features.progress.progressNavSerializerModule
import com.cmm.certificates.features.test.featureTestEntryProvider
import com.cmm.certificates.features.test.testNavSerializerModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Stable
class Navigator internal constructor(
    val backStack: NavBackStack<NavKey>,
) {
    val entries = entryProvider {
        featureHomeEntryProvider(this@Navigator)
        featureProgressEntryProvider(this@Navigator)
        featureTestEntryProvider(this@Navigator)
    }

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        backStack.removeLastOrNull()
    }
}

@Composable
fun rememberNavigator(
    startDestination: NavKey = HomeScreenRoute,
): Navigator {
    val navSerializers = remember {
        SerializersModule {
            polymorphic(NavKey::class) {
                include(homeNavSerializerModule)
                include(progressNavSerializerModule)
                include(testNavSerializerModule)
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
