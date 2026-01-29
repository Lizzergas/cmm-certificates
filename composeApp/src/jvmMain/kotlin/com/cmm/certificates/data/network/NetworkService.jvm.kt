package com.cmm.certificates.data.network

import java.net.NetworkInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NETWORK_POLL_INTERVAL_MS = 1_500L

actual class NetworkService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isNetworkAvailable = MutableStateFlow(checkNetwork())
    actual val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                val available = checkNetwork()
                if (available != _isNetworkAvailable.value) {
                    _isNetworkAvailable.value = available
                }
                delay(NETWORK_POLL_INTERVAL_MS)
            }
        }
    }

    actual fun refresh() {
        _isNetworkAvailable.value = checkNetwork()
    }

    private fun checkNetwork(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.any { address ->
                    !address.isLoopbackAddress && !address.isLinkLocalAddress
                }
                ?: false
        } catch (_: Exception) {
            false
        }
    }
}
