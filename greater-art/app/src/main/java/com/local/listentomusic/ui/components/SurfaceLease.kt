package com.local.listentomusic.ui.components

internal fun expectedSurfaceOwner(
    foreground: Boolean,
    nowPlaying: Boolean,
    pip: Boolean,
    fullscreenActivity: Boolean = false,
    systemOverlayOwner: String? = null,
): String = when {
    // A real fullscreen Activity must outrank the hidden overlay service that
    // remains registered for the eventual return transition.
    fullscreenActivity -> "NOW_PLAYING"
    systemOverlayOwner != null -> systemOverlayOwner
    pip -> "NOW_PLAYING"
    !foreground -> "MINI_WINDOW"
    nowPlaying -> "NOW_PLAYING"
    else -> "LIBRARY_MINI"
}

internal fun shouldRetainPrimarySurfaceDuringHandoff(
    currentOwner: String,
    expectedOwner: String,
    expectedCandidateReady: Boolean,
): Boolean {
    if (expectedCandidateReady) return false
    return (currentOwner == "MINI_WINDOW" && expectedOwner == "NOW_PLAYING") ||
        (currentOwner == "NOW_PLAYING" && expectedOwner == "MINI_WINDOW") ||
        (currentOwner == "LIBRARY_MINI" && expectedOwner == "MINI_WINDOW") ||
        (currentOwner == "MINI_WINDOW" && expectedOwner == "LIBRARY_MINI")
}

internal fun shouldShowPlayerWindow(
    ready: Boolean,
    fullscreenActivity: Boolean,
    expanded: Boolean,
    docked: Boolean,
    dockedVisible: Boolean,
    detachedVisible: Boolean,
): Boolean = ready && !fullscreenActivity && when {
    expanded -> true
    docked -> dockedVisible
    else -> detachedVisible
}

/** Platform-independent ownership state. Calls are serialized on main. */
internal data class SurfaceLease(
    val owner: String = "NONE", val view: Int = 0, val generation: Long = 0,
    val player: Int = 0,
    val sinceMs: Long = 0, val firstFrame: Boolean = false,
    val mediaFirstFrame: Boolean = false, val lastFrameOwner: String = "NONE",
    val lastFrameGeneration: Long = -1, val lastFrameAtMs: Long = -1, val staleDetaches: Int = 0,
    val framesByOwner: Map<String, Boolean> = emptyMap(),
    val mediaSinceMs: Long = 0,
    val transitionReason: String = "initial", val noOpAttaches: Int = 0,
    val decoder: String = "unknown", val codecError: String? = null, val droppedFrames: Int = 0,
) {
    fun attach(name: String, identity: Int, now: Long, playerIdentity: Int = 0, reason: String = "reconcile"): SurfaceLease {
        if (owner == name && view == identity && (player == playerIdentity || playerIdentity == 0)) {
            return copy(transitionReason = "attach-noop:$reason", noOpAttaches = noOpAttaches + 1)
        }
        return copy(owner = name, view = identity, player = playerIdentity,
            generation = generation + 1, sinceMs = now, firstFrame = false,
            transitionReason = "attach:$reason", framesByOwner = framesByOwner + (name to false))
    }
    fun detach(identity: Int, now: Long): SurfaceLease = if (identity != view)
        copy(staleDetaches = staleDetaches + 1, transitionReason = "stale-detach")
    else copy(owner = "NONE", view = 0, player = 0,
            generation = generation + 1, sinceMs = now, firstFrame = false,
            transitionReason = "detach")
    fun mediaChanged(now: Long, repeated: Boolean = false): SurfaceLease {
        if (repeated) return this
        val nextGeneration = generation + 1
        val frameAlreadyRendered = owner != "NONE" &&
            firstFrame &&
            lastFrameOwner == owner &&
            lastFrameGeneration == generation &&
            lastFrameAtMs >= now
        return copy(
            generation = nextGeneration,
            sinceMs = now,
            firstFrame = frameAlreadyRendered,
            mediaFirstFrame = frameAlreadyRendered,
            lastFrameGeneration = if (frameAlreadyRendered) nextGeneration else lastFrameGeneration,
            framesByOwner = if (frameAlreadyRendered) mapOf(owner to true) else emptyMap(),
            mediaSinceMs = now,
            transitionReason = if (frameAlreadyRendered) "media-changed:frame-already-rendered" else "media-changed",
            codecError = null,
            droppedFrames = 0,
        )
    }
    fun frame(eventMs: Long, outputMatches: Boolean): SurfaceLease {
        if (eventMs < mediaSinceMs) return this
        if (owner == "NONE" || eventMs < sinceMs || !outputMatches) return copy(mediaFirstFrame = true)
        return copy(firstFrame = true, mediaFirstFrame = true, lastFrameOwner = owner,
            lastFrameGeneration = generation, lastFrameAtMs = eventMs,
            framesByOwner = framesByOwner + (owner to true))
    }
    fun warnings(expected: String, readyVideo: Boolean, now: Long, controllerMediaFirstFrame: Boolean = false): List<String> {
        if (!readyVideo || now - sinceMs < 2_500L) return emptyList()
        return buildList {
            if (codecError != null) add("VIDEO_CODEC_ERROR")
            if (owner == "NONE") add("NO_SURFACE_OWNER")
            else if (owner != expected) add("SURFACE_OWNER_MISMATCH")
            // MediaController's current-media callback is independent confirmation that
            // the decoder rendered. It prevents a renderer-listener registration race
            // from being misreported as a black frame.
            if (!firstFrame && !controllerMediaFirstFrame) {
                add("READY_VIDEO_NO_FRAME"); add("FIRST_FRAME_TIMEOUT")
            }
        }
    }
}
