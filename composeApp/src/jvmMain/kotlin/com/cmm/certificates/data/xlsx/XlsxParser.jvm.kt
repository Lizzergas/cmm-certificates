package com.cmm.certificates.data.xlsx

import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * Minimal XLSX reader (no external libraries).
 *
 * Reads:
 * - column titles from first row
 * - row content as List<Map<header, cellValue>>
 *
 * Limitations:
 * - Does not evaluate formulas (reads cached <v> if present)
 * - Does not apply number/date formats (styles.xml ignored)
 * - Merged cells not handled
 */
actual object XlsxParser {
    actual fun parse(path: String): List<RegistrationEntry> {
        val sheet = readFirstSheet(path)
        return XlsxEntryMapper.mapEntries(sheet.headers, sheet.rows)
    }

    data class Sheet(
        val name: String,
        val headers: List<String>,
        val rows: List<Map<String, String?>>,
    )

    fun readFirstSheet(path: String): Sheet {
        ZipFile(path).use { zip ->
            val sharedStrings = readSharedStrings(zip)

            val workbook = readWorkbook(zip) // name + r:id
            require(workbook.sheets.isNotEmpty()) { "No sheets found in workbook." }

            val rels = readWorkbookRels(zip) // r:id -> target like "worksheets/sheet1.xml"
            val first = workbook.sheets.first()
            val target =
                rels[first.relId] ?: error("Missing relationship for sheet r:id=${first.relId}")
            val sheetEntryPath = "xl/$target".normalizeXlsxPath()

            val entry =
                zip.getEntry(sheetEntryPath) ?: error("Sheet XML not found: $sheetEntryPath")
            val parsed = zip.getInputStream(entry).use { parseSheet(it, sharedStrings) }
            return Sheet(
                name = first.name,
                headers = parsed.headers,
                rows = parsed.rows,
            )
        }
    }

    fun readAllSheets(path: String): List<Sheet> {
        ZipFile(path).use { zip ->
            val sharedStrings = readSharedStrings(zip)
            val workbook = readWorkbook(zip)
            val rels = readWorkbookRels(zip)

            return workbook.sheets.map { s ->
                val target =
                    rels[s.relId] ?: error("Missing relationship for sheet r:id=${s.relId}")
                val sheetEntryPath = "xl/$target".normalizeXlsxPath()

                val entry =
                    zip.getEntry(sheetEntryPath) ?: error("Sheet XML not found: $sheetEntryPath")
                val parsed = zip.getInputStream(entry).use { parseSheet(it, sharedStrings) }

                Sheet(
                    name = s.name,
                    headers = parsed.headers,
                    rows = parsed.rows,
                )
            }
        }
    }

    // -------------------- Internal parsing --------------------

    private data class Workbook(val sheets: List<WorkbookSheet>)
    private data class WorkbookSheet(val name: String, val relId: String)

    private data class ParsedSheet(val headers: List<String>, val rows: List<Map<String, String?>>)

    private fun readWorkbook(zip: ZipFile): Workbook {
        val entry = zip.getEntry("xl/workbook.xml") ?: error("Missing xl/workbook.xml")
        zip.getInputStream(entry).use { input ->
            val r = xmlReader(input)
            val sheets = mutableListOf<WorkbookSheet>()

            var inSheets = false
            while (r.hasNext()) {
                when (r.next()) {
                    XMLStreamConstants.START_ELEMENT -> {
                        when (r.localName) {
                            "sheets" -> inSheets = true
                            "sheet" -> if (inSheets) {
                                val name = r.getAttr("name") ?: ""
                                val relId = r.getAttr("r:id") ?: r.getAttr("id")
                                require(!relId.isNullOrBlank()) { "Sheet missing r:id" }
                                sheets += WorkbookSheet(name = name, relId = relId)
                            }
                        }
                    }

                    XMLStreamConstants.END_ELEMENT -> {
                        if (r.localName == "sheets") inSheets = false
                    }
                }
            }
            return Workbook(sheets)
        }
    }

    /**
     * Reads xl/_rels/workbook.xml.rels to map rId -> Target (relative to xl/)
     */
    private fun readWorkbookRels(zip: ZipFile): Map<String, String> {
        val entry = zip.getEntry("xl/_rels/workbook.xml.rels")
            ?: error("Missing xl/_rels/workbook.xml.rels")
        zip.getInputStream(entry).use { input ->
            val r = xmlReader(input)
            val map = mutableMapOf<String, String>()

            while (r.hasNext()) {
                when (r.next()) {
                    XMLStreamConstants.START_ELEMENT -> {
                        if (r.localName == "Relationship") {
                            val id = r.getAttr("Id") ?: continue
                            val target = r.getAttr("Target") ?: continue
                            // We only care about worksheets, but mapping all is fine
                            map[id] = target
                        }
                    }
                }
            }
            return map
        }
    }

    /**
     * Reads xl/sharedStrings.xml (if present).
     * Returns list where index is the shared-string id.
     */
    private fun readSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        zip.getInputStream(entry).use { input ->
            val r = xmlReader(input)
            val out = ArrayList<String>()

            var inSi = false
            val sb = StringBuilder()

            while (r.hasNext()) {
                when (r.next()) {
                    XMLStreamConstants.START_ELEMENT -> {
                        when (r.localName) {
                            "si" -> {
                                inSi = true
                                sb.setLength(0)
                            }
                            // shared strings may contain multiple <t> (rich text), also <r><t>
                            "t" -> if (inSi) {
                                sb.append(readElementTextPreserve(r))
                            }
                        }
                    }

                    XMLStreamConstants.END_ELEMENT -> {
                        if (r.localName == "si") {
                            inSi = false
                            out.add(sb.toString())
                        }
                    }
                }
            }
            return out
        }
    }

    private fun parseSheet(sheetXml: InputStream, sharedStrings: List<String>): ParsedSheet {
        val r = xmlReader(sheetXml)

        // We'll build each row as colIndex -> value
        val rowsByIndex = mutableMapOf<Int, MutableMap<Int, String?>>()

        var currentRowIndex: Int? = null
        var currentCellRef: String? = null
        var currentCellType: String? = null
        var currentInlineText: StringBuilder? = null
        var expectingValueForCell = false

        while (r.hasNext()) {
            when (r.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    when (r.localName) {
                        "row" -> {
                            val rowNum = r.getAttr("r")?.toIntOrNull()
                            // Excel row numbers are 1-based
                            currentRowIndex = rowNum?.minus(1)
                            if (currentRowIndex != null) {
                                rowsByIndex.getOrPut(currentRowIndex) { mutableMapOf() }
                            }
                        }

                        "c" -> { // cell
                            currentCellRef = r.getAttr("r") // like "B2"
                            currentCellType = r.getAttr("t") // "s", "inlineStr", "b", etc.
                            expectingValueForCell = false
                            currentInlineText = null
                        }

                        "v" -> {
                            expectingValueForCell = true
                        }

                        "is" -> { // inline string container
                            if (currentCellType == "inlineStr") {
                                currentInlineText = StringBuilder()
                            }
                        }

                        "t" -> {
                            if (currentCellType == "inlineStr" && currentInlineText != null) {
                                currentInlineText.append(readElementTextPreserve(r))
                            }
                        }
                    }
                }

                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                    if (expectingValueForCell) {
                        val text = r.text
                        val rowIdx = currentRowIndex
                        val colIdx = currentCellRef?.let { colIndexFromCellRef(it) }

                        if (rowIdx != null && colIdx != null) {
                            val value = decodeCellValue(
                                raw = text,
                                type = currentCellType,
                                sharedStrings = sharedStrings,
                            )
                            rowsByIndex[rowIdx]!![colIdx] = value
                        }
                    }
                }

                XMLStreamConstants.END_ELEMENT -> {
                    when (r.localName) {
                        "v" -> expectingValueForCell = false
                        "c" -> {
                            // If inline string, store whatever we collected.
                            if (currentCellType == "inlineStr") {
                                val rowIdx = currentRowIndex
                                val colIdx = currentCellRef?.let { colIndexFromCellRef(it) }
                                if (rowIdx != null && colIdx != null) {
                                    rowsByIndex[rowIdx]!![colIdx] = currentInlineText?.toString()
                                }
                            }
                            currentCellRef = null
                            currentCellType = null
                            currentInlineText = null
                        }
                    }
                }
            }
        }

        // Convert sparse row maps to headers + row maps keyed by header
        val headerRow = rowsByIndex[0] ?: emptyMap()
        val maxCol = (rowsByIndex.values.flatMap { it.keys }.maxOrNull() ?: -1)

        val headers = (0..maxCol).map { col ->
            headerRow[col]?.takeIf { it.isNotBlank() } ?: "Column${col + 1}"
        }

        val outRows = mutableListOf<Map<String, String?>>()
        val dataRowIndices = rowsByIndex.keys.filter { it > 0 }.sorted()

        for (ri in dataRowIndices) {
            val row = rowsByIndex[ri] ?: continue
            // Skip completely empty rows
            if (row.isEmpty()) continue

            val map = LinkedHashMap<String, String?>()
            for (col in 0..maxCol) {
                map[headers[col]] = row[col]
            }
            outRows.add(map)
        }

        return ParsedSheet(headers = headers, rows = outRows)
    }

    private fun decodeCellValue(raw: String, type: String?, sharedStrings: List<String>): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        return when (type) {
            "s" -> { // shared string
                val idx = trimmed.toIntOrNull() ?: return trimmed
                sharedStrings.getOrNull(idx) ?: trimmed
            }

            "b" -> if (trimmed == "1") "TRUE" else "FALSE"
            "str" -> trimmed
            "e" -> "ERROR:$trimmed"
            // default: number or general
            else -> trimmed
        }
    }

    // -------------------- Utilities --------------------

    private fun xmlReader(input: InputStream): XMLStreamReader {
        val factory = XMLInputFactory.newInstance()
        factory.setProperty(XMLInputFactory.IS_COALESCING, true)
        return factory.createXMLStreamReader(input)
    }

    private fun XMLStreamReader.getAttr(name: String): String? {
        for (i in 0 until attributeCount) {
            if (getAttributeLocalName(i) == name || getAttributeName(i).toString() == name) {
                return getAttributeValue(i)
            }
            // Handles "r:id" sometimes appearing with prefix in localName already, depending on parser
            if (getAttributeName(i).toString().endsWith(":$name")) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    /**
     * Reads element text but preserves whitespace better than reader.elementText in some files.
     * Assumes we're positioned on START_ELEMENT.
     */
    private fun readElementTextPreserve(r: XMLStreamReader): String {
        val sb = StringBuilder()
        while (r.hasNext()) {
            when (r.next()) {
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> sb.append(r.text)
                XMLStreamConstants.END_ELEMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    /**
     * Cell ref like "A1", "BC23" -> column index (0-based).
     */
    private fun colIndexFromCellRef(ref: String): Int {
        var i = 0
        var col = 0
        while (i < ref.length) {
            val ch = ref[i]
            if (ch in 'A'..'Z' || ch in 'a'..'z') {
                val v = (ch.uppercaseChar() - 'A') + 1
                col = col * 26 + v
                i++
            } else break
        }
        return col - 1
    }

    private fun String.normalizeXlsxPath(): String {
        // XLSX rel targets can be like "../worksheets/sheet1.xml" or "worksheets/sheet1.xml"
        // We only handle common cases: strip leading "./" and resolve simple "../" inside xl/
        var p = this.replace("\\", "/")
        while (p.startsWith("./")) p = p.removePrefix("./")
        // If it starts with "../", remove one directory level relative to xl/
        while (p.startsWith("../")) p = p.removePrefix("../")
        return p
    }
}
