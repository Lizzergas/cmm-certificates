package com.cmm.certificates.data.network

import kotlinx.coroutines.flow.StateFlow

expect class NetworkService() {
    val isNetworkAvailable: StateFlow<Boolean>

    fun refresh()
}
