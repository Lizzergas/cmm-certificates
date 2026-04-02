package com.cmm.certificates.presentation

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.DocumentIdFieldId
import com.cmm.certificates.domain.config.ManualTagField
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionManualFieldStateTest {

    @Test
    fun resolveConversionFormState_appliesDefaultsAndNormalizesInvalidSelectValues() {
        val configuration = CertificateConfiguration(
            id = "test",
            documentNumberTag = DocumentIdFieldId,
            manualFields = listOf(
                ManualTagField(
                    tag = "type",
                    type = CertificateFieldType.SELECT,
                    options = listOf("one", "two"),
                ),
                ManualTagField(
                    tag = "title",
                    label = "Title",
                    type = CertificateFieldType.TEXT,
                    defaultValue = "Default title",
                ),
                ManualTagField(
                    tag = DocumentIdFieldId,
                    type = CertificateFieldType.NUMBER,
                ),
            ),
        )

        val resolved = resolveConversionFormState(
            current = ConversionFormState(
                manualValues = mapOf(
                    "type" to "invalid",
                    DocumentIdFieldId to "15",
                ),
            ),
            configuration = configuration,
        )

        assertEquals("one", resolved.valueFor("type"))
        assertEquals("Default title", resolved.valueFor("title"))
        assertEquals("15", resolved.valueFor(DocumentIdFieldId))
    }

    @Test
    fun buildConversionManualFieldUiState_keepsDocumentNumberEnabledWhenTemplateTagIsMissing() {
        val configuration = defaultCertificateConfiguration()
        val resolvedForm = resolveConversionFormState(ConversionFormState(), configuration)
        val templateSupport = buildTemplateSupportState(
            configuration = configuration,
            templateAvailableTags = setOf("{{sertifikato_pavadinimas}}"),
        )
        val validation = buildConversionValidationState(
            files = ConversionFilesState(
                xlsxPath = "registrations.xlsx",
                templatePath = "template.docx",
                templateAvailableTags = setOf("{{sertifikato_pavadinimas}}"),
            ),
            configuration = configuration,
            formValues = resolvedForm.manualValues,
            entriesCount = 1,
            templateSupport = templateSupport,
            hasAttemptedSubmit = false,
        )

        val fields = buildConversionManualFieldUiState(
            configuration = configuration,
            resolvedForm = resolvedForm,
            templateSupport = templateSupport,
            validation = validation,
            enabled = true,
            isTemplateInspectionInProgress = false,
        )

        val documentNumberField = fields.first { it.tag == DocumentIdFieldId }
        val missingField = fields.first { it.tag == "akreditacijos_id" }

        assertTrue(documentNumberField.enabled)
        assertFalse(missingField.enabled)
        assertNotNull(missingField.helper)
        assertNotNull(missingField.tooltip)
    }

    @Test
    fun conversionFieldLabel_usesTrimmedLabelOrFallsBackToTag() {
        assertEquals("Pavadinimas", conversionFieldLabel("tag", "  Pavadinimas  "))
        assertEquals("tag", conversionFieldLabel("tag", "   "))
        assertEquals("tag", conversionFieldLabel("tag", null))
    }
}
