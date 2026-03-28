package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_progress_daily_limit_status
import certificates.composeapp.generated.resources.email_progress_daily_limit_unlimited_status
import certificates.composeapp.generated.resources.email_sending_unsupported_hint
import certificates.composeapp.generated.resources.settings_accredited_type_options_label
import certificates.composeapp.generated.resources.settings_authenticate
import certificates.composeapp.generated.resources.settings_authenticated
import certificates.composeapp.generated.resources.settings_back
import certificates.composeapp.generated.resources.settings_body_label
import certificates.composeapp.generated.resources.settings_clear_all
import certificates.composeapp.generated.resources.settings_clear_all_cancel
import certificates.composeapp.generated.resources.settings_clear_all_confirm
import certificates.composeapp.generated.resources.settings_clear_all_message
import certificates.composeapp.generated.resources.settings_clear_all_title
import certificates.composeapp.generated.resources.settings_daily_limit_label
import certificates.composeapp.generated.resources.settings_password_label
import certificates.composeapp.generated.resources.settings_port_label
import certificates.composeapp.generated.resources.settings_section_title
import certificates.composeapp.generated.resources.settings_server_label
import certificates.composeapp.generated.resources.settings_subject_label
import certificates.composeapp.generated.resources.settings_subtitle
import certificates.composeapp.generated.resources.settings_title
import certificates.composeapp.generated.resources.settings_transport_label
import certificates.composeapp.generated.resources.settings_transport_smtp
import certificates.composeapp.generated.resources.settings_transport_smtps
import certificates.composeapp.generated.resources.settings_transport_tls
import com.cmm.certificates.core.presentation.UiMessage
import com.cmm.certificates.core.presentation.asString
import certificates.composeapp.generated.resources.settings_username_label
import com.cmm.certificates.core.theme.AppTheme
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.AppVerticalScrollbar
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState
import com.cmm.certificates.presentation.components.SignatureEditorDialog
import com.cmm.certificates.presentation.components.SignatureSummaryCard
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MaxWidth = Grid.x240
private val PaddingHorizontal = Grid.x8
private val PaddingVertical = Grid.x6
private val CardPadding = Grid.x8
private val CardGap = Grid.x6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signatureEditorState by viewModel.signatureEditorState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(Res.string.settings_clear_all_title)) },
            text = { Text(stringResource(Res.string.settings_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.settings_clear_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(Res.string.settings_clear_all_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                onClearAll = { showClearDialog = true },
                onAuthenticate = viewModel::authenticate,
                canAuthenticate = state.supportsEmailSending && state.smtp.canAuthenticate && !state.smtp.isAuthenticating,
                supportsEmailSending = state.supportsEmailSending,
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
                actions = SettingsActions(
                    onHostChange = viewModel::setHost,
                    onPortChange = viewModel::setPort,
                    onTransportChange = viewModel::setTransport,
                    onUsernameChange = viewModel::setUsername,
                    onPasswordChange = viewModel::setPassword,
                    onSubjectChange = viewModel::setSubject,
                    onBodyChange = viewModel::setBody,
                    onDailyLimitChange = viewModel::setDailyLimit,
                    onAccreditedTypeOptionsChange = viewModel::setAccreditedTypeOptions,
                    onEditSignature = viewModel::openSignatureEditor,
                ),
            )
        }
    }

    SignatureEditorDialog(
        state = signatureEditorState,
        onDismiss = viewModel::closeSignatureEditor,
        onSave = viewModel::saveSignatureDraft,
        onReset = viewModel::resetSignatureToDefault,
        onModeChange = viewModel::setSignatureEditorMode,
        onDraftHtmlChange = viewModel::setSignatureDraftHtml,
        onValidate = viewModel::validateSignatureDraft,
        onConvertToBuilder = viewModel::convertSignatureToBuilder,
        onSetFont = viewModel::setSignatureFont,
        onSetFontSize = viewModel::setSignatureFontSize,
        onToggleItalic = viewModel::toggleSignatureItalic,
        onToggleBold = viewModel::toggleSignatureBold,
        onSetLineHeight = viewModel::setSignatureLineHeight,
        onSetColorHex = viewModel::setSignatureColorHex,
        onAddLine = viewModel::addSignatureLine,
        onRemoveLine = viewModel::removeSignatureLine,
        onMoveLineUp = viewModel::moveSignatureLineUp,
        onMoveLineDown = viewModel::moveSignatureLineDown,
        onLineTextChange = viewModel::setSignatureLineText,
    )
}

@Composable
private fun BoxScope.SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
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
        verticalArrangement = Arrangement.spacedBy(Grid.x8),
    ) {
        SettingsCard(title = Res.string.settings_section_title) {
            StatusLines(
                error = state.smtp.errorMessage,
                authenticated = state.smtp.isAuthenticated,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Grid.x5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsField(
                    label = Res.string.settings_server_label,
                    value = state.smtp.host,
                    onValueChange = actions.onHostChange,
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                SettingsField(
                    label = Res.string.settings_port_label,
                    value = state.smtp.port,
                    onValueChange = actions.onPortChange,
                    singleLine = true,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            SmtpTransportPicker(
                selected = state.smtp.transport,
                options = transportOptions,
                onSelect = actions.onTransportChange,
            )

            SettingsField(
                label = Res.string.settings_username_label,
                value = state.smtp.username,
                onValueChange = actions.onUsernameChange,
                singleLine = true,
            )
            SettingsField(
                label = Res.string.settings_password_label,
                value = state.smtp.password,
                onValueChange = actions.onPasswordChange,
                singleLine = true,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )

            SettingsField(
                label = Res.string.settings_subject_label,
                value = state.email.subject,
                onValueChange = actions.onSubjectChange,
                singleLine = true,
            )
            SettingsField(
                label = Res.string.settings_body_label,
                value = state.email.body,
                onValueChange = actions.onBodyChange,
                minLines = 5,
                maxLines = 10,
                showClearIcon = false,
            )
            SettingsField(
                label = Res.string.settings_daily_limit_label,
                value = state.email.dailyLimit.toString(),
                onValueChange = actions.onDailyLimitChange,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                supportingText = {
                    val statusText = if (state.email.dailyLimit > 0) {
                        stringResource(
                            Res.string.email_progress_daily_limit_status,
                            state.sentToday,
                            state.email.dailyLimit,
                        )
                    } else {
                        stringResource(
                            Res.string.email_progress_daily_limit_unlimited_status,
                            state.sentToday,
                        )
                    }
                    Text(statusText)
                }
            )
            SettingsField(
                label = Res.string.settings_accredited_type_options_label,
                value = state.certificate.accreditedTypeOptions,
                onValueChange = actions.onAccreditedTypeOptionsChange,
                minLines = 4,
                maxLines = 10,
                showClearIcon = false,
            )
            SignatureSummaryCard(
                signatureHtml = state.email.signatureHtml,
                onEdit = actions.onEditSignature,
            )
        }
    }

    AppVerticalScrollbar(
        scrollState = scrollState,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(end = Grid.x2),
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
            .padding(bottom = Grid.x4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Grid.x2)) {
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
    supportsEmailSending: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Grid.x3,
        shadowElevation = Grid.x3,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(Grid.x4),
        ) {
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.error),
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
            ) {
                Text(
                    text = stringResource(Res.string.settings_authenticate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!supportsEmailSending) {
                Text(
                    text = stringResource(Res.string.email_sending_unsupported_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = Grid.x6, vertical = Grid.x3),
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
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
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
    error: UiMessage?,
    authenticated: Boolean,
) {
    if (error != null) {
        Text(
            text = error.asString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (authenticated) {
        Text(
            text = stringResource(Res.string.settings_authenticated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
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
    supportingText: @Composable (() -> Unit)? = null,
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
        supportingText = supportingText,
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

private data class SettingsActions(
    val onHostChange: (String) -> Unit,
    val onPortChange: (String) -> Unit,
    val onTransportChange: (SmtpTransport) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubjectChange: (String) -> Unit,
    val onBodyChange: (String) -> Unit,
    val onDailyLimitChange: (String) -> Unit,
    val onAccreditedTypeOptionsChange: (String) -> Unit,
    val onEditSignature: () -> Unit,
)

@Preview
@Composable
private fun SettingsContentPreview() {
    AppTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsContent(
                state = SettingsUiState(
                    smtp = SmtpSettingsState(
                        host = "smtp.example.com",
                        port = "465",
                        username = "team@example.com",
                        password = "secret",
                        isAuthenticated = true,
                    ),
                    email = EmailTemplateSettingsState(
                        subject = "Certificate",
                        body = "Hello,\n\nAttached is your certificate.",
                        signatureHtml = "<div>Best regards</div>",
                        dailyLimit = 120,
                    ),
                    certificate = CertificateSettingsState(
                        accreditedTypeOptions = "lecture\nseminar\nconference",
                    ),
                    sentToday = 32,
                    supportsEmailSending = true,
                ),
                actions = SettingsActions(
                    onHostChange = {},
                    onPortChange = {},
                    onTransportChange = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubjectChange = {},
                    onBodyChange = {},
                    onDailyLimitChange = {},
                    onAccreditedTypeOptionsChange = {},
                    onEditSignature = {},
                ),
            )
        }
    }
}
