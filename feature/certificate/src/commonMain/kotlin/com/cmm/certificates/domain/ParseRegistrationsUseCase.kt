package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.domain.port.RegistrationParser

class ParseRegistrationsUseCase(
    private val registrationParser: RegistrationParser,
) {
    operator fun invoke(path: String): Result<List<RegistrationEntry>> {
        return runCatching { registrationParser.parse(path) }
    }
}
