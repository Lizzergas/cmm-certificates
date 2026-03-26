package com.cmm.certificates.feature.pdfconversion.data

import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressState
import kotlinx.coroutines.flow.StateFlow

class PdfConversionProgressRepositoryImpl(
    private val store: PdfConversionProgressStore,
) : PdfConversionProgressRepository {
    override val state: StateFlow<PdfConversionProgressState> = store.state

    override fun start(
        total: Int,
        outputDir: String,
        docIdStart: Long,
        entries: List<RegistrationEntry>,
    ) {
        store.start(
            total = total,
            outputDir = outputDir,
            docIdStart = docIdStart,
            entries = entries,
        )
    }

    override fun update(current: Int) {
        store.update(current)
    }

    override fun setCurrentDocId(docId: Long?) {
        store.setCurrentDocId(docId)
    }

    override fun finish() {
        store.finish()
    }

    override fun fail(message: UiMessage) {
        store.fail(message)
    }

    override fun requestCancel() {
        store.requestCancel()
    }

    override fun isCancelRequested(): Boolean = store.isCancelRequested()

    override fun clear() {
        store.clear()
    }
}
