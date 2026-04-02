package com.cmm.certificates.data.xlsx

import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
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

    private val configuration = defaultCertificateConfiguration().copy(
        xlsxFields = listOf(
            com.cmm.certificates.domain.config.XlsxTagField(
                tag = NameFieldId,
                headerName = "Name",
            ),
            com.cmm.certificates.domain.config.XlsxTagField(
                tag = SurnameFieldId,
                headerName = "Surname",
            ),
        ),
    )

    @Test
    fun mapEntries_mapsConfiguredHeadersAndKeepsLegacyEmailFields() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
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
            ),
            configuration = configuration,
        )

        assertEquals(1, entries.size)
        assertEquals("ada@example.com", entries.single().primaryEmail)
        assertEquals("Ada", entries.single().name)
        assertEquals("Lovelace", entries.single().surname)
        assertEquals("CMM", entries.single().institution)
        assertEquals("Ada", entries.single().fieldValues[NameFieldId])
        assertEquals("Lovelace", entries.single().fieldValues[SurnameFieldId])
    }

    @Test
    fun mapEntries_stopsAtFirstFullyEmptyRow() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
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
                    "Email" to "",
                    "Name" to "",
                    "Surname" to "",
                    "Institution" to "",
                    "Event" to "",
                    "Payment" to "",
                    "Publicity" to "",
                ),
            ),
            ),
            configuration = configuration,
        )

        assertEquals(listOf("first@example.com"), entries.map { it.primaryEmail })
    }

    @Test
    fun mapEntries_noLongerDependsOnLegacyTimestampSentinel() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
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
                ),
                mapOf(
                    "Timestamp" to "",
                    "Email" to "next@example.com",
                    "Name" to "Next",
                    "Surname" to "Person",
                    "Institution" to "CMM",
                    "Event" to "Workshop",
                    "Payment" to "",
                    "Publicity" to "yes",
                )
            ),
            ),
            configuration = configuration,
        )

        assertEquals(listOf("excel@example.com", "next@example.com"), entries.map { it.primaryEmail })
    }
}
