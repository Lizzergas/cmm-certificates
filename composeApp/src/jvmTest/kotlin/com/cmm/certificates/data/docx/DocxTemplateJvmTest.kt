package com.cmm.certificates.data.docx

import org.apache.poi.xwpf.usermodel.XWPFDocument
import com.cmm.certificates.domain.config.AccreditedHoursFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.presentation.buildTemplateSupportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun inspectTemplatePlaceholders_collectsParagraphTableHeaderAndFooterTags() {
        val templateBytes = buildTemplateDocx()
        val tempFile = kotlin.io.path.createTempFile(suffix = ".docx").toFile()
        tempFile.writeBytes(templateBytes)
        tempFile.deleteOnExit()

        val placeholders = DocxTemplate.inspectTemplatePlaceholders(tempFile.absolutePath)

        assertEquals(
            setOf(
                "{{vardas_pavarde}}",
                "{{destytojas}}",
                "{{sertifikato_pavadinimas}}",
                "{{data}}",
            ),
            placeholders,
        )
    }

    @Test
    fun inspectTemplatePlaceholders_ignoresMalformedTagAndDisablesMatchingField() {
        val document = XWPFDocument()
        document.createParagraph().createRun().setText(
            "Nr. {{dokumento_id}}\nAkreditacijos Nr. {{akreditacijos_id}}\n{{vardas_pavarde}}\ndalyvavo {{akreditacijos_valando} akademiniu valandu {{akreditacijos_tipas}}\n{{sertifikato_pavadinimas}}\n{{destytojo_tipas}} {{destytojas}}\nReg. data {{data}}"
        )
        val tempFile = kotlin.io.path.createTempFile(suffix = ".docx").toFile()
        tempFile.outputStream().use { document.write(it) }
        tempFile.deleteOnExit()

        val placeholders = DocxTemplate.inspectTemplatePlaceholders(tempFile.absolutePath)
        val support = buildTemplateSupportState(defaultCertificateConfiguration(), placeholders)

        assertTrue("{{akreditacijos_tipas}}" in placeholders)
        assertFalse("{{akreditacijos_valandos}}" in placeholders)
        assertFalse(support.field(AccreditedHoursFieldId).isEnabled)
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
