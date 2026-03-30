package com.cmm.certificates

expect object AppInstallation {
    fun installedResourcePath(fileName: String): String?

    fun installationDirectoryPath(): String?
}
