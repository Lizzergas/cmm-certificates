package com.cmm.certificates.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_accredited_type_options_label
import certificates.composeapp.generated.resources.settings_authenticate
import certificates.composeapp.generated.resources.settings_authenticated
import certificates.composeapp.generated.resources.settings_back
import certificates.composeapp.generated.resources.settings_body_label
import certificates.composeapp.generated.resources.settings_clear_all
import certificates.composeapp.generated.resources.settings_password_label
import certificates.composeapp.generated.resources.settings_port_label
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
import com.cmm.certificates.data.email.SmtpTransport
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFFF8FAFC)
private val CardColor = Color.White
private val BorderColor = Color(0xFFE2E8F0)
private val PrimaryColor = Color(0xFF2563EB)
private val SuccessColor = Color(0xFF16A34A)
private val DisabledBackground = Color(0xFFE2E8F0)
private val DisabledForeground = Color(0xFF94A3B8)

private val MaxWidth = 480.dp
private val PaddingHorizontal = 16.dp
private val PaddingVertical = 12.dp
private val CardPadding = 16.dp
private val CardGap = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SettingsTopBar(
                title = Res.string.settings_title,
                subtitle = Res.string.settings_subtitle,
                actionText = Res.string.settings_back,
                onAction = {
                    viewModel.save()
                    onBack()
                },
            )
        },
        bottomBar = {
            SettingsBottomBar(
                onClearAll = viewModel::clearAll,
                onAuthenticate = viewModel::authenticate,
                canAuthenticate = state.smtp.canAuthenticate && !state.smtp.isAuthenticating,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsContent(
                state = state,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun BoxScope.SettingsContent(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val scrollState = rememberScrollState()
    val transportOptions = remember {
        linkedMapOf(
            SmtpTransport.SMTP to Res.string.settings_transport_smtp,
            SmtpTransport.SMTPS to Res.string.settings_transport_smtps,
            SmtpTransport.SMTP_TLS to Res.string.settings_transport_tls,
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .widthIn(max = MaxWidth),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsCard(title = Res.string.settings_section_title) {
            StatusLines(
                error = state.smtp.errorMessage,
                authenticated = state.smtp.isAuthenticated,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsField(
                    label = Res.string.settings_server_label,
                    value = state.smtp.host,
                    onValueChange = viewModel::setHost,
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                SettingsField(
                    label = Res.string.settings_port_label,
                    value = state.smtp.port,
                    onValueChange = viewModel::setPort,
                    singleLine = true,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            SmtpTransportPicker(
                selected = state.smtp.transport,
                options = transportOptions,
                onSelect = viewModel::setTransport,
            )

            SettingsField(
                label = Res.string.settings_username_label,
                value = state.smtp.username,
                onValueChange = viewModel::setUsername,
                singleLine = true,
            )
            SettingsField(
                label = Res.string.settings_password_label,
                value = state.smtp.password,
                onValueChange = viewModel::setPassword,
                singleLine = true,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )

            SettingsField(
                label = Res.string.settings_subject_label,
                value = state.email.subject,
                onValueChange = viewModel::setSubject,
                singleLine = true,
            )
            SettingsField(
                label = Res.string.settings_body_label,
                value = state.email.body,
                onValueChange = viewModel::setBody,
                minLines = 5,
                maxLines = 10,
            )
            SettingsField(
                label = Res.string.settings_signature_html_label,
                value = state.email.signatureHtml,
                onValueChange = viewModel::setSignatureHtml,
                minLines = 4,
                maxLines = 8,
            )
            SettingsField(
                label = Res.string.settings_accredited_type_options_label,
                value = state.certificate.accreditedTypeOptions,
                onValueChange = viewModel::setAccreditedTypeOptions,
                minLines = 4,
                maxLines = 10,
            )
        }
    }

    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(end = 4.dp),
    )
}


@Composable
private fun SettingsTopBar(
    title: StringResource,
    subtitle: StringResource,
    actionText: StringResource,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onAction) { Text(stringResource(actionText)) }
    }
}

@Composable
private fun SettingsBottomBar(
    onClearAll: () -> Unit,
    onAuthenticate: () -> Unit,
    canAuthenticate: Boolean,
) {
    Surface(
        color = CardColor,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(
                    text = stringResource(Res.string.settings_clear_all),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Button(
                onClick = onAuthenticate,
                enabled = canAuthenticate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = DisabledBackground,
                    disabledContentColor = DisabledForeground,
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

@Composable
private fun SettingsCard(
    title: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardColor,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, BorderColor),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatusLines(
    error: String?,
    authenticated: Boolean,
) {
    if (!error.isNullOrBlank()) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (authenticated) {
        Text(
            text = stringResource(Res.string.settings_authenticated),
            style = MaterialTheme.typography.labelSmall,
            color = SuccessColor,
        )
    }
}

@Composable
private fun SettingsField(
    label: StringResource,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showClearIcon: Boolean = true,
) {
    ClearableOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        showClearIcon = showClearIcon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmtpTransportPicker(
    selected: SmtpTransport,
    options: Map<SmtpTransport, StringResource>,
    onSelect: (SmtpTransport) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labelRes = options[selected]
    val label = if (labelRes != null) stringResource(labelRes) else selected.name

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        SettingsField(
            label = Res.string.settings_transport_label,
            value = label,
            onValueChange = {},
            singleLine = true,
            showClearIcon = false,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (transport, res) ->
                DropdownMenuItem(
                    text = { Text(stringResource(res)) },
                    onClick = {
                        onSelect(transport)
                        expanded = false
                    },
                )
            }
        }
    }
}
