package com.cmm.certificates.domain

import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

class BuildCertificateReplacementsUseCase {
    operator fun invoke(
        request: GenerateCertificatesRequest,
        entry: RegistrationEntry,
        docId: Long,
    ): Map<String, String> {
        val fullName = listOf(entry.name, entry.surname)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return mapOf(
            "{{vardas_pavarde}}" to fullName,
            "{{data}}" to entry.formattedDate,
            "{{akreditacijos_id}}" to request.accreditedId,
            "{{dokumento_id}}" to docId.toString(),
            "{{akreditacijos_tipas}}" to request.accreditedType,
            "{{akreditacijos_valandos}}" to request.accreditedHours,
            "{{sertifikato_pavadinimas}}" to request.certificateName,
            "{{destytojas}}" to request.lector,
            "{{destytojo_tipas}}" to request.lectorGender,
        )
    }
}
