package com.cmm.certificates.data.xlsx

data class XlsxRowData(
    val rowNumber: Int,
    val cells: Map<String, String?>,
)

data class XlsxSheetData(
    val name: String,
    val headers: List<String>,
    val rows: List<XlsxRowData>,
)

expect object XlsxParser {
    fun readFirstSheet(path: String): XlsxSheetData
}
