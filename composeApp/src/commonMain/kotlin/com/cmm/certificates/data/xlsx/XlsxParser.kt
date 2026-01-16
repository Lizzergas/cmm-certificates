package com.cmm.certificates.data.xlsx

import kotlinx.datetime.LocalDateTime

data class RegistrationEntry(
    val date: LocalDateTime,
    val formattedDate: String,
    val primaryEmail: String,
    val name: String,
    val surname: String,
    val secondaryEmail: String,
    val institution: String,
    val forEvent: String,
    val publicityApproval: String,
)

expect object XlsxParser {
    fun parse(path: String): List<RegistrationEntry>
}
