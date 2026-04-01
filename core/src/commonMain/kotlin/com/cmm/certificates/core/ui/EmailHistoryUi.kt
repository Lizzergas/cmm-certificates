package com.cmm.certificates.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.email_stop_reason_cached_suffix
import certificates.composeapp.generated.resources.email_stop_reason_cancelled
import certificates.composeapp.generated.resources.email_stop_reason_consecutive_errors
import certificates.composeapp.generated.resources.email_stop_reason_daily_limit
import certificates.composeapp.generated.resources.email_stop_reason_doc_id_missing
import certificates.composeapp.generated.resources.email_stop_reason_generic_fail
import certificates.composeapp.generated.resources.email_stop_reason_gmail_quota
import certificates.composeapp.generated.resources.email_stop_reason_missing_attachments
import certificates.composeapp.generated.resources.email_stop_reason_missing_emails
import certificates.composeapp.generated.resources.email_stop_reason_no_cached
import certificates.composeapp.generated.resources.email_stop_reason_no_emails
import certificates.composeapp.generated.resources.email_stop_reason_no_entries
import certificates.composeapp.generated.resources.email_stop_reason_output_dir_missing
import certificates.composeapp.generated.resources.email_stop_reason_smtp_auth
import certificates.composeapp.generated.resources.email_stop_reason_smtp_settings_missing
import certificates.composeapp.generated.resources.network_unavailable_message
import certificates.composeapp.generated.resources.settings_history_date_hint
import certificates.composeapp.generated.resources.settings_history_date_label
import certificates.composeapp.generated.resources.settings_history_search_label
import com.cmm.certificates.core.theme.Grid
import com.cmm.certificates.core.theme.Stroke
import com.cmm.certificates.feature.emailsending.domain.EmailStopReason
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun HistoryFilters(
    query: String,
    dateQuery: String,
    onQueryChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x4)) {
        ClearableOutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(Res.string.settings_history_search_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ClearableOutlinedTextField(
            value = dateQuery,
            onValueChange = onDateChange,
            label = { Text(stringResource(Res.string.settings_history_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text(stringResource(Res.string.settings_history_date_hint)) },
        )
    }
}

@Composable
fun HistoryCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Grid.x1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Grid.x5),
            verticalArrangement = Arrangement.spacedBy(Grid.x3),
            content = content,
        )
    }
}

@Composable
fun HistoryHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Grid.x1)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyHistoryState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatEmailHistoryTimestamp(epochMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val date = emailHistoryDateKey(epochMillis)
    val time = "${dateTime.hour.pad2()}:${dateTime.minute.pad2()}"
    return "$date $time"
}

fun emailHistoryDateKey(epochMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year)
        append('-')
        append((dateTime.month.ordinal + 1).pad2())
        append('-')
        append(dateTime.day.pad2())
    }
}

@Composable
fun resolveEmailStopReason(reason: EmailStopReason): String {
    return when (reason) {
        EmailStopReason.SmtpAuthRequired -> stringResource(Res.string.email_stop_reason_smtp_auth)
        EmailStopReason.DocumentIdMissing -> stringResource(Res.string.email_stop_reason_doc_id_missing)
        EmailStopReason.OutputDirMissing -> stringResource(Res.string.email_stop_reason_output_dir_missing)
        EmailStopReason.NoEntries -> stringResource(Res.string.email_stop_reason_no_entries)
        is EmailStopReason.MissingEmailAddresses -> stringResource(
            Res.string.email_stop_reason_missing_emails,
            reason.preview,
        )

        EmailStopReason.NoEmailsToSend -> stringResource(Res.string.email_stop_reason_no_emails)
        EmailStopReason.SmtpSettingsMissing -> stringResource(Res.string.email_stop_reason_smtp_settings_missing)
        EmailStopReason.GmailQuotaExceeded -> stringResource(Res.string.email_stop_reason_gmail_quota)
        is EmailStopReason.ConsecutiveErrors -> stringResource(
            Res.string.email_stop_reason_consecutive_errors,
            reason.threshold,
        )

        is EmailStopReason.DailyLimitReached -> stringResource(
            Res.string.email_stop_reason_daily_limit,
            reason.limit,
        )

        EmailStopReason.Cancelled -> stringResource(Res.string.email_stop_reason_cancelled)
        EmailStopReason.GenericFailure -> stringResource(Res.string.email_stop_reason_generic_fail)
        EmailStopReason.NoCachedEmails -> stringResource(Res.string.email_stop_reason_no_cached)
        EmailStopReason.NetworkUnavailable -> stringResource(Res.string.network_unavailable_message)
        is EmailStopReason.MissingAttachments -> stringResource(
            Res.string.email_stop_reason_missing_attachments,
            reason.preview,
        )

        is EmailStopReason.Cached -> {
            val base = resolveEmailStopReason(reason.reason)
            val suffix = stringResource(Res.string.email_stop_reason_cached_suffix, reason.count)
            "$base $suffix"
        }

        is EmailStopReason.Raw -> reason.message
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
