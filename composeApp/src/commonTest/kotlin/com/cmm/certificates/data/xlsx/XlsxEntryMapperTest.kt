package com.cmm.certificates.data.xlsx

import kotlin.test.Test
import kotlin.test.assertEquals

class XlsxEntryMapperTest {

    private val headers = listOf(
        "Timestamp",
        "Email",
        "Name",
        "Surname",
        "Institution",
        "Event",
        "Payment",
        "Publicity",
    )

    @Test
    fun mapEntries_mapsNonDateColumnsUsingExistingPositions() {
        val entries = XlsxEntryMapper.mapEntries(
            headers = headers,
            rows = listOf(
                mapOf(
                    "Timestamp" to "03/05/2024 10:15:30",
                    "Email" to "ada@example.com",
                    "Name" to "Ada",
                    "Surname" to "Lovelace",
                    "Institution" to "CMM",
                    "Event" to "Workshop",
                    "Payment" to "https://pay",
                    "Publicity" to "yes",
                )
            ),
        )

        assertEquals(1, entries.size)
        assertEquals("ada@example.com", entries.single().primaryEmail)
        assertEquals("Ada", entries.single().name)
        assertEquals("Lovelace", entries.single().surname)
        assertEquals("CMM", entries.single().institution)
    }

    @Test
    fun mapEntries_stopsAtFirstBlankTimestamp() {
        val entries = XlsxEntryMapper.mapEntries(
            headers = headers,
            rows = listOf(
                mapOf(
                    "Timestamp" to "03/05/2024 10:15:30",
                    "Email" to "first@example.com",
                    "Name" to "First",
                    "Surname" to "Person",
                    "Institution" to "CMM",
                    "Event" to "Workshop",
                    "Payment" to "",
                    "Publicity" to "yes",
                ),
                mapOf(
                    "Timestamp" to "",
                    "Email" to "second@example.com",
                    "Name" to "Second",
                    "Surname" to "Person",
                    "Institution" to "CMM",
                    "Event" to "Workshop",
                    "Payment" to "",
                    "Publicity" to "yes",
                ),
            ),
        )

        assertEquals(listOf("first@example.com"), entries.map { it.primaryEmail })
    }

    @Test
    fun mapEntries_keepsReadingRowsWithNumericFirstColumn() {
        val entries = XlsxEntryMapper.mapEntries(
            headers = headers,
            rows = listOf(
                mapOf(
                    "Timestamp" to "61.5",
                    "Email" to "excel@example.com",
                    "Name" to "Excel",
                    "Surname" to "Serial",
                    "Institution" to "CMM",
                    "Event" to "Workshop",
                    "Payment" to "",
                    "Publicity" to "yes",
                )
            ),
        )

        assertEquals(listOf("excel@example.com"), entries.map { it.primaryEmail })
    }
}
