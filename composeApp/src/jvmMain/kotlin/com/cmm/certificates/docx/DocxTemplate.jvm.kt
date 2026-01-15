package com.cmm.certificates.docx

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.docx4j.Docx4J
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import java.util.zip.ZipInputStream

actual object DocxTemplate {
    actual fun loadTemplate(path: String): ByteArray {
        return File(path).readBytes()
    }

    actual fun fillTemplate(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        val docxBytes = buildDocxBytes(templateBytes, replacements)
        FileOutputStream(outputPath).use { output -> output.write(docxBytes) }
    }

    actual fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) {
        // 1) Fill DOCX template (Apache POI)
        val docxBytes = buildDocxBytes(templateBytes, replacements)

        // 2) Extract background image from DOCX (image1.* preferred)
        val bg = extractFirstImageFromDocx(docxBytes)

        // 3) Convert DOCX -> PDF (docx4j)
        val wordPackage = WordprocessingMLPackage.load(ByteArrayInputStream(docxBytes))
        val pdfBytes = ByteArrayOutputStream().use { baos ->
            Docx4J.toPDF(wordPackage, baos)
            baos.toByteArray()
        }

        // 4) Stamp background image behind content (PDFBox)
        val stampedPdfBytes = if (bg != null) {
            addBackgroundToPdfBytes(
                pdfBytes = pdfBytes,
                backgroundImageBytes = bg.bytes,
                imageExtLower = bg.extLower,
                mode = BackgroundMode.COVER,
                bleedPx = 2f
            )
        } else {
            pdfBytes
        }

        // 5) Keep only the first page
        val singlePagePdfBytes = keepOnlyFirstPage(stampedPdfBytes)

        // 6) Write result
        FileOutputStream(outputPath).use { it.write(singlePagePdfBytes) }
    }



    private fun buildDocxBytes(
        templateBytes: ByteArray,
        replacements: Map<String, String>,
    ): ByteArray {
        val doc = XWPFDocument(ByteArrayInputStream(templateBytes))
        doc.use { document ->
            document.paragraphs.forEach { paragraph ->
                replaceInParagraph(paragraph, replacements)
            }
            document.tables.forEach { table ->
                replaceInTable(table, replacements)
            }
            document.headerList.forEach { header ->
                header.paragraphs.forEach { paragraph ->
                    replaceInParagraph(paragraph, replacements)
                }
                header.tables.forEach { table -> replaceInTable(table, replacements) }
            }
            document.footerList.forEach { footer ->
                footer.paragraphs.forEach { paragraph ->
                    replaceInParagraph(paragraph, replacements)
                }
                footer.tables.forEach { table -> replaceInTable(table, replacements) }
            }
            val output = ByteArrayOutputStream()
            document.write(output)
            return output.toByteArray()
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

enum class BackgroundMode { COVER, CONTAIN, STRETCH }

private data class Quad(val w: Float, val h: Float, val x: Float, val y: Float)

private fun addBackgroundToPdfBytes(
    pdfBytes: ByteArray,
    backgroundImageBytes: ByteArray,
    imageExtLower: String,
    mode: BackgroundMode = BackgroundMode.COVER,
    bleedPx: Float = 2f, // avoids white edges due to rounding
): ByteArray {
    PDDocument.load(ByteArrayInputStream(pdfBytes)).use { doc ->

        val buffered: BufferedImage = ImageIO.read(ByteArrayInputStream(backgroundImageBytes))
            ?: error("Failed to decode background image ($imageExtLower)")

        val bg = when (imageExtLower) {
            "jpg", "jpeg" -> JPEGFactory.createFromImage(doc, buffered)
            else -> LosslessFactory.createFromImage(doc, buffered) // png, etc.
        }

        for (page in doc.pages) {
            val mb = page.mediaBox
            val pageW = mb.width
            val pageH = mb.height

            val imgW = bg.width.toFloat()
            val imgH = bg.height.toFloat()

            val quad = when (mode) {
                BackgroundMode.STRETCH -> {
                    Quad(pageW + 2 * bleedPx, pageH + 2 * bleedPx, -bleedPx, -bleedPx)
                }
                BackgroundMode.CONTAIN -> {
                    val scale = minOf(pageW / imgW, pageH / imgH)
                    val drawW = imgW * scale
                    val drawH = imgH * scale
                    Quad(
                        w = drawW + 2 * bleedPx,
                        h = drawH + 2 * bleedPx,
                        x = (pageW - drawW) / 2f - bleedPx,
                        y = (pageH - drawH) / 2f - bleedPx
                    )
                }
                BackgroundMode.COVER -> {
                    val scale = maxOf(pageW / imgW, pageH / imgH)
                    val drawW = imgW * scale
                    val drawH = imgH * scale
                    Quad(
                        w = drawW + 2 * bleedPx,
                        h = drawH + 2 * bleedPx,
                        x = (pageW - drawW) / 2f - bleedPx,
                        y = (pageH - drawH) / 2f - bleedPx
                    )
                }
            }

            // PREPEND => draw behind existing content
            PDPageContentStream(doc, page, AppendMode.PREPEND, true, true).use { cs ->
                cs.drawImage(bg, quad.x, quad.y, quad.w, quad.h)
            }
        }

        ByteArrayOutputStream().use { out ->
            doc.save(out)
            return out.toByteArray()
        }
    }
}


private data class DocxImage(val bytes: ByteArray, val extLower: String)

private fun extractFirstImageFromDocx(docxBytes: ByteArray): DocxImage? {
    val entries = mutableListOf<Pair<String, ByteArray>>()

    ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
        while (true) {
            val e = zis.nextEntry ?: break
            val name = e.name
            if (!e.isDirectory && name.startsWith("word/media/")) {
                // common: word/media/image1.jpeg, image2.png, etc.
                val bytes = zis.readBytes()
                entries += name to bytes
            }
            zis.closeEntry()
        }
    }

    if (entries.isEmpty()) return null

    // Prefer image1.* if present; otherwise smallest "imageN"; otherwise first.
    val preferred = entries
        .sortedWith(
            compareBy<Pair<String, ByteArray>>(
                { !it.first.substringAfterLast('/').startsWith("image1.", ignoreCase = true) }, // false first
                { it.first } // lexicographic (image1, image2...)
            )
        )
        .first()

    val ext = preferred.first.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return DocxImage(preferred.second, ext)
}

private fun keepOnlyFirstPage(pdfBytes: ByteArray): ByteArray {
    PDDocument.load(ByteArrayInputStream(pdfBytes)).use { doc ->
        // remove pages from the end down to index 1
        for (i in doc.numberOfPages - 1 downTo 1) {
            doc.removePage(i)
        }
        ByteArrayOutputStream().use { out ->
            doc.save(out)
            return out.toByteArray()
        }
    }
}
