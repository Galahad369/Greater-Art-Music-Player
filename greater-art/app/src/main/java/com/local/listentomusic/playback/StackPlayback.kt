package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A temporary combination of local files, not a playlist or saved preset. */
data class StackSlot(
    val file: MediaFile,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val error: String? = null,
    val resolvedDurationMs: Long = 0L,
)

data class StackSession(
    val slots: List<StackSlot> = emptyList(),
    val primaryPath: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playing: Boolean = false,
    val loopEnabled: Boolean = false,
) {
    val active: Boolean get() = slots.isNotEmpty()
}

internal fun stackDuration(slots: List<StackSlot>): Long = slots.maxOfOrNull { it.file.durationMs.coerceAtLeast(0L) } ?: 0L
internal fun canAddStackTrack(slots: List<StackSlot>, file: MediaFile): Boolean =
    slots.size < StackPlayback.MAX_TRACKS && slots.none { it.file.path == file.path }

internal fun stackAudibleTrackCount(slots: List<StackSlot>): Int {
    val anySolo = slots.any { it.solo && !it.muted && it.error == null }
    return slots.count { slot ->
        !slot.muted && slot.error == null && (!anySolo || slot.solo)
    }.coerceAtLeast(1)
}

internal fun stackAudibleVolume(slot: StackSlot, anySolo: Boolean, audibleCount: Int): Float =
    if (slot.muted || anySolo && !slot.solo || slot.error != null) 0f
    else slot.volume.coerceIn(0f, 1f) / audibleCount.coerceAtLeast(1)

internal fun stackMasterPosition(primaryPositionMs: Long, fallbackPositionMs: Long, primaryAvailable: Boolean): Long =
    (if (primaryAvailable) primaryPositionMs else fallbackPositionMs).coerceAtLeast(0L)

internal fun shouldCorrectStackVoice(voicePositionMs: Long, masterPositionMs: Long): Boolean =
    kotlin.math.abs(voicePositionMs - masterPositionMs) > STACK_DRIFT_CORRECTION_MS

internal fun shouldRestartStack(loopEnabled: Boolean, playing: Boolean, positionMs: Long, durationMs: Long): Boolean =
    loopEnabled && playing && durationMs > 0L && positionMs >= durationMs

internal fun persistedRepeatMode(stackActive: Boolean, repeatBeforeStack: Int, currentRepeat: Int): Int =
    if (stackActive) repeatBeforeStack else currentRepeat

internal const val STACK_START_ALIGNMENT_MS = 120L
internal const val STACK_DRIFT_CORRECTION_MS = 600L
internal const val STACK_CORRECTION_INTERVAL_MS = 5_000L

/** Main-thread commands are attached by the single PlaybackService. */
object StackPlayback {
    const val MAX_TRACKS = 8
    private val mutable = MutableStateFlow(StackSession())
    val state = mutable.asStateFlow()
    internal var startCommand: ((List<MediaFile>) -> Boolean)? = null
    internal var addCommand: ((MediaFile) -> Boolean)? = null
    internal var removeCommand: ((String) -> Unit)? = null
    internal var primaryCommand: ((String) -> Unit)? = null
    internal var volumeCommand: ((String, Float) -> Unit)? = null
    internal var muteCommand: ((String) -> Unit)? = null
    internal var soloCommand: ((String) -> Unit)? = null
    internal var playCommand: (() -> Unit)? = null
    internal var pauseCommand: (() -> Unit)? = null
    internal var seekCommand: ((Long) -> Unit)? = null
    internal var loopCommand: ((Boolean) -> Unit)? = null
    internal var stopCommand: (() -> Unit)? = null

    fun start(files: List<MediaFile>): Boolean = startCommand?.invoke(files) ?: false
    fun add(file: MediaFile): Boolean = addCommand?.invoke(file) ?: false
    fun remove(path: String) { removeCommand?.invoke(path) }
    fun setPrimary(path: String) { primaryCommand?.invoke(path) }
    fun setVolume(path: String, volume: Float) { volumeCommand?.invoke(path, volume) }
    fun toggleMute(path: String) { muteCommand?.invoke(path) }
    fun toggleSolo(path: String) { soloCommand?.invoke(path) }
    fun play() { playCommand?.invoke() }
    fun pause() { pauseCommand?.invoke() }
    fun seek(positionMs: Long) { seekCommand?.invoke(positionMs) }
    fun setLoop(enabled: Boolean) { loopCommand?.invoke(enabled) }
    fun stop() { stopCommand?.invoke() }
    internal fun publish(session: StackSession) { mutable.value = session }
    internal fun detach() {
        startCommand = null; addCommand = null; removeCommand = null; primaryCommand = null
        volumeCommand = null; muteCommand = null; soloCommand = null
        playCommand = null; pauseCommand = null; seekCommand = null; loopCommand = null; stopCommand = null
        mutable.value = StackSession()
    }
}
