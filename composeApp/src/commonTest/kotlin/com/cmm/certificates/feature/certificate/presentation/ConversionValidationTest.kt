package com.cmm.certificates.feature.certificate.presentation

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_dynamic_required
import certificates.composeapp.generated.resources.conversion_error_recipient_email_header_required
import certificates.composeapp.generated.resources.conversion_error_xlsx_parse
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.domain.config.AccreditedHoursFieldId
import com.cmm.certificates.domain.config.AccreditedIdFieldId
import com.cmm.certificates.domain.config.AccreditedTypeFieldId
import com.cmm.certificates.domain.config.CertificateNameFieldId
import com.cmm.certificates.domain.config.DocumentIdFieldId
import com.cmm.certificates.domain.config.LectorFieldId
import com.cmm.certificates.domain.config.LectorLabelFieldId
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.buildConversionValidationState
import com.cmm.certificates.presentation.buildTemplateSupportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionValidationTest {
    private val configuration = defaultCertificateConfiguration()

    @Test
    fun missingTemplateTag_disablesOnlyConfiguredFields() {
        val support = buildTemplateSupportState(
            configuration,
            setOf(
                "{{dokumento_id}}",
                "{{sertifikato_pavadinimas}}",
            ),
        )

        assertTrue(support.field(AccreditedIdFieldId).isEnabled.not())
        assertTrue(support.field(AccreditedTypeFieldId).isEnabled.not())
        assertTrue(support.field(AccreditedHoursFieldId).isEnabled.not())
        assertTrue(support.field(LectorFieldId).isEnabled.not())
        assertTrue(support.field(LectorLabelFieldId).isEnabled.not())
        assertTrue(support.field(DocumentIdFieldId).isEnabled)
        assertTrue(support.field(CertificateNameFieldId).isEnabled)
    }

    @Test
    fun submitValidation_requiresOnlyEnabledFields_andKeepsFileErrors() {
        val support = buildTemplateSupportState(
            configuration,
            setOf("{{dokumento_id}}", "{{sertifikato_pavadinimas}}"),
        )
        val validation = buildConversionValidationState(
            files = ConversionFilesState(
                xlsxLoadError = UiMessage(Res.string.conversion_error_xlsx_parse),
                templatePath = "template.docx",
            ),
            configuration = configuration,
            formValues = mapOf(LectorLabelFieldId to "Lecturer:"),
            entriesCount = 0,
            templateSupport = support,
            hasAttemptedSubmit = true,
        )

        assertEquals(Res.string.conversion_error_xlsx_parse, validation.xlsxError?.resource)
        assertNull(validation.errorFor(AccreditedIdFieldId))
        assertNull(validation.errorFor(AccreditedTypeFieldId))
        assertNull(validation.errorFor(AccreditedHoursFieldId))
        assertNull(validation.errorFor(LectorFieldId))
        assertNull(validation.errorFor(LectorLabelFieldId))
        assertEquals(Res.string.conversion_error_dynamic_required, validation.errorFor(DocumentIdFieldId)?.resource)
        assertEquals(Res.string.conversion_error_dynamic_required, validation.errorFor(CertificateNameFieldId)?.resource)
        assertTrue(validation.hasBlockingErrors)
    }

    @Test
    fun submitValidation_requiresRecipientEmailColumnWhenMissing() {
        val validation = buildConversionValidationState(
            files = ConversionFilesState(
                xlsxPath = "registrations.xlsx",
                templatePath = "template.docx",
            ),
            configuration = configuration,
            formValues = emptyMap(),
            entriesCount = 1,
            templateSupport = buildTemplateSupportState(configuration, emptySet()),
            hasAttemptedSubmit = true,
            requiresRecipientEmailSelection = true,
        )

        assertEquals(Res.string.conversion_error_recipient_email_header_required, validation.xlsxError?.resource)
        assertTrue(validation.hasBlockingErrors)
    }
}
