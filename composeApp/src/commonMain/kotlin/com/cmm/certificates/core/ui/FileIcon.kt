package com.cmm.certificates.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FileIcon(
    iconText: String,
    selected: Boolean,
    onClick: () -> Unit,
    tooltipText: String? = null,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Color(0xFF22C55E) else Color(0xFFE2E8F0)
    val background = if (selected) Color(0xFFDCFCE7) else Color.White
    val iconBackground = if (selected) Color.White else Color(0xFFF1F5F9)
    val iconColor = if (selected) Color(0xFF15803D) else Color(0xFF94A3B8)

    TooltipWrapper(
        tooltipText = tooltipText,
        modifier = modifier.height(96.dp),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = background,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = if (selected) 2.dp else 0.dp,
            shadowElevation = if (selected) 2.dp else 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
