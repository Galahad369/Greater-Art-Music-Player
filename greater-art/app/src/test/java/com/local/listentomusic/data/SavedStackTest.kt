package com.local.listentomusic.data

import org.junit.Assert.*
import org.junit.Test

class SavedStackTest {
    @Test fun offsetsPersistAndLegacyFourFieldsRemainReadable() {
        val aligned = mix.copy(tracks = mix.tracks.mapIndexed { index, track -> track.copy(offsetMs = index * -1000L) })
        assertEquals(aligned, SavedStackCodec.decode(SavedStackCodec.encode(listOf(aligned))).single())
        val legacy = SavedStackCodec.encode(listOf(mix)).replace("~false~0", "~false").replace("~true~0", "~true")
        assertEquals(mix, SavedStackCodec.decode(legacy).single())
    }
    private val mix = SavedStack("mix", "鋼琴 | live 🎵", listOf(
        SavedStackTrack("/Download/一.mp3", .35f, muted = true),
        SavedStackTrack("/Download/a,b|c.mp4", .8f, solo = true)),
        "/Download/a,b|c.mp4", false, "live")

    @Test fun restoresTheMixNotJustItsSongNames() {
        assertEquals(listOf(mix), SavedStackCodec.decode(SavedStackCodec.encode(listOf(mix))))
    }
    @Test fun corruptRowDoesNotDiscardOtherStacks() {
        assertEquals(listOf(mix), SavedStackCodec.decode("broken\n" + SavedStackCodec.encode(listOf(mix))))
    }
    @Test fun missingPrimaryFallsBackToASavedTrack() {
        val restored = SavedStackCodec.decode(SavedStackCodec.encode(listOf(mix.copy(primaryPath = "missing"))))
        assertEquals(mix.tracks.first().path, restored.single().primaryPath)
    }
    @Test fun nonFiniteLevelsAreRejectedBeforeReachingThePlayer() {
        assertTrue(SavedStackCodec.decode(SavedStackCodec.encode(listOf(
            mix.copy(tracks = listOf(SavedStackTrack("/Download/x.mp3", Float.NaN)))))).isEmpty())
    }
    @Test fun deletingOneMixPreservesOthers() {
        val other = mix.copy(id = "other")
        val restored = SavedStackCodec.decode(SavedStackCodec.encode(listOf(mix, other)))
        assertEquals(listOf(other), SavedStackCodec.decode(SavedStackCodec.encode(restored.filterNot { it.id == mix.id })))
    }
}
