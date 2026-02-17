package com.cmm.certificates.feature.emailsending.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface EmailStopReason {
    @Serializable
    data object SmtpAuthRequired : EmailStopReason
    @Serializable
    data object DocumentIdMissing : EmailStopReason
    @Serializable
    data object OutputDirMissing : EmailStopReason
    @Serializable
    data object NoEntries : EmailStopReason
    @Serializable
    data class MissingEmailAddresses(val preview: String) : EmailStopReason
    @Serializable
    data object NoEmailsToSend : EmailStopReason
    @Serializable
    data object SmtpSettingsMissing : EmailStopReason
    @Serializable
    data object GmailQuotaExceeded : EmailStopReason
    @Serializable
    data class ConsecutiveErrors(val threshold: Int) : EmailStopReason
    @Serializable
    data class DailyLimitReached(val limit: Int) : EmailStopReason
    @Serializable
    data object Cancelled : EmailStopReason
    @Serializable
    data object GenericFailure : EmailStopReason
    @Serializable
    data object NoCachedEmails : EmailStopReason
    @Serializable
    data class Cached(val reason: EmailStopReason, val count: Int) : EmailStopReason
    @Serializable
    data class Raw(val message: String) : EmailStopReason
}
