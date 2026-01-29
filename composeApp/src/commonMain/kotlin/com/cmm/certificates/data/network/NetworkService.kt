package com.cmm.certificates.data.network

import kotlinx.coroutines.flow.StateFlow

const val NETWORK_UNAVAILABLE_MESSAGE = "No network connection."

expect class NetworkService() {
    val isNetworkAvailable: StateFlow<Boolean>

    fun refresh()
}
