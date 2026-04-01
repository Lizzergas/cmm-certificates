package com.cmm.certificates.core.logging

enum class LogSubmissionResult {
    SUCCESS,
    NO_LOGS,
    FAILED,
    UNSUPPORTED,
}

expect object AppLogSupport {
    fun initialize()

    fun isSupported(): Boolean

    fun currentLogFilePath(): String?

    suspend fun submitCurrentLog(): LogSubmissionResult
}
