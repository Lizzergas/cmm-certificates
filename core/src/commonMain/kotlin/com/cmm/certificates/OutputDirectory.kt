package com.cmm.certificates

expect object OutputDirectory {
    fun resolve(path: String): String
    fun ensureExists(path: String): Boolean
}
