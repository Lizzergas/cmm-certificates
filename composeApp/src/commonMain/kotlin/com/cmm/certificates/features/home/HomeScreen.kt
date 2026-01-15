package com.cmm.certificates.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
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
import certificates.composeapp.generated.resources.home_output_directory_hint
import certificates.composeapp.generated.resources.home_output_directory_title
import certificates.composeapp.generated.resources.home_source_excel_hint
import certificates.composeapp.generated.resources.home_source_excel_title
import certificates.composeapp.generated.resources.home_subtitle
import certificates.composeapp.generated.resources.home_template_hint
import certificates.composeapp.generated.resources.home_template_title
import certificates.composeapp.generated.resources.home_title
import certificates.composeapp.generated.resources.home_validation_hint
import com.cmm.certificates.components.PrimaryActionButton
import com.cmm.certificates.components.SelectionCard
import com.cmm.certificates.components.SelectionCardState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val backgroundColor = Color(0xFFF8FAFC)

    MaterialTheme {
        Scaffold(
            containerColor = backgroundColor,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, backgroundColor),
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
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
                                        Color.White.copy(alpha = 0.6f),
                                        MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                        PrimaryActionButton(
                            text = stringResource(Res.string.home_convert_button),
                            onClick = { scope.launch { viewModel.generateDocuments() } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = actionEnabled,
                        )
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
                val cardHeight = if (maxWidth < 360.dp) 180.dp else 200.dp
                val hasXlsx = state.xlsxPath.isNotBlank()
                val hasTemplate = state.templatePath.isNotBlank()
                val hasOutput = state.outputDir.isNotBlank()
                val xlsxState = if (hasXlsx) {
                    SelectionCardState.Selected
                } else {
                    SelectionCardState.Idle
                }
                val templateState = if (hasTemplate) {
                    SelectionCardState.Selected
                } else {
                    SelectionCardState.Idle
                }
                val outputState = if (hasOutput) {
                    SelectionCardState.Selected
                } else {
                    SelectionCardState.Idle
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .align(Alignment.TopCenter)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier.clickable(onClick = onProfileClick),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.home_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.home_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SelectionCard(
                        title = stringResource(Res.string.home_source_excel_title),
                        subtitle = state.xlsxPath.ifBlank {
                            stringResource(Res.string.home_source_excel_hint)
                        },
                        badgeText = "XLSX",
                        minHeight = cardHeight,
                        state = xlsxState,
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker(
                                    mode = FileKitMode.Single,
                                    type = FileKitType.File(listOf("xlsx")),
                                )
                                viewModel.selectXlsx(file?.toString().orEmpty())
                            }
                        },
                    )

                    SelectionCard(
                        title = stringResource(Res.string.home_template_title),
                        subtitle = state.templatePath.ifBlank {
                            stringResource(Res.string.home_template_hint)
                        },
                        badgeText = "DOCX",
                        minHeight = cardHeight,
                        state = templateState,
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker(
                                    mode = FileKitMode.Single,
                                    type = FileKitType.File(listOf("docx")),
                                )
                                viewModel.setTemplatePath(file?.toString().orEmpty())
                            }
                        },
                    )

                    SelectionCard(
                        title = stringResource(Res.string.home_output_directory_title),
                        subtitle = state.outputDir.ifBlank {
                            stringResource(Res.string.home_output_directory_hint)
                        },
                        badgeText = "Folder",
                        minHeight = cardHeight,
                        state = outputState,
                        onClick = {
                            scope.launch {
                                val directory = FileKit.openDirectoryPicker()
                                viewModel.setOutputDir(directory?.toString().orEmpty())
                            }
                        },
                    )

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.home_form_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = accreditedId,
            onValueChange = onAccreditedIdChange,
            label = { Text(stringResource(Res.string.home_accredited_id_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = docIdStart,
            onValueChange = onDocIdStartChange,
            label = { Text(stringResource(Res.string.home_doc_id_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
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
        )
        OutlinedTextField(
            value = certificateName,
            onValueChange = onCertificateNameChange,
            label = { Text(stringResource(Res.string.home_certificate_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = lector,
            onValueChange = onLectorChange,
            label = { Text(stringResource(Res.string.home_lector_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}
