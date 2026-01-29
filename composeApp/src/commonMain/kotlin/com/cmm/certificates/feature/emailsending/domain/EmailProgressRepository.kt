package com.cmm.certificates.feature.emailsending.domain

import com.cmm.certificates.feature.emailsending.data.EmailProgressState
import kotlinx.coroutines.flow.StateFlow

interface EmailProgressRepository {
    val state: StateFlow<EmailProgressState>

    fun start(total: Int)

    fun update(current: Int)

    fun setCurrentRecipient(recipient: String?)

    fun finish()

    fun fail(message: String)

    fun requestCancel()

    fun isCancelRequested(): Boolean

    fun clear()
}
