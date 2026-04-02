package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_back
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AppVerticalScrollbar
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.ui.rememberFilePickerLauncher
import com.cmm.certificates.presentation.components.ManualTagFieldDraftForm
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MaxWidth = Grid.x240
private val PaddingHorizontal = Grid.x8
private val PaddingVertical = Grid.x6

@Composable
fun CertificateConfigScreen(
    onBack: () -> Unit,
    viewModel: CertificateConfigViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchFilePicker = rememberFilePickerLauncher()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConfigTopBar(
                onBack = onBack,
                onSave = { viewModel.save() },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .widthIn(max = MaxWidth)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(Grid.x8),
            ) {
                ConfigCard(title = "Sertifikato konfigūracija") {
                    Text("DOCX šablone naudokite žymes formatu {{tag}}.")
                    state.externalPath?.let { path ->
                        Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.loadFailureMessage?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.message?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                ConfigCard(title = "XLSX žymės") {
                    OutlinedButton(
                        onClick = { launchFilePicker("xlsx", viewModel::setSampleXlsxPath) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.sampleXlsxPath.isBlank()) "Pasirinkti pavyzdinį XLSX" else state.sampleXlsxPath)
                    }
                    state.xlsxFields.forEachIndexed { index, field ->
                        XlsxFieldEditor(
                            field = field,
                            headers = state.sampleHeaders,
                            onChange = { updated -> viewModel.updateXlsxField(index) { updated } },
                            onRemove = { viewModel.removeXlsxField(index) },
                        )
                    }
                    OutlinedButton(onClick = viewModel::addXlsxField, modifier = Modifier.fillMaxWidth()) {
                        Text("Pridėti XLSX žymę")
                    }
                }

                ConfigCard(title = "Įvedami laukai") {
                    state.manualFields.forEachIndexed { index, field ->
                        ManualFieldEditor(
                            field = field,
                            onChange = { updated -> viewModel.updateManualField(index) { updated } },
                            onRemove = { viewModel.removeManualField(index) },
                        )
                    }
                    OutlinedButton(onClick = viewModel::addManualField, modifier = Modifier.fillMaxWidth()) {
                        Text("Pridėti įvedamą lauką")
                    }
                }

                ConfigCard(title = "Dokumento numerio žymė") {
                    TagDropdown(
                        label = "Dokumento numerio žymė",
                        value = state.documentNumberTag,
                        options = state.manualFields.map { it.tag }.filter { it.isNotBlank() },
                        onSelect = viewModel::setDocumentNumberTag,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Grid.x4),
                ) {
                    OutlinedButton(
                        onClick = viewModel::resetToDefault,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Atstatyti numatytąją")
                    }
                    OutlinedButton(
                        onClick = { viewModel.save() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Išsaugoti")
                    }
                }
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

@Composable
private fun ConfigTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingHorizontal, vertical = Grid.x4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Grid.x1)) {
            Text("Sertifikato konfigūracija", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Sukurkite žymes XLSX ir DOCX šablonams.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Grid.x2)) {
            TextButton(onClick = onBack) {
                Text(stringResource(Res.string.settings_back))
            }
            TextButton(onClick = onSave) {
                Text("Išsaugoti")
            }
        }
    }
}

@Composable
private fun ConfigCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Grid.x8),
            verticalArrangement = Arrangement.spacedBy(Grid.x4),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XlsxFieldEditor(
    field: XlsxTagFieldDraft,
    headers: List<String>,
    onChange: (XlsxTagFieldDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
        ClearableOutlinedTextField(
            value = field.tag,
            onValueChange = { onChange(field.copy(tag = it)) },
            label = { Text("Žymė") },
            supportingText = { Text("DOCX šablone bus {{${field.tag.ifBlank { "tag" }}}}") },
            modifier = Modifier.fillMaxWidth(),
        )
        ClearableOutlinedTextField(
            value = field.label,
            onValueChange = { onChange(field.copy(label = it)) },
            label = { Text("Pavadinimas") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (headers.isEmpty()) {
            ClearableOutlinedTextField(
                value = field.headerName,
                onValueChange = { onChange(field.copy(headerName = it)) },
                label = { Text("XLSX antraštė") },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TagDropdown(
                label = "XLSX antraštė",
                value = field.headerName,
                options = headers,
                onSelect = { onChange(field.copy(headerName = it)) },
            )
        }
        TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
            Text("Pašalinti")
        }
    }
}

@Composable
private fun ManualFieldEditor(
    field: ManualTagFieldDraft,
    onChange: (ManualTagFieldDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
        ManualTagFieldDraftForm(
            draft = field,
            onChange = onChange,
        )
        TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
            Text("Pašalinti")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        ClearableOutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            showClearIcon = false,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
