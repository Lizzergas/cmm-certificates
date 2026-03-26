package com.cmm.certificates.feature.pdfconversion.domain

import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import kotlinx.coroutines.flow.StateFlow

interface PdfConversionProgressRepository {
    val state: StateFlow<PdfConversionProgressState>

    fun start(
        total: Int,
        outputDir: String,
        docIdStart: Long,
        entries: List<RegistrationEntry>,
    )

    fun update(current: Int)

    fun setCurrentDocId(docId: Long?)

    fun finish()

    fun fail(message: UiMessage)

    fun requestCancel()

    fun isCancelRequested(): Boolean

    fun clear()
}
