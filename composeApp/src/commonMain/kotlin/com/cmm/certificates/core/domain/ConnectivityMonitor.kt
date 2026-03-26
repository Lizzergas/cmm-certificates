package com.cmm.certificates.core.domain

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {
    val isNetworkAvailable: StateFlow<Boolean>

    fun refresh()
}
