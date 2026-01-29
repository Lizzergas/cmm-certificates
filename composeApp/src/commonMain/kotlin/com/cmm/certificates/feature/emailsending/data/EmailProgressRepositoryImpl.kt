package com.cmm.certificates.feature.emailsending.data

import com.cmm.certificates.feature.emailsending.domain.EmailProgressRepository
import kotlinx.coroutines.flow.StateFlow

class EmailProgressRepositoryImpl(
    private val store: EmailProgressStore,
) : EmailProgressRepository {
    override val state: StateFlow<EmailProgressState> = store.state

    override fun start(total: Int) {
        store.start(total)
    }

    override fun update(current: Int) {
        store.update(current)
    }

    override fun setCurrentRecipient(recipient: String?) {
        store.setCurrentRecipient(recipient)
    }

    override fun finish() {
        store.finish()
    }

    override fun fail(message: String) {
        store.fail(message)
    }

    override fun requestCancel() {
        store.requestCancel()
    }

    override fun isCancelRequested(): Boolean = store.isCancelRequested()

    override fun clear() {
        store.clear()
    }
}
