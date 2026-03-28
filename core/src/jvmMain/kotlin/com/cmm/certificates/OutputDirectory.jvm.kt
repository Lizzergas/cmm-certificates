package com.cmm.certificates

import java.io.File
import java.nio.file.Paths

actual object OutputDirectory {
    actual fun resolve(path: String): String {
        val trimmed = path.trim()
        val candidate = Paths.get(trimmed)
        val resolved = if (candidate.isAbsolute) {
            candidate
        } else {
            Paths.get(System.getProperty("user.dir")).resolve(candidate)
        }
        return resolved.normalize().toAbsolutePath().toString()
    }

    actual fun ensureExists(path: String): Boolean {
        val dir = File(path)
        return if (dir.exists()) dir.isDirectory else dir.mkdirs()
    }
}
