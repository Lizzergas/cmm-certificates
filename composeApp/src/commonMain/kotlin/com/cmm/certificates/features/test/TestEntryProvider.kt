package com.cmm.certificates.features.test

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmm.certificates.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class TestScreenRoute(val message: String) : NavKey

fun EntryProviderScope<NavKey>.featureTestEntryProvider(
    navigator: Navigator,
) {
    entry<TestScreenRoute> { key ->
        TestScreen(key.message, navigator::back)
    }
}
