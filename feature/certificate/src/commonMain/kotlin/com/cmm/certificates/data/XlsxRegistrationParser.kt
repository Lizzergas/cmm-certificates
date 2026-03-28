package com.cmm.certificates.data

import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.domain.port.RegistrationParser

class XlsxRegistrationParser : RegistrationParser {
    override fun parse(path: String) = XlsxParser.parse(path)
}
