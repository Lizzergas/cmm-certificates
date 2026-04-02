package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.data.xlsx.XlsxSheetData
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.domain.port.RegistrationParser

class ParseRegistrationsUseCase(
    private val registrationParser: RegistrationParser,
) {
    fun inspect(path: String): Result<XlsxSheetData> {
        return runCatching { registrationParser.inspect(path) }
    }

    operator fun invoke(
        path: String,
        configuration: CertificateConfiguration,
    ): Result<List<RegistrationEntry>> {
        return runCatching { registrationParser.parse(path, configuration) }
    }
}
