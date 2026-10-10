package com.local.listentomusic.playback

import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** User-owned local recordings only. Logs abstentions, never equates a match with audible lock. */
class StackBackingCorpusTest {
    @Test fun reportBakaCandidateEvidence(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun media(name: String): MediaFile {
            val source = File("/sdcard/Download", name)
            val retriever = MediaMetadataRetriever()
            val duration = try { retriever.setDataSource(source.canonicalPath)
                requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toLong()
            } finally { retriever.release() }
            return MediaFile(source.canonicalPath, source.name, duration, source.length(), source.lastModified(), MediaKind.VIDEO)
        }
        val primary = media("Baka self cover.mp4")
        val companion = media("オリジナルMV馬鹿 歌いましたAdo.mp4")
        val aligner = StackInstrumentAlign(context)
        val mono = ArrayList<List<StackInstrumentAnchor>>()
        val side = ArrayList<List<StackInstrumentAnchor>>()
        for (start in stackRegionStarts(primary.durationMs)) {
            val search = (start - STACK_SEARCH_MS).coerceAtLeast(0)
            val end = minOf(companion.durationMs, start + STACK_REGION_MS + STACK_SEARCH_MS)
            if (end - search < STACK_REGION_MS) continue
            val a = aligner.region(primary, start, STACK_REGION_MS)
            val b = aligner.region(companion, search, end - search)
            mono += stackInstrumentCandidates(a.mono, b.mono, minimumScore = .4)
            if (a.side != null && b.side != null) side += stackInstrumentCandidates(a.side, b.side, minimumScore = .4)
            Log.i("GreaterArtBackingCorpus", "WEAK Baka region=$start mono=${mono.last()} side=${side.lastOrNull()}")
        }
        Log.i("GreaterArtBackingCorpus", "WEAK Baka mono=${stackFitInstrumentCandidates(mono, primary.durationMs)} side=${stackFitInstrumentCandidates(side, primary.durationMs)}; diagnostic only")
        Unit
    }

    @Test fun reportFourBackingFamilies() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter { it.extension == "mp4" }
        val requested = InstrumentationRegistry.getArguments().getString("family")
        val families = listOf("ビターチョコデコレーション", "Baka", "ギャンブル", "孤独毒毒")
            .filter { requested == null || it == requested }
        assertTrue("Unknown corpus family", families.isNotEmpty())
        val aligner = StackAudioAlign(context)
        val limit = InstrumentationRegistry.getArguments().getString("takes")?.toIntOrNull()?.coerceIn(2, 8) ?: 6
        for (family in families) {
            val takes = sources.filter {
                it.name.contains(family, ignoreCase = true) || (family == "Baka" && it.name.contains("馬鹿"))
            }.sortedBy { if (it.name.contains("self", true) || it.name.startsWith("初音ミク")) "0" else it.name }.take(limit)
            assertTrue("Missing local $family takes; do not silently skip", takes.size >= 2)
            fun media(source: File): MediaFile {
                val retriever = MediaMetadataRetriever()
                val duration = try {
                    retriever.setDataSource(source.canonicalPath)
                    requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toLong()
                } finally { retriever.release() }
                return MediaFile(source.canonicalPath, source.name, duration, source.length(), source.lastModified(), MediaKind.VIDEO)
            }
            val primary = media(takes.first())
            for (source in takes.drop(1)) {
                val before = SystemClock.elapsedRealtime()
                val match = aligner.estimate(primary, media(source))
                Log.i("GreaterArtBackingCorpus", "family=$family primary=${takes.first().name} take=${source.name} result=$match analysisMs=${SystemClock.elapsedRealtime() - before}")
                assertTrue(match.offsetUs.isFinite() && match.timeScale.isFinite())
                val warmStart = SystemClock.elapsedRealtime()
                assertTrue("Persisted decision changed", match == StackAudioAlign(context).estimate(primary, media(source)))
                Log.i("GreaterArtBackingCorpus", "warm family=$family take=${source.name} analysisMs=${SystemClock.elapsedRealtime() - warmStart}")
            }
        }
    }
}
