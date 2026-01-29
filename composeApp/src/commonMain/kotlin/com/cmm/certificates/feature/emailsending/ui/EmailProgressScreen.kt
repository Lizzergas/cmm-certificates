package com.cmm.certificates.feature.emailsending.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_progress_cancel
import certificates.composeapp.generated.resources.email_progress_error_title
import certificates.composeapp.generated.resources.email_progress_finish
import certificates.composeapp.generated.resources.email_progress_recipient_label
import certificates.composeapp.generated.resources.email_progress_success_title
import certificates.composeapp.generated.resources.email_progress_title
import com.cmm.certificates.core.ui.ProgressErrorContent
import com.cmm.certificates.core.ui.ProgressIndicatorContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val FADE_IN_DURATION_MS = 420
private const val FADE_OUT_DURATION_MS = 300
private const val SIZE_ANIMATION_DURATION_MS = 360

@Composable
fun EmailProgressScreen(
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EmailSenderViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startSendingIfIdle()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            EmailProgressBottomBar(
                isInProgress = uiState.inProgress,
                onFinish = onFinish,
                onCancel = {
                    viewModel.cancelSending()
                    onCancel()
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .safeContentPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
        AnimatedContent(
            targetState = uiState.completed,
            modifier = contentModifier,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = FADE_IN_DURATION_MS)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = FADE_OUT_DURATION_MS)) using
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(durationMillis = SIZE_ANIMATION_DURATION_MS) },
                        )
            },
        ) { completed ->
            when {
                uiState.errorMessage != null -> {
                    ProgressErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(Res.string.email_progress_error_title),
                        message = uiState.errorMessage.orEmpty(),
                    )
                }

                completed -> {
                    EmailSuccessContent(
                        modifier = Modifier.fillMaxSize(),
                        total = uiState.total,
                    )
                }

                else -> {
                    val infoText = uiState.currentRecipient?.let {
                        stringResource(Res.string.email_progress_recipient_label, it)
                    }.orEmpty()
                    ProgressIndicatorContent(
                        modifier = Modifier.fillMaxSize(),
                        current = uiState.current,
                        total = uiState.total,
                        progress = uiState.progress,
                        title = stringResource(Res.string.email_progress_title),
                        infoText = infoText,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailProgressBottomBar(
    isInProgress: Boolean,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val bottomBarHeight = 128.dp
    Surface(
        modifier = Modifier.height(bottomBarHeight),
        color = Color.White,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isInProgress) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE2E8F0),
                    ),
                ) {
                    Text(text = stringResource(Res.string.email_progress_cancel))
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8),
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.email_progress_finish),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailSuccessContent(
    modifier: Modifier,
    total: Int,
) {
    val successTitle = stringResource(Res.string.email_progress_success_title, total)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(Color(0x332563EB), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0x1A2563EB), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = successTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
