package com.cmm.certificates.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_certificate_name_label
import certificates.composeapp.generated.resources.email_progress_overview_cached_empty
import certificates.composeapp.generated.resources.email_progress_overview_cached_to
import certificates.composeapp.generated.resources.email_progress_overview_sent_empty
import certificates.composeapp.generated.resources.email_progress_overview_sent_to
import certificates.composeapp.generated.resources.email_progress_overview_summary
import certificates.composeapp.generated.resources.email_progress_overview_title
import certificates.composeapp.generated.resources.settings_history_cache_cached_tab
import certificates.composeapp.generated.resources.settings_history_cache_close
import certificates.composeapp.generated.resources.settings_history_cache_sent_tab
import certificates.composeapp.generated.resources.settings_history_cached_at_label
import certificates.composeapp.generated.resources.settings_history_reason_label
import certificates.composeapp.generated.resources.settings_history_results_summary
import certificates.composeapp.generated.resources.settings_history_sent_at_label
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.EmptyHistoryState
import com.cmm.certificates.core.ui.HistoryCard
import com.cmm.certificates.core.ui.HistoryFilters
import com.cmm.certificates.core.ui.HistoryHeader
import com.cmm.certificates.core.ui.emailHistoryDateKey
import com.cmm.certificates.core.ui.formatEmailHistoryTimestamp
import com.cmm.certificates.core.ui.resolveEmailStopReason
import com.cmm.certificates.feature.emailsending.domain.CachedEmailEntry
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import com.cmm.certificates.feature.emailsending.domain.SentEmailRecord
import com.composables.icons.lucide.Check
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

    Dialog(onDismissRequest = onDismiss) {
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

                Text(
                    text = stringResource(
                        Res.string.email_progress_overview_summary,
                        sentHistory.size,
                        cachedEmails.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    val tabs = listOf(
                        "${stringResource(Res.string.settings_history_cache_sent_tab)} (${sentHistory.size})",
                        "${stringResource(Res.string.settings_history_cache_cached_tab)} (${cachedEmails.size})",
                    )
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(label) },
                        )
                    }
                }

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
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.settings_history_cache_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailSessionSentPage(sentHistory: List<SentEmailRecord>) {
    var query by rememberSaveable { mutableStateOf("") }
    var dateQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(sentHistory, query, dateQuery) {
        sentHistory.filter { it.matches(query = query, dateQuery = dateQuery) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Grid.x4),
    ) {
        HistoryFilters(
            query = query,
            dateQuery = dateQuery,
            onQueryChange = { query = it },
            onDateChange = { dateQuery = it },
        )
        Text(
            text = stringResource(
                Res.string.settings_history_results_summary,
                filtered.size,
                sentHistory.size,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (filtered.isEmpty()) {
            EmptyHistoryState(stringResource(Res.string.email_progress_overview_sent_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                items(filtered, key = SentEmailRecord::id) { record ->
                    EmailSessionSentCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun EmailSessionCachedPage(
    cachedEmails: List<CachedEmailEntry>,
    cachedLastReason: EmailStopReason?,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var dateQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(cachedEmails, query, dateQuery) {
        cachedEmails.filter { it.matches(query = query, dateQuery = dateQuery) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Grid.x4),
    ) {
        HistoryFilters(
            query = query,
            dateQuery = dateQuery,
            onQueryChange = { query = it },
            onDateChange = { dateQuery = it },
        )
        Text(
            text = stringResource(
                Res.string.settings_history_results_summary,
                filtered.size,
                cachedEmails.size,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        cachedLastReason?.let { reason ->
            Text(
                text = "${stringResource(Res.string.settings_history_reason_label)}: ${
                    resolveEmailStopReason(
                        reason
                    )
                }",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (filtered.isEmpty()) {
            EmptyHistoryState(stringResource(Res.string.email_progress_overview_cached_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                items(filtered, key = CachedEmailEntry::id) { entry ->
                    EmailSessionCachedCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun EmailSessionSentCard(record: SentEmailRecord) {
    HistoryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x4),
            verticalAlignment = Alignment.Top,
        ) {
            StatusBadge(
                isSuccess = true,
                modifier = Modifier.padding(top = Grid.x1),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Grid.x2),
            ) {
                HistoryHeader(
                    title = record.attachmentDisplayName(),
                    subtitle = stringResource(Res.string.settings_history_sent_at_label) + ": " +
                            formatEmailHistoryTimestamp(record.sentAt),
                )
                Text(
                    text = stringResource(
                        Res.string.email_progress_overview_sent_to,
                        record.requestLabel()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stringResource(Res.string.conversion_certificate_name_label)}: ${record.certificateName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmailSessionCachedCard(entry: CachedEmailEntry) {
    HistoryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Grid.x4),
            verticalAlignment = Alignment.Top,
        ) {
            StatusBadge(
                isSuccess = false,
                modifier = Modifier.padding(top = Grid.x1),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Grid.x2),
            ) {
                HistoryHeader(
                    title = entry.attachmentDisplayName(),
                    subtitle = stringResource(Res.string.settings_history_cached_at_label) + ": " +
                            formatEmailHistoryTimestamp(entry.cachedAt),
                )
                Text(
                    text = stringResource(
                        Res.string.email_progress_overview_cached_to,
                        entry.requestLabel()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stringResource(Res.string.conversion_certificate_name_label)}: ${entry.request.certificateName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSuccess) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isSuccess) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Box(
        modifier = modifier
            .size(Grid.x16)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSuccess) Lucide.Check else Lucide.X,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Grid.x8),
        )
    }
}

private fun SentEmailRecord.matches(query: String, dateQuery: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    val normalizedDate = dateQuery.trim()
    val queryMatches = normalizedQuery.isBlank() || listOf(
        toName,
        toEmail,
        certificateName,
        subject,
        attachmentName.orEmpty(),
        attachmentPath.orEmpty(),
    ).any { it.lowercase().contains(normalizedQuery) }
    val dateMatches =
        normalizedDate.isBlank() || emailHistoryDateKey(sentAt).contains(normalizedDate)
    return queryMatches && dateMatches
}

private fun CachedEmailEntry.matches(query: String, dateQuery: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    val normalizedDate = dateQuery.trim()
    val queryMatches = normalizedQuery.isBlank() || listOf(
        request.toName,
        request.toEmail,
        request.certificateName,
        request.subject,
        request.attachmentName.orEmpty(),
        request.attachmentPath.orEmpty(),
    ).any { it.lowercase().contains(normalizedQuery) }
    val dateMatches =
        normalizedDate.isBlank() || emailHistoryDateKey(cachedAt).contains(normalizedDate)
    return queryMatches && dateMatches
}

private fun SentEmailRecord.requestLabel(): String = "$toName <$toEmail>"

private fun CachedEmailEntry.requestLabel(): String = "${request.toName} <${request.toEmail}>"

private fun SentEmailRecord.attachmentDisplayName(): String =
    attachmentName?.takeIf { it.isNotBlank() }
        ?: certificateName

private fun CachedEmailEntry.attachmentDisplayName(): String =
    request.attachmentName?.takeIf { it.isNotBlank() }
        ?: request.certificateName
