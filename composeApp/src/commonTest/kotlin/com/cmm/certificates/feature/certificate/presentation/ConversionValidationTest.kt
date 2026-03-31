package com.cmm.certificates.feature.certificate.presentation

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_error_certificate_name_required
import certificates.composeapp.generated.resources.conversion_error_doc_id_required
import certificates.composeapp.generated.resources.conversion_error_xlsx_parse
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.presentation.ConversionFilesState
import com.cmm.certificates.presentation.ConversionFormState
import com.cmm.certificates.presentation.buildConversionValidationState
import com.cmm.certificates.presentation.buildTemplateSupportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionValidationTest {

    @Test
    fun missingTemplateTag_disablesOnlyConfiguredFields() {
        val support = buildTemplateSupportState(
            setOf(
                "{{dokumento_id}}",
                "{{sertifikato_pavadinimas}}",
            )
        )

        assertTrue(support.accreditedId.isEnabled.not())
        assertTrue(support.accreditedType.isEnabled.not())
        assertTrue(support.accreditedHours.isEnabled.not())
        assertTrue(support.lector.isEnabled.not())
        assertTrue(support.lectorGender.isEnabled.not())
        assertTrue(support.docIdStart.isEnabled)
        assertTrue(support.certificateName.isEnabled)
    }

    @Test
    fun submitValidation_requiresOnlyEnabledFields_andKeepsFileErrors() {
        val support = buildTemplateSupportState(setOf("{{dokumento_id}}", "{{sertifikato_pavadinimas}}"))
        val validation = buildConversionValidationState(
            files = ConversionFilesState(
                xlsxLoadError = UiMessage(Res.string.conversion_error_xlsx_parse),
                templatePath = "template.docx",
            ),
            form = ConversionFormState(lectorGender = "Lecturer:"),
            entriesCount = 0,
            templateSupport = support,
            hasAttemptedSubmit = true,
        )

        assertEquals(Res.string.conversion_error_xlsx_parse, validation.xlsxError?.resource)
        assertNull(validation.accreditedIdError)
        assertNull(validation.accreditedTypeError)
        assertNull(validation.accreditedHoursError)
        assertNull(validation.lectorError)
        assertNull(validation.lectorGenderError)
        assertEquals(Res.string.conversion_error_doc_id_required, validation.docIdStartError?.resource)
        assertEquals(Res.string.conversion_error_certificate_name_required, validation.certificateNameError?.resource)
        assertTrue(validation.hasBlockingErrors)
    }
}
