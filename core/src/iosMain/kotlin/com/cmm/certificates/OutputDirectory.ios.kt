package com.cmm.certificates

actual object OutputDirectory {
    actual fun resolve(path: String): String {
        throw UnsupportedOperationException("Output directory resolution is not supported on iOS yet.")
    }

    actual fun ensureExists(path: String): Boolean {
        throw UnsupportedOperationException("Output directory resolution is not supported on iOS yet.")
    }
}
