package com.cmm.certificates.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun TooltipWrapper(
    tooltipText: String?,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (tooltipText.isNullOrBlank()) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(0xFF111827),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        },
        modifier = modifier,
        delayMillis = 600,
        tooltipPlacement = TooltipPlacement.CursorPoint(alignment = Alignment.TopStart),
    ) {
        content()
    }
}
