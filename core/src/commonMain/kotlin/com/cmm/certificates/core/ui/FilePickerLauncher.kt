package com.cmm.certificates.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch

@Composable
fun rememberFilePickerLauncher(): (String, (String) -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        { extension: String, onSelect: (String) -> Unit ->
            scope.launch {
                val file = FileKit.openFilePicker(
                    mode = FileKitMode.Single,
                    type = FileKitType.File(listOf(extension)),
                )
                file?.toString()?.let(onSelect)
            }
        }
    }
}

@Composable
fun rememberFilePickerLauncher(
    extension: String,
    onSelect: (String) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnSelect = rememberUpdatedState(onSelect)
    return remember(scope, extension) {
        {
            scope.launch {
                val file = FileKit.openFilePicker(
                    mode = FileKitMode.Single,
                    type = FileKitType.File(listOf(extension)),
                )
                file?.toString()?.let(currentOnSelect.value)
            }
        }
    }
}

@Composable
fun rememberDirectoryPickerLauncher(): (String?, (String) -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        { directory: String?, onSelect: (String) -> Unit ->
            scope.launch {
                val initialDirectory = directory
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::PlatformFile)
                val selectedDirectory = FileKit.openDirectoryPicker(
                    directory = initialDirectory,
                )
                selectedDirectory?.toString()?.let(onSelect)
            }
        }
    }
}
