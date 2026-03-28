package com.cmm.certificates.domain.port

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

interface RegistrationParser {
    fun parse(path: String): List<RegistrationEntry>
}
