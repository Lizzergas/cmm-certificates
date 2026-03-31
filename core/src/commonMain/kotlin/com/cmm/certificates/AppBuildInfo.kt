package com.cmm.certificates

expect object AppBuildInfo {
    fun versionName(): String?

    fun commitHash(): String?
}
