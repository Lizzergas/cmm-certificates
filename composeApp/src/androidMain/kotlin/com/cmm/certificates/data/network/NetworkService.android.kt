package com.cmm.certificates.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class NetworkService {
    private val _isNetworkAvailable = MutableStateFlow(true)
    actual val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    actual fun refresh() = Unit
}
