package com.cmm.certificates.presentation.actions

import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.presentation.ConversionUiState

internal class GenerateDocumentsAction(
    private val requestBuilder: GenerateCertificatesRequestBuilder,
    private val generateDocuments: suspend (GenerateCertificatesRequest) -> Unit,
) {
    suspend fun execute(
        snapshot: ConversionUiState,
        settings: SettingsState,
    ) {
        generateDocuments(requestBuilder.build(snapshot, settings))
    }
}
