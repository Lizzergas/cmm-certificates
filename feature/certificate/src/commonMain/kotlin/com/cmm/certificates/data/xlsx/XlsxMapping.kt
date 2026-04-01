package com.cmm.certificates.data.xlsx

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

object XlsxEntryMapper {
    fun mapEntries(
        headers: List<String>,
        rows: List<Map<String, String?>>,
    ): List<RegistrationEntry> {
        val entries = mutableListOf<RegistrationEntry>()
        for (row in rows) {
            if (row.isEmptyRow()) break
            val timestamp = row.valueAt(0, headers)
            if (timestamp.isNullOrBlank()) break
            entries.add(
                RegistrationEntry(
                    primaryEmail = row.valueAt(1, headers).orEmpty(),
                    name = row.valueAt(2, headers).orEmpty(),
                    surname = row.valueAt(3, headers).orEmpty(),
                    institution = row.valueAt(4, headers).orEmpty(),
                    paymentUrl = row.valueAt(6, headers).orEmpty(),
                    forEvent = row.valueAt(5, headers).orEmpty(),
                    publicityApproval = row.valueAt(7, headers).orEmpty(),
                ),
            )
        }
        return entries
    }

    private fun Map<String, String?>.isEmptyRow(): Boolean {
        return values.all { it.isNullOrBlank() }
    }

    private fun Map<String, String?>.valueAt(index: Int, headers: List<String>): String? {
        val header = headers.getOrNull(index) ?: return null
        return this[header]
    }
}
