package com.cmm.certificates

fun joinPath(directory: String, fileName: String): String {
    val trimmed = directory.trimEnd('/', '\\')
    return if (trimmed.isEmpty()) fileName else "$trimmed/$fileName"
}
