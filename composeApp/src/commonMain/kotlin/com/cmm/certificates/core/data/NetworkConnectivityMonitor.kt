package com.cmm.certificates.core.data

import com.cmm.certificates.core.domain.ConnectivityMonitor
import com.cmm.certificates.data.network.NetworkService

class NetworkConnectivityMonitor(
    private val networkService: NetworkService,
) : ConnectivityMonitor {
    override val isNetworkAvailable = networkService.isNetworkAvailable

    override fun refresh() {
        networkService.refresh()
    }
}
