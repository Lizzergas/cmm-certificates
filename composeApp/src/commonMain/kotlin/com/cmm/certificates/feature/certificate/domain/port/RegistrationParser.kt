package com.cmm.certificates.feature.certificate.domain.port

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

interface RegistrationParser {
    fun parse(path: String): List<RegistrationEntry>
}
