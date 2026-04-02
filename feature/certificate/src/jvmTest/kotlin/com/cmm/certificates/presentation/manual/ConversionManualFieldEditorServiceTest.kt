package com.cmm.certificates.presentation.manual

import com.cmm.certificates.configeditor.ManualTagFieldDraft
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.ManualTagField
import com.cmm.certificates.presentation.ConversionManualFieldEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionManualFieldEditorServiceTest {

    private val service = ConversionManualFieldEditorService()

    @Test
    fun prepareSave_updatesFieldAndDocumentNumberTag() {
        val configuration = CertificateConfiguration(
            id = "default",
            documentNumberTag = "old_tag",
            manualFields = listOf(
                ManualTagField(
                    tag = "old_tag",
                    label = "Senas",
                    type = CertificateFieldType.TEXT,
                ),
                ManualTagField(
                    tag = "other",
                    label = "Kitas",
                    type = CertificateFieldType.TEXT,
                ),
            ),
        )

        val result = service.prepareSave(
            currentConfiguration = configuration,
            editorState = ConversionManualFieldEditorState(
                originalTag = "old_tag",
                draft = ManualTagFieldDraft(
                    tag = "new_tag",
                    label = "Naujas",
                    type = CertificateFieldType.NUMBER,
                ),
            ),
        )

        assertTrue(result is SaveManualFieldEditResult.Success)
        result as SaveManualFieldEditResult.Success
        assertEquals("new_tag", result.updatedFieldTag)
        assertEquals("new_tag", result.updatedConfiguration.documentNumberTag)
        assertEquals("new_tag", result.updatedConfiguration.manualFields.first().tag)
        assertEquals(
            CertificateFieldType.NUMBER,
            result.updatedConfiguration.manualFields.first().type
        )
    }

    @Test
    fun prepareSave_returnsFailureWhenEditedFieldIsMissing() {
        val configuration = CertificateConfiguration(
            id = "default",
            documentNumberTag = "existing",
            manualFields = listOf(
                ManualTagField(
                    tag = "existing",
                    type = CertificateFieldType.TEXT,
                ),
            ),
        )

        val result = service.prepareSave(
            currentConfiguration = configuration,
            editorState = ConversionManualFieldEditorState(
                originalTag = "missing",
                draft = ManualTagFieldDraft(tag = "updated"),
            ),
        )

        assertEquals(
            SaveManualFieldEditResult.Failure("Nepavyko rasti redaguojamo lauko."),
            result,
        )
    }

    @Test
    fun migrateManualValues_movesExistingValueToRenamedTag() {
        val migrated = service.migrateManualValues(
            currentValues = mapOf(
                "old_tag" to "123",
                "other" to "abc",
            ),
            oldTag = "old_tag",
            newTag = "new_tag",
        )

        assertEquals(null, migrated["old_tag"])
        assertEquals("123", migrated["new_tag"])
        assertEquals("abc", migrated["other"])
    }
}
