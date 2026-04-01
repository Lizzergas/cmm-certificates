package com.cmm.certificates.feature.certificate.domain.model

data class RegistrationEntry(
    val primaryEmail: String,
    val name: String,
    val surname: String,
    val institution: String,
    val forEvent: String,
    val paymentUrl: String = "",
    val publicityApproval: String,
)
