package com.cmm.certificates.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_certificate_name_label
import certificates.composeapp.generated.resources.settings_history_cache_cached_tab
import certificates.composeapp.generated.resources.settings_history_cache_close
import certificates.composeapp.generated.resources.settings_history_cache_sent_tab
import certificates.composeapp.generated.resources.settings_history_cache_title
import certificates.composeapp.generated.resources.settings_history_cached_at_label
import certificates.composeapp.generated.resources.settings_history_cached_empty
import certificates.composeapp.generated.resources.settings_history_file_label
import certificates.composeapp.generated.resources.settings_history_reason_label
import certificates.composeapp.generated.resources.settings_history_remove_cached
import certificates.composeapp.generated.resources.settings_history_results_summary
import certificates.composeapp.generated.resources.settings_history_sent_at_label
import certificates.composeapp.generated.resources.settings_history_sent_empty
import certificates.composeapp.generated.resources.settings_history_sent_on_date_summary
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.ui.AnimatedDialog
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val DialogWidth = Grid.x240

@Composable
fun HistoryCacheDialog(
    isOpen: Boolean,
    sentHistory: List<SentEmailRecord>,
    cachedEmails: List<CachedEmailEntry>,
    cachedLastReason: EmailStopReason?,
    onRemoveCachedEmail: (String) -> Unit,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_history_cache_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = requestDismiss) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = stringResource(Res.string.settings_history_cache_close),
                        )
                    }
                }

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
                        0 -> SentHistoryPage(sentHistory = sentHistory)
                        else -> CachedEmailsPage(
                            cachedEmails = cachedEmails,
                            cachedLastReason = cachedLastReason,
                            onRemoveCachedEmail = onRemoveCachedEmail,
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
private fun SentHistoryPage(sentHistory: List<SentEmailRecord>) {
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
        if (dateQuery.isNotBlank()) {
            Text(
                text = stringResource(
                    Res.string.settings_history_sent_on_date_summary,
                    filtered.size,
                    dateQuery,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (filtered.isEmpty()) {
            EmptyHistoryState(stringResource(Res.string.settings_history_sent_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                items(filtered, key = SentEmailRecord::id) { record ->
                    ExpandableHistoryRecordCard(record)
                }
            }
        }
    }
}

@Composable
private fun CachedEmailsPage(
    cachedEmails: List<CachedEmailEntry>,
    cachedLastReason: EmailStopReason?,
    onRemoveCachedEmail: (String) -> Unit,
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
            EmptyHistoryState(stringResource(Res.string.settings_history_cached_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Grid.x4),
            ) {
                items(filtered, key = CachedEmailEntry::id) { entry ->
                    ExpandableCachedEmailCard(
                        entry = entry,
                        cachedLastReason = cachedLastReason,
                        onRemove = { onRemoveCachedEmail(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableHistoryRecordCard(record: SentEmailRecord) {
    var expanded by rememberSaveable(record.id) { mutableStateOf(false) }

    HistoryCard(onClick = { expanded = !expanded }) {
        HistoryHeader(
            title = record.requestLabel(),
            subtitle = stringResource(Res.string.settings_history_sent_at_label) + ": " + formatEmailHistoryTimestamp(
                record.sentAt
            ),
        )
        Text(
            text = record.subject,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
                Text(
                    text = "${stringResource(Res.string.conversion_certificate_name_label)}: ${record.certificateName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                record.attachmentName?.takeIf { it.isNotBlank() }?.let { attachmentName ->
                    Text(
                        text = "${stringResource(Res.string.settings_history_file_label)}: $attachmentName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCachedEmailCard(
    entry: CachedEmailEntry,
    cachedLastReason: EmailStopReason?,
    onRemove: () -> Unit,
) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }

    HistoryCard(onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Grid.x2),
            ) {
                HistoryHeader(
                    title = entry.requestLabel(),
                    subtitle = stringResource(Res.string.settings_history_cached_at_label) + ": " + formatEmailHistoryTimestamp(
                        entry.cachedAt
                    ),
                )
                Text(
                    text = entry.request.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Grid.x3)) {
                        Text(
                            text = "${stringResource(Res.string.conversion_certificate_name_label)}: ${entry.request.certificateName}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        entry.request.attachmentName?.takeIf { it.isNotBlank() }
                            ?.let { attachmentName ->
                                Text(
                                    text = "${stringResource(Res.string.settings_history_file_label)}: $attachmentName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        (entry.failureReason ?: cachedLastReason)?.let { reason ->
                            Text(
                                text = "${stringResource(Res.string.settings_history_reason_label)}: ${
                                    resolveEmailStopReason(
                                        reason
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = stringResource(Res.string.settings_history_remove_cached),
                )
            }
        }
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
