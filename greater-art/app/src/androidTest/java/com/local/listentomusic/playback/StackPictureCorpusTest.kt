package com.local.listentomusic.playback

import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** User-owned A55 media only; the APK does not include the videos. */
class StackPictureCorpusTest {
    @Test fun sameArtworkMatchesAcrossFrameRatesButDifferentArtworkAbstains() = checkCorpus(StackPictureSearch.WIDE)

    @Test fun quickSearchPreservesTheSameMatches() = checkCorpus(StackPictureSearch.QUICK)

    private fun checkCorpus(search: StackPictureSearch): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("stackPictureCorpus") == "true")
        val context = instrumentation.targetContext
        val folder = File(requireNotNull(context.getExternalFilesDir(null)), "qa-stack-picture-20261105")
        val tracks = (0..5).map { index ->
            val file = File(folder, "take-$index.mp4")
            assertTrue("Provision picture fixture $index", file.isFile)
            MediaFile(file.path, file.name, 188_000L, file.length(), file.lastModified(), MediaKind.VIDEO)
        }
        val start = android.os.SystemClock.elapsedRealtime()
        val analysis = StackPictureAlign(context).estimateAll(tracks.first(), tracks.drop(1), search) { index ->
            android.util.Log.i("GA-StackPictureQA", "$search analysed $index / 5")
        }
        val result = tracks.drop(1).map { analysis.matches[it.path]?.offsetUs }
        android.util.Log.i("GA-StackPictureQA", "$search offsetsUs=$result elapsedMs=${android.os.SystemClock.elapsedRealtime() - start}")
        assertTrue("Corpus analysis should complete within the quick budget", !analysis.quickTimedOut)
        assertNull("Different video must not change the track", result[0])
        for (i in listOf(1, 2, 4)) {
            assertTrue("Take ${i + 1} should match within 2 ms of zero", result[i]?.let { kotlin.math.abs(it) <= 2_000.0 } == true)
        }
        assertTrue("Mixed-cadence take should match at +12 to +28 ms", result[3]?.let { it in 12_000.0..28_000.0 } == true)
    }
}
