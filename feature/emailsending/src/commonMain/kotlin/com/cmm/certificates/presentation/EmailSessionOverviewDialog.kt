package com.cmm.certificates.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_progress_overview_summary
import certificates.composeapp.generated.resources.email_progress_overview_title
import certificates.composeapp.generated.resources.settings_history_cache_cached_tab
import certificates.composeapp.generated.resources.settings_history_cache_close
import certificates.composeapp.generated.resources.settings_history_cache_sent_tab
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.AnimatedDialog
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val DialogWidth = Grid.x240

@Composable
fun EmailSessionOverviewDialog(
    isOpen: Boolean,
    sentHistory: List<SentEmailRecord>,
    cachedEmails: List<CachedEmailEntry>,
    cachedLastReason: EmailStopReason?,
    onDismiss: () -> Unit,
) {
    if (!isOpen) return

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    AnimatedDialog(onDismiss = onDismiss) { requestDismiss ->
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Grid.x3,
            shadowElevation = Grid.x3,
            modifier = Modifier.widthIn(min = DialogWidth, max = DialogWidth),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.x8),
                verticalArrangement = Arrangement.spacedBy(Grid.x6),
            ) {
                EmailSessionOverviewHeader(onDismiss = requestDismiss)
                Text(
                    text = stringResource(
                        Res.string.email_progress_overview_summary,
                        sentHistory.size,
                        cachedEmails.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EmailSessionOverviewTabs(
                    selectedPage = pagerState.currentPage,
                    sentCount = sentHistory.size,
                    cachedCount = cachedEmails.size,
                    onSelectPage = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.heightIn(min = Grid.x80, max = Grid.x240),
                ) { page ->
                    when (page) {
                        0 -> EmailSessionSentPage(sentHistory = sentHistory)
                        else -> EmailSessionCachedPage(
                            cachedEmails = cachedEmails,
                            cachedLastReason = cachedLastReason,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = requestDismiss) {
                        Text(stringResource(Res.string.settings_history_cache_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailSessionOverviewHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.email_progress_overview_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = stringResource(Res.string.settings_history_cache_close),
            )
        }
    }
}

@Composable
private fun EmailSessionOverviewTabs(
    selectedPage: Int,
    sentCount: Int,
    cachedCount: Int,
    onSelectPage: (Int) -> Unit,
) {
    SecondaryTabRow(selectedTabIndex = selectedPage) {
        val tabs = listOf(
            "${stringResource(Res.string.settings_history_cache_sent_tab)} ($sentCount)",
            "${stringResource(Res.string.settings_history_cache_cached_tab)} ($cachedCount)",
        )
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = selectedPage == index,
                onClick = { onSelectPage(index) },
                text = { Text(label) },
            )
        }
    }
}
