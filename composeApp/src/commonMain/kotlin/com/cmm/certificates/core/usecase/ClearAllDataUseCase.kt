package com.cmm.certificates.core.usecase

import com.cmm.certificates.data.email.SmtpSettingsRepository
import com.cmm.certificates.feature.email.EmailProgressStore
import com.cmm.certificates.feature.progress.PdfConversionProgressStore
import com.cmm.certificates.feature.settings.SmtpSettingsStore

class ClearAllDataUseCase(
    private val smtpSettingsRepository: SmtpSettingsRepository,
    private val smtpSettingsStore: SmtpSettingsStore,
    private val pdfConversionProgressStore: PdfConversionProgressStore,
    private val emailProgressStore: EmailProgressStore,
) {
    suspend fun clearAll() {
        smtpSettingsRepository.clear()
        smtpSettingsStore.resetToDefaults()
        pdfConversionProgressStore.clear()
        emailProgressStore.clear()
    }
}
