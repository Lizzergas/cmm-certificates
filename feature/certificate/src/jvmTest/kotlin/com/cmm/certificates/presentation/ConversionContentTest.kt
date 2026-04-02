package com.cmm.certificates.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.cmm.certificates.core.theme.AppTheme
import org.junit.Rule
import org.junit.Test

class ConversionContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsValidationHintAndInlineFileErrors_whenValidationIsBlocking() {
        val state = ConversionUiState(
            validation = buildConversionValidationState(
                files = ConversionFilesState(),
                form = ConversionFormState(),
                entriesCount = 0,
                templateSupport = buildTemplateSupportState(templateAvailableTags = null),
                hasAttemptedSubmit = true,
            )
        )

        composeRule.setContent {
            AppTheme {
                ConversionContent(
                    state = state,
                    onProfileClick = {},
                    onRetryCachedEmails = {},
                    onPreviewClick = {},
                    onConversionClick = {},
                    onSelectXlsx = {},
                    onSelectTemplate = {},
                    actions = ConversionFormActions(
                        onCertificateDateChange = {},
                        onAccreditedIdChange = {},
                        onDocIdStartChange = {},
                        onAccreditedTypeChange = {},
                        onAccreditedHoursChange = {},
                        onCertificateNameChange = {},
                        onFeedbackUrlChange = {},
                        onLectorChange = {},
                        onLectorGenderChange = {},
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("conversion-validation-hint").assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-xlsx-error", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-template-error", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-convert-button").assertIsDisplayed()
    }
}
