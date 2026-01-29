package com.cmm.certificates.core.usecase

import com.cmm.certificates.feature.email.EmailProgressStore
import com.cmm.certificates.feature.progress.PdfConversionProgressStore
import com.cmm.certificates.feature.settings.domain.SettingsRepository

class ClearAllDataUseCase(
    private val settingsRepository: SettingsRepository,
    private val pdfConversionProgressStore: PdfConversionProgressStore,
    private val emailProgressStore: EmailProgressStore,
) {
    suspend fun clearAll() {
        settingsRepository.resetAndClear()
        pdfConversionProgressStore.clear()
        emailProgressStore.clear()
    }
}
