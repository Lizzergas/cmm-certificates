package com.cmm.certificates.data.xlsx

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.domain.config.XlsxTagField
import com.cmm.certificates.domain.config.recipientEmailField
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

private const val EmptyRowLookahead = 3

object XlsxEntryMapper {
    fun mapEntries(
        sheet: XlsxSheetData,
        configuration: CertificateConfiguration,
    ): List<RegistrationEntry> {
        val normalizedMappings = configuration.xlsxFields.associate { field ->
            val selectedHeader = field.headerName?.trim().takeUnless { it.isNullOrBlank() }
                ?: throw IllegalArgumentException("Missing XLSX header mapping for '${field.tag}'")
            require(selectedHeader in sheet.headers) {
                "XLSX header '$selectedHeader' for '${field.tag}' was not found in the selected file"
            }
            field.tag to selectedHeader
        }
        val entries = mutableListOf<RegistrationEntry>()
        val rowIssues = mutableListOf<XlsxRowIssue>()
        val requiredFields = configuration.xlsxFields
        for ((index, row) in sheet.rows.withIndex()) {
            if (row.isEmptyRow()) {
                if (!sheet.hasDataWithinLookahead(index)) break
                continue
            }
            val fieldValues = normalizedMappings.mapValues { (_, header) ->
                row.cells[header].orEmpty().trim()
            }
            val missingFields = requiredFields.mapNotNull { field ->
                field.takeIf { fieldValues[field.tag].isNullOrBlank() }
            }
            if (missingFields.isNotEmpty()) {
                rowIssues += XlsxRowIssue(
                    rowNumber = row.rowNumber,
                    missingColumns = missingFields.map(::fieldDescriptor),
                )
                continue
            }
            entries.add(
                mapEntry(
                    fieldValues = fieldValues,
                    configuration = configuration,
                ),
            )
        }
        if (rowIssues.isNotEmpty()) {
            throw XlsxMissingValuesException(rowIssues)
        }
        return entries
    }

    private fun mapEntry(
        fieldValues: Map<String, String>,
        configuration: CertificateConfiguration,
    ): RegistrationEntry {
        return RegistrationEntry(
            primaryEmail = configuration.recipientEmailField()?.tag?.let(fieldValues::get).orEmpty(),
            name = fieldValues[NameFieldId].orEmpty(),
            surname = fieldValues[SurnameFieldId].orEmpty(),
            institution = "",
            paymentUrl = "",
            forEvent = "",
            publicityApproval = "",
            fieldValues = fieldValues,
        )
    }

    private fun XlsxRowData.isEmptyRow(): Boolean {
        return cells.values.all { it.isNullOrBlank() }
    }

    private fun XlsxSheetData.hasDataWithinLookahead(index: Int): Boolean {
        val endExclusive = minOf(index + 1 + EmptyRowLookahead, rows.size)
        return rows.subList(index + 1, endExclusive).any { !it.isEmptyRow() }
    }

    private fun fieldDescriptor(field: XlsxTagField): String {
        return field.headerName?.trim().takeUnless { it.isNullOrBlank() }
            ?: field.label?.trim().takeUnless { it.isNullOrBlank() }
            ?: field.tag
    }
}

data class XlsxRowIssue(
    val rowNumber: Int,
    val missingColumns: List<String>,
)

class XlsxMissingValuesException(
    val issues: List<XlsxRowIssue>,
) : IllegalArgumentException(buildMessage(issues)) {
    companion object {
        private fun buildMessage(issues: List<XlsxRowIssue>): String {
            val preview = issues.take(3).joinToString(separator = "; ") { issue ->
                "row ${issue.rowNumber}: missing ${issue.missingColumns.joinToString()}"
            }
            val suffix = if (issues.size > 3) "; +${issues.size - 3} more row(s)" else ""
            return "$preview$suffix"
        }
    }
}
