package com.cmm.certificates.configeditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AnimatedDialog
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.domain.config.CertificateFieldType

@Composable
fun ManualTagFieldDraftForm(
    draft: ManualTagFieldDraft,
    onChange: (ManualTagFieldDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Grid.x3),
    ) {
        ClearableOutlinedTextField(
            value = draft.tag,
            onValueChange = { onChange(draft.copy(tag = it)) },
            label = { Text("Žymė") },
            supportingText = { Text("DOCX šablone bus {{${draft.tag.ifBlank { "tag" }}}}") },
            modifier = Modifier.fillMaxWidth(),
        )
        ClearableOutlinedTextField(
            value = draft.label,
            onValueChange = { onChange(draft.copy(label = it)) },
            label = { Text("Pavadinimas") },
            supportingText = { Text("Jei tuščia, bus rodoma pati žymė") },
            modifier = Modifier.fillMaxWidth(),
        )
        CertificateFieldTypeDropdown(
            selected = draft.type,
            onSelect = { onChange(draft.copy(type = it)) },
        )
        ClearableOutlinedTextField(
            value = draft.defaultValue,
            onValueChange = { onChange(draft.copy(defaultValue = it)) },
            label = { Text("Numatytoji reikšmė") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.type == CertificateFieldType.SELECT) {
            ClearableOutlinedTextField(
                value = draft.optionsText,
                onValueChange = { onChange(draft.copy(optionsText = it)) },
                label = { Text("Pasirinkimai") },
                singleLine = false,
                minLines = 3,
                maxLines = 8,
                supportingText = { Text("Kiekviena eilutė - atskiras pasirinkimas") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateFieldTypeDropdown(
    selected: CertificateFieldType,
    onSelect: (CertificateFieldType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        ClearableOutlinedTextField(
            value = selected.name,
            onValueChange = {},
            label = { Text("Lauko tipas") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            showClearIcon = false,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CertificateFieldType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun ManualFieldConfigDialog(
    title: String,
    draft: ManualTagFieldDraft,
    message: String?,
    onDraftChange: (ManualTagFieldDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AnimatedDialog(onDismiss = onDismiss) { requestDismiss ->
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Grid.x3,
            shadowElevation = Grid.x3,
            border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(min = Grid.x240, max = Grid.x240),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Grid.x4),
                ) {
                    ManualTagFieldDraftForm(
                        draft = draft,
                        onChange = onDraftChange,
                    )
                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = requestDismiss) {
                        Text("Atšaukti")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
                        Button(onClick = onSave) {
                            Text("Išsaugoti")
                        }
                    }
                }
            }
        }
    }
}
