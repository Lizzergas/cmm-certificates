package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_action_cancel
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.conversion_certificate_date_not_selected
import com.cmm.certificates.core.presentation.asString
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.TooltipWrapper
import com.cmm.certificates.domain.certificateDateInputToUtcMillis
import com.cmm.certificates.domain.formatCertificateDate
import com.cmm.certificates.domain.parseCertificateDateInput
import com.cmm.certificates.domain.utcMillisToCertificateDateInput
import com.cmm.certificates.presentation.ConversionManualFieldUiState
import com.cmm.certificates.presentation.conversionFieldLabel
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Composable
internal fun CertificateDateField(
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
                    .then(if (field.enabled) Modifier.clickable { isDatePickerVisible = true } else Modifier),
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
        DoubleClickConfirmingDatePicker(
            state = datePickerState,
            onDateConfirmed = { selectedDateMillis ->
                onDateSelected(utcMillisToCertificateDateInput(selectedDateMillis))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoubleClickConfirmingDatePicker(
    state: androidx.compose.material3.DatePickerState,
    onDateConfirmed: (Long) -> Unit,
) {
    var hasObservedInitialSelection by remember { mutableStateOf(false) }
    var lastSelectionChangeMark by remember { mutableStateOf<TimeMark?>(null) }

    LaunchedEffect(state.selectedDateMillis) {
        if (!hasObservedInitialSelection) {
            hasObservedInitialSelection = true
            return@LaunchedEffect
        }
        if (state.selectedDateMillis != null) {
            lastSelectionChangeMark = TimeSource.Monotonic.markNow()
        }
    }

    DatePicker(
        state = state,
        modifier = Modifier.nonConsumingDoubleTap {
            val selectedDateMillis = state.selectedDateMillis ?: return@nonConsumingDoubleTap
            val lastSelectionChange = lastSelectionChangeMark ?: return@nonConsumingDoubleTap
            if (lastSelectionChange.elapsedNow() <= DoubleTapConfirmWindow) {
                onDateConfirmed(selectedDateMillis)
            }
        },
    )
}

private fun Modifier.nonConsumingDoubleTap(onDoubleTap: () -> Unit): Modifier {
    return pointerInput(onDoubleTap) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val firstUp = waitForUpIgnoringConsumption(firstDown.id) ?: return@awaitEachGesture
            val secondDown = waitForSecondDownIgnoringConsumption(firstUp.uptimeMillis)
                ?: return@awaitEachGesture
            val secondUp = waitForUpIgnoringConsumption(secondDown.id) ?: return@awaitEachGesture
            if (secondUp.uptimeMillis - firstUp.uptimeMillis <= viewConfiguration.doubleTapTimeoutMillis) {
                onDoubleTap()
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.waitForUpIgnoringConsumption(
    pointerId: PointerId,
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        if (change.changedToUpIgnoreConsumed()) return change
        if (!change.pressed) return null
    }
}

private suspend fun AwaitPointerEventScope.waitForSecondDownIgnoringConsumption(
    firstUpUptimeMillis: Long,
): PointerInputChange? {
    val minDownUptimeMillis = firstUpUptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    val maxDownUptimeMillis = firstUpUptimeMillis + viewConfiguration.doubleTapTimeoutMillis

    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val downChange = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
        if (downChange.uptimeMillis < minDownUptimeMillis) continue
        if (downChange.uptimeMillis > maxDownUptimeMillis) return null
        return downChange
    }
}

private val DoubleTapConfirmWindow = 800.milliseconds
