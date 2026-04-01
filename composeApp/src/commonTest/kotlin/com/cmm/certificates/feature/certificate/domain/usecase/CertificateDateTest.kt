package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.formatCertificateDateInput
import com.cmm.certificates.domain.parseCertificateDateInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CertificateDateTest {

    @Test
    fun parsesRecommendedDateInputAndFormatsForCertificateTemplate() {
        val date = parseCertificateDateInput("2025-11-25")

        requireNotNull(date)
        assertEquals("2025 m. lapkričio 25 d.", formatCertificateDate(date))
        assertEquals("2025-11-25", formatCertificateDateInput(date))
    }

    @Test
    fun rejectsInvalidRecommendedDateInput() {
        assertNull(parseCertificateDateInput("11/25/2025"))
        assertNull(parseCertificateDateInput("2025-02-30"))
    }
}
