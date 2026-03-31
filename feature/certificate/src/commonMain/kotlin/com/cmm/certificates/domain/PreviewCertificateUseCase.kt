package com.cmm.certificates.domain

import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import com.cmm.certificates.domain.port.CertificateDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class PreviewCertificateUseCase(
    private val buildCertificateReplacements: BuildCertificateReplacementsUseCase,
    private val documentGenerator: CertificateDocumentGenerator,
) {
    private val logTag = "PreviewCertificate"

    suspend operator fun invoke(request: GenerateCertificatesRequest): String? =
        withContext(Dispatchers.IO) {
            if (request.templatePath.isBlank()) {
                logWarn(logTag, "Preview aborted: missing template path")
                return@withContext null
            }
            if (request.entries.isEmpty()) {
                logWarn(logTag, "Preview aborted: no parsed entries")
                return@withContext null
            }
            if (request.accreditedId.isBlank() ||
                request.docIdStart.isBlank() ||
                request.accreditedHours.isBlank() ||
                request.certificateName.isBlank() ||
                request.lector.isBlank()
            ) {
                logWarn(logTag, "Preview aborted: missing certificate form fields")
                return@withContext null
            }

            val docId = request.docIdStart.trim().toLongOrNull()
            if (docId == null) {
                logWarn(
                    logTag,
                    "Preview aborted: invalid document id start '${request.docIdStart}'"
                )
                return@withContext null
            }

            try {
                logInfo(logTag, "Loading template bytes for preview")
                val templateBytes = documentGenerator.loadTemplate(request.templatePath)
                val entry = request.entries.first()
                val replacements = buildCertificateReplacements(
                    request = request,
                    entry = entry,
                    docId = docId,
                )
                logInfo(logTag, "Generating preview PDF for docId=$docId")
                documentGenerator.createPreviewPdf(templateBytes, replacements)
            } catch (e: Exception) {
                logError(logTag, "Failed to generate preview PDF", e)
                null
            }
        }
}
