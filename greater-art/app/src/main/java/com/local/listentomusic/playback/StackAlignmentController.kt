package com.local.listentomusic.playback

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StackAlignmentProgress(val running: Boolean = false, val completed: Int = 0,
    val total: Int = 0, val matched: Int? = null, val failed: Boolean = false,
    val primaryPath: String? = null, val paths: List<String> = emptyList())

/** Analysis belongs to the playback session, not a disposable pager page. */
object StackAlignmentController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(StackAlignmentProgress())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        val snapshot = StackPlayback.state.value
        val primary = snapshot.slots.firstOrNull { it.file.path == snapshot.primaryPath } ?: return
        val companions = snapshot.slots.filterNot { it.file.path == snapshot.primaryPath || it.error != null }
        if (companions.isEmpty()) return
        if (snapshot.playing) StackPlayback.pause()
        val intent = StackPlayback.transportRevision
        fun unchanged() = StackPlayback.state.value.let {
            it.active && it.primaryPath == snapshot.primaryPath &&
                it.slots.map { slot -> slot.file.path } == snapshot.slots.map { slot -> slot.file.path }
        }
        mutable.value = StackAlignmentProgress(running = true, total = companions.size,
            primaryPath = snapshot.primaryPath, paths = snapshot.slots.map { it.file.path })
        job = scope.launch {
            try {
                coroutineScope {
                    val watcher = launch {
                        StackPlayback.state.collect {
                            if (!unchanged() || StackPlayback.transportRevision != intent) this@coroutineScope.cancel()
                        }
                    }
                    try {
                        val matches = StackAudioAlign(context.applicationContext).estimateAll(primary.file,
                            companions.map { it.file }) { index ->
                            mutable.value = mutable.value.copy(completed = index)
                        }
                        ensureActive()
                        if (unchanged() && StackPlayback.transportRevision == intent) {
                            val offsets = matches.filterValues { it.confident }.mapValues { it.value.offsetMs }
                            StackPlayback.setOffsets(offsets)
                            mutable.value = mutable.value.copy(running = false, completed = companions.size, matched = offsets.size)
                        }
                    } finally { watcher.cancel() }
                }
            } catch (_: CancellationException) {
                mutable.value = StackAlignmentProgress()
            } catch (failure: Exception) {
                com.local.listentomusic.diagnostics.CrashReports.recordRecoverable("stack-alignment", failure)
                mutable.value = mutable.value.copy(running = false, failed = true)
            } finally {
                if (snapshot.playing && unchanged() && StackPlayback.transportRevision == intent) StackPlayback.play()
                mutable.value = mutable.value.copy(running = false)
                job = null
            }
        }
    }

    fun cancel() { job?.cancel() }
}
