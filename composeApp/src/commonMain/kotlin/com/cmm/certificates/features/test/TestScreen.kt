package com.cmm.certificates.features.test

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Composable
fun TestScreen(
    message: String,
    onBack: () -> Unit,
) {
    Column {
        Text("This is a test screen with message: $message")
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

val testNavSerializerModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(TestScreenRoute::class)
    }
}
