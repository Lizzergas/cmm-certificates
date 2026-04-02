package com.cmm.certificates.data.xlsx

import java.io.InputStream
import java.util.zip.ZipFile

actual object XlsxParser {
    actual fun readFirstSheet(path: String): XlsxSheetData {
        ZipFile(path).use { zip ->
            val sharedStrings = readSharedStrings(zip)
            val workbookXml = zip.getInputStream(zip.getEntry("xl/workbook.xml")).bufferedReader().readText()
            val workbookRelsXml = zip.getInputStream(zip.getEntry("xl/_rels/workbook.xml.rels")).bufferedReader().readText()
            val workbook = parseWorkbook(workbookXml, workbookRelsXml)
            val first = workbook.firstOrNull() ?: return XlsxSheetData("", emptyList(), emptyList())
            val sheetPath = "xl/${first.path}"
            val entry = zip.getEntry(sheetPath) ?: return XlsxSheetData(first.name, emptyList(), emptyList())
            val parsed = zip.getInputStream(entry).use { input ->
                parseSheet(input, sharedStrings)
            }
            return XlsxSheetData(
                name = first.name,
                headers = parsed.headers,
                rows = parsed.rows,
            )
        }
    }

    private data class SheetInfo(
        val name: String,
        val path: String,
    )

    private data class ParsedSheet(
        val headers: List<String>,
        val rows: List<Map<String, String?>>,
    )

    private fun readSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        return zip.getInputStream(entry).bufferedReader().readText()
            .let(::parseSharedStrings)
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val siRegex = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
        return siRegex.findAll(xml).map { match ->
            val si = match.groupValues[1]
            val tRegex = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
            tRegex.findAll(si)
                .joinToString(separator = "") { decodeXmlEntities(it.groupValues[1]) }
        }.toList()
    }

    private fun parseWorkbook(workbookXml: String, relsXml: String): List<SheetInfo> {
        val relMap = Regex("<Relationship[^>]*Id=\"(.*?)\"[^>]*Target=\"(.*?)\"")
            .findAll(relsXml)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val sheetRegex = Regex("<sheet[^>]*name=\"(.*?)\"[^>]*r:id=\"(.*?)\"")
        return sheetRegex.findAll(workbookXml).mapNotNull { match ->
            val name = decodeXmlEntities(match.groupValues[1])
            val relId = match.groupValues[2]
            val target = relMap[relId] ?: return@mapNotNull null
            SheetInfo(name = name, path = target.removePrefix("/"))
        }.toList()
    }

    private fun parseSheet(input: InputStream, sharedStrings: List<String>): ParsedSheet {
        val xml = input.bufferedReader().readText()
        val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
        val rows = mutableListOf<Map<String, String?>>()
        val headers = linkedSetOf<String>()
        rowRegex.findAll(xml).forEachIndexed { index, rowMatch ->
            val rowXml = rowMatch.groupValues[1]
            val rowMap = linkedMapOf<String, String?>()
            cellRegex.findAll(rowXml).forEach { cellMatch ->
                val attrs = cellMatch.groupValues[1]
                val body = cellMatch.groupValues[2]
                val ref = Regex("r=\"([A-Z]+)(\\d+)\"").find(attrs)?.groupValues?.get(1).orEmpty()
                val type = Regex("t=\"(.*?)\"").find(attrs)?.groupValues?.get(1)
                val rawValue = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)
                val inlineValue = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)
                val value = when (type) {
                    "s" -> rawValue?.toIntOrNull()?.let { sharedStrings.getOrNull(it) }
                    "inlineStr" -> inlineValue?.let(::decodeXmlEntities)
                    else -> rawValue?.let(::decodeXmlEntities)
                }
                val header = if (index == 0) value.orEmpty().ifBlank { ref } else headers.elementAtOrNull(columnIndex(ref)) ?: ref
                headers += header
                rowMap[header] = value
            }
            if (index > 0) rows += rowMap
        }
        return ParsedSheet(headers = headers.toList(), rows = rows)
    }

    private fun columnIndex(columnRef: String): Int {
        var result = 0
        columnRef.forEach { char ->
            result = result * 26 + (char - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
