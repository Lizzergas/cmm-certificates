package com.cmm.certificates.feature.pdfconversion.domain

import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry

data class PdfConversionProgressState(
    val current: Int = 0,
    val total: Int = 0,
    val inProgress: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: UiMessage? = null,
    val outputDir: String = "",
    val certificateName: String = "",
    val docIdStart: Long? = null,
    val feedbackUrl: String = "",
    val entries: List<RegistrationEntry> = emptyList(),
    val currentDocId: Long? = null,
    val cancelRequested: Boolean = false,
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
)
