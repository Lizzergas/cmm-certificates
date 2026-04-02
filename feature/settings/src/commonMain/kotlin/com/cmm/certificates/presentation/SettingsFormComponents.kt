package com.cmm.certificates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.VisualTransformation
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_clear_all
import certificates.composeapp.generated.resources.settings_theme_label
import certificates.composeapp.generated.resources.settings_transport_label
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.core.ui.ClearableOutlinedTextField
import com.cmm.certificates.feature.settings.domain.AppThemeMode
import com.cmm.certificates.feature.settings.domain.SmtpTransport
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val CardPadding = Grid.x8
private val CardGap = Grid.x6

internal data class SettingsActions(
    val onHostChange: (String) -> Unit,
    val onPortChange: (String) -> Unit,
    val onTransportChange: (SmtpTransport) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubjectChange: (String) -> Unit,
    val onBodyChange: (String) -> Unit,
    val onDailyLimitChange: (String) -> Unit,
    val onThemeModeChange: (AppThemeMode) -> Unit,
    val onUseInAppPdfPreviewChange: (Boolean) -> Unit,
    val onOutputDirectoryReset: () -> Unit,
    val onChooseOutputDirectory: () -> Unit,
    val onOpenHistoryCache: () -> Unit,
    val onOpenInstallationDirectory: () -> Unit,
    val onOpenLegalResourcesDirectory: () -> Unit,
    val onEditSignature: () -> Unit,
    val onAuthenticate: () -> Unit,
    val onOpenEmailConfiguration: () -> Unit,
    val onSendLogs: () -> Unit,
)

@Composable
internal fun SettingsTopBar(
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
        TextButton(onClick = onAction) {
            Text(stringResource(actionText))
        }
    }
}

@Composable
internal fun SettingsBottomBar(
    onClearAll: () -> Unit,
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
        }
    }
}

@Composable
internal fun SettingsCard(
    title: StringResource,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (onClick != null) modifier.clickable(onClick = onClick) else modifier
            },
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
internal fun SettingsField(
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
private fun <T> SettingsDropdownField(
    label: StringResource,
    selected: T,
    options: Map<T, StringResource>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labelRes = options[selected]
    val selectedLabel = if (labelRes != null) stringResource(labelRes) else selected.toString()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        SettingsField(
            label = label,
            value = selectedLabel,
            onValueChange = {},
            singleLine = true,
            showClearIcon = false,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, res) ->
                DropdownMenuItem(
                    text = { Text(stringResource(res)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun SmtpTransportPicker(
    selected: SmtpTransport,
    options: Map<SmtpTransport, StringResource>,
    onSelect: (SmtpTransport) -> Unit,
) {
    SettingsDropdownField(
        label = Res.string.settings_transport_label,
        selected = selected,
        options = options,
        onSelect = onSelect,
    )
}

@Composable
internal fun ThemeModePicker(
    selected: AppThemeMode,
    options: Map<AppThemeMode, StringResource>,
    onSelect: (AppThemeMode) -> Unit,
) {
    SettingsDropdownField(
        label = Res.string.settings_theme_label,
        selected = selected,
        options = options,
        onSelect = onSelect,
    )
}
