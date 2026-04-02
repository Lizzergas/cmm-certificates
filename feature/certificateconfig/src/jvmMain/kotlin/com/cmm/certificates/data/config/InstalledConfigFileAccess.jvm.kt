package com.cmm.certificates.data.config

import java.io.File

actual object InstalledConfigFileAccess {
    actual fun read(path: String): String = File(path).readText()

    actual fun write(path: String, content: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
}
