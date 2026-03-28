package com.cmm.certificates.core.logging

import io.sentry.kotlin.multiplatform.Sentry
import java.util.logging.Level
import java.util.logging.Logger

private fun logger(tag: String): Logger = Logger.getLogger(tag)

actual fun logInfo(tag: String, message: String) {
    logger(tag).log(Level.INFO, message)
}

actual fun logWarn(tag: String, message: String) {
    logger(tag).log(Level.WARNING, message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    logger(tag).log(Level.SEVERE, message, throwable)
    if (throwable != null) {
        Sentry.captureException(throwable)
    } else {
        Sentry.captureMessage("[$tag] $message")
    }
}
