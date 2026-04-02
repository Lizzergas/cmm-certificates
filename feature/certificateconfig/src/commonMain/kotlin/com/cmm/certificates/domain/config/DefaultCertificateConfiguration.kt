package com.cmm.certificates.domain.config

const val CertificateDateFieldId = "data"
const val AccreditedIdFieldId = "akreditacijos_id"
const val DocumentIdFieldId = "dokumento_id"
const val AccreditedTypeFieldId = "akreditacijos_tipas"
const val AccreditedHoursFieldId = "akreditacijos_valandos"
const val CertificateNameFieldId = "sertifikato_pavadinimas"
const val LectorFieldId = "destytojas"
const val LectorLabelFieldId = "destytojo_tipas"
const val NameFieldId = "vardas"
const val SurnameFieldId = "pavarde"
const val EmailFieldId = "email"

private val DefaultAccreditedTypeOptions = listOf(
    "paskaitoje",
    "seminare",
    "konferencijoje",
    "mokymuose",
)

fun defaultCertificateConfiguration(): CertificateConfiguration {
    return CertificateConfiguration(
        id = "default-certificate",
        documentNumberTag = DocumentIdFieldId,
        xlsxFields = listOf(
            XlsxTagField(tag = NameFieldId, label = "Vardas", headerName = "Vardas"),
            XlsxTagField(tag = SurnameFieldId, label = "Pavardė", headerName = "Pavardė"),
        ),
        manualFields = listOf(
            ManualTagField(tag = CertificateDateFieldId, type = CertificateFieldType.DATE),
            ManualTagField(
                tag = AccreditedIdFieldId,
                type = CertificateFieldType.TEXT,
                defaultValue = "IVP-10",
            ),
            ManualTagField(tag = DocumentIdFieldId, type = CertificateFieldType.NUMBER),
            ManualTagField(
                tag = AccreditedTypeFieldId,
                type = CertificateFieldType.SELECT,
                options = DefaultAccreditedTypeOptions,
            ),
            ManualTagField(tag = AccreditedHoursFieldId, type = CertificateFieldType.NUMBER),
            ManualTagField(tag = CertificateNameFieldId, type = CertificateFieldType.TEXT),
            ManualTagField(tag = LectorFieldId, type = CertificateFieldType.TEXT),
            ManualTagField(
                tag = LectorLabelFieldId,
                type = CertificateFieldType.TEXT,
                defaultValue = "Lektorius:",
            ),
        ),
    )
}
