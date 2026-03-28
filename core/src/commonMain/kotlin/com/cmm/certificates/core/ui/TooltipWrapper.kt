package com.cmm.certificates.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun TooltipWrapper(
    tooltipText: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
