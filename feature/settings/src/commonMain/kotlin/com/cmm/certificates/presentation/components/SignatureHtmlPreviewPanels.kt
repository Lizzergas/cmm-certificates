package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_validate
import certificates.composeapp.generated.resources.settings_signature_custom_html_notice
import certificates.composeapp.generated.resources.settings_signature_error_forbidden_attr
import certificates.composeapp.generated.resources.settings_signature_error_forbidden_tag
import certificates.composeapp.generated.resources.settings_signature_error_invalid_html
import certificates.composeapp.generated.resources.settings_signature_error_missing_root
import certificates.composeapp.generated.resources.settings_signature_error_too_long
import certificates.composeapp.generated.resources.settings_signature_error_too_many_lines
import certificates.composeapp.generated.resources.settings_signature_html_convert
import certificates.composeapp.generated.resources.settings_signature_html_label
import certificates.composeapp.generated.resources.settings_signature_preview_unavailable
import com.cmm.certificates.core.signature.SignatureBuilderState
import com.cmm.certificates.core.signature.SignatureFont
import com.cmm.certificates.core.signature.SignatureValidationError
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignatureHtmlPanel(
    draftHtml: String,
    isBuilderCompatible: Boolean,
    validationError: SignatureValidationError?,
    validationMessage: String?,
    onDraftHtmlChange: (String) -> Unit,
    onValidate: () -> Unit,
    onConvertToBuilder: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x4)) {
        if (!isBuilderCompatible) {
            Text(
                text = stringResource(Res.string.settings_signature_custom_html_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = draftHtml,
            onValueChange = onDraftHtmlChange,
            label = { Text(text = stringResource(Res.string.settings_signature_html_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            isError = validationError != null,
            supportingText = {
                if (validationError != null && !validationMessage.isNullOrBlank()) {
                    Text(
                        text = validationMessage,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(Grid.x4)) {
            OutlinedButton(onClick = onValidate) {
                Text(text = stringResource(Res.string.settings_signature_action_validate))
            }
            if (isBuilderCompatible) {
                TextButton(onClick = onConvertToBuilder) {
                    Text(text = stringResource(Res.string.settings_signature_html_convert))
                }
            }
        }
    }
}

@Composable
internal fun SignaturePreviewPanel(
    builder: SignatureBuilderState,
    isBuilderCompatible: Boolean,
) {
    if (!isBuilderCompatible) {
        Text(
            text = stringResource(Res.string.settings_signature_preview_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SignatureBuilderPreview(builder = builder)
}

@Composable
internal fun SignatureBuilderPreview(builder: SignatureBuilderState) {
    val style = builder.style
    val colors = MaterialTheme.colorScheme
    val fontFamily = when (style.fontFamily) {
        SignatureFont.TimesNewRoman -> FontFamily.Serif
        SignatureFont.Arial, SignatureFont.Calibri -> FontFamily.SansSerif
    }
    val fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal
    val fontSize = style.fontSizePt.sp
    val lineHeight = (style.fontSizePt * style.lineHeight).sp
    val parsedColor = parseHexColor(style.colorHex, colors.onSurface)
    val textColor = if (colors.surface.luminance() < 0.5f && parsedColor == Color.Black) {
        colors.onSurface
    } else {
        parsedColor
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, colors.outlineVariant),
        color = colors.surface,
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Grid.x6),
            verticalArrangement = Arrangement.spacedBy(Grid.x2),
        ) {
            builder.lines.forEach { line ->
                Text(
                    text = line,
                    color = textColor,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                )
            }
        }
    }
}

@Composable
internal fun validationMessage(
    error: SignatureValidationError,
    detail: String?,
): String {
    return when (error) {
        SignatureValidationError.MissingRoot -> stringResource(Res.string.settings_signature_error_missing_root)
        SignatureValidationError.ForbiddenTag -> stringResource(
            Res.string.settings_signature_error_forbidden_tag,
            detail.orEmpty(),
        )

        SignatureValidationError.ForbiddenAttribute -> stringResource(Res.string.settings_signature_error_forbidden_attr)
        SignatureValidationError.TooLong -> stringResource(Res.string.settings_signature_error_too_long)
        SignatureValidationError.TooManyLines -> stringResource(Res.string.settings_signature_error_too_many_lines)
        SignatureValidationError.InvalidHtml -> stringResource(Res.string.settings_signature_error_invalid_html)
    }
}

@Preview
@Composable
private fun SignatureBuilderPreviewLight() {
    AppTheme(darkTheme = false) {
        SignatureBuilderPreview(SignatureBuilderState())
    }
}

@Preview
@Composable
private fun SignatureBuilderPreviewDark() {
    AppTheme(darkTheme = true) {
        SignatureBuilderPreview(SignatureBuilderState())
    }
}
