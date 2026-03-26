package com.cmm.certificates.feature.certificate.data

import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.feature.certificate.domain.port.RegistrationParser

class XlsxRegistrationParser : RegistrationParser {
    override fun parse(path: String) = XlsxParser.parse(path)
}
