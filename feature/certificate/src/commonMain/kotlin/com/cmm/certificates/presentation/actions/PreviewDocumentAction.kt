package com.cmm.certificates.presentation.actions

import com.cmm.certificates.domain.GenerateCertificatesRequest
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.presentation.ConversionUiState

internal class PreviewDocumentAction(
    private val requestBuilder: GenerateCertificatesRequestBuilder,
    private val previewDocument: suspend (GenerateCertificatesRequest) -> String?,
    private val openFile: (String) -> Boolean,
) {
    suspend fun execute(
        snapshot: ConversionUiState,
        settings: SettingsState,
        useInAppPdfPreview: Boolean,
    ): PreviewDocumentResult {
        val previewPath = previewDocument(requestBuilder.build(snapshot, settings))
        if (previewPath.isNullOrBlank()) return PreviewDocumentResult.NoPreview
        if (useInAppPdfPreview) return PreviewDocumentResult.ShowInApp(previewPath)
        return if (openFile(previewPath)) {
            PreviewDocumentResult.NoPreview
        } else {
            PreviewDocumentResult.ExternalOpenFailed(previewPath)
        }
    }
}

internal sealed interface PreviewDocumentResult {
    data object NoPreview : PreviewDocumentResult
    data class ShowInApp(val path: String) : PreviewDocumentResult
    data class ExternalOpenFailed(val path: String) : PreviewDocumentResult
}
