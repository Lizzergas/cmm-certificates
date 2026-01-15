package com.cmm.certificates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.compose_multiplatform
import com.cmm.certificates.di.viewModelModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(
) {
    val navSerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeScreenRoute::class)
            subclass(TestScreenRoute::class)
        }
    }
    val savedStateConfiguration = SavedStateConfiguration {
        serializersModule = navSerializersModule
    }
    val backStack = rememberNavBackStack(
        configuration = savedStateConfiguration,
        HomeScreenRoute
    )
    KoinApplication(configuration = koinConfiguration(declaration = {
        modules(viewModelModule)
    })) {
        val entries = entryProvider {
            entry<HomeScreenRoute> {
                GreetingScreen(
                    onProfileClick = {
                        backStack.add(TestScreenRoute("Hello from Home Screen"))
                    }
                )
            }
            entry<TestScreenRoute>(
                metadata = mapOf("extraDataKey" to "extraDataValue")
            ) { key -> TestScreen(key.message) }
            entry<Unit> { Text("No screen...") }
        }
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entries,
        )
    }
}

@Composable
fun GreetingScreen(
    onProfileClick: () -> Unit,
    viewModel: TestViewModel = koinViewModel<TestViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val owner = LocalLifecycleOwner.current

        DisposableEffect(owner) {
            owner.lifecycle.addObserver(MyLifecycleObserer())
            onDispose { owner.lifecycle.removeObserver(MyLifecycleObserer()) }
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Clicckkk me!")
            }
            Button(onClick = onProfileClick) {
                Text("Go to Test Screen")
            }
            AnimatedVisibility(showContent) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: ${state.greeting}")
                }
            }
        }
    }
}

@Composable
fun TestScreen(message: String) {
    Text("This is a test screen with message: $message")
}

class MyLifecycleObserer : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        println("LifecycleOwner resumed: $owner")
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        println("LifecycleOwner created: $owner")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        println("LifecycleOwner paused: $owner")
    }
}
