package com.cmm.certificates.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmm.certificates.components.PrimaryActionButton
import com.cmm.certificates.components.SelectionCard
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.home_convert_button
import certificates.composeapp.generated.resources.home_output_directory_hint
import certificates.composeapp.generated.resources.home_output_directory_title
import certificates.composeapp.generated.resources.home_source_excel_hint
import certificates.composeapp.generated.resources.home_source_excel_title
import certificates.composeapp.generated.resources.home_subtitle
import certificates.composeapp.generated.resources.home_title
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val backgroundColor = Color(0xFFF8FAFC)
    val primaryColor = Color(0xFF2563EB)
    val primaryLight = Color(0xFFEFF6FF)

    MaterialTheme {
        Scaffold(
            containerColor = backgroundColor,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, backgroundColor),
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    PrimaryActionButton(
                        text = stringResource(Res.string.home_convert_button),
                        onClick = { println("Convert to PDF") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(padding)
                    .safeContentPadding()
                    .fillMaxSize(),
            ) {
                val cardHeight = if (maxWidth < 360.dp) 180.dp else 220.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .align(Alignment.TopCenter)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        modifier = Modifier.clickable(onClick = onProfileClick),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.home_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.home_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SelectionCard(
                        title = stringResource(Res.string.home_source_excel_title),
                        subtitle = state.xlsxPath.ifBlank {
                            stringResource(Res.string.home_source_excel_hint)
                        },
                        selected = state.xlsxPath.isNotBlank(),
                        badgeText = "XLSX",
                        badgeBackground = primaryLight,
                        badgeContentColor = primaryColor,
                        minHeight = cardHeight,
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker(
                                    mode = FileKitMode.Single,
                                    type = FileKitType.File(listOf("xlsx")),
                                )
                                viewModel.selectXlsx(file?.toString().orEmpty())
                            }
                        },
                    )

                    SelectionCard(
                        title = stringResource(Res.string.home_output_directory_title),
                        subtitle = state.outputDir.ifBlank {
                            stringResource(Res.string.home_output_directory_hint)
                        },
                        selected = state.outputDir.isNotBlank(),
                        badgeText = "Folder",
                        badgeBackground = primaryLight,
                        badgeContentColor = primaryColor,
                        minHeight = cardHeight,
                        onClick = {
                            scope.launch {
                                val directory = FileKit.openDirectoryPicker()
                                viewModel.setOutputDir(directory?.toString().orEmpty())
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
