package com.cmm.certificates.presentation.components

import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.settings_signature_tab_builder
import certificates.composeapp.generated.resources.settings_signature_tab_html
import certificates.composeapp.generated.resources.settings_signature_tab_preview
import com.cmm.certificates.core.signature.SignatureEditorMode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignatureEditorTabs(
    mode: SignatureEditorMode,
    isBuilderEnabled: Boolean,
    onModeChange: (SignatureEditorMode) -> Unit,
) {
    val tabs = listOf(
        SignatureEditorMode.Builder to Res.string.settings_signature_tab_builder,
        SignatureEditorMode.Html to Res.string.settings_signature_tab_html,
        SignatureEditorMode.Preview to Res.string.settings_signature_tab_preview,
    )
    val selectedIndex = tabs.indexOfFirst { it.first == mode }.coerceAtLeast(0)

    SecondaryTabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, (tabMode, titleRes) ->
            val enabled = tabMode != SignatureEditorMode.Builder || isBuilderEnabled
            Tab(
                selected = index == selectedIndex,
                onClick = { if (enabled) onModeChange(tabMode) },
                enabled = enabled,
                text = { Text(text = stringResource(titleRes)) },
            )
        }
    }
}
