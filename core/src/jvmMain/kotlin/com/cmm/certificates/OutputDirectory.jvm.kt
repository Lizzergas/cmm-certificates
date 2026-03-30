package com.cmm.certificates

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

actual object OutputDirectory {
    actual fun resolve(path: String): String {
        val trimmed = path.trim()
        val candidate = Paths.get(trimmed)
        val resolved = if (candidate.isAbsolute) {
            candidate
        } else {
            Paths.get(System.getProperty("user.home")).resolve(candidate)
        }
        return resolved.normalize().toAbsolutePath().toString()
    }

    actual fun ensureExists(path: String): Boolean {
        val dir = File(path)
        return if (dir.exists()) dir.isDirectory else dir.mkdirs()
    }

    actual fun canWrite(path: String): Boolean {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return false

        val target = File(trimmed).absoluteFile
        val probeDirectory = when {
            target.exists() -> target.takeIf(File::isDirectory) ?: return false
            else -> generateSequence(target.parentFile) { it.parentFile }
                .firstOrNull { it.exists() } ?: return false
        }

        if (!probeDirectory.isDirectory || !probeDirectory.canWrite()) return false

        return runCatching {
            val probeFile = Files.createTempFile(probeDirectory.toPath(), "cmm-", ".tmp")
            Files.deleteIfExists(probeFile)
            true
        }.getOrDefault(false)
    }
}
