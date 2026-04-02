package com.cmm.certificates.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.material3.SnackbarHostState
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.domain.config.defaultCertificateConfiguration
import org.junit.Rule
import org.junit.Test

class ConversionContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsValidationHintAndInlineFileErrors_whenValidationIsBlocking() {
        val configuration = defaultCertificateConfiguration()
        val state = ConversionUiState(
            configuration = configuration,
            validation = buildConversionValidationState(
                files = ConversionFilesState(),
                configuration = configuration,
                formValues = emptyMap(),
                entriesCount = 0,
                templateSupport = buildTemplateSupportState(configuration, templateAvailableTags = null),
                hasAttemptedSubmit = true,
            ),
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
                    onFieldValueChange = { _, _ -> },
                    onEditField = {},
                    onRecipientEmailHeaderChange = {},
                    onSaveRecipientEmailHeaderAsDefault = {},
                    onFeedbackUrlChange = {},
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }

        composeRule.onNodeWithTag("conversion-validation-hint").assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-xlsx-error", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-template-error", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("conversion-convert-button").assertIsDisplayed()
    }
}
