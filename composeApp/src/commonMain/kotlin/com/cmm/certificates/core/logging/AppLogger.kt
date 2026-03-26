package com.cmm.certificates.core.logging

expect fun logInfo(tag: String, message: String)

expect fun logWarn(tag: String, message: String)

expect fun logError(tag: String, message: String, throwable: Throwable? = null)
