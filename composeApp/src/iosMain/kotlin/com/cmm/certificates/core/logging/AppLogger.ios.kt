package com.cmm.certificates.core.logging

import platform.Foundation.NSLog

actual fun logInfo(tag: String, message: String) {
    NSLog("[%@] %@", tag, message)
}

actual fun logWarn(tag: String, message: String) {
    NSLog("[%@] WARN: %@", tag, message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    NSLog("[%@] ERROR: %@ %@", tag, message, throwable?.message ?: "")
}
