package com.cmm.certificates.feature.certificate

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.cmm_logo
import certificates.composeapp.generated.resources.conversion_accredited_hours_label
import certificates.composeapp.generated.resources.conversion_accredited_id_label
import certificates.composeapp.generated.resources.conversion_accredited_type_label
import certificates.composeapp.generated.resources.conversion_certificate_name_label
import certificates.composeapp.generated.resources.conversion_convert_button
import certificates.composeapp.generated.resources.conversion_doc_id_label
import certificates.composeapp.generated.resources.conversion_form_section_title
import certificates.composeapp.generated.resources.conversion_lector_gender_label
import certificates.composeapp.generated.resources.conversion_lector_label
import certificates.composeapp.generated.resources.conversion_title
import certificates.composeapp.generated.resources.conversion_tooltip_docx
import certificates.composeapp.generated.resources.conversion_tooltip_xlsx
import certificates.composeapp.generated.resources.conversion_validation_hint
import certificates.composeapp.generated.resources.email_progress_cached_status
import certificates.composeapp.generated.resources.email_progress_retry_cached
import certificates.composeapp.generated.resources.network_unavailable_message
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AppVerticalScrollbar
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.ui.PrimaryActionButton
import com.cmm.certificates.core.ui.SelectFileIcon
import com.cmm.certificates.core.ui.rememberFilePickerLauncher
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
                onConversionClick = {
                    onStartConversion()
                    viewModel.generateDocuments()
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
                        onClick = onRetryCachedEmails
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
                        iconText = "XLSX",
                        selected = hasXlsx,
                        tooltipText = xlsxTooltip,
                        onClick = { launchFilePicker("xlsx", viewModel::selectXlsx) },
                        modifier = Modifier.weight(1f),
                    )
                    SelectFileIcon(
                        iconText = "DOCX",
                        selected = hasTemplate,
                        tooltipText = docxTooltip,
                        onClick = { launchFilePicker("docx", viewModel::setTemplatePath) },
                        modifier = Modifier.weight(1f),
                    )
                }

                CertificateDetailsSection(
                    form = state.form,
                    accreditedTypeOptions = state.accreditedTypeOptions,
                    actions = ConversionFormActions(
                        onAccreditedIdChange = viewModel::setAccreditedId,
                        onDocIdStartChange = viewModel::setDocIdStart,
                        onAccreditedTypeChange = viewModel::setAccreditedType,
                        onAccreditedHoursChange = viewModel::setAccreditedHours,
                        onCertificateNameChange = viewModel::setCertificateName,
                        onLectorChange = viewModel::setLector,
                        onLectorGenderChange = viewModel::setLectorGender,
                    ),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateDetailsSection(
    form: ConversionFormState,
    accreditedTypeOptions: List<String>,
    actions: ConversionFormActions,
) {
    var expanded by remember { mutableStateOf(false) }

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
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ClearableOutlinedTextField(
                    value = form.docIdStart,
                    onValueChange = actions.onDocIdStartChange,
                    label = { Text(stringResource(Res.string.conversion_doc_id_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
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
                    readOnly = true,
                    singleLine = true,
                    showClearIcon = false,
                    textStyle = MaterialTheme.typography.bodySmall,
                    onClear = { actions.onAccreditedTypeChange("") },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
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
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ClearableOutlinedTextField(
                value = form.certificateName,
                onValueChange = actions.onCertificateNameChange,
                label = { Text(stringResource(Res.string.conversion_certificate_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
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
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ClearableOutlinedTextField(
                    value = form.lector,
                    onValueChange = actions.onLectorChange,
                    label = { Text(stringResource(Res.string.conversion_lector_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private data class ConversionFormActions(
    val onAccreditedIdChange: (String) -> Unit,
    val onDocIdStartChange: (String) -> Unit,
    val onAccreditedTypeChange: (String) -> Unit,
    val onAccreditedHoursChange: (String) -> Unit,
    val onCertificateNameChange: (String) -> Unit,
    val onLectorChange: (String) -> Unit,
    val onLectorGenderChange: (String) -> Unit,
)

@Composable
private fun ConversionBottomBar(
    state: ConversionUiState,
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
                if (!state.isNetworkAvailable || !state.isConversionEnabled) {
                    val hintText = if (!state.isNetworkAvailable) {
                        stringResource(Res.string.network_unavailable_message)
                    } else {
                        stringResource(Res.string.conversion_validation_hint)
                    }
                    val hintColor = if (!state.isNetworkAvailable) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.labelSmall,
                        color = hintColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = Grid.x6, vertical = Grid.x3),
                        textAlign = TextAlign.Center,
                    )
                }
                PrimaryActionButton(
                    text = stringResource(Res.string.conversion_convert_button),
                    onClick = onConversionClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isConversionEnabled,
                )
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

@Composable
private fun CachedEmailsCard(
    count: Int,
    onClick: () -> Unit,
) {
    val enabled = count > 0
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
            }
        }
    }
}
