package com.cmm.certificates.data.config

expect object InstalledConfigFileAccess {
    fun read(path: String): String

    fun write(path: String, content: String)
}
