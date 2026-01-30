package com.cmm.certificates.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (enabled) colors.primary else colors.surface
    val contentColor = if (enabled) colors.onPrimary else colors.onSurfaceVariant
    val shadowElevation = if (enabled) Grid.x6 else Grid.x0
    val border = if (enabled) null else BorderStroke(Stroke.thin, colors.outlineVariant)
    val badgeColor = if (enabled) contentColor.copy(alpha = 0.2f) else colors.surfaceVariant

    Button(
        onClick = onClick,
        modifier = modifier
            .height(Grid.x28)
            .shadow(shadowElevation, MaterialTheme.shapes.large),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(Grid.x14),
                color = badgeColor,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PDF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(Grid.x4))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
