package com.cmm.certificates.feature.certificate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import certificates.composeapp.generated.resources.network_unavailable_message
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.ui.PrimaryActionButton
import com.cmm.certificates.core.ui.SelectFileIcon
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionScreen(
    onProfileClick: () -> Unit,
    onStartConversion: () -> Unit,
    viewModel: ConversionViewModel = koinViewModel<ConversionViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            ConversionBottomBar(
                state.isConversionEnabled,
                state.isNetworkAvailable,
                onConversionClick = {
                    onStartConversion()
                    scope.launch { viewModel.generateDocuments() }
                }
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            val hasXlsx = state.xlsxPath.isNotBlank()
            val hasTemplate = state.templatePath.isNotBlank()
            val xlsxTooltip = buildPathTooltip(
                Res.string.conversion_tooltip_xlsx,
                state.xlsxPath,
            )

            val docxTooltip = buildPathTooltip(
                Res.string.conversion_tooltip_docx,
                state.templatePath,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
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
                        modifier = Modifier.size(120.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SelectFileIcon(
                        iconText = "XLSX",
                        selected = hasXlsx,
                        tooltipText = xlsxTooltip,
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker(
                                    mode = FileKitMode.Single,
                                    type = FileKitType.File(listOf("xlsx")),
                                )
                                viewModel.selectXlsx(file?.toString().orEmpty())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SelectFileIcon(
                        iconText = "DOCX",
                        selected = hasTemplate,
                        tooltipText = docxTooltip,
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker(
                                    mode = FileKitMode.Single,
                                    type = FileKitType.File(listOf("docx")),
                                )
                                viewModel.setTemplatePath(file?.toString().orEmpty())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                CertificateDetailsSection(
                    accreditedId = state.accreditedId,
                    docIdStart = state.docIdStart,
                    accreditedType = state.accreditedType,
                    accreditedTypeOptions = state.accreditedTypeOptions,
                    accreditedHours = state.accreditedHours,
                    certificateName = state.certificateName,
                    lector = state.lector,
                    lectorGender = state.lectorGender,
                    onAccreditedIdChange = viewModel::setAccreditedId,
                    onDocIdStartChange = viewModel::setDocIdStart,
                    onAccreditedTypeChange = viewModel::setAccreditedType,
                    onAccreditedHoursChange = viewModel::setAccreditedHours,
                    onCertificateNameChange = viewModel::setCertificateName,
                    onLectorChange = viewModel::setLector,
                    onLectorGenderChange = viewModel::setLectorGender,
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateDetailsSection(
    accreditedId: String,
    docIdStart: String,
    accreditedType: String,
    accreditedTypeOptions: List<String>,
    accreditedHours: String,
    certificateName: String,
    lector: String,
    lectorGender: String,
    onAccreditedIdChange: (String) -> Unit,
    onDocIdStartChange: (String) -> Unit,
    onAccreditedTypeChange: (String) -> Unit,
    onAccreditedHoursChange: (String) -> Unit,
    onCertificateNameChange: (String) -> Unit,
    onLectorChange: (String) -> Unit,
    onLectorGenderChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.conversion_form_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClearableOutlinedTextField(
                    value = accreditedId,
                    onValueChange = onAccreditedIdChange,
                    label = { Text(stringResource(Res.string.conversion_accredited_id_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ClearableOutlinedTextField(
                    value = docIdStart,
                    onValueChange = onDocIdStartChange,
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
                val fillMaxWidth = Modifier
                    .fillMaxWidth()
                ClearableOutlinedTextField(
                    value = accreditedType,
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
                    onClear = { onAccreditedTypeChange("") },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    accreditedTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onAccreditedTypeChange(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
            ClearableOutlinedTextField(
                value = accreditedHours,
                onValueChange = onAccreditedHoursChange,
                label = { Text(stringResource(Res.string.conversion_accredited_hours_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ClearableOutlinedTextField(
                value = certificateName,
                onValueChange = onCertificateNameChange,
                label = { Text(stringResource(Res.string.conversion_certificate_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClearableOutlinedTextField(
                    value = lectorGender,
                    onValueChange = onLectorGenderChange,
                    label = { Text(stringResource(Res.string.conversion_lector_gender_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ClearableOutlinedTextField(
                    value = lector,
                    onValueChange = onLectorChange,
                    label = { Text(stringResource(Res.string.conversion_lector_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConversionBottomBar(
    isConversionEnabled: Boolean,
    isNetworkAvailable: Boolean,
    onConversionClick: () -> Unit,
) {
    Surface(
        color = Color.White,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isNetworkAvailable || !isConversionEnabled) {
                    val hintText = if (!isNetworkAvailable) {
                        stringResource(Res.string.network_unavailable_message)
                    } else {
                        stringResource(Res.string.conversion_validation_hint)
                    }
                    val hintColor = if (!isNetworkAvailable) {
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
                                Color(0xFFF8FAFC),
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                PrimaryActionButton(
                    text = stringResource(Res.string.conversion_convert_button),
                    onClick = onConversionClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConversionEnabled,
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
