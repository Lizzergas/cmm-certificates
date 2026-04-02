package com.cmm.certificates.domain.port

actual fun createFileChangeObserver(): FileChangeObserver = NoOpFileChangeObserver

private data object NoOpFileChangeObserver : FileChangeObserver {
    override fun watch(path: String, onChange: () -> Unit): FileChangeSubscription {
        return NoOpFileChangeSubscription
    }
}

private data object NoOpFileChangeSubscription : FileChangeSubscription {
    override fun cancel() = Unit
}
