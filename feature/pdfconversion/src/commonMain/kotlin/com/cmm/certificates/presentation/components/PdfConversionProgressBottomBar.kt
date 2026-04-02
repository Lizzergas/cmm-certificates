package com.cmm.certificates.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_preview_button
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import certificates.composeapp.generated.resources.network_unavailable_message
import certificates.composeapp.generated.resources.progress_cancel
import certificates.composeapp.generated.resources.progress_convert_another
import certificates.composeapp.generated.resources.progress_open_folder
import certificates.composeapp.generated.resources.progress_open_folder_unsupported_hint
import certificates.composeapp.generated.resources.progress_send_emails
import certificates.composeapp.generated.resources.progress_send_emails_hint
import com.cmm.certificates.core.openFolder
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProgressBottomBar(
    isCompleted: Boolean,
    outputDir: String,
    isSendPreviewEnabled: Boolean,
    isSendEmailsEnabled: Boolean,
    isNetworkAvailable: Boolean,
    isSmtpAuthenticated: Boolean,
    supportsEmailSending: Boolean,
    canOpenGeneratedFolders: Boolean,
    isPreviewSending: Boolean,
    onSendPreview: () -> Unit,
    onSendEmails: () -> Unit,
    onCancel: () -> Unit,
    onConvertAnother: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Grid.x3,
        shadowElevation = Grid.x3,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Grid.x8, vertical = Grid.x6),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Grid.x4),
                ) {
                    OutlinedButton(
                        onClick = onSendPreview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPreviewSending && isSendPreviewEnabled,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            Stroke.thin,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.email_preview_button))
                    }
                    if (!supportsEmailSending || !isNetworkAvailable || !isSmtpAuthenticated) {
                        Text(
                            text = if (!supportsEmailSending) {
                                stringResource(Res.string.email_sending_unsupported_hint)
                            } else if (!isNetworkAvailable) {
                                stringResource(Res.string.network_unavailable_message)
                            } else {
                                stringResource(Res.string.progress_send_emails_hint)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = Grid.x6, vertical = Grid.x3),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = onSendEmails,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSendEmailsEnabled,
                    ) {
                        Text(
                            text = stringResource(Res.string.progress_send_emails),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = { openFolder(outputDir) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canOpenGeneratedFolders,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            Stroke.thin,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.progress_open_folder))
                    }
                    if (!canOpenGeneratedFolders) {
                        Text(
                            text = stringResource(Res.string.progress_open_folder_unsupported_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = Grid.x6, vertical = Grid.x3),
                            textAlign = TextAlign.Center,
                        )
                    }
                    TextButton(
                        onClick = onConvertAnother,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.progress_convert_another))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = BorderStroke(
                        Stroke.thin,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(text = stringResource(Res.string.progress_cancel))
                }
            }
        }
    }
}
