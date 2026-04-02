package com.cmm.certificates.domain.port

import com.cmm.certificates.data.xlsx.XlsxSheetData
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

interface RegistrationParser {
    fun inspect(path: String): XlsxSheetData

    fun parse(
        path: String,
        configuration: CertificateConfiguration,
    ): List<RegistrationEntry>
}
