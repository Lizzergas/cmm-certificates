package com.cmm.certificates.feature.progress.domain

import com.cmm.certificates.data.xlsx.RegistrationEntry
import com.cmm.certificates.feature.progress.data.PdfConversionProgressState
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

    fun fail(message: String)

    fun requestCancel()

    fun isCancelRequested(): Boolean

    fun clear()
}
