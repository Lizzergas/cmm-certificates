package com.cmm.certificates.data.xlsx

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

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
        for (row in sheet.rows) {
            if (row.isEmptyRow()) break
            val fieldValues = normalizedMappings.mapValues { (_, header) ->
                row[header].orEmpty().trim()
            }
            if (configuration.xlsxFields.isNotEmpty() && fieldValues.values.all { it.isBlank() }) continue
            entries.add(
                mapLegacyEntry(
                    row = row,
                    headers = sheet.headers,
                    fieldValues = fieldValues,
                ),
            )
        }
        return entries
    }

    private fun mapLegacyEntry(
        row: Map<String, String?>,
        headers: List<String>,
        fieldValues: Map<String, String>,
    ): RegistrationEntry {
        val mappedName = fieldValues[NameFieldId].orEmpty().ifBlank { row.valueAt(2, headers).orEmpty() }
        val mappedSurname = fieldValues[SurnameFieldId].orEmpty().ifBlank { row.valueAt(3, headers).orEmpty() }
        return RegistrationEntry(
            primaryEmail = row.valueAt(1, headers).orEmpty(),
            name = mappedName,
            surname = mappedSurname,
            institution = row.valueAt(4, headers).orEmpty(),
            paymentUrl = row.valueAt(6, headers).orEmpty(),
            forEvent = row.valueAt(5, headers).orEmpty(),
            publicityApproval = row.valueAt(7, headers).orEmpty(),
            fieldValues = fieldValues,
        )
    }

    private fun Map<String, String?>.isEmptyRow(): Boolean {
        return values.all { it.isNullOrBlank() }
    }

    private fun Map<String, String?>.valueAt(index: Int, headers: List<String>): String? {
        val header = headers.getOrNull(index) ?: return null
        return this[header]
    }
}
