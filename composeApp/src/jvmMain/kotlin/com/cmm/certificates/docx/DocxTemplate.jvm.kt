package com.cmm.certificates.docx

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTable

actual object DocxTemplate {
    actual fun loadTemplate(path: String): ByteArray {
        return File(path).readBytes()
    }

    actual fun fillTemplate(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        val doc = XWPFDocument(ByteArrayInputStream(templateBytes))
        doc.use { doc ->
            doc.paragraphs.forEach { paragraph ->
                replaceInParagraph(paragraph, replacements)
            }
            doc.tables.forEach { table ->
                replaceInTable(table, replacements)
            }
            doc.headerList.forEach { header ->
                header.paragraphs.forEach { paragraph ->
                    replaceInParagraph(
                        paragraph,
                        replacements
                    )
                }
                header.tables.forEach { table -> replaceInTable(table, replacements) }
            }
            doc.footerList.forEach { footer ->
                footer.paragraphs.forEach { paragraph ->
                    replaceInParagraph(
                        paragraph,
                        replacements
                    )
                }
                footer.tables.forEach { table -> replaceInTable(table, replacements) }
            }
            FileOutputStream(outputPath).use { output -> doc.write(output) }
        }
    }

    private fun replaceInTable(table: XWPFTable, replacements: Map<String, String>) {
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEach { paragraph -> replaceInParagraph(paragraph, replacements) }
                cell.tables.forEach { nested -> replaceInTable(nested, replacements) }
            }
        }
    }

    private fun replaceInParagraph(paragraph: XWPFParagraph, replacements: Map<String, String>) {
        if (paragraph.runs.isNullOrEmpty()) return

        val runs = paragraph.runs
        val runTexts = runs.map { it.getText(0) ?: "" }
        val fullText = runTexts.joinToString(separator = "")
        if (fullText.isEmpty()) return

        val ordered = replacements.entries.sortedByDescending { it.key.length }

        for ((needle, replacement) in ordered) {
            if (needle.isEmpty()) continue

            while (true) {
                val snapshotRuns = paragraph.runs
                val snapshotTexts = snapshotRuns.map { it.getText(0) ?: "" }
                val snapshotFull = snapshotTexts.joinToString(separator = "")
                val start = snapshotFull.indexOf(needle)
                if (start < 0) break
                val endExclusive = start + needle.length

                val positions = buildPositions(snapshotTexts)
                val startPos = positions.globalToRunPos(start)
                val endPos = positions.globalToRunPos(endExclusive)
                if (startPos == null || endPos == null) break

                val (startRunIndex, startOffset) = startPos
                val (endRunIndex, endOffset) = endPos

                val startRun = snapshotRuns[startRunIndex]
                val startRunText = snapshotTexts[startRunIndex]
                val prefix = startRunText.substring(0, startOffset)

                val endRunText = snapshotTexts[endRunIndex]
                val suffix = endRunText.substring(endOffset)

                setRunTextPreservingSuffixWithBreaks(
                    run = startRun,
                    prefix = prefix,
                    replacement = replacement,
                    suffix = suffix,
                )

                for (i in startRunIndex + 1 until endRunIndex) {
                    snapshotRuns[i].setText("", 0)
                }
                if (endRunIndex != startRunIndex) {
                    snapshotRuns[endRunIndex].setText("", 0)
                }
            }
        }
    }

    private data class Positions(val runStarts: IntArray, val runTexts: List<String>) {
        fun globalToRunPos(globalIndex: Int): Pair<Int, Int>? {
            if (globalIndex < 0) return null
            if (runTexts.isEmpty()) return null
            val total = runTexts.sumOf { it.length }
            if (globalIndex > total) return null

            var lo = 0
            var hi = runStarts.size - 1
            var ans = 0
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (runStarts[mid] <= globalIndex) {
                    ans = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            val offset = globalIndex - runStarts[ans]
            if (ans == runTexts.lastIndex && offset == runTexts[ans].length) return ans to offset
            if (offset == runTexts[ans].length && ans + 1 < runTexts.size) return (ans + 1) to 0
            return ans to offset
        }
    }

    private fun buildPositions(runTexts: List<String>): Positions {
        val starts = IntArray(runTexts.size)
        var acc = 0
        for (i in runTexts.indices) {
            starts[i] = acc
            acc += runTexts[i].length
        }
        return Positions(starts, runTexts)
    }

    private fun setRunTextPreservingSuffixWithBreaks(
        run: XWPFRun,
        prefix: String,
        replacement: String,
        suffix: String,
    ) {
        val normalized = replacement
            .replace("\\n", "\n")
            .replace(Regex("\\bw:br\\b", RegexOption.IGNORE_CASE), "\n")
            .replace("\r\n", "\n")

        val lines = splitPreserveEmpty(normalized)
        if (lines.size == 1) {
            run.setText(prefix + normalized + suffix, 0)
            return
        }

        run.setText(prefix + lines.first(), 0)
        for (i in 1 until lines.size) {
            run.addBreak()
            run.setText(lines[i])
        }
        if (suffix.isNotEmpty()) {
            run.setText(suffix)
        }
    }

    private fun splitPreserveEmpty(value: String): List<String> {
        val out = ArrayList<String>()
        var start = 0
        for (i in value.indices) {
            if (value[i] == '\n') {
                out.add(value.substring(start, i))
                start = i + 1
            }
        }
        out.add(value.substring(start))
        return out
    }
}
