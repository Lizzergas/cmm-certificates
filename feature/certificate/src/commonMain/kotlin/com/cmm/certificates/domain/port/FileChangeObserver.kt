package com.cmm.certificates.domain.port

interface FileChangeObserver {
    fun watch(path: String, onChange: () -> Unit): FileChangeSubscription
}

interface FileChangeSubscription {
    fun cancel()
}

expect fun createFileChangeObserver(): FileChangeObserver
