package com.cmm.certificates.test

import java.nio.file.Files

actual fun createTestPreferencesFilePath(name: String): String {
    val tempDir = Files.createTempDirectory("cmmcertificates-")
    return tempDir.resolve("$name.preferences_pb").toAbsolutePath().toString()
}
