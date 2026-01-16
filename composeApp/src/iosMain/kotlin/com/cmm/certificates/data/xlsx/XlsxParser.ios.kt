package com.cmm.certificates.data.xlsx

import com.cmm.certificates.data.xlsx.RegistrationEntry

actual object XlsxParser {
    actual fun parse(path: String): List<RegistrationEntry> {
        throw UnsupportedOperationException("XLSX parsing is only supported on JVM desktop targets for now.")
    }
}
