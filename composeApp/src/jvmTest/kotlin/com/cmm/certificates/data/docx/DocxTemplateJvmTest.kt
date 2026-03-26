package com.cmm.certificates.data.docx

import org.apache.poi.xwpf.usermodel.XWPFDocument
import kotlin.test.Test
import kotlin.test.assertTrue

class DocxTemplateJvmTest {

    @Test
    fun buildDocxBytes_replacesSplitRunsTablesHeadersAndFooters() {
        val templateBytes = buildTemplateDocx()
        val method = DocxTemplate::class.java.getDeclaredMethod(
            "buildDocxBytes",
            ByteArray::class.java,
            Map::class.java,
        )
        method.isAccessible = true

        val docxBytes = method.invoke(
            DocxTemplate,
            templateBytes,
            mapOf(
                "{{vardas_pavarde}}" to "Ada Lovelace",
                "{{destytojas}}" to "Line 1\\nLine 2",
                "{{sertifikato_pavadinimas}}" to "Music Workshop",
                "{{data}}" to "2026-03-26",
            ),
        ) as ByteArray

        XWPFDocument(docxBytes.inputStream()).use { document ->
            assertTrue(document.paragraphs.joinToString("\n") { it.text }.contains("Ada Lovelace"))
            assertTrue(document.tables.joinToString("\n") { it.text }.contains("Line 1"))
            assertTrue(document.headerList.joinToString("\n") { it.text }.contains("Music Workshop"))
            assertTrue(document.footerList.joinToString("\n") { it.text }.contains("2026-03-26"))
        }
    }

    private fun buildTemplateDocx(): ByteArray {
        val document = XWPFDocument()
        document.createParagraph().apply {
            createRun().setText("{{vard")
            createRun().setText("as_pavarde}}")
        }
        document.createTable(1, 1).rows.first().getCell(0).setText("{{destytojas}}")
        document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT)
            .createParagraph().createRun().setText("{{sertifikato_pavadinimas}}")
        document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT)
            .createParagraph().createRun().setText("{{data}}")

        return java.io.ByteArrayOutputStream().use { output ->
            document.write(output)
            output.toByteArray()
        }
    }
}
