package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class StackSyncControlTest {
    // ---- noise estimate ---------------------------------------------------------------------------------

    private fun noisy(sigma: Double, count: Int, seed: Long, rampPerReadingMs: Double = 0.0): StackDriftNoise {
        val random = Random(seed)
        val noise = StackDriftNoise()
        repeat(count) { noise.add(Math.round(it * rampPerReadingMs + random.nextGaussian() * sigma).toDouble()) }
        return noise
    }

    @Test
    fun noiseIsUnknownUntilEnoughReadings() {
        assertNull(noisy(3.0, 11, 1).sigmaMs())
        assertNotNull(noisy(3.0, 12, 1).sigmaMs())
    }

    @Test
    fun noiseEstimateTracksTheRealJitter() {
        for (sigma in doubleArrayOf(1.0, 2.0, 4.0, 8.0)) {
            val estimates = (0 until 40L).map { noisy(sigma, 24, it).sigmaMs()!! }.sorted()
            val typical = estimates[estimates.size / 2]
            assertEquals("sigma=$sigma", sigma, typical, sigma * 0.35 + 0.3)
        }
    }

    @Test
    fun aSteadyTrimRampDoesNotLookLikeNoise() {
        // A 0.5% trim moves the error 0.25 ms per 50 ms tick; that must not widen the estimate.
        val flat = (0 until 40L).map { noisy(2.0, 24, it).sigmaMs()!! }.sorted()[20]
        val ramp = (0 until 40L).map { noisy(2.0, 24, it, rampPerReadingMs = 0.25).sigmaMs()!! }.sorted()[20]
        assertEquals(flat, ramp, 0.5)
    }

    @Test
    fun oneOutlierDoesNotInflateTheEstimate() {
        val clean = noisy(1.0, 24, 5)
        val withOutlier = noisy(1.0, 24, 5).also { it.add(60.0) }
        assertEquals(clean.sigmaMs()!!, withOutlier.sigmaMs()!!, 0.6)
    }

    @Test
    fun nonFiniteReadingsAreIgnored() {
        val noise = noisy(2.0, 24, 3)
        val before = noise.sigmaMs()!!
        noise.add(Double.NaN); noise.add(Double.POSITIVE_INFINITY)
        assertEquals(before, noise.sigmaMs()!!, 1e-9)
    }

    // ---- dead zone and actions ---------------------------------------------------------------------------

    @Test
    fun deadZoneIsClampedBetweenResolvableAndTheOldFixedValue() {
        assertEquals(STACK_MAX_DEADBAND_MS, stackAdaptiveDeadbandMs(null), 0.0)
        assertEquals(STACK_MAX_DEADBAND_MS, stackAdaptiveDeadbandMs(Double.NaN), 0.0)
        assertEquals(STACK_MIN_DEADBAND_MS, stackAdaptiveDeadbandMs(0.0), 0.0)
        assertEquals(3.0, stackAdaptiveDeadbandMs(2.0), 1e-9)
        assertEquals(STACK_MAX_DEADBAND_MS, stackAdaptiveDeadbandMs(50.0), 0.0)
        assertTrue(STACK_MAX_DEADBAND_MS <= STACK_SYNC_DEADBAND_MS.toDouble())
    }

    @Test
    fun withTheOldDeadZoneTheDecisionsAreExactlyTheOriginal() {
        for (drift in -600..600 step 3) {
            for (sinceSeek in longArrayOf(0L, 1_999L, 2_000L, 10_000L)) {
                assertEquals(
                    "drift=$drift since=$sinceSeek",
                    stackSyncAction(drift.toDouble(), sinceSeek),
                    stackSyncActionFor(drift.toDouble(), sinceSeek, STACK_SYNC_DEADBAND_MS.toDouble()),
                )
            }
        }
    }

    @Test
    fun trimIsProportionalFinelyQuantizedAndClamped() {
        assertEquals(1f, stackRateTrimFor(2.4, 2.5), 0f)
        assertEquals(1f, stackRateTrimFor(Double.NaN, 2.5), 0f)
        assertEquals(0.996f, stackRateTrimFor(8.0, 2.5), 1e-6f)   // ahead: slow down
        assertEquals(1.004f, stackRateTrimFor(-8.0, 2.5), 1e-6f)  // behind: speed up
        assertEquals(0.998f, stackRateTrimFor(3.1, 2.5), 1e-6f)   // a small error gets a small trim, not a 0.5% step
        assertEquals(1f - STACK_MAX_RATE_TRIM, stackRateTrimFor(5_000.0, 2.5), 1e-6f)
        assertEquals(1f + STACK_MAX_RATE_TRIM, stackRateTrimFor(-5_000.0, 2.5), 1e-6f)
    }

    @Test
    fun releaseAndApplyRules() {
        assertTrue("Float rounding must not suppress a two-step trim", stackTrimWorthApplying(1f, 0.998f))
        assertTrue(stackTrimShouldRelease(1.25, 2.5))
        assertFalse(stackTrimShouldRelease(1.3, 2.5))
        assertTrue(stackTrimWorthApplying(1.002f, 1f))
        assertFalse(stackTrimWorthApplying(0.998f, 0.999f))
        assertTrue(stackTrimWorthApplying(0.998f, 0.996f))
        assertTrue(needsDwell(12.0))
        assertFalse(needsDwell(12.1))
    }

    @Test
    fun dwellNeedsConsecutiveReadingsAndResetsOnAnyGap() {
        val dwell = StackTrimDwell()
        repeat(STACK_TRIM_DWELL_TICKS - 1) { assertFalse(dwell.confirm(true)) }
        assertTrue(dwell.confirm(true))
        assertFalse(dwell.confirm(false))
        repeat(STACK_TRIM_DWELL_TICKS - 1) { assertFalse(dwell.confirm(true)) }
        assertTrue(dwell.confirm(true))
        dwell.reset()
        assertFalse(dwell.confirm(true))
    }

    // ---- closed loop -------------------------------------------------------------------------------------

    @Test
    fun dwellRestartsWhenDriftChangesDirection() {
        val dwell = StackTrimDwell()
        repeat(STACK_TRIM_DWELL_TICKS - 1) { assertFalse(dwell.confirm(true, 1)) }
        assertFalse(dwell.confirm(true, -1))
        repeat(STACK_TRIM_DWELL_TICKS - 2) { assertFalse(dwell.confirm(true, -1)) }
        assertTrue(dwell.confirm(true, -1))
    }

    private class Outcome(val errors: List<Double>, val toggles: Int, val seconds: Int) {
        fun percentile(p: Double) = errors.sorted()[((errors.size - 1) * p).toInt()]
        val togglesPerMinute get() = toggles * 60.0 / seconds
    }

    /** Mirrors StackPlaybackCoordinator.syncVoice for one voice: EMA of integer-ms readings, trims every >= 200 ms. */
    private fun simulate(adaptive: Boolean, seed: Long, sigmaMs: Double, startErrorMs: Double, seconds: Int = 70): Outcome {
        val random = Random(seed)
        val noise = StackDriftNoise(); val dwell = StackTrimDwell()
        var trueError = startErrorMs; var rate = 1f; var ema = 0.0; var has = false
        var lastRate = -10_000L; var toggles = 0; var t = 0L
        val errors = ArrayList<Double>()
        while (t < seconds * 1000L) {
            trueError += (rate - 1f) * STACK_SYNC_TICK_MS
            t += STACK_SYNC_TICK_MS
            if (t < STACK_SETTLE_MS) continue
            val reading = Math.round(trueError + random.nextGaussian() * sigmaMs).toDouble()
            noise.add(reading)
            ema = if (has) ema * 0.6 + reading * 0.4 else reading; has = true
            if (adaptive) {
                val deadband = stackAdaptiveDeadbandMs(noise.sigmaMs())
                when (stackSyncActionFor(ema, 10_000L, deadband)) {
                    StackSyncAction.RATE -> {
                        val confirmed = if (rate == 1f && needsDwell(ema)) dwell.confirm(true, if (ema > 0.0) 1 else -1) else true
                        if (confirmed && t - lastRate >= STACK_RATE_UPDATE_MS) {
                            lastRate = t
                            val wanted = stackRateTrimFor(ema, deadband)
                            if (wanted != rate && stackTrimWorthApplying(rate, wanted)) { if ((rate == 1f) != (wanted == 1f)) toggles++; rate = wanted }
                        }
                    }
                    StackSyncAction.NONE -> {
                        dwell.confirm(false)
                        if (rate != 1f && stackTrimShouldRelease(ema, deadband)) { rate = 1f; toggles++ }
                    }
                    StackSyncAction.SEEK -> {}
                }
            } else {
                when (stackSyncAction(ema, 10_000L)) {
                    StackSyncAction.RATE -> if (t - lastRate >= STACK_RATE_UPDATE_MS) {
                        lastRate = t
                        val wanted = stackRateTrim(ema)
                        if (wanted != rate) { if ((rate == 1f) != (wanted == 1f)) toggles++; rate = wanted }
                    }
                    StackSyncAction.NONE -> if (rate != 1f && abs(ema) <= STACK_SYNC_DEADBAND_MS / 2.0) { rate = 1f; toggles++ }
                    StackSyncAction.SEEK -> {}
                }
            }
            if (t >= 20_000L) errors += abs(trueError)
        }
        return Outcome(errors, toggles, seconds)
    }

    private fun runs(adaptive: Boolean, sigma: Double): List<Outcome> {
        val start = Random(7)
        return (0 until 60).map { simulate(adaptive, 1_000L + it, sigma, (start.nextDouble() * 2 - 1) * 30.0) }
    }

    private fun pooled(outcomes: List<Outcome>, p: Double) = outcomes.flatMap { it.errors }.sorted().let { it[((it.size - 1) * p).toInt()] }

    @Test
    fun theOriginalControllerLeavesAVoiceSeveralMillisecondsOff() {
        // Documents the problem this file fixes, even with perfect readings.
        assertTrue(pooled(runs(false, 0.0), 0.5) > 4.0)
        assertTrue(pooled(runs(false, 0.0), 0.95) > 9.0)
    }

    @Test
    fun adaptiveControlConvergesFarTighterWhenReadingsAreClean() {
        for (sigma in doubleArrayOf(0.0, 1.0, 2.0)) {
            val shipped = runs(false, sigma)
            val adaptive = runs(true, sigma)
            assertTrue("sigma=$sigma median ${pooled(adaptive, .5)} vs ${pooled(shipped, .5)}", pooled(adaptive, .5) < pooled(shipped, .5) / 2.0)
            assertTrue("sigma=$sigma p95 ${pooled(adaptive, .95)}", pooled(adaptive, .95) <= 5.0)
        }
    }

    @Test
    fun adaptiveControlDoesNotChaseNoiseOrTrimMoreOften() {
        for (sigma in doubleArrayOf(0.0, 2.0, 4.0)) {
            val shipped = runs(false, sigma).map { it.togglesPerMinute }.average()
            val adaptive = runs(true, sigma).map { it.togglesPerMinute }.average()
            assertTrue("sigma=$sigma toggles/min adaptive=$adaptive shipped=$shipped", adaptive <= shipped + 1.5)
        }
    }

    @Test
    fun veryNoisyReadingsFallBackToTheOldBehaviourAndAreNeverWorse() {
        val shipped = runs(false, 8.0); val adaptive = runs(true, 8.0)
        assertTrue(pooled(adaptive, .95) <= pooled(shipped, .95) + 0.5)
        assertTrue(adaptive.flatMap { it.errors }.maxOrNull()!! <= STACK_MAX_DEADBAND_MS)
    }
}
