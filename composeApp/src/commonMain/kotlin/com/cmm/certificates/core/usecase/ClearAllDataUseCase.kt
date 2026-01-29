package com.cmm.certificates.core.usecase

import com.cmm.certificates.feature.email.EmailProgressStore
import com.cmm.certificates.feature.progress.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository

class ClearAllDataUseCase(
    private val settingsRepository: SettingsRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val emailProgressStore: EmailProgressStore,
) {
    suspend fun clearAll() {
        settingsRepository.resetAndClear()
        pdfConversionProgressRepository.clear()
        emailProgressStore.clear()
    }
}
