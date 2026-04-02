package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.common_action_ok
import certificates.composeapp.generated.resources.progress_duration_minutes_seconds
import certificates.composeapp.generated.resources.progress_duration_seconds
import certificates.composeapp.generated.resources.progress_output_label
import certificates.composeapp.generated.resources.progress_success_title
import certificates.composeapp.generated.resources.progress_time_label
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SuccessContent(
    modifier: Modifier,
    total: Int,
    outputDir: String,
    elapsedSeconds: Long,
    warningMessage: String?,
) {
    val successTitle = stringResource(Res.string.progress_success_title, total)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessBadge()
        Spacer(modifier = Modifier.height(Grid.x8))
        Text(
            text = successTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!warningMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Grid.x4))
            Text(
                text = warningMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(Grid.x10))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = Grid.x1,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                SummarySection(
                    label = stringResource(Res.string.progress_output_label),
                    value = outputDir.ifBlank { "-" },
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.thin)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                SummarySection(
                    label = stringResource(Res.string.progress_time_label),
                    value = formatDuration(elapsedSeconds),
                )
            }
        }
    }
}

@Composable
private fun SuccessBadge() {
    Box(
        modifier = Modifier
            .size(Grid.x64)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Grid.x48)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.common_action_ok),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SummarySection(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        stringResource(Res.string.progress_duration_minutes_seconds, minutes, seconds)
    } else {
        stringResource(Res.string.progress_duration_seconds, seconds)
    }
}

@Preview
@Composable
private fun PdfSuccessContentPreview() {
    AppTheme {
        SuccessContent(
            modifier = Modifier.fillMaxWidth(),
            total = 24,
            outputDir = "/tmp/certificates",
            elapsedSeconds = 12,
            warningMessage = null,
        )
    }
}
