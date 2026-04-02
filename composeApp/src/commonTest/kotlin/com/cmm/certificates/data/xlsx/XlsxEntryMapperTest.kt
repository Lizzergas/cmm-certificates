package com.cmm.certificates.data.xlsx

import com.cmm.certificates.domain.config.EmailFieldId
import com.cmm.certificates.domain.config.NameFieldId
import com.cmm.certificates.domain.config.SurnameFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class XlsxEntryMapperTest {

    @Test
    fun defaultConfiguration_exposesRecipientEmailRole() {
        val configuration = defaultCertificateConfiguration()

        assertEquals(EmailFieldId, configuration.recipientEmailTag)
        assertTrue(configuration.xlsxFields.any { it.tag == EmailFieldId })
    }

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
                tag = EmailFieldId,
                headerName = "Email",
            ),
            com.cmm.certificates.domain.config.XlsxTagField(
                tag = SurnameFieldId,
                headerName = "Surname",
            ),
        ),
    )

    @Test
    fun mapEntries_mapsOnlyConfiguredTags() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
                headers = headers,
                rows = listOf(
                    row(
                        rowNumber = 2,
                        "Timestamp" to "03/05/2024 10:15:30",
                        "Email" to "ada@example.com",
                        "Name" to "Ada",
                        "Surname" to "Lovelace",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "https://pay",
                        "Publicity" to "yes",
                    ),
                ),
            ),
            configuration = configuration,
        )

        assertEquals(1, entries.size)
        assertEquals("ada@example.com", entries.single().primaryEmail)
        assertEquals("Ada", entries.single().name)
        assertEquals("Lovelace", entries.single().surname)
        assertEquals("", entries.single().institution)
        assertEquals("ada@example.com", entries.single().fieldValues[EmailFieldId])
        assertEquals("Ada", entries.single().fieldValues[NameFieldId])
        assertEquals("Lovelace", entries.single().fieldValues[SurnameFieldId])
    }

    @Test
    fun mapEntries_skipsSingleEmptyRowWhenDataResumesWithinLookahead() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
                headers = headers,
                rows = listOf(
                    row(
                        rowNumber = 2,
                        "Timestamp" to "03/05/2024 10:15:30",
                        "Email" to "first@example.com",
                        "Name" to "First",
                        "Surname" to "Person",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "",
                        "Publicity" to "yes",
                    ),
                    row(
                        rowNumber = 3,
                        "Timestamp" to "",
                        "Email" to "",
                        "Name" to "",
                        "Surname" to "",
                        "Institution" to "",
                        "Event" to "",
                        "Payment" to "",
                        "Publicity" to "",
                    ),
                    row(
                        rowNumber = 4,
                        "Timestamp" to "03/05/2024 10:16:30",
                        "Email" to "second@example.com",
                        "Name" to "Second",
                        "Surname" to "Person",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "",
                        "Publicity" to "yes",
                    ),
                ),
            ),
            configuration = configuration,
        )

        assertEquals(listOf("first@example.com", "second@example.com"), entries.map { it.primaryEmail })
    }

    @Test
    fun mapEntries_stopsWhenNoDataExistsWithinThreeRowLookahead() {
        val entries = XlsxEntryMapper.mapEntries(
            sheet = XlsxSheetData(
                name = "Sheet1",
                headers = headers,
                rows = listOf(
                    row(
                        rowNumber = 2,
                        "Timestamp" to "03/05/2024 10:15:30",
                        "Email" to "first@example.com",
                        "Name" to "First",
                        "Surname" to "Person",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "",
                        "Publicity" to "yes",
                    ),
                    row(rowNumber = 3),
                    row(rowNumber = 4),
                    row(rowNumber = 5),
                    row(rowNumber = 6),
                    row(
                        rowNumber = 7,
                        "Timestamp" to "03/05/2024 10:16:30",
                        "Email" to "ignored@example.com",
                        "Name" to "Ignored",
                        "Surname" to "Person",
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
                    row(
                        rowNumber = 2,
                        "Timestamp" to "61.5",
                        "Email" to "excel@example.com",
                        "Name" to "Excel",
                        "Surname" to "Serial",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "",
                        "Publicity" to "yes",
                    ),
                    row(
                        rowNumber = 3,
                        "Timestamp" to "",
                        "Email" to "next@example.com",
                        "Name" to "Next",
                        "Surname" to "Person",
                        "Institution" to "CMM",
                        "Event" to "Workshop",
                        "Payment" to "",
                        "Publicity" to "yes",
                    ),
                ),
            ),
            configuration = configuration,
        )

        assertEquals(listOf("excel@example.com", "next@example.com"), entries.map { it.primaryEmail })
    }

    @Test
    fun mapEntries_reportsMissingRequiredValuesWithRowNumbersAndHeaders() {
        val error = assertFailsWith<XlsxMissingValuesException> {
            XlsxEntryMapper.mapEntries(
                sheet = XlsxSheetData(
                    name = "Sheet1",
                    headers = headers,
                    rows = listOf(
                        row(
                            rowNumber = 2,
                            "Timestamp" to "03/05/2024 10:15:30",
                            "Email" to "",
                            "Name" to "Ada",
                            "Surname" to "Lovelace",
                        ),
                        row(
                            rowNumber = 4,
                            "Timestamp" to "03/05/2024 10:16:30",
                            "Email" to "grace@example.com",
                            "Name" to "",
                            "Surname" to "",
                        ),
                    ),
                ),
                configuration = configuration,
            )
        }

        assertEquals(
            "row 2: missing Email; row 4: missing Name, Surname",
            error.message,
        )
    }

    private fun row(rowNumber: Int, vararg cells: Pair<String, String?>): XlsxRowData {
        return XlsxRowData(
            rowNumber = rowNumber,
            cells = linkedMapOf(*cells),
        )
    }
}
