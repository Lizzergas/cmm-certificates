package com.cmm.certificates

actual object AppInstallation {
    actual fun installedResourcePath(fileName: String): String? = null

    actual fun installationDirectoryPath(): String? = null

    actual fun preferredOutputBaseDirectoryPath(): String? = null
}
