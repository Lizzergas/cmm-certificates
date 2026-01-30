package com.cmm.certificates.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_action_edit
import certificates.composeapp.generated.resources.settings_signature_section_title
import certificates.composeapp.generated.resources.settings_signature_style_bold
import certificates.composeapp.generated.resources.settings_signature_style_bold_italic
import certificates.composeapp.generated.resources.settings_signature_style_italic
import certificates.composeapp.generated.resources.settings_signature_summary_custom
import certificates.composeapp.generated.resources.settings_signature_summary_template
import com.cmm.certificates.core.signature.SignatureSummary
import com.cmm.certificates.core.signature.SignatureSummaryParser
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignatureSummaryCard(
    signatureHtml: String,
    onEdit: () -> Unit,
) {
    val summary = remember(signatureHtml) { SignatureSummaryParser.fromHtml(signatureHtml) }
    val summaryText = if (summary.isCustomHtml) {
        stringResource(Res.string.settings_signature_summary_custom)
    } else {
        val styleText = buildStyleSummary(summary)
        stringResource(
            Res.string.settings_signature_summary_template,
            summary.lineCount,
            styleText,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Grid.x6),
            verticalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            Text(
                text = stringResource(Res.string.settings_signature_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.settings_signature_action_edit))
            }
        }
    }
}

@Composable
private fun buildStyleSummary(summary: SignatureSummary): String {
    val style = mutableListOf<String>()
    val font = summary.font?.displayName
    if (!font.isNullOrBlank()) style.add(font)
    summary.fontSizePt?.let { style.add("${it}pt") }
    when {
        summary.bold && summary.italic -> style.add(stringResource(Res.string.settings_signature_style_bold_italic))
        summary.bold -> style.add(stringResource(Res.string.settings_signature_style_bold))
        summary.italic -> style.add(stringResource(Res.string.settings_signature_style_italic))
    }
    return style.joinToString(separator = " • ")
}
