package com.cmm.certificates

actual object AppBuildInfo {
    actual fun versionName(): String? = null

    actual fun commitHash(): String? = null
}
