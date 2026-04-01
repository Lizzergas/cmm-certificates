package com.cmm.certificates.core.logging

import com.cmm.certificates.AppInstallation
import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.preferredDefaultOutputDirectory
import io.sentry.kotlin.multiplatform.Attachment
import io.sentry.kotlin.multiplatform.Sentry
import java.io.File
import java.io.RandomAccessFile

private const val LICENSE_FILE_NAME = "LICENSE.txt"
private const val APP_LOG_FILE_NAME = "app.log"
private const val APP_LOG_FILE_PROPERTY = "cmm.log.file"

actual object AppLogSupport {
    actual fun initialize() {
        val logFile = resolveLogFile()
        ensureLogFileExists(logFile)
        System.setProperty(APP_LOG_FILE_PROPERTY, logFile.absolutePath)
    }

    actual fun isSupported(): Boolean = true

    actual fun currentLogFilePath(): String? {
        initialize()
        return System.getProperty(APP_LOG_FILE_PROPERTY) ?: resolveLogFile().absolutePath
    }

    actual suspend fun submitCurrentLog(): LogSubmissionResult {
        initialize()
        val logFile = resolveLogFile()
        ensureLogFileExists(logFile)
        if (!logFile.isFile || logFile.length() <= 0L) {
            return LogSubmissionResult.NO_LOGS
        }

        return runCatching {
            val attachmentBytes = logFile.readBytes()
            val attachment = Attachment(attachmentBytes, logFile.name)
            Sentry.captureMessage("User submitted app logs") { scope ->
                scope.addAttachment(attachment)
                scope.setTag("manual_log_submission", "true")
                scope.setTag("log_file_name", logFile.name)
            }
            clearLogFile(logFile)
            LogSubmissionResult.SUCCESS
        }.getOrElse {
            LogSubmissionResult.FAILED
        }
    }

    private fun resolveLogFile(): File {
        val configuredPath = System.getProperty(APP_LOG_FILE_PROPERTY)
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
        if (configuredPath != null) {
            return configuredPath
        }

        val directory = resolveLogDirectory()
        return File(directory, APP_LOG_FILE_NAME)
    }

    private fun resolveLogDirectory(): File {
        val licenseDirectory = AppInstallation.installedResourcePath(LICENSE_FILE_NAME)
            ?.let(::File)
            ?.parentFile
        if (licenseDirectory != null && ensureWritableDirectory(licenseDirectory)) {
            return licenseDirectory
        }

        val installationDirectory = AppInstallation.installationDirectoryPath()
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
        if (installationDirectory != null && ensureWritableDirectory(installationDirectory)) {
            return installationDirectory
        }

        val fallbackDirectory = File(
            preferredDefaultOutputDirectory(AppInstallation.preferredOutputBaseDirectoryPath())
        )
        if (ensureWritableDirectory(fallbackDirectory)) {
            return fallbackDirectory
        }

        val userDirectory = File(System.getProperty("user.home"))
        if (ensureWritableDirectory(userDirectory)) {
            return userDirectory
        }

        return File(System.getProperty("user.dir"))
    }
}

private fun ensureWritableDirectory(directory: File): Boolean {
    val path = directory.absolutePath
    return OutputDirectory.ensureExists(path) && OutputDirectory.canWrite(path)
}

private fun ensureLogFileExists(logFile: File) {
    logFile.parentFile?.let(::ensureWritableDirectory)
    if (!logFile.exists()) {
        logFile.createNewFile()
    }
}

private fun clearLogFile(logFile: File) {
    RandomAccessFile(logFile, "rw").use { file ->
        file.setLength(0L)
    }
}
