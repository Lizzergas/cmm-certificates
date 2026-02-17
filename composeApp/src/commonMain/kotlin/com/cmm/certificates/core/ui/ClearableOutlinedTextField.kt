package com.cmm.certificates.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.cmm.certificates.core.theme.Grid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

@Composable
fun ClearableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = LocalTextStyle.current,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = {},
    showClearIcon: Boolean = value.isNotBlank(),
    onClear: (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val canClear = showClearIcon && value.isNotBlank() && enabled && (onClear != null || !readOnly)
    val trailingContent: (@Composable () -> Unit)? = if (trailingIcon == null && !canClear) {
        null
    } else {
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Grid.x2),
            ) {
                trailingIcon?.invoke()
                if (canClear) {
                    IconButton(
                        onClick = { (onClear ?: { onValueChange("") })() },
                        modifier = Modifier.size(Grid.x16),
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Grid.x8),
                        )
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        readOnly = readOnly,
        enabled = enabled,
        trailingIcon = trailingContent,
        supportingText = supportingText,
    )
}
