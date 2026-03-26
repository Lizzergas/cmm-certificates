package com.cmm.certificates.feature.certificate.domain.usecase

import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.logging.logError
import com.cmm.certificates.core.logging.logInfo
import com.cmm.certificates.core.logging.logWarn
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_create_output_dir
import certificates.composeapp.generated.resources.conversion_error_fields_required
import certificates.composeapp.generated.resources.conversion_error_invalid_doc_id
import certificates.composeapp.generated.resources.conversion_error_load_template
import certificates.composeapp.generated.resources.conversion_error_no_entries
import certificates.composeapp.generated.resources.conversion_error_template_required
import certificates.composeapp.generated.resources.conversion_error_write_pdf
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.feature.certificate.domain.port.CertificateDocumentGenerator
import com.cmm.certificates.feature.certificate.domain.port.OutputDirectoryResolver
import com.cmm.certificates.feature.pdfconversion.domain.PdfConversionProgressRepository
import com.cmm.certificates.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

private const val DEFAULT_OUTPUT_PATH = "pdf/"

data class GenerateCertificatesRequest(
    val templatePath: String,
    val entries: List<RegistrationEntry>,
    val accreditedId: String,
    val docIdStart: String,
    val accreditedType: String,
    val accreditedHours: String,
    val certificateName: String,
    val lector: String,
    val lectorGender: String,
)

class GenerateCertificatesUseCase(
    private val progressRepository: PdfConversionProgressRepository,
    private val buildCertificateReplacements: BuildCertificateReplacementsUseCase,
    private val documentGenerator: CertificateDocumentGenerator,
    private val outputDirectoryResolver: OutputDirectoryResolver,
) {
    private val logTag = "GenerateCertificates"

    suspend operator fun invoke(request: GenerateCertificatesRequest) {
        if (request.templatePath.isBlank()) {
            logWarn(logTag, "Conversion aborted: missing template path")
            progressRepository.fail(UiMessage(Res.string.conversion_error_template_required))
            return
        }
        if (request.entries.isEmpty()) {
            logWarn(logTag, "Conversion aborted: no parsed entries")
            progressRepository.fail(UiMessage(Res.string.conversion_error_no_entries))
            return
        }
        if (request.accreditedId.isBlank() ||
            request.docIdStart.isBlank() ||
            request.accreditedHours.isBlank() ||
            request.certificateName.isBlank() ||
            request.lector.isBlank()
        ) {
            logWarn(logTag, "Conversion aborted: missing certificate form fields")
            progressRepository.fail(UiMessage(Res.string.conversion_error_fields_required))
            return
        }

        val templateBytes = try {
            logInfo(logTag, "Loading template bytes")
            documentGenerator.loadTemplate(request.templatePath)
        } catch (e: Exception) {
            logError(logTag, "Failed to load template: ${request.templatePath}", e)
            progressRepository.fail(UiMessage(Res.string.conversion_error_load_template))
            return
        }

        val docIdStart = request.docIdStart.trim().toLongOrNull()
        if (docIdStart == null) {
            logWarn(logTag, "Conversion aborted: invalid document id start '${request.docIdStart}'")
            progressRepository.fail(UiMessage(Res.string.conversion_error_invalid_doc_id))
            return
        }

        val baseOutputDir = outputDirectoryResolver.resolve(DEFAULT_OUTPUT_PATH)
        val sanitizedFolder = sanitizeFolderName(request.certificateName)
        val outputDir = joinPath(baseOutputDir, sanitizedFolder)
        if (!outputDirectoryResolver.ensureExists(outputDir)) {
            logError(logTag, "Failed to create output directory: $outputDir")
            progressRepository.fail(UiMessage(Res.string.conversion_error_create_output_dir))
            return
        }

        withContext(Dispatchers.IO) {
            logInfo(logTag, "Generating ${request.entries.size} PDFs into $outputDir")
            progressRepository.start(
                total = request.entries.size,
                outputDir = outputDir,
                docIdStart = docIdStart,
                entries = request.entries,
            )
            for ((index, entry) in request.entries.withIndex()) {
                if (progressRepository.isCancelRequested()) return@withContext

                val docId = docIdStart + index
                progressRepository.setCurrentDocId(docId)
                val replacements = buildCertificateReplacements(
                    request = request,
                    entry = entry,
                    docId = docId,
                )
                val outputPath = joinPath(outputDir, "$docId.pdf")

                try {
                    logInfo(logTag, "Generating PDF for docId=$docId")
                    documentGenerator.fillTemplateToPdf(
                        templateBytes = templateBytes,
                        outputPath = outputPath,
                        replacements = replacements,
                    )
                    if (progressRepository.isCancelRequested()) return@withContext
                    progressRepository.update(index + 1)
                } catch (e: Exception) {
                    logError(logTag, "Failed while generating PDF for docId=$docId", e)
                    progressRepository.fail(UiMessage(Res.string.conversion_error_write_pdf))
                    return@withContext
                }
            }

            if (!progressRepository.isCancelRequested()) {
                logInfo(logTag, "Conversion finished successfully")
                progressRepository.finish()
            } else {
                logWarn(logTag, "Conversion cancelled")
            }
        }
    }

    private fun sanitizeFolderName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return "certificate"
        val cleaned = trimmed
            .replace(Regex("""[\\/:*?\"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('.')
        return cleaned.ifBlank { "certificate" }
    }
}
