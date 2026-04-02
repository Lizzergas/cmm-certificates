package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.cmm_logo
import certificates.composeapp.generated.resources.common_action_cancel
import certificates.composeapp.generated.resources.common_action_edit
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.common_file_docx
import certificates.composeapp.generated.resources.common_file_xlsx
import certificates.composeapp.generated.resources.conversion_certificate_date_not_selected
import certificates.composeapp.generated.resources.conversion_convert_button
import certificates.composeapp.generated.resources.conversion_email_extras_section_title
import certificates.composeapp.generated.resources.conversion_feedback_url_hint
import certificates.composeapp.generated.resources.conversion_feedback_url_label
import certificates.composeapp.generated.resources.conversion_form_section_title
import certificates.composeapp.generated.resources.conversion_offline_hint
import certificates.composeapp.generated.resources.conversion_preview_button
import certificates.composeapp.generated.resources.conversion_title
import certificates.composeapp.generated.resources.conversion_tooltip_docx
import certificates.composeapp.generated.resources.conversion_tooltip_xlsx
import certificates.composeapp.generated.resources.conversion_unsupported_hint
import certificates.composeapp.generated.resources.conversion_validation_hint
import certificates.composeapp.generated.resources.docx
import certificates.composeapp.generated.resources.email_progress_cached_retry_requirements_hint
import certificates.composeapp.generated.resources.email_progress_cached_status
import certificates.composeapp.generated.resources.email_progress_retry_cached
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import certificates.composeapp.generated.resources.xlsx
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AppVerticalScrollbar
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.ui.TextFieldTrailingAction
import com.cmm.certificates.core.ui.rememberFilePickerLauncher
import com.cmm.certificates.domain.certificateDateInputToUtcMillis
import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.parseCertificateDateInput
import com.cmm.certificates.domain.utcMillisToCertificateDateInput
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.presentation.components.PrimaryActionButton
import com.cmm.certificates.presentation.components.SelectFileIcon
import com.cmm.certificates.presentation.components.SelectFileIconState
import com.cmm.certificates.presentation.components.ManualFieldConfigDialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MaxWidth = Grid.x240
private val ContentPadding = Grid.x8
private val ContentSpacing = Grid.x10
private val CardPadding = Grid.x8
private val BottomBarPadding = Grid.x6

@Composable
fun ConversionScreen(
    onProfileClick: () -> Unit,
    onStartConversion: () -> Unit,
    onRetryCachedEmails: () -> Unit,
    viewModel: ConversionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchFilePicker = rememberFilePickerLauncher()

    state.previewPdfPath?.let { previewPdfPath ->
        PdfPreviewDialog(
            pdfPath = previewPdfPath,
            onDismiss = viewModel::dismissPreview,
        )
    }

    state.editingManualField?.let { editorState ->
        ManualFieldConfigDialog(
            title = editorState.draft.label.ifBlank { editorState.draft.tag.ifBlank { "Redaguoti lauką" } },
            draft = editorState.draft,
            message = editorState.message,
            onDraftChange = { updated -> viewModel.updateEditingManualField { updated } },
            onDismiss = viewModel::dismissManualFieldEditor,
            onSave = viewModel::saveEditingManualField,
        )
    }

    ConversionContent(
        state = state,
        onProfileClick = onProfileClick,
        onRetryCachedEmails = onRetryCachedEmails,
        onPreviewClick = viewModel::previewDocument,
        onConversionClick = {
            if (viewModel.generateDocuments()) {
                onStartConversion()
            }
        },
        onSelectXlsx = { launchFilePicker("xlsx", viewModel::selectXlsx) },
        onSelectTemplate = { launchFilePicker("docx", viewModel::setTemplatePath) },
        onFieldValueChange = viewModel::setManualFieldValue,
        onEditField = viewModel::openManualFieldEditor,
        onFeedbackUrlChange = viewModel::setFeedbackUrl,
    )
}

@Composable
internal fun ConversionContent(
    state: ConversionUiState,
    onProfileClick: () -> Unit,
    onRetryCachedEmails: () -> Unit,
    onPreviewClick: () -> Unit,
    onConversionClick: () -> Unit,
    onSelectXlsx: () -> Unit,
    onSelectTemplate: () -> Unit,
    onFieldValueChange: (String, String) -> Unit,
    onEditField: (String) -> Unit,
    onFeedbackUrlChange: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ConversionBottomBar(
                state = state,
                onPreviewClick = onPreviewClick,
                onConversionClick = onConversionClick,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            val hasXlsx = state.files.hasXlsx
            val hasTemplate = state.files.hasTemplate
            val xlsxTooltip = buildPathTooltip(Res.string.conversion_tooltip_xlsx, state.files.xlsxPath)
            val docxTooltip = buildPathTooltip(Res.string.conversion_tooltip_docx, state.files.templatePath)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MaxWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .padding(ContentPadding),
                verticalArrangement = Arrangement.spacedBy(ContentSpacing),
            ) {
                if (state.cachedEmailsCount > 0) {
                    CachedEmailsCard(
                        count = state.cachedEmailsCount,
                        enabled = state.canRetryCachedEmails,
                        supportsEmailSending = state.supportsEmailSending,
                        onClick = onRetryCachedEmails,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onProfileClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.cmm_logo),
                        contentDescription = null,
                        modifier = Modifier.size(Grid.x60),
                    )
                    Spacer(modifier = Modifier.width(Grid.x6))
                    Text(
                        text = stringResource(Res.string.conversion_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Grid.x5),
                ) {
                    SelectFileIcon(
                        icon = Res.drawable.xlsx,
                        label = stringResource(Res.string.common_file_xlsx),
                        state = selectFileIconState(hasXlsx, state.validation.xlsxError?.asString()),
                        fileName = state.files.xlsxFileName,
                        errorText = state.validation.xlsxError?.asString(),
                        errorTextModifier = Modifier.testTag("conversion-xlsx-error"),
                        tooltipText = xlsxTooltip,
                        onClick = onSelectXlsx,
                        enabled = state.supportsConversion,
                        modifier = Modifier.weight(1f),
                    )
                    SelectFileIcon(
                        icon = Res.drawable.docx,
                        label = stringResource(Res.string.common_file_docx),
                        state = selectFileIconState(hasTemplate, state.validation.templateError?.asString()),
                        fileName = state.files.templateFileName,
                        errorText = state.validation.templateError?.asString(),
                        errorTextModifier = Modifier.testTag("conversion-template-error"),
                        tooltipText = docxTooltip,
                        onClick = onSelectTemplate,
                        enabled = state.supportsConversion,
                        modifier = Modifier.weight(1f),
                    )
                }

                CertificateDetailsSection(
                    fields = state.manualFields,
                    onFieldValueChange = onFieldValueChange,
                    onEditField = onEditField,
                )

                EmailExtrasSection(
                    feedbackUrl = state.form.feedbackUrl,
                    enabled = state.supportsConversion,
                    onFeedbackUrlChange = onFeedbackUrlChange,
                )

                Spacer(modifier = Modifier.height(Grid.x6))
            }

            AppVerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Grid.x2),
            )
        }
    }
}

@Composable
private fun CertificateDetailsSection(
    fields: List<ConversionManualFieldUiState>,
    onFieldValueChange: (String, String) -> Unit,
    onEditField: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(Grid.x6),
        ) {
            Text(
                text = stringResource(Res.string.conversion_form_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            fields.forEach { field ->
                ManualFieldInput(
                    field = field,
                    onValueChange = { onFieldValueChange(field.tag, it) },
                    onEditDefinition = { onEditField(field.tag) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualFieldInput(
    field: ConversionManualFieldUiState,
    onValueChange: (String) -> Unit,
    onEditDefinition: () -> Unit,
) {
    val editAction = TextFieldTrailingAction(
        key = "edit-${field.tag}",
        icon = Lucide.Pencil,
        contentDescription = stringResource(Res.string.common_action_edit),
        onClick = onEditDefinition,
        enabled = true,
    )
    when (field.type) {
        CertificateFieldType.DATE -> ConfigDateField(
            field = field,
            onValueChange = onValueChange,
            onEditDefinition = onEditDefinition,
        )
        CertificateFieldType.SELECT -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (field.enabled) expanded = !expanded },
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ClearableOutlinedTextField(
                        value = field.value,
                        onValueChange = {},
                        label = { Text(fieldLabelText(field.tag, field.label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                        enabled = field.enabled,
                        isError = field.error != null,
                        tooltipText = field.tooltip?.asString(),
                        readOnly = true,
                        singleLine = true,
                        showClearIcon = false,
                        trailingIcon = null,
                        textStyle = MaterialTheme.typography.bodySmall,
                        onClear = {},
                        supportingText = supportingTextFor(field.error, field.helper),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = Grid.x4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onEditDefinition,
                            modifier = Modifier.size(Grid.x16),
                        ) {
                            Icon(
                                imageVector = Lucide.Pencil,
                                contentDescription = stringResource(Res.string.common_action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Grid.x8),
                            )
                        }
                    }
                }
                ExposedDropdownMenu(
                    expanded = field.enabled && expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    field.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        else -> {
            ClearableOutlinedTextField(
                value = field.value,
                onValueChange = onValueChange,
                label = { Text(fieldLabelText(field.tag, field.label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = field.enabled,
                isError = field.error != null,
                tooltipText = field.tooltip?.asString(),
                singleLine = field.type != CertificateFieldType.MULTILINE,
                keyboardOptions = when (field.type) {
                    CertificateFieldType.NUMBER -> KeyboardOptions(keyboardType = KeyboardType.Number)
                    else -> KeyboardOptions.Default
                },
                showClearIcon = false,
                trailingActions = listOf(editAction),
                textStyle = MaterialTheme.typography.bodySmall,
                supportingText = supportingTextFor(field.error, field.helper),
            )
        }
    }
}

@Composable
private fun ConfigDateField(
    field: ConversionManualFieldUiState,
    onValueChange: (String) -> Unit,
    onEditDefinition: () -> Unit,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    val parsedDate = parseCertificateDateInput(field.value)
    val displayText = parsedDate?.let(::formatCertificateDate)
        ?: stringResource(Res.string.conversion_certificate_date_not_selected)
    val displayColor = when {
        !field.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        field.error != null -> MaterialTheme.colorScheme.error
        parsedDate == null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    if (isDatePickerVisible) {
        CertificateDatePickerDialog(
            value = field.value,
            onDismissRequest = { isDatePickerVisible = false },
            onDateSelected = {
                onValueChange(it)
                isDatePickerVisible = false
            },
        )
    }

    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
            Surface(
                modifier = Modifier.fillMaxWidth().then(if (field.enabled) Modifier.clickable { isDatePickerVisible = true } else Modifier),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (field.enabled) 1f else 0.5f),
                border = BorderStroke(
                    Stroke.thin,
                    if (field.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Grid.x6, vertical = Grid.x5),
                    verticalArrangement = Arrangement.spacedBy(Grid.x2),
                ) {
                    Text(
                        text = fieldLabelText(field.tag, field.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = displayColor,
                        )
                        IconButton(
                            onClick = onEditDefinition,
                            modifier = Modifier.size(Grid.x16),
                        ) {
                            Icon(
                                imageVector = Lucide.Pencil,
                                contentDescription = stringResource(Res.string.common_action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Grid.x8),
                            )
                        }
                    }
                }
            }
            supportingTextFor(field.error, field.helper)?.invoke()
        }
    }

    if (field.tooltip == null) {
        content()
    } else {
        com.cmm.certificates.core.ui.TooltipWrapper(
            tooltipText = field.tooltip.asString(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateDatePickerDialog(
    value: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = certificateDateInputToUtcMillis(value))
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis ?: return@TextButton
                    onDateSelected(utcMillisToCertificateDateInput(selectedDateMillis))
                },
                enabled = datePickerState.selectedDateMillis != null,
            ) { Text(stringResource(Res.string.common_action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.common_action_cancel)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun EmailExtrasSection(
    feedbackUrl: String,
    enabled: Boolean,
    onFeedbackUrlChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            Text(
                text = stringResource(Res.string.conversion_email_extras_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ClearableOutlinedTextField(
                value = feedbackUrl,
                onValueChange = onFeedbackUrlChange,
                label = { Text(stringResource(Res.string.conversion_feedback_url_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                supportingText = { Text(stringResource(Res.string.conversion_feedback_url_hint)) },
            )
        }
    }
}

@Composable
private fun ConversionBottomBar(
    state: ConversionUiState,
    onPreviewClick: () -> Unit,
    onConversionClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Grid.x3,
        shadowElevation = Grid.x3,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ContentPadding, vertical = BottomBarPadding)) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Grid.x4)) {
                if (!state.supportsConversion) {
                    Text(
                        text = stringResource(Res.string.conversion_unsupported_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                } else if (state.validation.hasBlockingErrors) {
                    Text(
                        text = stringResource(Res.string.conversion_validation_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("conversion-validation-hint").fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                } else if (!state.isNetworkAvailable) {
                    Text(
                        text = stringResource(Res.string.conversion_offline_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
                    PrimaryActionButton(
                        text = stringResource(Res.string.conversion_preview_button),
                        onClick = onPreviewClick,
                        modifier = Modifier.weight(1f),
                        enabled = state.supportsConversion,
                        loading = state.isPreviewLoading,
                    )
                    PrimaryActionButton(
                        text = stringResource(Res.string.conversion_convert_button),
                        onClick = onConversionClick,
                        modifier = Modifier.weight(1f).testTag("conversion-convert-button"),
                        enabled = state.supportsConversion,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConversionBottomBarPreview() {
    AppTheme(darkTheme = false) {
        ConversionBottomBar(
            state = ConversionUiState(),
            onPreviewClick = {},
            onConversionClick = {},
        )
    }
}

@Composable
private fun buildPathTooltip(
    labelRes: StringResource,
    path: String,
): String = buildString {
    append(stringResource(labelRes))
    if (path.isNotBlank()) {
        append("\n")
        append(path)
    }
}

private fun selectFileIconState(
    selected: Boolean,
    errorText: String?,
): SelectFileIconState = when {
    !errorText.isNullOrBlank() -> SelectFileIconState.Error
    selected -> SelectFileIconState.Selected
    else -> SelectFileIconState.NotSelected
}

@Composable
private fun fieldLabelText(fieldTag: String, explicitLabel: String?): String {
    return explicitLabel?.trim().takeUnless { it.isNullOrBlank() } ?: fieldTag
}

private fun supportingTextFor(
    error: UiMessage?,
    helper: UiMessage?,
): (@Composable () -> Unit)? {
    if (error == null && helper == null) return null
    return {
        val color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        Text(text = (error ?: helper)?.asString().orEmpty(), color = color)
    }
}

@Composable
private fun CachedEmailsCard(
    count: Int,
    enabled: Boolean,
    supportsEmailSending: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(Stroke.thin, if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(Grid.x6), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
            Box(
                modifier = Modifier.size(Grid.x10).background(
                    if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.surface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.email_progress_retry_cached),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.email_progress_cached_status, count),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!enabled) {
                    Text(
                        text = if (!supportsEmailSending) stringResource(Res.string.email_sending_unsupported_hint) else stringResource(Res.string.email_progress_cached_retry_requirements_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
