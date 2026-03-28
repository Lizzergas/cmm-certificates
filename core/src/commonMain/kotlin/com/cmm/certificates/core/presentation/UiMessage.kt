package com.cmm.certificates.core.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class UiMessage(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
)

@Composable
fun UiMessage.asString(): String {
    return stringResource(resource, *args.toTypedArray())
}
