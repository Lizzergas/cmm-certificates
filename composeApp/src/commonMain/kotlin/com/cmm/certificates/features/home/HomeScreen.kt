package com.cmm.certificates.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.cmm_logo
import certificates.composeapp.generated.resources.home_accredited_hours_label
import certificates.composeapp.generated.resources.home_accredited_id_label
import certificates.composeapp.generated.resources.home_accredited_type_conference
import certificates.composeapp.generated.resources.home_accredited_type_label
import certificates.composeapp.generated.resources.home_accredited_type_lecture
import certificates.composeapp.generated.resources.home_accredited_type_seminar
import certificates.composeapp.generated.resources.home_accredited_type_training
import certificates.composeapp.generated.resources.home_certificate_name_label
import certificates.composeapp.generated.resources.home_convert_button
import certificates.composeapp.generated.resources.home_doc_id_label
import certificates.composeapp.generated.resources.home_form_section_title
import certificates.composeapp.generated.resources.home_lector_label
import certificates.composeapp.generated.resources.home_title
import certificates.composeapp.generated.resources.home_tooltip_docx
import certificates.composeapp.generated.resources.home_tooltip_output
import certificates.composeapp.generated.resources.home_tooltip_xlsx
import certificates.composeapp.generated.resources.home_validation_hint
import com.cmm.certificates.components.FileIcon
import com.cmm.certificates.components.PrimaryActionButton
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onStartConversion: () -> Unit,
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val backgroundColor = Color(0xFFF8FAFC)

    MaterialTheme {
        Scaffold(
            containerColor = backgroundColor,
            bottomBar = {
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
                        val hasXlsx = state.xlsxPath.isNotBlank()
                        val hasTemplate = state.templatePath.isNotBlank()
                        val hasOutput = state.outputDir.isNotBlank()
                        val hasAccreditedId = state.accreditedId.isNotBlank()
                        val hasDocIdStart = state.docIdStart.isNotBlank()
                        val hasAccreditedHours = state.accreditedHours.isNotBlank()
                        val hasCertificateName = state.certificateName.isNotBlank()
                        val hasLector = state.lector.isNotBlank()
                        val actionEnabled = hasXlsx &&
                                hasTemplate &&
                                hasOutput &&
                                hasAccreditedId &&
                                hasDocIdStart &&
                                hasAccreditedHours &&
                                hasCertificateName &&
                                hasLector &&
                                state.entries.isNotEmpty()

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!actionEnabled) {
                                Text(
                                    text = stringResource(Res.string.home_validation_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                text = stringResource(Res.string.home_convert_button),
                                onClick = {
                                    onStartConversion()
                                    scope.launch { viewModel.generateDocuments() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = actionEnabled,
                            )
                        }
                    }
                }
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
                val hasOutput = state.outputDir.isNotBlank()
                val xlsxTooltip = buildString {
                    append(stringResource(Res.string.home_tooltip_xlsx))
                    if (state.xlsxPath.isNotBlank()) {
                        append("\n")
                        append(state.xlsxPath)
                    }
                }
                val docxTooltip = buildString {
                    append(stringResource(Res.string.home_tooltip_docx))
                    if (state.templatePath.isNotBlank()) {
                        append("\n")
                        append(state.templatePath)
                    }
                }
                val outputTooltip = buildString {
                    append(stringResource(Res.string.home_tooltip_output))
                    if (state.outputDir.isNotBlank()) {
                        append("\n")
                        append(state.outputDir)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .align(Alignment.TopCenter)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            text = stringResource(Res.string.home_title),
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
                        FileIcon(
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
                        FileIcon(
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
                        FileIcon(
                            iconText = "DIR",
                            selected = hasOutput,
                            tooltipText = outputTooltip,
                            onClick = {
                                scope.launch {
                                    val directory = FileKit.openDirectoryPicker()
                                    viewModel.setOutputDir(directory?.toString().orEmpty())
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    CertificateDetailsSection(
                        accreditedId = state.accreditedId,
                        docIdStart = state.docIdStart,
                        accreditedType = state.accreditedType,
                        accreditedHours = state.accreditedHours,
                        certificateName = state.certificateName,
                        lector = state.lector,
                        onAccreditedIdChange = viewModel::setAccreditedId,
                        onDocIdStartChange = viewModel::setDocIdStart,
                        onAccreditedTypeChange = viewModel::setAccreditedType,
                        onAccreditedHoursChange = viewModel::setAccreditedHours,
                        onCertificateNameChange = viewModel::setCertificateName,
                        onLectorChange = viewModel::setLector,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateDetailsSection(
    accreditedId: String,
    docIdStart: String,
    accreditedType: String,
    accreditedHours: String,
    certificateName: String,
    lector: String,
    onAccreditedIdChange: (String) -> Unit,
    onDocIdStartChange: (String) -> Unit,
    onAccreditedTypeChange: (String) -> Unit,
    onAccreditedHoursChange: (String) -> Unit,
    onCertificateNameChange: (String) -> Unit,
    onLectorChange: (String) -> Unit,
) {
    val options = listOf(
        stringResource(Res.string.home_accredited_type_lecture),
        stringResource(Res.string.home_accredited_type_seminar),
        stringResource(Res.string.home_accredited_type_conference),
        stringResource(Res.string.home_accredited_type_training),
    )
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
                text = stringResource(Res.string.home_form_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = accreditedId,
                    onValueChange = onAccreditedIdChange,
                    label = { Text(stringResource(Res.string.home_accredited_id_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = docIdStart,
                    onValueChange = onDocIdStartChange,
                    label = { Text(stringResource(Res.string.home_doc_id_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = accreditedType,
                    onValueChange = {},
                    label = { Text(stringResource(Res.string.home_accredited_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
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
            OutlinedTextField(
                value = accreditedHours,
                onValueChange = onAccreditedHoursChange,
                label = { Text(stringResource(Res.string.home_accredited_hours_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = certificateName,
                onValueChange = onCertificateNameChange,
                label = { Text(stringResource(Res.string.home_certificate_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = lector,
                onValueChange = onLectorChange,
                label = { Text(stringResource(Res.string.home_lector_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
