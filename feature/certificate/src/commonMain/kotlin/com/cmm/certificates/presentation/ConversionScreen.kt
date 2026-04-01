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
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.cmm_logo
import certificates.composeapp.generated.resources.common_action_cancel
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.common_file_docx
import certificates.composeapp.generated.resources.common_file_xlsx
import certificates.composeapp.generated.resources.conversion_accredited_hours_label
import certificates.composeapp.generated.resources.conversion_accredited_id_label
import certificates.composeapp.generated.resources.conversion_accredited_type_label
import certificates.composeapp.generated.resources.conversion_certificate_date_label
import certificates.composeapp.generated.resources.conversion_certificate_date_not_selected
import certificates.composeapp.generated.resources.conversion_certificate_name_label
import certificates.composeapp.generated.resources.conversion_convert_button
import certificates.composeapp.generated.resources.conversion_doc_id_label
import certificates.composeapp.generated.resources.conversion_email_extras_section_title
import certificates.composeapp.generated.resources.conversion_feedback_url_hint
import certificates.composeapp.generated.resources.conversion_feedback_url_label
import certificates.composeapp.generated.resources.conversion_form_section_title
import certificates.composeapp.generated.resources.conversion_lector_gender_label
import certificates.composeapp.generated.resources.conversion_lector_label
import certificates.composeapp.generated.resources.conversion_offline_hint
import certificates.composeapp.generated.resources.conversion_pick_date_button
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
import com.cmm.certificates.core.ui.rememberFilePickerLauncher
import com.cmm.certificates.domain.certificateDateInputToUtcMillis
import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.parseCertificateDateInput
import com.cmm.certificates.domain.utcMillisToCertificateDateInput
import com.cmm.certificates.feature.certificate.domain.model.RegistrationEntry
import com.cmm.certificates.presentation.components.PrimaryActionButton
import com.cmm.certificates.presentation.components.SelectFileIcon
import com.cmm.certificates.presentation.components.SelectFileIconState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MaxWidth = Grid.x240
private val ContentPadding = Grid.x8
private val ContentSpacing = Grid.x10
private val CardPadding = Grid.x8
private val BottomBarPadding = Grid.x6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionScreen(
    onProfileClick: () -> Unit,
    onStartConversion: () -> Unit,
    onRetryCachedEmails: () -> Unit,
    viewModel: ConversionViewModel = koinViewModel<ConversionViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchFilePicker = rememberFilePickerLauncher()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ConversionBottomBar(
                state = state,
                onPreviewClick = viewModel::previewDocument,
                onConversionClick = {
                    if (viewModel.generateDocuments()) {
                        onStartConversion()
                    }
                }
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
            val xlsxTooltip = buildPathTooltip(
                Res.string.conversion_tooltip_xlsx,
                state.files.xlsxPath,
            )

            val docxTooltip = buildPathTooltip(
                Res.string.conversion_tooltip_docx,
                state.files.templatePath,
            )

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onProfileClick),
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
                        state = selectFileIconState(
                            hasXlsx,
                            state.validation.xlsxError?.asString()
                        ),
                        fileName = state.files.xlsxFileName,
                        errorText = state.validation.xlsxError?.asString(),
                        tooltipText = xlsxTooltip,
                        onClick = { launchFilePicker("xlsx", viewModel::selectXlsx) },
                        enabled = state.supportsConversion,
                        modifier = Modifier.weight(1f),
                    )
                    SelectFileIcon(
                        icon = Res.drawable.docx,
                        label = stringResource(Res.string.common_file_docx),
                        state = selectFileIconState(
                            hasTemplate,
                            state.validation.templateError?.asString()
                        ),
                        fileName = state.files.templateFileName,
                        errorText = state.validation.templateError?.asString(),
                        tooltipText = docxTooltip,
                        onClick = { launchFilePicker("docx", viewModel::setTemplatePath) },
                        enabled = state.supportsConversion,
                        modifier = Modifier.weight(1f),
                    )
                }

                CertificateDetailsSection(
                    form = state.form,
                    templateSupport = state.templateSupport,
                    validation = state.validation,
                    isTemplateInspectionInProgress = state.files.isTemplateInspectionInProgress,
                    accreditedTypeOptions = state.accreditedTypeOptions,
                    enabled = state.supportsConversion,
                    actions = ConversionFormActions(
                        onCertificateDateChange = viewModel::setCertificateDate,
                        onAccreditedIdChange = viewModel::setAccreditedId,
                        onDocIdStartChange = viewModel::setDocIdStart,
                        onAccreditedTypeChange = viewModel::setAccreditedType,
                        onAccreditedHoursChange = viewModel::setAccreditedHours,
                        onCertificateNameChange = viewModel::setCertificateName,
                        onLectorChange = viewModel::setLector,
                        onLectorGenderChange = viewModel::setLectorGender,
                    ),
                )

                EmailExtrasSection(
                    feedbackUrl = state.form.feedbackUrl,
                    enabled = state.supportsConversion,
                    onFeedbackUrlChange = viewModel::setFeedbackUrl,
                )

                Spacer(modifier = Modifier.height(Grid.x6))
            }
            AppVerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = Grid.x2),
            )
        }
    }
}

@Preview
@Composable
private fun ConversionBottomBarPreview() {
    AppTheme(darkTheme = false) {
        ConversionBottomBar(
            state = ConversionUiState(
                files = ConversionFilesState(
                    xlsxPath = "participants.xlsx",
                    templatePath = "template.docx"
                ),
                form = ConversionFormState(
                    certificateDate = "2026-03-26",
                    docIdStart = "100",
                    accreditedHours = "4",
                    certificateName = "Certificate",
                    lector = "Lecturer",
                ),
                entries = listOf(
                    RegistrationEntry(
                        primaryEmail = "preview@example.com",
                        name = "Ada",
                        surname = "Lovelace",
                        institution = "CMM",
                        forEvent = "Workshop",
                        publicityApproval = "yes",
                    )
                ),
            ),
            onPreviewClick = {},
            onConversionClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateDetailsSection(
    form: ConversionFormState,
    templateSupport: ConversionTemplateSupportState,
    validation: ConversionValidationState,
    isTemplateInspectionInProgress: Boolean,
    accreditedTypeOptions: List<String>,
    enabled: Boolean,
    actions: ConversionFormActions,
) {
    var expanded by remember { mutableStateOf(false) }
    val inputsEnabled = enabled && !isTemplateInspectionInProgress
    var isDatePickerVisible by remember { mutableStateOf(false) }
    val certificateDateEnabled = inputsEnabled && templateSupport.certificateDate.isEnabled

    if (isDatePickerVisible) {
        CertificateDatePickerDialog(
            value = form.certificateDate,
            onDismissRequest = { isDatePickerVisible = false },
            onDateSelected = {
                actions.onCertificateDateChange(it)
                isDatePickerVisible = false
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(Grid.x6),
        ) {
            Text(
                text = stringResource(Res.string.conversion_form_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Grid.x5),
            ) {
                ClearableOutlinedTextField(
                    value = form.accreditedId,
                    onValueChange = actions.onAccreditedIdChange,
                    label = { Text(stringResource(Res.string.conversion_accredited_id_label)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled && templateSupport.accreditedId.isEnabled,
                    isError = validation.accreditedIdError != null,
                    tooltipText = templateSupport.accreditedId.disabledTooltip?.asString(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    supportingText = supportingTextFor(
                        error = validation.accreditedIdError,
                        helper = templateSupport.accreditedId.disabledSupportingText,
                    ),
                )
                ClearableOutlinedTextField(
                    value = form.docIdStart,
                    onValueChange = actions.onDocIdStartChange,
                    label = { Text(stringResource(Res.string.conversion_doc_id_label)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled && templateSupport.docIdStart.isEnabled,
                    isError = validation.docIdStartError != null,
                    tooltipText = templateSupport.docIdStart.disabledTooltip?.asString(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodySmall,
                    supportingText = supportingTextFor(
                        error = validation.docIdStartError,
                        helper = templateSupport.docIdStart.disabledSupportingText,
                    ),
                )
            }
            val accreditedTypeEnabled = enabled && templateSupport.accreditedType.isEnabled
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (accreditedTypeEnabled) expanded = !expanded },
            ) {
                val fillMaxWidth = Modifier.fillMaxWidth()
                ClearableOutlinedTextField(
                    value = form.accreditedType,
                    onValueChange = {},
                    label = { Text(stringResource(Res.string.conversion_accredited_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = fillMaxWidth.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        true
                    ),
                    enabled = accreditedTypeEnabled,
                    isError = validation.accreditedTypeError != null,
                    tooltipText = templateSupport.accreditedType.disabledTooltip?.asString(),
                    readOnly = true,
                    singleLine = true,
                    showClearIcon = false,
                    textStyle = MaterialTheme.typography.bodySmall,
                    onClear = { actions.onAccreditedTypeChange("") },
                    supportingText = supportingTextFor(
                        error = validation.accreditedTypeError,
                        helper = templateSupport.accreditedType.disabledSupportingText,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = accreditedTypeEnabled && expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    accreditedTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                actions.onAccreditedTypeChange(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
            ClearableOutlinedTextField(
                value = form.accreditedHours,
                onValueChange = actions.onAccreditedHoursChange,
                label = { Text(stringResource(Res.string.conversion_accredited_hours_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && templateSupport.accreditedHours.isEnabled,
                isError = validation.accreditedHoursError != null,
                tooltipText = templateSupport.accreditedHours.disabledTooltip?.asString(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = supportingTextFor(
                    error = validation.accreditedHoursError,
                    helper = templateSupport.accreditedHours.disabledSupportingText,
                ),
            )
            ClearableOutlinedTextField(
                value = form.certificateName,
                onValueChange = actions.onCertificateNameChange,
                label = { Text(stringResource(Res.string.conversion_certificate_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && templateSupport.certificateName.isEnabled,
                isError = validation.certificateNameError != null,
                tooltipText = templateSupport.certificateName.disabledTooltip?.asString(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                supportingText = supportingTextFor(
                    error = validation.certificateNameError,
                    helper = templateSupport.certificateName.disabledSupportingText,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Grid.x5),
            ) {
                ClearableOutlinedTextField(
                    value = form.lectorGender,
                    onValueChange = actions.onLectorGenderChange,
                    label = { Text(stringResource(Res.string.conversion_lector_gender_label)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled && templateSupport.lectorGender.isEnabled,
                    isError = validation.lectorGenderError != null,
                    tooltipText = templateSupport.lectorGender.disabledTooltip?.asString(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    supportingText = supportingTextFor(
                        error = validation.lectorGenderError,
                        helper = templateSupport.lectorGender.disabledSupportingText,
                    ),
                )
                ClearableOutlinedTextField(
                    value = form.lector,
                    onValueChange = actions.onLectorChange,
                    label = { Text(stringResource(Res.string.conversion_lector_label)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled && templateSupport.lector.isEnabled,
                    isError = validation.lectorError != null,
                    tooltipText = templateSupport.lector.disabledTooltip?.asString(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    supportingText = supportingTextFor(
                        error = validation.lectorError,
                        helper = templateSupport.lector.disabledSupportingText,
                    ),
                )
            }
            CertificateDateField(
                value = form.certificateDate,
                onOpenPicker = { isDatePickerVisible = true },
                enabled = certificateDateEnabled,
                isError = validation.certificateDateError != null,
                tooltipText = templateSupport.certificateDate.disabledTooltip?.asString(),
                supportingText = certificateDateSupportingText(
                    error = validation.certificateDateError,
                    helper = templateSupport.certificateDate.disabledSupportingText,
                ),
            )
        }
    }
}

private data class ConversionFormActions(
    val onCertificateDateChange: (String) -> Unit,
    val onAccreditedIdChange: (String) -> Unit,
    val onDocIdStartChange: (String) -> Unit,
    val onAccreditedTypeChange: (String) -> Unit,
    val onAccreditedHoursChange: (String) -> Unit,
    val onCertificateNameChange: (String) -> Unit,
    val onLectorChange: (String) -> Unit,
    val onLectorGenderChange: (String) -> Unit,
)

@Composable
private fun CertificateDateField(
    value: String,
    onOpenPicker: () -> Unit,
    enabled: Boolean,
    isError: Boolean,
    tooltipText: String?,
    supportingText: (@Composable () -> Unit)?,
) {
    val parsedDate = parseCertificateDateInput(value)
    val displayText = parsedDate?.let(::formatCertificateDate)
        ?: stringResource(Res.string.conversion_certificate_date_not_selected)
    val displayColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isError -> MaterialTheme.colorScheme.error
        parsedDate == null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.clickable(onClick = onOpenPicker) else Modifier),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                border = BorderStroke(
                    Stroke.thin,
                    if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Grid.x6, vertical = Grid.x5),
                    verticalArrangement = Arrangement.spacedBy(Grid.x2),
                ) {
                    Text(
                        text = stringResource(Res.string.conversion_certificate_date_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleMedium,
                        color = displayColor,
                    )
                }
            }
            supportingText?.invoke()
        }
    }

    if (tooltipText.isNullOrBlank()) {
        content()
    } else {
        com.cmm.certificates.core.ui.TooltipWrapper(
            tooltipText = tooltipText,
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = certificateDateInputToUtcMillis(value),
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis ?: return@TextButton
                    onDateSelected(utcMillisToCertificateDateInput(selectedDateMillis))
                },
                enabled = datePickerState.selectedDateMillis != null,
            ) {
                Text(stringResource(Res.string.common_action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.common_action_cancel))
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
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
                supportingText = {
                    Text(stringResource(Res.string.conversion_feedback_url_hint))
                },
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding, vertical = BottomBarPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                if (!state.supportsConversion) {
                    Text(
                        text = stringResource(Res.string.conversion_unsupported_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                } else if (state.validation.hasBlockingErrors) {
                    Text(
                        text = stringResource(Res.string.conversion_validation_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                } else if (!state.isNetworkAvailable) {
                    Text(
                        text = stringResource(Res.string.conversion_offline_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Grid.x4),
                ) {
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
                        modifier = Modifier.weight(1f),
                        enabled = state.supportsConversion,
                    )
                }
            }
        }
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
): SelectFileIconState {
    return when {
        !errorText.isNullOrBlank() -> SelectFileIconState.Error
        selected -> SelectFileIconState.Selected
        else -> SelectFileIconState.NotSelected
    }
}

private fun supportingTextFor(
    error: UiMessage?,
    helper: UiMessage?,
): (@Composable () -> Unit)? {
    if (error == null && helper == null) return null
    return {
        val color = if (error != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = (error ?: helper)?.asString().orEmpty(),
            color = color,
        )
    }
}

@Composable
private fun certificateDateSupportingText(
    error: UiMessage?,
    helper: UiMessage?,
): (@Composable () -> Unit)? {
    if (error == null && helper == null) return null
    return {
        val resolved = error ?: helper
        val color = when {
            error != null -> MaterialTheme.colorScheme.error
            helper != null -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = resolved?.asString().orEmpty(),
            color = color,
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ),
        contentColor = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.5f
        ),
        border = BorderStroke(
            Stroke.thin,
            if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Row(
            modifier = Modifier.padding(Grid.x6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Grid.x4)
        ) {
            Box(
                modifier = Modifier
                    .size(Grid.x10)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.surface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.email_progress_retry_cached),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.email_progress_cached_status, count),
                    style = MaterialTheme.typography.bodySmall
                )
                if (!enabled) {
                    Text(
                        text = if (!supportsEmailSending) {
                            stringResource(Res.string.email_sending_unsupported_hint)
                        } else {
                            stringResource(Res.string.email_progress_cached_retry_requirements_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
