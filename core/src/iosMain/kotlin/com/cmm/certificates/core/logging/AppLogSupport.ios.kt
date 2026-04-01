package com.cmm.certificates.core.logging

actual object AppLogSupport {
    actual fun initialize() = Unit

    actual fun isSupported(): Boolean = false

    actual fun currentLogFilePath(): String? = null

    actual suspend fun submitCurrentLog(): LogSubmissionResult = LogSubmissionResult.UNSUPPORTED
}
