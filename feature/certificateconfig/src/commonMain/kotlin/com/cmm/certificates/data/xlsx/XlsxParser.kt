package com.cmm.certificates.data.xlsx

data class XlsxSheetData(
    val name: String,
    val headers: List<String>,
    val rows: List<Map<String, String?>>,
)

expect object XlsxParser {
    fun readFirstSheet(path: String): XlsxSheetData
}
