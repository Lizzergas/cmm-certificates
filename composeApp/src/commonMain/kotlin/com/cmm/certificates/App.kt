package com.cmm.certificates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.ui.NavDisplay
import com.cmm.certificates.core.di.appModule
import com.cmm.certificates.core.i18n.AppEnvironment
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided")
}

@Composable
@Preview
fun App(
) {
    val navigator = rememberNavigator()

    KoinApplication(koinConfiguration { modules(appModule) }) {
        // Providing navigation to composable hierarchy might be redundant
        AppEnvironment {
            CompositionLocalProvider(
                LocalNavigator provides navigator,
            ) {
                NavDisplay(
                    backStack = navigator.backStack,
                    entryProvider = navigator.entries,
                    onBack = navigator::back,
                )
            }
        }
    }
}
