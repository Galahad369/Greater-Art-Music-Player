package com.local.listentomusic.playback

import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** User-owned A55 media only; the APK does not include the videos. */
class StackPictureCorpusTest {
    /** Explicit fixture setup only; never run or loosen the corpus checks implicitly. */
    @Test fun provisionVerifiedPictureFilesAsTheActualAppUid() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("stackPictureProvision") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(requireNotNull(context.getExternalFilesDir(null)), "qa-stack-picture-20261105")
        assertTrue("Preserve any existing fixtures before provisioning", !folder.exists())
        val source = File("/sdcard/Download/GreaterArt-Alignment-QA-20261111")
        val names = listOf("Miku", "Riona", "unc", "Forte", "Yoshino", "Morisray")
        val hashes = listOf(
            "530c4f8c57ff6a56d92b4d459da63ec2d2124a2616d63de2d03799d0484552ca",
            "ba399a4ad3c080f3a1a476d61c2fd5bcde8928ce977cc9753523d7fc8802a3a2",
            "21735ed1ce37165c9d20e2a9feb94a79680f6fbddfb931f83127528343aab2c8",
            "2b912aa8c3f5c632f0a62c98d34f5591d9996c786e3e04815d449d7c85912a6f",
            "4ebbcca41deb929b5c79d6549978d84a9b80b6c8346790ea86f5f671792bd60f",
            "6e9c7d7bb1b04989328ee477cb4d3b2b62570a98fece5c7cc5ffa9e452142471",
        )
        fun digest(file: File): String {
            val sha = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    sha.update(buffer, 0, count)
                }
            }
            return sha.digest().joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 255) }
        }
        names.forEachIndexed { index, name ->
            org.junit.Assert.assertEquals("Original fixture hash $index", hashes[index], digest(File(source, "孤独毒毒-$name.mp4")))
        }
        assertTrue("App UID could not create its own fixture folder", folder.mkdir())
        names.forEachIndexed { index, name ->
            val destination = File(folder, "take-$index.mp4")
            assertTrue("Never overwrite a fixture", destination.createNewFile())
            File(source, "孤独毒毒-$name.mp4").inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            org.junit.Assert.assertEquals("Copied fixture hash $index", hashes[index], digest(destination))
        }
        android.util.Log.i("GA-StackPictureQA", "PROVISIONED verified6 uid=${android.os.Process.myUid()}")
    }

    @Test fun sameArtworkMatchesAcrossFrameRatesButDifferentArtworkAbstains() = checkCorpus(StackPictureSearch.WIDE)

    @Test fun quickSearchPreservesTheSameMatches() = checkCorpus(StackPictureSearch.QUICK)

    private fun checkCorpus(search: StackPictureSearch): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("stackPictureCorpus") == "true")
        val context = instrumentation.targetContext
        val folder = File(requireNotNull(context.getExternalFilesDir(null)), "qa-stack-picture-20261105")
        val tracks = (0..5).map { index ->
            val file = File(folder, "take-$index.mp4")
            assertTrue(
                "Provision picture fixture $index: ${file.absolutePath}; " +
                    "package=${context.packageName}, uid=${android.os.Process.myUid()}, " +
                    "parentReadable=${folder.canRead()}, exists=${file.exists()}",
                file.isFile,
            )
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
