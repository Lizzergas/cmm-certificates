package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.docx
import certificates.composeapp.generated.resources.xlsx
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.ui.TooltipWrapper
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class SelectFileIconState {
    NotSelected,
    Selected,
    Error,
}

@Composable
fun SelectFileIcon(
    icon: DrawableResource,
    label: String,
    state: SelectFileIconState,
    fileName: String?,
    onClick: () -> Unit,
    errorText: String? = null,
    tooltipText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = selectFileContainerColor(colors, enabled, state)
    val borderColor = selectFileBorderColor(colors, enabled, state)
    val labelColor = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f)
    val fileNameColor = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.7f)
    val isSelected = state != SelectFileIconState.NotSelected

    TooltipWrapper(
        tooltipText = tooltipText,
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Grid.x56),
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            border = BorderStroke(Stroke.thin, borderColor),
            tonalElevation = if (isSelected) Grid.x1 else Grid.x0,
            shadowElevation = if (isSelected) Grid.x1 else Grid.x0,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Grid.x56)
                    .padding(horizontal = Grid.x6, vertical = Grid.x8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(Grid.x20),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                )
                if (fileName != null) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = fileNameColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state == SelectFileIconState.Error && !errorText.isNullOrBlank()) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun selectFileContainerColor(
    colors: ColorScheme,
    enabled: Boolean,
    state: SelectFileIconState,
) = when {
    !enabled -> FileCardNeutralBackground.copy(alpha = 0.55f)
    state == SelectFileIconState.Error -> FileCardErrorBackground
    state == SelectFileIconState.Selected -> FileCardSelectedBackground
    else -> FileCardNeutralBackground
}

private fun selectFileBorderColor(
    colors: ColorScheme,
    enabled: Boolean,
    state: SelectFileIconState,
) = when {
    !enabled -> colors.outlineVariant.copy(alpha = 0.5f)
    state == SelectFileIconState.Error -> FileCardErrorBorder
    state == SelectFileIconState.Selected -> FileCardSelectedBorder
    else -> colors.outlineVariant
}

private val FileCardNeutralBackground = Color(0xFFE5E7EB)
private val FileCardSelectedBackground = Color(0xFFDDF3E4)
private val FileCardErrorBackground = Color(0xFFF9E0E0)
private val FileCardSelectedBorder = Color(0xFF43A35B)
private val FileCardErrorBorder = Color(0xFFD74B4B)

@Preview
@Composable
private fun SelectFileIconPreviewIdle() {
    AppTheme {
        SelectFileIcon(
            icon = Res.drawable.xlsx,
            label = "XLSX",
            state = SelectFileIconState.NotSelected,
            fileName = null,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SelectFileIconPreviewSelected() {
    AppTheme {
        SelectFileIcon(
            icon = Res.drawable.docx,
            label = "DOCX",
            state = SelectFileIconState.Selected,
            fileName = "participants-template.docx",
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SelectFileIconPreviewError() {
    AppTheme {
        SelectFileIcon(
            icon = Res.drawable.xlsx,
            label = "XLSX",
            state = SelectFileIconState.Error,
            fileName = "registrations.xlsx",
            errorText = "Stub error message",
            onClick = {},
        )
    }
}
