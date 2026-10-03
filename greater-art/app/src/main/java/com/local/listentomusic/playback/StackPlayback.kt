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
    val offsetMs: Long = 0L,
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

internal fun stackDuration(slots: List<StackSlot>, primaryPath: String? = slots.firstOrNull()?.file?.path): Long =
    slots.firstOrNull { it.file.path == primaryPath }?.let {
        (it.resolvedDurationMs.takeIf { duration -> duration > 0L } ?: it.file.durationMs).coerceAtLeast(0L)
    } ?: 0L
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

/** New Stack sessions loop the staged set until the user turns loop off. */
internal const val STACK_LOOP_DEFAULT = true

internal fun shouldRestartStack(loopEnabled: Boolean, playing: Boolean, positionMs: Long, durationMs: Long): Boolean =
    loopEnabled && playing && durationMs > 0L && positionMs >= durationMs

internal data class StackSeekPlan(val positionMs: Long, val seekPrimary: Boolean)

/** A primary event is an acknowledgement, not a new primary seek command. */
internal fun stackSeekPlan(positionMs: Long, durationMs: Long, primaryEvent: Boolean): StackSeekPlan =
    StackSeekPlan(
        if (durationMs > 0L) positionMs.coerceIn(0L, durationMs) else positionMs.coerceAtLeast(0L),
        !primaryEvent,
    )

internal fun persistedRepeatMode(stackActive: Boolean, repeatBeforeStack: Int, currentRepeat: Int): Int =
    if (stackActive) repeatBeforeStack else currentRepeat

/** Preserve Stack order while omitting the primary already shown by normal Now Playing chrome. */
internal fun stackSecondarySlots(slots: List<StackSlot>, primaryPath: String?): List<StackSlot> =
    slots.filterNot { it.file.path == primaryPath }

/** Stack transport uses the loop glyph, never shuffle / repeat-one / playlist-repeat. */
internal fun stackTransportUsesLoopIcon(stackCount: Int): Boolean = stackCount > 0

internal const val STACK_START_ALIGNMENT_MS = 30L
internal const val STACK_DRIFT_CORRECTION_MS = 100L
internal const val STACK_CORRECTION_INTERVAL_MS = 750L

/** Drift under this is inaudible as an echo; leave the voice at the master rate. */
internal const val STACK_SYNC_DEADBAND_MS = 12L
/** Only drift this large is worth a seek (and its rebuffer); smaller drift is rate-nudged. */
internal const val STACK_HARD_RESYNC_MS = 400L
internal const val STACK_SEEK_COOLDOWN_MS = 2_000L
/** Ignore position readings right after start/seek while AudioTrack timestamps settle. */
internal const val STACK_SETTLE_MS = 350L
internal const val STACK_RATE_UPDATE_MS = 200L
/** Pitch-preserving rate trim limit. ±5% closes 100 ms in ~2 s without audible warble. */
internal const val STACK_MAX_RATE_TRIM = 0.05f
/** Start gate waits for every voice to be READY, but never longer than this. */
internal const val STACK_START_GATE_TIMEOUT_MS = 2_500L
internal const val STACK_MAX_SEEK_LEAD_MS = 400L
internal const val STACK_INITIAL_SEEK_LEAD_MS = 150L
/** A parked voice starts once the master is this close to its parked position. */
internal const val STACK_ENTRY_LEAD_MS = 25L
internal const val STACK_SYNC_TICK_MS = 50L

internal enum class StackSyncAction { NONE, RATE, SEEK }

/** Positive drift means the voice is ahead of where the master says it should be. */
internal fun stackSyncAction(driftMs: Double, msSinceLastSeek: Long): StackSyncAction = when {
    kotlin.math.abs(driftMs) >= STACK_HARD_RESYNC_MS && msSinceLastSeek >= STACK_SEEK_COOLDOWN_MS -> StackSyncAction.SEEK
    kotlin.math.abs(driftMs) > STACK_SYNC_DEADBAND_MS -> StackSyncAction.RATE
    else -> StackSyncAction.NONE
}

/**
 * Proportional rate trim, quantized to 0.5% steps so playback parameters are not
 * rewritten on every noisy reading. A voice ahead of the master slows down.
 */
internal fun stackRateTrim(driftMs: Double): Float {
    if (!driftMs.isFinite() || kotlin.math.abs(driftMs) <= STACK_SYNC_DEADBAND_MS) return 1f
    val raw = (-driftMs / 2_000.0).coerceIn(-STACK_MAX_RATE_TRIM.toDouble(), STACK_MAX_RATE_TRIM.toDouble())
    val quantized = kotlin.math.round(raw * 200.0) / 200.0
    return (1.0 + quantized).toFloat()
}

/** Learn how far a voice falls behind during a seek so the next seek lands on time. */
internal fun stackNextSeekLead(currentLeadMs: Long, residualDriftMs: Double): Long {
    if (!residualDriftMs.isFinite()) return currentLeadMs
    return (currentLeadMs - residualDriftMs * 0.6).toLong().coerceIn(0L, STACK_MAX_SEEK_LEAD_MS)
}

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
    internal var offsetsCommand: ((Map<String, Long>) -> Unit)? = null
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
    fun setOffsets(offsets: Map<String, Long>) { offsetsCommand?.invoke(offsets) }
    fun setOffset(path: String, offsetMs: Long) = setOffsets(mapOf(path to offsetMs))
    fun stop() { stopCommand?.invoke() }
    internal fun publish(session: StackSession) { mutable.value = session }
    internal fun detach() {
        startCommand = null; addCommand = null; removeCommand = null; primaryCommand = null
        volumeCommand = null; muteCommand = null; soloCommand = null
        playCommand = null; pauseCommand = null; seekCommand = null; loopCommand = null; offsetsCommand = null; stopCommand = null
        mutable.value = StackSession()
    }
}
