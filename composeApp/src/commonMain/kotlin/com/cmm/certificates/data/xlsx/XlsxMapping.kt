package com.cmm.certificates.data.xlsx

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.math.floor
import kotlin.math.roundToLong

internal object XlsxEntryMapper {
    fun mapEntries(
        headers: List<String>,
        rows: List<Map<String, String?>>,
    ): List<RegistrationEntry> {
        val entries = mutableListOf<RegistrationEntry>()
        for (row in rows) {
            if (row.isEmptyRow()) break
            val timestamp = row.valueAt(0, headers)
            if (timestamp.isNullOrBlank()) break
            val parsedTimestamp = parseTimestamp(timestamp)
            val formattedDate = formatLithuanianDate(parsedTimestamp.date)
            entries.add(
                RegistrationEntry(
                    date = parsedTimestamp,
                    formattedDate = formattedDate,
                    primaryEmail = row.valueAt(1, headers).orEmpty(),
                    name = row.valueAt(2, headers).orEmpty(),
                    surname = row.valueAt(3, headers).orEmpty(),
                    secondaryEmail = row.valueAt(4, headers).orEmpty(),
                    institution = row.valueAt(5, headers).orEmpty(),
                    forEvent = row.valueAt(6, headers).orEmpty(),
                    publicityApproval = row.valueAt(7, headers).orEmpty(),
                ),
            )
        }
        return entries
    }

    private fun parseTimestamp(raw: String): LocalDateTime {
        val trimmed = raw.trim()
        val numeric = trimmed.toDoubleOrNull()
        if (numeric != null) {
            return parseExcelSerialDate(numeric)
        }
        val parts = trimmed.split(" ", limit = 2)
        require(parts.size == 2) { "Invalid timestamp format: $raw" }

        val dateParts = parts[0].split("/")
        require(dateParts.size == 3) { "Invalid date format: $raw" }
        val month = dateParts[0].toIntOrNull() ?: error("Invalid month in timestamp: $raw")
        val day = dateParts[1].toIntOrNull() ?: error("Invalid day in timestamp: $raw")
        val year = dateParts[2].toIntOrNull() ?: error("Invalid year in timestamp: $raw")

        val timeParts = parts[1].split(":")
        require(timeParts.size >= 2) { "Invalid time format: $raw" }
        val hour = timeParts[0].toIntOrNull() ?: error("Invalid hour in timestamp: $raw")
        val minute = timeParts[1].toIntOrNull() ?: error("Invalid minute in timestamp: $raw")
        val second = timeParts.getOrNull(2)?.toIntOrNull() ?: 0

        val date = LocalDate(year, month, day)
        val time = LocalTime(hour, minute, second)
        return LocalDateTime(date, time)
    }

    private fun parseExcelSerialDate(serial: Double): LocalDateTime {
        var days = floor(serial).toLong()
        val fraction = serial - floor(serial)
        if (serial >= 60.0) {
            days -= 1
        }

        var totalSeconds = (fraction * 86_400.0).roundToLong()
        if (totalSeconds >= 86_400) {
            totalSeconds -= 86_400
            days += 1
        }

        val date = LocalDate(1899, 12, 31).plus(days.toInt(), DateTimeUnit.DAY)
        val hour = (totalSeconds / 3600).toInt()
        val minute = ((totalSeconds % 3600) / 60).toInt()
        val second = (totalSeconds % 60).toInt()
        return LocalDateTime(date, LocalTime(hour, minute, second))
    }

    private fun formatLithuanianDate(date: LocalDate): String {
        val monthName = when (date.monthNumber) {
            1 -> "sausio"
            2 -> "vasario"
            3 -> "kovo"
            4 -> "baland\u017Eio"
            5 -> "gegu\u017E\u0117s"
            6 -> "bir\u017Eelio"
            7 -> "liepos"
            8 -> "rugpj\u016B\u010Dio"
            9 -> "rugs\u0117jo"
            10 -> "spalio"
            11 -> "lapkri\u010Dio"
            12 -> "gruod\u017Eio"
            else -> ""
        }
        return "${date.year} m. $monthName ${date.dayOfMonth} d."
    }

    private fun Map<String, String?>.isEmptyRow(): Boolean {
        return values.all { it.isNullOrBlank() }
    }

    private fun Map<String, String?>.valueAt(index: Int, headers: List<String>): String? {
        val header = headers.getOrNull(index) ?: return null
        return this[header]
    }
}
