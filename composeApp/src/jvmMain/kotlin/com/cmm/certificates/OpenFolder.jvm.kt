package com.cmm.certificates

import java.awt.Desktop
import java.io.File

actual fun openFolder(path: String): Boolean {
    return runCatching {
        if (!Desktop.isDesktopSupported()) return false
        Desktop.getDesktop().open(File(path))
        true
    }.getOrDefault(false)
}
