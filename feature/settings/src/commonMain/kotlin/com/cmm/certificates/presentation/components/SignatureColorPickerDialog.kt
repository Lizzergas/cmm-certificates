package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_cancel
import certificates.composeapp.generated.resources.settings_signature_action_pick_color
import certificates.composeapp.generated.resources.settings_signature_dialog_color_title
import com.cmm.certificates.core.signature.SignatureHtmlCodec
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AnimatedDialog
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun SignatureColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val controller = rememberColorPickerController()
    val colors = MaterialTheme.colorScheme
    var selectedHex by remember { mutableStateOf(normalizeHexInput(initialHex)) }

    AnimatedDialog(onDismiss = onDismiss) { requestDismiss ->
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Grid.x3,
            shadowElevation = Grid.x3,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x6),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                Text(
                    text = stringResource(Res.string.settings_signature_dialog_color_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Grid.x240)
                        .padding(Grid.x5),
                    controller = controller,
                    onColorChanged = { envelope: ColorEnvelope ->
                        selectedHex = normalizeHexInput(envelopeToHex(envelope))
                    },
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Grid.x18)
                        .padding(horizontal = Grid.x5),
                    controller = controller,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Grid.x3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(Grid.x12)
                                .height(Grid.x12)
                                .background(
                                    parseHexColor(selectedHex, colors.onSurface),
                                    MaterialTheme.shapes.small,
                                )
                                .border(
                                    BorderStroke(Stroke.thin, colors.outlineVariant),
                                    MaterialTheme.shapes.small,
                                ),
                        )
                        Text(
                            text = selectedHex,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Grid.x3)) {
                        TextButton(onClick = requestDismiss) {
                            Text(text = stringResource(Res.string.settings_signature_action_cancel))
                        }
                        Button(onClick = { onConfirm(selectedHex) }) {
                            Text(text = stringResource(Res.string.settings_signature_action_pick_color))
                        }
                    }
                }
            }
        }
    }
}

internal fun parseHexColor(hex: String, fallback: Color): Color {
    val sanitized = hex.removePrefix("#").trim()
    val value = when (sanitized.length) {
        3 -> sanitized.map { "$it$it" }.joinToString("")
        6 -> sanitized
        else -> return fallback
    }
    return runCatching { Color(0xFF000000 or value.toLong(16)) }.getOrDefault(fallback)
}

internal fun normalizeHexInput(hex: String): String {
    if (hex.isBlank()) return "#000000"
    return SignatureHtmlCodec.normalizeColorHex(hex)
}

private fun envelopeToHex(envelope: ColorEnvelope): String {
    val color = envelope.color
    val red = (color.red * 255).roundToInt().coerceIn(0, 255)
    val green = (color.green * 255).roundToInt().coerceIn(0, 255)
    val blue = (color.blue * 255).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(red, green, blue)
}
