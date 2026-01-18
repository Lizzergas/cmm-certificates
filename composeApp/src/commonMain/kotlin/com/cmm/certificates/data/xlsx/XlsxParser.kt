package com.cmm.certificates.data.xlsx

expect object XlsxParser {
    fun parse(path: String): List<RegistrationEntry>
}
