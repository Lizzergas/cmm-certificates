package com.cmm.certificates.presentation.manual

import com.cmm.certificates.configeditor.toField
import com.cmm.certificates.configeditor.updateItem
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.presentation.ConversionManualFieldEditorState

internal class ConversionManualFieldEditorService {
    fun prepareSave(
        currentConfiguration: CertificateConfiguration,
        editorState: ConversionManualFieldEditorState,
    ): SaveManualFieldEditResult {
        val fieldIndex =
            currentConfiguration.manualFields.indexOfFirst { it.tag == editorState.originalTag }
        if (fieldIndex < 0) {
            return SaveManualFieldEditResult.Failure("Nepavyko rasti redaguojamo lauko.")
        }

        val updatedField = editorState.draft.toField()
        val updatedDocumentNumberTag =
            if (currentConfiguration.documentNumberTag == editorState.originalTag) {
                updatedField.tag
            } else {
                currentConfiguration.documentNumberTag
            }

        return SaveManualFieldEditResult.Success(
            updatedConfiguration = currentConfiguration.copy(
                documentNumberTag = updatedDocumentNumberTag,
                manualFields = currentConfiguration.manualFields.updateItem(fieldIndex) { updatedField },
            ),
            updatedFieldTag = updatedField.tag,
        )
    }

    fun migrateManualValues(
        currentValues: Map<String, String>,
        oldTag: String,
        newTag: String,
    ): Map<String, String> {
        if (oldTag == newTag) return currentValues
        val currentValue = currentValues[oldTag] ?: return currentValues
        return (currentValues - oldTag) + (newTag to currentValue)
    }
}

internal sealed interface SaveManualFieldEditResult {
    data class Success(
        val updatedConfiguration: CertificateConfiguration,
        val updatedFieldTag: String,
    ) : SaveManualFieldEditResult

    data class Failure(
        val message: String,
    ) : SaveManualFieldEditResult
}
