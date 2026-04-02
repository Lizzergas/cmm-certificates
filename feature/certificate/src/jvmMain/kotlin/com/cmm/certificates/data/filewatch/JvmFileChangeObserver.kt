package com.cmm.certificates.domain.port

import java.nio.file.ClosedWatchServiceException
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

actual fun createFileChangeObserver(): FileChangeObserver = JvmFileChangeObserver()

private class JvmFileChangeObserver : FileChangeObserver {
    override fun watch(path: String, onChange: () -> Unit): FileChangeSubscription {
        val targetPath = runCatching { Path.of(path).normalize() }.getOrNull()
            ?: return NoOpFileChangeSubscription
        val parent = targetPath.parent ?: return NoOpFileChangeSubscription
        val fileName = targetPath.fileName?.toString() ?: return NoOpFileChangeSubscription
        val watchService = runCatching { parent.fileSystem.newWatchService() }.getOrNull()
            ?: return NoOpFileChangeSubscription
        return try {
            parent.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            val active = AtomicBoolean(true)
            val worker = thread(
                start = true,
                isDaemon = true,
                name = "certificate-file-watch-$fileName",
            ) {
                try {
                    while (active.get()) {
                        val key = try {
                            watchService.take()
                        } catch (_: InterruptedException) {
                            break
                        } catch (_: ClosedWatchServiceException) {
                            break
                        }
                        var matched = false
                        key.pollEvents().forEach { event ->
                            val changed = event.context() as? Path ?: return@forEach
                            if (changed.fileName.toString() == fileName) {
                                matched = true
                            }
                        }
                        if (!key.reset()) {
                            break
                        }
                        if (matched && active.get()) {
                            onChange()
                        }
                    }
                } finally {
                    runCatching { watchService.close() }
                }
            }
            WatchServiceFileChangeSubscription(active, watchService, worker)
        } catch (_: Exception) {
            runCatching { watchService.close() }
            NoOpFileChangeSubscription
        }
    }
}

private data object NoOpFileChangeSubscription : FileChangeSubscription {
    override fun cancel() = Unit
}

private class WatchServiceFileChangeSubscription(
    private val active: AtomicBoolean,
    private val watchService: java.nio.file.WatchService,
    private val worker: Thread,
) : FileChangeSubscription {
    override fun cancel() {
        if (!active.getAndSet(false)) return
        runCatching { watchService.close() }
        worker.interrupt()
    }
}
