package com.cmm.certificates.data.xlsx

import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.domain.config.recipientEmailField
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
                mapEntry(
                    fieldValues = fieldValues,
                    configuration = configuration,
                ),
            )
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

    private fun Map<String, String?>.isEmptyRow(): Boolean {
        return values.all { it.isNullOrBlank() }
    }
}
