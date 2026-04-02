package com.cmm.certificates.presentation.refresh

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_refresh_xlsx_failed
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.data.xlsx.XlsxSheetData
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.domain.port.FileChangeObserver
import com.cmm.certificates.domain.port.FileChangeSubscription
import com.cmm.certificates.domain.port.RegistrationParser
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.usecase.ParseRegistrationsUseCase
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.xlsxParseErrorMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionRefreshCoordinatorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshXlsx_autoRefreshParseFailure_keepsExistingEntriesAndPostsFailure() = runTest {
        var files = ConversionFilesState(xlsxPath = "registrations.xlsx")
        val existingEntries = mutableListOf(
            RegistrationEntry(
                primaryEmail = "user@example.com",
                name = "Jonas",
                surname = "Jonaitis",
                institution = "CMM",
                forEvent = "Renginys",
                publicityApproval = "yes",
            ),
        )
        val notifications = mutableListOf<Pair<UiMessage, Boolean>>()
        val coordinator = ConversionRefreshCoordinator(
            scope = this,
            fileChangeObserver = FakeFileChangeObserver(),
            parseRegistrations = ParseRegistrationsUseCase(
                registrationParser = FakeRegistrationParser(
                    inspectedSheet = XlsxSheetData(
                        name = "Sheet1",
                        headers = listOf("name"),
                        rows = emptyList(),
                    ),
                    parseError = IllegalStateException("parse failed"),
                ),
            ),
            documentGenerator = FakeCertificateDocumentGenerator(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            refreshDebounceMillis = 0L,
            currentConfiguration = {
                CertificateConfiguration(
                    id = "test",
                    documentNumberTag = "doc_id",
                )
            },
            currentFiles = { files },
            currentEntries = { existingEntries.toList() },
            updateFiles = { update -> files = update(files) },
            setParsedEntries = { _, updated ->
                existingEntries.clear()
                existingEntries.addAll(updated)
            },
            postNotification = { message, isError -> notifications += message to isError },
        )

        coordinator.refreshXlsx(path = "registrations.xlsx", isAutoRefresh = true)
        advanceUntilIdle()

        assertEquals(1, existingEntries.size)
        assertEquals(xlsxParseErrorMessage(), files.xlsxLoadError)
        assertEquals(1, notifications.size)
        assertEquals(UiMessage(Res.string.conversion_refresh_xlsx_failed), notifications.single().first)
        assertTrue(notifications.single().second)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshXlsx_ignoresParsedEntriesWhenPathIsNoLongerSelected() = runTest {
        var files = ConversionFilesState(xlsxPath = "second.xlsx")
        val existingEntries = mutableListOf(
            RegistrationEntry(
                primaryEmail = "second@example.com",
                name = "Second",
                surname = "Entry",
                institution = "CMM",
                forEvent = "Renginys",
                publicityApproval = "yes",
            ),
        )
        val coordinator = ConversionRefreshCoordinator(
            scope = this,
            fileChangeObserver = FakeFileChangeObserver(),
            parseRegistrations = ParseRegistrationsUseCase(
                registrationParser = FakeRegistrationParser(
                    inspectedSheet = XlsxSheetData(
                        name = "Sheet1",
                        headers = listOf("email"),
                        rows = emptyList(),
                    ),
                    parsedEntries = listOf(
                        RegistrationEntry(
                            primaryEmail = "first@example.com",
                            name = "First",
                            surname = "Entry",
                            institution = "CMM",
                            forEvent = "Renginys",
                            publicityApproval = "yes",
                        ),
                    ),
                ),
            ),
            documentGenerator = FakeCertificateDocumentGenerator(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            refreshDebounceMillis = 0L,
            currentConfiguration = {
                CertificateConfiguration(
                    id = "test",
                    documentNumberTag = "doc_id",
                    xlsxFields = listOf(
                        com.cmm.certificates.domain.config.XlsxTagField(
                            tag = "email",
                            headerName = "email",
                        ),
                    ),
                )
            },
            currentFiles = { files },
            currentEntries = { existingEntries.toList() },
            updateFiles = { update -> files = update(files) },
            setParsedEntries = { _, updated ->
                existingEntries.clear()
                existingEntries.addAll(updated)
            },
            postNotification = { _, _ -> },
        )

        coordinator.refreshXlsx(path = "first.xlsx", isAutoRefresh = false)
        advanceUntilIdle()

        assertEquals("second@example.com", existingEntries.single().primaryEmail)
        assertEquals("second.xlsx", files.xlsxPath)
    }

    @Test
    fun missingHeaders_usesLabelWhenAvailableAndTagFallbackOtherwise() {
        val configuration = CertificateConfiguration(
            id = "test",
            documentNumberTag = "doc_id",
            xlsxFields = listOf(
                com.cmm.certificates.domain.config.XlsxTagField(
                    tag = "first_name",
                    label = "Vardas",
                    headerName = "Name",
                ),
                com.cmm.certificates.domain.config.XlsxTagField(
                    tag = "email",
                    headerName = "Email",
                ),
            ),
        )

        val result = missingHeaders(
            configuration = configuration,
            actualHeaders = listOf("Other"),
        )

        assertEquals(listOf("Vardas -> Name", "email -> Email"), result)
    }
}

private class FakeFileChangeObserver : FileChangeObserver {
    override fun watch(path: String, onChange: () -> Unit): FileChangeSubscription {
        return object : FileChangeSubscription {
            override fun cancel() = Unit
        }
    }
}

private class FakeRegistrationParser(
    private val inspectedSheet: XlsxSheetData,
    private val parseError: Throwable? = null,
    private val parsedEntries: List<RegistrationEntry> = emptyList(),
) : RegistrationParser {
    override fun inspect(path: String): XlsxSheetData = inspectedSheet

    override fun parse(
        path: String,
        configuration: CertificateConfiguration,
    ): List<RegistrationEntry> {
        parseError?.let { throw it }
        return parsedEntries
    }
}

private class FakeCertificateDocumentGenerator : CertificateDocumentGenerator {
    override fun loadTemplate(path: String): ByteArray = byteArrayOf()

    override fun inspectTemplatePlaceholders(path: String): Set<String> = emptySet()

    override fun fillTemplateToPdf(
        templateBytes: ByteArray,
        outputPath: String,
        replacements: Map<String, String>,
    ) = Unit

    override fun createPreviewPdf(
        templateBytes: ByteArray,
        replacements: Map<String, String>,
    ): String? = null
}
