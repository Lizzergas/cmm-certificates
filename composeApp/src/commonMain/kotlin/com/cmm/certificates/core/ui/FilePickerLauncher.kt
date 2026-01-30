package com.cmm.certificates.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
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
                onSelect(file?.toString().orEmpty())
            }
        }
    }
}
