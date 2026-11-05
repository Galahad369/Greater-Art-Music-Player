package com.local.listentomusic.playback

import android.content.Context
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StackAlignmentMethod { SOUND, PICTURE }

data class StackAlignmentProgress(val running: Boolean = false, val completed: Int = 0,
    val total: Int = 0, val matched: Int? = null, val failed: Boolean = false,
    val primaryPath: String? = null, val paths: List<String> = emptyList(),
    val method: StackAlignmentMethod = StackAlignmentMethod.SOUND,
    val proposal: Map<String, StackAlignment> = emptyMap(),
    val applied: Boolean = false,
) {
    internal fun belongsTo(session: StackSession): Boolean =
        session.active && primaryPath == session.primaryPath && paths == session.slots.map { it.file.path }
}

/** Analysis belongs to the playback session, not a disposable pager page. */
object StackAlignmentController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(StackAlignmentProgress())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun start(context: Context, method: StackAlignmentMethod) {
        if (job?.isActive == true) return
        val snapshot = StackPlayback.state.value
        val primary = snapshot.slots.firstOrNull { it.file.path == snapshot.primaryPath } ?: return
        if (method == StackAlignmentMethod.PICTURE && primary.file.kind != MediaKind.VIDEO) return
        val companions = snapshot.slots.filter {
            it.file.path != snapshot.primaryPath && it.error == null &&
                (method == StackAlignmentMethod.SOUND || it.file.kind == MediaKind.VIDEO)
        }
        if (companions.isEmpty()) return
        if (snapshot.playing) StackPlayback.pause()
        val intent = StackPlayback.transportRevision
        fun unchanged() = StackPlayback.state.value.let {
            it.active && it.primaryPath == snapshot.primaryPath &&
                it.slots.map { slot -> slot.file.path } == snapshot.slots.map { slot -> slot.file.path }
        }
        mutable.value = StackAlignmentProgress(running = true, total = companions.size,
            primaryPath = snapshot.primaryPath, paths = snapshot.slots.map { it.file.path }, method = method)
        job = scope.launch {
            try {
                coroutineScope {
                    val watcher = launch {
                        StackPlayback.state.collect {
                            if (!unchanged() || StackPlayback.transportRevision != intent) this@coroutineScope.cancel()
                        }
                    }
                    try {
                        val files = companions.map { it.file }
                        val progress: (Int) -> Unit = { index ->
                            mutable.value = mutable.value.copy(completed = index)
                        }
                        val matches = when (method) {
                            StackAlignmentMethod.SOUND -> StackAudioAlign(context.applicationContext)
                                .estimateAll(primary.file, files, progress)
                            StackAlignmentMethod.PICTURE -> StackPictureAlign(context.applicationContext)
                                .estimateAll(primary.file, files, progress)
                        }
                        ensureActive()
                        if (unchanged() && StackPlayback.transportRevision == intent) {
                            val proposal = matches.filterValues { it.confident }
                            mutable.value = mutable.value.copy(running = false, completed = companions.size,
                                matched = proposal.size, proposal = proposal)
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

    fun apply() {
        val pending = mutable.value
        if (pending.running || pending.proposal.isEmpty()) return
        if (!pending.belongsTo(StackPlayback.state.value)) {
            discard()
            return
        }
        StackPlayback.setAlignments(pending.proposal)
        mutable.value = pending.copy(proposal = emptyMap(), applied = true)
    }

    fun discard() {
        if (!mutable.value.running) mutable.value = StackAlignmentProgress()
    }
}
