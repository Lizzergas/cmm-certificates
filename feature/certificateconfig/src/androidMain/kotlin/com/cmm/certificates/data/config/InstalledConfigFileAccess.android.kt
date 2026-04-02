package com.cmm.certificates.data.config

actual object InstalledConfigFileAccess {
    actual fun read(path: String): String {
        error("Installed config file access is unsupported on Android: $path")
    }

    actual fun write(path: String, content: String) {
        error("Installed config file access is unsupported on Android: $path")
    }
}
