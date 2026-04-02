package com.cmm.certificates.domain.config

import org.junit.Test

class CertificateConfigurationValidatorTest {

    @Test(expected = InvalidCertificateConfigurationException::class)
    fun validate_rejectsDocumentNumberTagOutsideManualFields() {
        val configuration = defaultCertificateConfiguration().copy(
            documentNumberTag = NameFieldId,
        )

        validateCertificateConfiguration(configuration).getOrThrow()
    }

    @Test(expected = InvalidCertificateConfigurationException::class)
    fun validate_rejectsMissingHeaderNamesForXlsxFields() {
        val configuration = defaultCertificateConfiguration().copy(
            xlsxFields = listOf(
                XlsxTagField(tag = NameFieldId, headerName = null),
            ),
        )

        validateCertificateConfiguration(configuration).getOrThrow()
    }
}
