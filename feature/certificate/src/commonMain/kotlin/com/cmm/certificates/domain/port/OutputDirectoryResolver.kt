package com.cmm.certificates.domain.port

interface OutputDirectoryResolver {
    fun resolve(path: String): String

    fun ensureExists(path: String): Boolean

    fun canWrite(path: String): Boolean
}
