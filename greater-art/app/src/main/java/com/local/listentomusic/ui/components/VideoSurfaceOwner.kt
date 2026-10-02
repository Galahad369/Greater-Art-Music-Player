package com.local.listentomusic.ui.components

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow

/** Authoritative primary presentation. Wallpaper uses a different player entirely. */
object VideoSurfaceOwner {
    private data class Candidate(val view: WeakReference<PlayerView>, val player: WeakReference<Player>, val owner: String)
    private val candidates = mutableMapOf<String, Candidate>()
    private var active = WeakReference<PlayerView>(null)
    private var foreground = false
    private var nowPlaying = false
    private var pip = false
    private var fullscreenActivity = false
    private var handoffTarget: String? = null
    private val systemOverlayOwners = linkedMapOf<String, String>()
    private var unifiedExpanded = false
    private var noOpReconciles = 0
    internal val state = MutableStateFlow(SurfaceLease())
    val expectedOwner: String
        get() = handoffTarget ?: expectedSurfaceOwner(
            foreground = foreground,
            nowPlaying = nowPlaying,
            pip = pip,
            fullscreenActivity = fullscreenActivity,
            systemOverlayOwner = systemOverlayOwners.values.lastOrNull(),
        )
    val systemOverlayActive: Boolean get() = systemOverlayOwners.isNotEmpty()
    val expandedOverlayActive: Boolean
        get() = unifiedExpanded || fullscreenActivity || systemOverlayOwners.containsValue("NOW_PLAYING")
    fun setUnifiedExpanded(value: Boolean) {
        if (unifiedExpanded == value) return
        unifiedExpanded = value
        com.local.listentomusic.playback.PlayerWindowVisibility.expanded(expandedOverlayActive)
    }
    fun setActivityForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        log("foreground=$value expected=$expectedOwner", active.get())
        reconcile()
    }
    fun setFullscreenActivityVisible(value: Boolean) {
        if (fullscreenActivity == value) return
        fullscreenActivity = value
        com.local.listentomusic.playback.PlayerWindowVisibility.expanded(expandedOverlayActive)
        log("fullscreenActivity=$value expected=$expectedOwner", active.get())
        reconcile()
    }
    fun setPresentation(nowPlayingVisible: Boolean, pictureInPicture: Boolean) {
        val changed = nowPlaying != nowPlayingVisible || pip != pictureInPicture
        if (!changed) return
        nowPlaying = nowPlayingVisible
        pip = pictureInPicture
        log("presentationRequested=$expectedOwner", active.get())
        reconcile()
    }
    fun setSystemOverlayVisible(token: String, owner: String, visible: Boolean) {
        val changed = if (visible) {
            val previous = systemOverlayOwners.put(token, owner)
            previous != owner
        } else {
            systemOverlayOwners.remove(token) != null
        }
        if (changed) {
            com.local.listentomusic.playback.PlayerWindowVisibility.expanded(expandedOverlayActive)
            log("systemOverlay=$visible token=$token owner=$owner expected=$expectedOwner", active.get())
            reconcile()
        }
    }
    fun beginHandoff(targetOwner: String) {
        if (handoffTarget == targetOwner) return
        handoffTarget = targetOwner
        log("handoffBegin target=$targetOwner", active.get())
        reconcile()
    }
    fun finishHandoff(targetOwner: String) {
        if (handoffTarget != targetOwner) return
        handoffTarget = null
        log("handoffFinish target=$targetOwner expected=$expectedOwner", active.get())
        reconcile()
    }
    fun attach(player: Player?, target: PlayerView, overlay: Boolean = false) {
        if (player == null) { detach(target); return }
        val owner = if (overlay) "MINI_WINDOW" else if (target.tag == "NOW_PLAYING") "NOW_PLAYING" else "LIBRARY_MINI"
        val registered = candidates[owner]
        if (registered?.view?.get() === target &&
            registered.player.get() === player &&
            expectedOwner == owner &&
            state.value.owner == owner &&
            active.get() === target &&
            target.player === player
        ) return
        if (registered?.view?.get() !== target) log("register requestedOwner=$owner expected=$expectedOwner", target)
        candidates[owner] = Candidate(WeakReference(target), WeakReference(player), owner)
        reconcile()
    }
    private fun reconcile() {
        val previous = active.get()
        val candidate = candidates[expectedOwner]
        val target = candidate?.view?.get()
        val player = candidate?.player?.get()
        if (target == null || player == null) {
            // Mini -> app is a direct handoff to Now Playing. Keep the working overlay
            // surface until the NOW_PLAYING PlayerView has actually registered instead
            // of briefly dropping through LIBRARY_MINI / no-surface ownership.
            if (shouldRetainPrimarySurfaceDuringHandoff(
                    currentOwner = state.value.owner,
                    expectedOwner = expectedOwner,
                    expectedCandidateReady = false,
                )
            ) {
                log("handoff-wait expected=$expectedOwner", previous)
                return
            }
            if (previous != null && state.value.owner != expectedOwner) {
                previous.player = null
                active.clear()
                state.value = state.value.detach(System.identityHashCode(previous), SystemClock.elapsedRealtime())
                log("relinquish", previous)
            }
            return
        }
        if (previous === target && target.player === player) {
            noOpReconciles++
            return
        }
        val previousOwner = state.value.owner
        state.value = state.value.attach(candidate.owner, System.identityHashCode(target), SystemClock.elapsedRealtime(),
            System.identityHashCode(player), "expected-owner-change:$previousOwner->${candidate.owner}")
        // Media3 requires NEW before OLD for one Player. Different controllers have
        // independent surface caches, so clear the old controller first in that case.
        if (previous?.player === player) PlayerView.switchTargetView(player, previous, target)
        else { previous?.player = null; target.player = player }
        active = WeakReference(target)
        log("attach previousOwner=$previousOwner", target)
    }
    fun detach(view: PlayerView) {
        val wasRegistered = candidates.entries.removeAll { it.value.view.get() === view }
        val stale = active.get() !== view
        if (!stale || wasRegistered) {
            state.value = state.value.detach(System.identityHashCode(view), SystemClock.elapsedRealtime())
        }
        log("release requestedOwner=${view.tag} stale=$stale ignored=$stale", view)
        if (stale) return // No setter and no MediaController surface-clear command.
        view.player = null
        active.clear()
        reconcile()
    }
    fun mediaChanged(eventMs: Long, repeated: Boolean = false) { state.value = state.value.mediaChanged(eventMs, repeated) }
    fun serviceEvent(event: String) { log("MINI_WINDOW service=$event", active.get()) }
    fun decoder(name: String) { state.value = state.value.copy(decoder = name, codecError = null) }
    fun codecError(name: String) { state.value = state.value.copy(codecError = name) }
    fun dropped(count: Int) { state.value = state.value.copy(droppedFrames = state.value.droppedFrames + count) }
    fun firstFrame(output: Any, eventMs: Long) {
        val surface = active.get()?.videoSurfaceView
        // MediaController IPC can parcel a different Surface wrapper. Reference equality
        // with the View's Surface cannot prove identity. This is timestamp attribution.
        val match = surface != null
        state.value = state.value.frame(eventMs, match)
        log("firstFrame output=${System.identityHashCode(output)} attribution=renderer-time accepted=${match && eventMs >= state.value.sinceMs}", active.get())
    }
    fun describe(): String = state.value.let {
        "owner=${it.owner} expected=$expectedOwner generation=${it.generation} view=${it.view} player=${it.player} activeFirstFrame=${it.firstFrame} mediaFirstFrame=${it.mediaFirstFrame}\n" +
            "transition=${it.transitionReason} noOpReconciles=$noOpReconciles ownerFrames=${it.framesByOwner} lastFrameOwner=${it.lastFrameOwner} lastFrameGeneration=${it.lastFrameGeneration} lastFrameAtMs=${it.lastFrameAtMs} staleDetachesIgnored=${it.staleDetaches}\n" +
            "decoder=${it.decoder} codecError=${it.codecError ?: "none"} droppedFrames=${it.droppedFrames}"
    }
    private fun log(event: String, view: PlayerView?) {
        if (!com.local.listentomusic.BuildConfig.DEBUG) return
        val player = view?.player ?: active.get()?.player
        android.util.Log.d("GreaterArtSurface", "$event owner=${state.value.owner} generation=${state.value.generation} view=${System.identityHashCode(view)} player=${System.identityHashCode(player)} position=${player?.currentPosition} state=${player?.playbackState} playing=${player?.isPlaying}")
    }
}
