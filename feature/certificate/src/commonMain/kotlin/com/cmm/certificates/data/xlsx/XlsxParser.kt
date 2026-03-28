package com.cmm.certificates.data.xlsx

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

expect object XlsxParser {
    fun parse(path: String): List<RegistrationEntry>
}
