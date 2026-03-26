package com.cmm.certificates.feature.certificate.domain.model

import kotlinx.datetime.LocalDateTime

data class RegistrationEntry(
    val date: LocalDateTime,
    val formattedDate: String,
    val primaryEmail: String,
    val name: String,
    val surname: String,
    val institution: String,
    val forEvent: String,
    val paymentUrl: String = "",
    val publicityApproval: String,
)
