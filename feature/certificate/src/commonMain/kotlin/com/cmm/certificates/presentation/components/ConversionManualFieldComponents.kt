package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_action_cancel
import certificates.composeapp.generated.resources.common_action_edit
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.conversion_certificate_date_not_selected
import certificates.composeapp.generated.resources.conversion_form_section_title
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.ui.TextFieldTrailingAction
import com.cmm.certificates.core.ui.TooltipWrapper
import com.cmm.certificates.domain.certificateDateInputToUtcMillis
import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.parseCertificateDateInput
import com.cmm.certificates.domain.utcMillisToCertificateDateInput
import com.cmm.certificates.presentation.ConversionManualFieldUiState
import com.cmm.certificates.presentation.conversionFieldLabel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import org.jetbrains.compose.resources.stringResource

private val CardPadding = Grid.x8

@Composable
internal fun CertificateDetailsSection(
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
    )

    when (field.type) {
        CertificateFieldType.DATE -> CertificateDateField(
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
                        label = { Text(conversionFieldLabel(field.tag, field.label)) },
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
                        supportingText = manualFieldSupportingText(field.error, field.helper),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = Grid.x4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditFieldIconButton(onClick = onEditDefinition)
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
                label = { Text(conversionFieldLabel(field.tag, field.label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = field.enabled,
                isError = field.error != null,
                tooltipText = field.tooltip?.asString(),
                singleLine = field.type != CertificateFieldType.MULTILINE,
                keyboardOptions = when (field.type) {
                    CertificateFieldType.NUMBER -> androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    )

                    else -> androidx.compose.foundation.text.KeyboardOptions.Default
                },
                showClearIcon = false,
                trailingActions = listOf(editAction),
                textStyle = MaterialTheme.typography.bodySmall,
                supportingText = manualFieldSupportingText(field.error, field.helper),
            )
        }
    }
}

@Composable
private fun CertificateDateField(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (field.enabled) Modifier.clickable {
                        isDatePickerVisible = true
                    } else Modifier),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (field.enabled) 1f else 0.5f),
                border = BorderStroke(
                    Stroke.thin,
                    if (field.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Grid.x6, vertical = Grid.x5),
                    verticalArrangement = Arrangement.spacedBy(Grid.x2),
                ) {
                    Text(
                        text = conversionFieldLabel(field.tag, field.label),
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
                        EditFieldIconButton(onClick = onEditDefinition)
                    }
                }
            }
            manualFieldSupportingText(field.error, field.helper)?.invoke()
        }
    }

    if (field.tooltip == null) {
        content()
    } else {
        TooltipWrapper(
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
private fun EditFieldIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
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

private fun manualFieldSupportingText(
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
