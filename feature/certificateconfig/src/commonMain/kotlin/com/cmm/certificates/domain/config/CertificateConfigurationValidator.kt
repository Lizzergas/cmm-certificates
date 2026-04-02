package com.cmm.certificates.domain.config

private val TagRegex = Regex("^[a-z][a-z0-9_]*$")

class InvalidCertificateConfigurationException(
    message: String,
) : IllegalArgumentException(message)

object CertificateConfigurationValidator {
    fun validate(configuration: CertificateConfiguration): CertificateConfiguration {
        require(configuration.version == 1) {
            "Unsupported certificate configuration version: ${configuration.version}"
        }
        require(configuration.id.isNotBlank()) {
            "Certificate configuration id must not be blank"
        }

        val xlsxTags = configuration.xlsxFields.map(XlsxTagField::tag)
        val manualTags = configuration.manualFields.map(ManualTagField::tag)
        val duplicates = (xlsxTags + manualTags).groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) {
            "Certificate configuration contains duplicate tags: ${duplicates.joinToString()}"
        }

        configuration.xlsxFields.forEach { field ->
            require(field.tag.isNotBlank()) { "XLSX field tag must not be blank" }
            require(field.tag.matches(TagRegex)) { "Invalid XLSX field tag '${field.tag}'" }
            require(!field.headerName.isNullOrBlank()) { "XLSX field '${field.tag}' must define headerName" }
        }

        configuration.manualFields.forEach { field ->
            require(field.tag.isNotBlank()) { "Manual field tag must not be blank" }
            require(field.tag.matches(TagRegex)) { "Invalid manual field tag '${field.tag}'" }
            require(field.type != CertificateFieldType.SELECT || field.options.any { it.isNotBlank() }) {
                "Select field '${field.tag}' must define at least one option"
            }
        }

        val recipientEmailTag = configuration.recipientEmailTag?.trim().takeUnless { it.isNullOrBlank() }
        require(recipientEmailTag == null || recipientEmailTag in xlsxTags) {
            "recipientEmailTag '$recipientEmailTag' must reference an XLSX field"
        }

        require(configuration.documentNumberTag in manualTags) {
            "documentNumberTag '${configuration.documentNumberTag}' must reference a manual field"
        }
        require(configuration.manualField(configuration.documentNumberTag)?.type == CertificateFieldType.NUMBER) {
            "documentNumberTag '${configuration.documentNumberTag}' must reference a NUMBER manual field"
        }
        return configuration
    }
}

fun validateCertificateConfiguration(configuration: CertificateConfiguration): Result<CertificateConfiguration> {
    return runCatching { CertificateConfigurationValidator.validate(configuration) }
        .fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(InvalidCertificateConfigurationException(it.message ?: "Invalid certificate configuration")) },
        )
}
