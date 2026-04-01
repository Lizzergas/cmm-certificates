package com.cmm.certificates.core.logging

import io.sentry.kotlin.multiplatform.Sentry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

private fun logger(tag: String): Logger = LogManager.getLogger("com.cmm.certificates.$tag")

actual fun logInfo(tag: String, message: String) {
    logger(tag).info(message)
}

actual fun logWarn(tag: String, message: String) {
    logger(tag).warn(message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    logger(tag).error(message, throwable)
    if (throwable != null) {
        Sentry.captureException(throwable)
    } else {
        Sentry.captureMessage("[$tag] $message")
    }
}
