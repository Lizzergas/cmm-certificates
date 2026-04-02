package com.cmm.certificates.data

import com.cmm.certificates.data.xlsx.XlsxParser
import com.cmm.certificates.data.xlsx.XlsxEntryMapper
import com.cmm.certificates.data.xlsx.XlsxSheetData
import com.cmm.certificates.domain.config.CertificateConfiguration
import com.cmm.certificates.domain.port.RegistrationParser

class XlsxRegistrationParser : RegistrationParser {
    override fun inspect(path: String): XlsxSheetData = XlsxParser.readFirstSheet(path)

    override fun parse(
        path: String,
        configuration: CertificateConfiguration,
    ) = XlsxEntryMapper.mapEntries(
        sheet = XlsxParser.readFirstSheet(path),
        configuration = configuration,
    )
}
