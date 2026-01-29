package com.cmm.certificates

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry() {
    Sentry.init { options ->
        options.dsn = "https://c9a9d71e36b7daf29bad28b94c643aa4@o648127.ingest.us.sentry.io/4510792300232704"
        options.sendDefaultPii = true
    }
}
