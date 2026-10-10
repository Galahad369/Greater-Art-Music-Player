package com.local.listentomusic.playback

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

/*
 * Stack voice sync control with a noise-adaptive dead zone.
 *
 * The original controller (stackSyncAction / stackRateTrim) acts only when the smoothed drift
 * exceeds a fixed 12 ms and stops trimming at 6 ms, so a voice can sit anywhere inside +-12 ms for
 * as long as it plays. Two copies of one instrumental that far apart sound hollow (comb filter)
 * and flam on drums. A fixed small dead zone is not an answer either: position readings are
 * integer milliseconds with unknown jitter, and a dead zone below that jitter makes the loop chase
 * noise with speed changes.
 *
 * So the dead zone is measured: it is 1.5 sigma of the voice's own reading noise (the EMA the
 * coordinator uses has half that sigma, so this is ~3 sigma of what is compared), clamped between
 * a floor the integer-ms readings can resolve and the old 12 ms. Until enough readings exist, or
 * if the readings are very noisy, the trigger threshold stays at the old 12 ms.
 * Fine trim quantization still differs; this is not a sample-output clock.
 */

/** Below this, integer-ms position readings cannot tell drift from rounding and jitter. */
internal const val STACK_MIN_DEADBAND_MS = 2.5

/** Never looser than the original fixed dead zone. */
internal const val STACK_MAX_DEADBAND_MS = 12.0

private const val NOISE_WINDOW = 24
private const val NOISE_MIN_SAMPLES = 12
private const val DEADBAND_PER_SIGMA = 1.5

/** Trim resolution for small corrections. The original 0.5% step could not express a 1-3 ms/s trim. */
private const val FINE_TRIM_STEP = 0.001

/** Online estimate of how noisy one voice's position readings are. */
internal class StackDriftNoise {
    private val ring = DoubleArray(NOISE_WINDOW)
    private var count = 0
    private var next = 0

    fun reset() {
        count = 0
        next = 0
    }

    fun add(reading: Double) {
        if (!reading.isFinite()) return
        ring[next] = reading
        next = (next + 1) % NOISE_WINDOW
        if (count < NOISE_WINDOW) count++
    }

    /**
     * Robust sigma of a single reading, or null until there are enough of them. It comes from the
     * median absolute deviation of successive differences, so a steady ramp (an active speed
     * trim) shifts the differences without widening them, and one outlier cannot inflate it.
     */
    fun sigmaMs(): Double? {
        if (count < NOISE_MIN_SAMPLES) return null
        val start = if (count < NOISE_WINDOW) 0 else next
        val differences = DoubleArray(count - 1)
        var previous = ring[start]
        for (i in 1 until count) {
            val value = ring[(start + i) % NOISE_WINDOW]
            differences[i - 1] = value - previous
            previous = value
        }
        val center = median(differences)
        val spread = median(DoubleArray(differences.size) { abs(differences[it] - center) })
        return 1.4826 * spread / sqrt(2.0)
    }

    private fun median(values: DoubleArray): Double {
        val sorted = values.sortedArray()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }
}

/** 600 ms of same-direction drift; 400 ms exceeded the model's noise-activation budget after Float rounding was fixed. */
internal const val STACK_TRIM_DWELL_TICKS = 12

/**
 * Starts a fine trim only after the drift has been outside the dead zone for [requiredTicks]
 * consecutive readings. Errors beyond the old 12 ms trigger bypass it (see [needsDwell]).
 */
internal class StackTrimDwell(private val requiredTicks: Int = STACK_TRIM_DWELL_TICKS) {
    private var run = 0
    private var direction = 0

    fun reset() {
        run = 0
        direction = 0
    }

    fun confirm(outsideDeadzone: Boolean, driftDirection: Int = 0): Boolean {
        if (!outsideDeadzone || (direction != 0 && driftDirection != 0 && direction != driftDirection)) run = 0
        direction = if (outsideDeadzone) driftDirection else 0
        run = if (outsideDeadzone) (run + 1).coerceAtMost(requiredTicks) else 0
        return run >= requiredTicks
    }
}

/** Only drifts inside the old dead zone are small enough to be mistaken for noise. */
internal fun needsDwell(driftMs: Double): Boolean = abs(driftMs) <= STACK_MAX_DEADBAND_MS

/** A new trim value is worth applying when it changes the speed by >= 0.2% or returns to 1.0. */
internal fun stackTrimWorthApplying(current: Float, wanted: Float): Boolean =
    wanted == 1f || round(abs(wanted - current).toDouble() * 1_000.0) >= 2.0

/** Dead zone for a voice whose readings have noise [sigmaMs]; null means "not known yet". */
internal fun stackAdaptiveDeadbandMs(sigmaMs: Double?): Double =
    if (sigmaMs == null || !sigmaMs.isFinite()) STACK_MAX_DEADBAND_MS
    else (DEADBAND_PER_SIGMA * sigmaMs).coerceIn(STACK_MIN_DEADBAND_MS, STACK_MAX_DEADBAND_MS)

/** Same decision table as [stackSyncAction], with the dead zone supplied instead of fixed. */
internal fun stackSyncActionFor(driftMs: Double, msSinceLastSeek: Long, deadbandMs: Double): StackSyncAction = when {
    abs(driftMs) >= STACK_HARD_RESYNC_MS && msSinceLastSeek >= STACK_SEEK_COOLDOWN_MS -> StackSyncAction.SEEK
    abs(driftMs) > deadbandMs -> StackSyncAction.RATE
    else -> StackSyncAction.NONE
}

/**
 * Proportional trim like [stackRateTrim], but quantized to 0.1% so a 3-10 ms error gets a 0.1-0.5%
 * trim (1-5 ms/s) instead of being rounded to either nothing or a full 0.5% step.
 */
internal fun stackRateTrimFor(driftMs: Double, deadbandMs: Double): Float {
    if (!driftMs.isFinite() || abs(driftMs) <= deadbandMs) return 1f
    val raw = (-driftMs / 2_000.0).coerceIn(-STACK_MAX_RATE_TRIM.toDouble(), STACK_MAX_RATE_TRIM.toDouble())
    val quantized = round(raw / FINE_TRIM_STEP) * FINE_TRIM_STEP
    return (1.0 + quantized).toFloat()
}

/** The trim stays on until the drift is comfortably inside the dead zone. */
internal fun stackTrimShouldRelease(driftMs: Double, deadbandMs: Double): Boolean = abs(driftMs) <= deadbandMs / 2.0
