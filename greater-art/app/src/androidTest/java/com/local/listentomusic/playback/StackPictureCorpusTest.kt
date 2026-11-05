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
    @Test fun sameArtworkMatchesAcrossFrameRatesButDifferentArtworkAbstains(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("stackPictureCorpus") == "true")
        val context = instrumentation.targetContext
        val folder = File(requireNotNull(context.getExternalFilesDir(null)), "qa-stack-picture-20261105")
        val tracks = (0..5).map { index ->
            val file = File(folder, "take-$index.mp4")
            assertTrue("Provision picture fixture $index", file.isFile)
            MediaFile(file.path, file.name, 188_000L, file.length(), file.lastModified(), MediaKind.VIDEO)
        }
        val matches = StackPictureAlign(context).estimateAll(tracks.first(), tracks.drop(1)) { index ->
            android.util.Log.i("GA-StackPictureQA", "analysed $index / 5")
        }
        val result = tracks.drop(1).map { matches[it.path]?.offsetUs }
        android.util.Log.i("GA-StackPictureQA", "offsetsUs=$result")
        assertNull("Different video must not change the track", result[0])
        for (i in listOf(1, 2, 4)) {
            assertTrue("Take ${i + 1} should match within 2 ms of zero", result[i]?.let { kotlin.math.abs(it) <= 2_000.0 } == true)
        }
        assertTrue("Mixed-cadence take should match at +12 to +28 ms", result[3]?.let { it in 12_000.0..28_000.0 } == true)
    }
}
