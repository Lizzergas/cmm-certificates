package com.cmm.certificates.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_accredited_type_options_hint
import certificates.composeapp.generated.resources.settings_accredited_type_options_label
import certificates.composeapp.generated.resources.settings_authenticate
import certificates.composeapp.generated.resources.settings_authenticated
import certificates.composeapp.generated.resources.settings_back
import certificates.composeapp.generated.resources.settings_body_label
import certificates.composeapp.generated.resources.settings_clear_all
import certificates.composeapp.generated.resources.settings_password_label
import certificates.composeapp.generated.resources.settings_port_label
import certificates.composeapp.generated.resources.settings_save
import certificates.composeapp.generated.resources.settings_section_title
import certificates.composeapp.generated.resources.settings_server_label
import certificates.composeapp.generated.resources.settings_signature_html_label
import certificates.composeapp.generated.resources.settings_subject_label
import certificates.composeapp.generated.resources.settings_subtitle
import certificates.composeapp.generated.resources.settings_title
import certificates.composeapp.generated.resources.settings_transport_label
import certificates.composeapp.generated.resources.settings_transport_smtp
import certificates.composeapp.generated.resources.settings_transport_smtps
import certificates.composeapp.generated.resources.settings_transport_tls
import certificates.composeapp.generated.resources.settings_username_label
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.core.usecase.ClearAllDataUseCase
import com.cmm.certificates.data.email.SmtpTransport
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    store: SmtpSettingsStore = koinInject(),
    clearAllDataUseCase: ClearAllDataUseCase = koinInject(),
) {
    val state by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val transportLabels = mapOf(
        SmtpTransport.SMTP to stringResource(Res.string.settings_transport_smtp),
        SmtpTransport.SMTPS to stringResource(Res.string.settings_transport_smtps),
        SmtpTransport.SMTP_TLS to stringResource(Res.string.settings_transport_tls),
    )
    var transportExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { store.save() } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_save),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        OutlinedButton(
                            onClick = { scope.launch { clearAllDataUseCase.clearAll() } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_clear_all),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Button(
                            onClick = { scope.launch { store.authenticate() } },
                            enabled = state.canAuthenticate && !state.isAuthenticating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8),
                            ),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_authenticate),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.settings_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(Res.string.settings_back))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_section_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!state.errorMessage.isNullOrBlank()) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.isAuthenticated) {
                            Text(
                                text = stringResource(Res.string.settings_authenticated),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClearableOutlinedTextField(
                                value = state.host,
                                onValueChange = store::setHost,
                                label = { Text(stringResource(Res.string.settings_server_label)) },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                            )
                            ClearableOutlinedTextField(
                                value = state.port,
                                onValueChange = store::setPort,
                                label = { Text(stringResource(Res.string.settings_port_label)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        ExposedDropdownMenuBox(
                            expanded = transportExpanded,
                            onExpandedChange = { transportExpanded = !transportExpanded },
                        ) {
                            val fillMaxWidth = Modifier.fillMaxWidth()
                            ClearableOutlinedTextField(
                                value = transportLabels[state.transport].orEmpty(),
                                onValueChange = {},
                                label = { Text(stringResource(Res.string.settings_transport_label)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = transportExpanded)
                                },
                                modifier = fillMaxWidth.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true,
                                ),
                                readOnly = true,
                                singleLine = true,
                                showClearIcon = false,
                            )
                            ExposedDropdownMenu(
                                expanded = transportExpanded,
                                onDismissRequest = { transportExpanded = false },
                            ) {
                                transportLabels.forEach { (transport, label) ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            store.setTransport(transport)
                                            transportExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        ClearableOutlinedTextField(
                            value = state.username,
                            onValueChange = store::setUsername,
                            label = { Text(stringResource(Res.string.settings_username_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        ClearableOutlinedTextField(
                            value = state.password,
                            onValueChange = store::setPassword,
                            label = { Text(stringResource(Res.string.settings_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        ClearableOutlinedTextField(
                            value = state.subject,
                            onValueChange = store::setSubject,
                            label = { Text(stringResource(Res.string.settings_subject_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        ClearableOutlinedTextField(
                            value = state.body,
                            onValueChange = store::setBody,
                            label = { Text(stringResource(Res.string.settings_body_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 5,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        ClearableOutlinedTextField(
                            value = state.signatureHtml,
                            onValueChange = store::setSignatureHtml,
                            label = { Text(stringResource(Res.string.settings_signature_html_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 4,
                            maxLines = 8,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        ClearableOutlinedTextField(
                            value = state.accreditedTypeOptions,
                            onValueChange = store::setAccreditedTypeOptions,
                            label = { Text(stringResource(Res.string.settings_accredited_type_options_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 4,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        Text(
                            text = stringResource(Res.string.settings_accredited_type_options_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
