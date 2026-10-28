package com.local.listentomusic.playback

import kotlin.math.ceil

/** Longest healthy take in master-frame coordinates, including delayed entry and native tails.
 * Muting a take does not shorten the session, matching the production Stack contract. */
internal fun pcmSessionEndFrames(slots: List<StackSlot>, specs: List<PcmSpec>, outputRate: Int): Long {
    require(slots.size == specs.size && slots.isNotEmpty() && outputRate in 8000..192000)
    return slots.indices.filter { slots[it].error == null }.maxOfOrNull { index ->
        val slot = slots[index]
        val spec = specs[index]
        val offsetUs = slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0
        require(slot.alignmentScale.isFinite() && slot.alignmentScale > 0 && offsetUs.isFinite())
        ceil((spec.durationFrames.toDouble() * outputRate / spec.rate - offsetUs * outputRate / 1_000_000) /
            slot.alignmentScale).toLong().coerceAtLeast(0)
    } ?: 0L
}
