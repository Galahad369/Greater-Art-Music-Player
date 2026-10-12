package art.greater.desktop

import com.sun.jna.Pointer
import com.local.listentomusic.graph.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Canvas
import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class Voice(val media: Media, val player: Pointer, val gain: Int = 70, val muted: Boolean = false, val solo: Boolean = false)
data class DesktopState(val library: List<Media> = emptyList(), val roots: List<Path> = emptyList(),
    val favorites: Set<String> = emptySet(), val current: Media? = null, val queue: List<Media> = emptyList(),
    val playback: Playback = Playback(), val graph: LibraryGraph? = null, val voices: List<Voice> = emptyList(),
    val speed: Float = 1f, val volume: Int = 80, val repeat: Boolean = true, val busy: Boolean = false, val error: String? = null)

/** One session owns all windows and voices; panes never create players. */
class DesktopSession(val store: DesktopStore, val engine: NativePlayback) : AutoCloseable {
    val state = MutableStateFlow(DesktopState(roots = store.roots(), favorites = store.favorites()))
    val primary = engine.create()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null
    private var previousNativeState = 0
    private var surfaceReady = false
    private var pendingMedia: Media? = null
    init {
        engine.volume(primary, state.value.volume)
        engine.executor.scheduleWithFixedDelay({
            // Window closure queues native release on this same executor. A periodic
            // poll must never dereference the player after shutdown has begun.
            if (engine.closed) return@scheduleWithFixedDelay
            runCatching {
                val snapshot = engine.snapshot(primary)
                update { it.copy(playback = snapshot) }
                if (snapshot.state == 6 && previousNativeState != 6) {
                    if (state.value.repeat) {
                        engine.stop(primary); engine.play(primary)
                        state.value.voices.forEach { engine.stop(it.player); engine.play(it.player) }
                    }
                    else next(1)
                }
                previousNativeState = snapshot.state
            }.onFailure { failure(it) }
        }, 0, 150, TimeUnit.MILLISECONDS)
        rescan()
    }
    private fun update(block: (DesktopState) -> DesktopState) = synchronized(state) { state.value = block(state.value) }
    private fun failure(error: Throwable) { update { it.copy(error = error.message ?: "Playback failed") } }
    private fun command(block: () -> Unit) { if (!engine.closed) engine.executor.submit { runCatching(block).onFailure(::failure) } }
    fun clearError() = update { it.copy(error = null) }
    fun roots(roots: List<Path>) { store.setRoots(roots.distinct()); update { it.copy(roots = roots.distinct()) }; rescan() }
    fun rescan() {
        scanJob?.cancel()
        scanJob = ioScope.launch {
            update { it.copy(busy = true) }
            try {
                val files = LocalLibrary.scan(state.value.roots)
                ensureActive()
                val nodes = files.map { GraphInput(it.id, it.name) }
                val edges = GraphBuilder.edges(nodes)
                val graph = LibraryGraph(nodes, edges, GraphLayout.settle(nodes, edges))
                update { it.copy(library = files, graph = graph, busy = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { failure(error); update { it.copy(busy = false) } }
        }
    }
    fun attach(canvas: Canvas) = command {
        engine.attach(primary, canvas)
        surfaceReady = true
        pendingMedia?.let { pendingMedia = null; startMedia(it) }
    }
    fun play(media: Media) = command {
        if (!surfaceReady) pendingMedia = media else startMedia(media)
    }
    private fun startMedia(media: Media) {
        state.value.voices.forEach { engine.release(it.player) }
        engine.load(primary, media.id)
        engine.volume(primary, state.value.volume); engine.speed(primary, state.value.speed)
        engine.play(primary)
        update { it.copy(current = media, queue = it.library, voices = emptyList(), error = null) }
    }
    fun toggle() = command {
        val active = state.value.playback.playing
        engine.pause(primary, active)
        state.value.voices.forEach { engine.pause(it.player, active) }
    }
    fun stop() = command { engine.stop(primary); state.value.voices.forEach { engine.stop(it.player) } }
    fun next(direction: Int) = command {
        val snapshot = state.value
        val queue = snapshot.queue
        if (queue.isNotEmpty()) {
            val index = queue.indexOfFirst { it.id == snapshot.current?.id }
            val target = queue[Math.floorMod(index + direction, queue.size)]
            snapshot.voices.forEach { engine.release(it.player) }
            engine.load(primary, target.id); engine.play(primary)
            engine.volume(primary, snapshot.volume)
            engine.speed(primary, snapshot.speed)
            update { it.copy(current = target, voices = emptyList()) }
        }
    }
    fun seek(time: Long) = command { engine.seek(primary, time); state.value.voices.forEach { engine.seek(it.player, time) } }
    fun speed(rate: Float) = command {
        check(engine.speed(primary, rate)) { "Speed is unsupported for this source" }
        state.value.voices.forEach { check(engine.speed(it.player, rate)) }
        update { it.copy(speed = rate) }
    }
    fun volume(gain: Int) = command { engine.volume(primary, gain); update { it.copy(volume = gain) } }
    fun repeat() = update { it.copy(repeat = !it.repeat) }
    fun favorite(media: Media) {
        val updated = state.value.favorites.toMutableSet().apply { if (!add(media.id)) remove(media.id) }
        store.setFavorites(updated); update { it.copy(favorites = updated) }
    }
    fun addVoice(media: Media) = command {
        if (state.value.voices.size >= 7) return@command
        val player = engine.create()
        try {
            engine.load(player, media.id, audioOnly = true); engine.volume(player, 70)
            engine.speed(player, state.value.speed); engine.play(player)
            engine.seek(player, state.value.playback.time)
            if (!state.value.playback.playing) engine.pause(player, true)
            update { it.copy(voices = it.voices + Voice(media, player)) }
        } catch (error: Exception) { engine.release(player); throw error }
    }
    fun removeVoice(voice: Voice) = command { engine.release(voice.player); update { it.copy(voices = it.voices - voice) } }
    fun voice(voice: Voice, gain: Int = voice.gain, mute: Boolean = voice.muted, solo: Boolean = voice.solo) = command {
        update { it.copy(voices = it.voices.map { old -> if (old.player == voice.player) old.copy(gain = gain, muted = mute, solo = solo) else old }) }
        val voices = state.value.voices
        val solos = voices.any { it.solo }
        voices.forEach { engine.volume(it.player, if (it.muted || solos && !it.solo) 0 else it.gain) }
        engine.volume(primary, if (solos) 0 else state.value.volume)
    }
    override fun close() { ioScope.cancel(); engine.close() }
}
