package com.cmm.certificates.feature.certificate.domain.port

interface OutputDirectoryResolver {
    fun resolve(path: String): String

    fun ensureExists(path: String): Boolean
}
