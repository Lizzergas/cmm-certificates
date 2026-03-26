package com.cmm.certificates.feature.certificate.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.ui.TooltipWrapper

@Composable
fun SelectFileIcon(
    iconText: String,
    selected: Boolean,
    onClick: () -> Unit,
    tooltipText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (!enabled) {
        colors.outlineVariant.copy(alpha = 0.5f)
    } else if (selected) {
        colors.tertiary
    } else {
        colors.outlineVariant
    }
    val background = if (!enabled) {
        colors.surfaceVariant.copy(alpha = 0.5f)
    } else if (selected) {
        colors.tertiaryContainer
    } else {
        colors.surface
    }
    val iconBackground = if (!enabled) {
        colors.surface.copy(alpha = 0.7f)
    } else if (selected) {
        colors.surface
    } else {
        colors.surfaceVariant
    }
    val iconColor = if (!enabled) {
        colors.onSurfaceVariant.copy(alpha = 0.6f)
    } else if (selected) {
        colors.tertiary
    } else {
        colors.onSurfaceVariant
    }

    TooltipWrapper(
        tooltipText = tooltipText,
        modifier = modifier.height(Grid.x48),
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = background,
            border = BorderStroke(Stroke.thin, borderColor),
            tonalElevation = if (selected) Grid.x1 else Grid.x0,
            shadowElevation = if (selected) Grid.x1 else Grid.x0,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(Grid.x28)
                        .background(iconBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = iconText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectFileIconPreview() {
    AppTheme {
        SelectFileIcon(iconText = "XLSX", selected = true, onClick = {})
    }
}
