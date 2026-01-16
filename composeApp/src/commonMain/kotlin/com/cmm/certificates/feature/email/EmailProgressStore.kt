package com.cmm.certificates.feature.email

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
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
)

class EmailProgressStore {
    private val _state = MutableStateFlow(EmailProgressState())
    val state: StateFlow<EmailProgressState> = _state
    private val cancelRequested = MutableStateFlow(false)

    fun start(total: Int) {
        cancelRequested.value = false
        _state.value = EmailProgressState(
            current = 0,
            total = total,
            inProgress = true,
            startedAtMillis = nowMillis(),
        )
    }

    fun update(current: Int) {
        _state.update { it.copy(current = current, inProgress = true) }
    }

    fun finish() {
        _state.update {
            it.copy(
                current = it.total,
                inProgress = false,
                completed = true,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun fail(message: String) {
        _state.update {
            it.copy(
                inProgress = false,
                errorMessage = message,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun requestCancel() {
        cancelRequested.value = true
        _state.update {
            it.copy(
                inProgress = false,
                endedAtMillis = nowMillis(),
            )
        }
    }

    fun isCancelRequested(): Boolean = cancelRequested.value

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
