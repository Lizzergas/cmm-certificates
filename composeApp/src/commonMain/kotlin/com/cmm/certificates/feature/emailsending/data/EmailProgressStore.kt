package com.cmm.certificates.feature.emailsending.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

data class EmailProgressState(
    val current: Int = 0,
    val total: Int = 0,
    val inProgress: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: String? = null,
    val currentRecipient: String? = null,
    val cancelRequested: Boolean = false,
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
)

class EmailProgressStore {
    private val _state = MutableStateFlow(EmailProgressState())
    val state: StateFlow<EmailProgressState> = _state

    fun start(total: Int) {
        _state.value = EmailProgressState(
            current = 0,
            total = total,
            inProgress = true,
            currentRecipient = null,
            cancelRequested = false,
            startedAtMillis = nowMillis(),
        )
    }

    fun update(current: Int) {
        _state.update { it.copy(current = current, inProgress = true) }
    }

    fun setCurrentRecipient(recipient: String?) {
        _state.update { it.copy(currentRecipient = recipient) }
    }

    fun finish() {
        _state.update {
            it.copy(
                current = it.total,
                inProgress = false,
                completed = true,
                currentRecipient = null,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun fail(message: String) {
        _state.update {
            it.copy(
                inProgress = false,
                errorMessage = message,
                currentRecipient = null,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun requestCancel() {
        _state.update {
            it.copy(
                inProgress = false,
                currentRecipient = null,
                cancelRequested = true,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun isCancelRequested(): Boolean = _state.value.cancelRequested

    fun clear() {
        _state.value = EmailProgressState()
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
