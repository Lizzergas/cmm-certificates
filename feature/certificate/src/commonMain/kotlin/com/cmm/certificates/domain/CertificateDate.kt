package com.cmm.certificates.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun parseCertificateDateInput(raw: String): LocalDate? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    val parts = trimmed.split("-")
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    return try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun formatCertificateDate(date: LocalDate): String {
    val monthName = when (date.month.number) {
        1 -> "sausio"
        2 -> "vasario"
        3 -> "kovo"
        4 -> "balandžio"
        5 -> "gegužės"
        6 -> "birželio"
        7 -> "liepos"
        8 -> "rugpjūčio"
        9 -> "rugsėjo"
        10 -> "spalio"
        11 -> "lapkričio"
        12 -> "gruodžio"
        else -> ""
    }
    return "${date.year} m. $monthName ${date.day} d."
}

fun formatCertificateDateInput(date: LocalDate): String {
    return buildString {
        append(date.year.toString().padStart(4, '0'))
        append('-')
        append(date.month.number.toString().padStart(2, '0'))
        append('-')
        append(date.day.toString().padStart(2, '0'))
    }
}

fun certificateDateInputToUtcMillis(input: String): Long? {
    return parseCertificateDateInput(input)
        ?.atStartOfDayIn(TimeZone.UTC)
        ?.toEpochMilliseconds()
}

fun utcMillisToCertificateDateInput(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return formatCertificateDateInput(date)
}
