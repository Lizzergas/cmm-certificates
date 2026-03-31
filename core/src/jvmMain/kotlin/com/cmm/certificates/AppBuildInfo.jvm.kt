package com.cmm.certificates

actual object AppBuildInfo {
    actual fun versionName(): String? = readProperty("app.version")

    actual fun commitHash(): String? = readProperty("app.commitHash")

    private fun readProperty(name: String): String? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }
}
