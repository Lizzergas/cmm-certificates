package com.cmm.certificates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SelectionCardState {
    Idle,
    Selected,
}

@Composable
fun SelectionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    state: SelectionCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 220.dp,
) {
    val shape = RoundedCornerShape(28.dp)
    val isSelected = state == SelectionCardState.Selected

    val accentColor = Color(0xFF2563EB)
    val accentBackground = Color(0xFFEFF6FF)
    val neutralContent = Color(0xFF94A3B8)

    val subtitleColor = when {
        isSelected -> accentColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val badgeContentColor = when {
        isSelected -> accentColor
        else -> neutralContent
    }
    val borderColor = when {
        isSelected -> accentColor
        else -> Color(0xFFD1D5DB)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, borderColor.copy(alpha = 0.35f), shape)
                    } else {
                        Modifier.dashedBorder(borderColor, 2.dp, 28.dp)
                    }
                )
                .clip(shape)
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IconBadge(
                    text = badgeText,
                    background = accentBackground,
                    contentColor = badgeContentColor,
                    size = if (isSelected) 72.dp else 64.dp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SelectionIndicator(
                state = state,
                accent = borderColor,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun IconBadge(
    text: String,
    background: Color,
    contentColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SelectionIndicator(
    state: SelectionCardState,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val isSelected = state == SelectionCardState.Selected
    val borderColor = when {
        isSelected -> accent.copy(alpha = 0.4f)
        else -> Color(0xFFE2E8F0)
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape),
            )
        }
    }
}

private fun Modifier.dashedBorder(color: Color, strokeWidth: Dp, cornerRadius: Dp): Modifier {
    return drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
        )
        val inset = stroke.width / 2
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                size.width - inset * 2,
                size.height - inset * 2,
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx() - inset, cornerRadius.toPx() - inset),
            style = stroke,
        )
    }
}
