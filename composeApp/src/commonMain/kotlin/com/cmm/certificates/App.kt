package com.cmm.certificates

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.ui.NavDisplay
import com.cmm.certificates.core.di.appModule
import com.cmm.certificates.core.i18n.AppEnvironment
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.SettingsRepository
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided")
}

@Composable
@Preview
fun App(
) {
    val navigator = rememberNavigator()

    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(appModule) }),
        content = {
            val settingsRepository = koinInject<SettingsRepository>()
            val settingsState by settingsRepository.state.collectAsState()

            // Providing navigation to composable hierarchy might be redundant
            AppEnvironment {
                AppTheme(darkTheme = settingsState.appearance.themeMode == AppThemeMode.DARK) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
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
        })
}
