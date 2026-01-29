package com.cmm.certificates.core.usecase

import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.feature.settings.domain.SettingsRepository

class ClearAllDataUseCase(
    private val settingsRepository: SettingsRepository,
    private val pdfConversionProgressRepository: PdfConversionProgressRepository,
    private val emailProgressRepository: EmailProgressRepository,
) {
    suspend fun clearAll() {
        settingsRepository.resetAndClear()
        pdfConversionProgressRepository.clear()
        emailProgressRepository.clear()
    }
}
