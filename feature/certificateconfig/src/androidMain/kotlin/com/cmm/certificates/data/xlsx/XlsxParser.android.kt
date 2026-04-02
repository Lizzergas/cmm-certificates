package com.cmm.certificates.data.xlsx

actual object XlsxParser {
    actual fun readFirstSheet(path: String): XlsxSheetData {
        throw UnsupportedOperationException("XLSX parsing is only supported on JVM desktop targets for now.")
    }
}
