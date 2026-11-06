# A55 cold scan: 20 versus 30 fps picture analysis

**20 fps preserved the expected offsets and rejection in all four runs. It did not provide a consistent speedup.** It finished under 45 s in 4/4 runs; 30 fps finished in 3/4. Across completed scans the median difference was 0.227 s, much smaller than the paired swings. Keep the app unchanged.

## Cold scan times

A55 API 36 emulator, 1080×2340. One fresh process and analyzer per scan, with new decoders and no prior analysis results. Android's shared file cache was **not** flushed; input hashing read the files before the batch. This is process-cold analysis time, not cold storage I/O or APK startup. Pair order was fixed before the run: 30→20, 20→30, 30→20, 20→30.

| Pair | Order | 30 fps | 20 fps | Observation |
| --- | --- | ---: | ---: | --- |
| 1 | 30 → 20 | 35.711 s | 32.247 s | 20 fps 3.464 s faster |
| 2 | 20 → 30 | 36.067 s | 32.631 s | 20 fps 3.436 s faster |
| 3 | 30 → 20 | 33.436 s | 38.336 s | 20 fps 4.901 s slower |
| 4 | 20 → 30 | 45.029 s (timeout) | 40.585 s | Only 20 fps completed |
| Median of completed scans | — | 35.711 s (3/4) | 35.484 s (4/4) | Timeout excluded from complete-scan median |

The fourth 30 fps run returned after the unchanged 45 s limit while analysing Morislei. Its output slot was missing, **not** a second valid rejection. The time at cancellation is not a complete-scan timing. Completed times ranged from 33.436–36.067 s at 30 fps and 32.247–40.585 s at 20 fps. Windows free physical memory at launch ranged from 184 to 859 MiB; this batch does not isolate host scheduling or I/O effects.

## Accuracy and same-work check

All six `孤独毒毒` fixture sizes and SHA-256 values matched the previous A55 corpus receipt. On every complete scan, all 18 windows returned the independently expected picture counts. Both caps submitted 6,449 pictures and decoded 5,328 pictures within the windows, using `c2.android.avc.decoder` throughout with no fallback. The cap changes which decoded pictures are sampled; it does not skip decoder input.

| Take | Result, both caps | Decoded in windows | Retained at 30 fps | Retained at 20 fps |
| --- | --- | ---: | ---: | ---: |
| Hatsune Miku | Reference | 576 | 575 | 479 |
| Riona | Rejected | 1,080 | 720 | 720 |
| unc | 0 ms | 864 | 863 | 719 |
| Forte | 0 ms | 864 | 863 | 719 |
| Yoshino | +26 ms | 1,080 | 721 | 721 |
| Morislei | 0 ms | 864 | 863 | 719 |
| Total | All completed scans passed | **5,328** | **4,605** | **4,077** |

The original gate requires the Riona rejection, offsets within ±2 ms for unc/Forte/Morislei, and +12 to +28 ms for Yoshino. The runner also requires all six takes and all three windows per take, so a missing or unreadable clip cannot pass as a rejection. The 20 fps cap retained 528 fewer features (11.5%). The median sum of scoring time was 61.3 ms at 30 fps and 62.4 ms at 20 fps; the large timing swings were in acquisition (extract, decode, and sample), not matching.

## Scope

- Analyzer source: `553673a38f049dc90cfd081597b68b5656634878` (Android 1.22.12, same analyzer and scorer as `86ba54f`). In the standalone copy, only the sampling cap and measurement hooks differ. The matcher, quick windows, IDR seek handling, decode completeness checks, progress dispatch, and 45 s deadline were preserved. The local-file adapter calls the same `MediaExtractor.setDataSource(path)` as the app's local-file branch.
- One DEX ZIP, SHA-256 `00a562a62d69c0db426c1c134bb73d94d7d03d4af2650721eb443dc69f1210ca`, ran through Android's [standalone app process entry point](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/cmds/app_process/app_main.cpp). Both caps used the identical archive and the same six inputs.
- Installed APK SHA-256 `8799240c33a38b1a96d1c8aabb79e28611cb06c865c9c7b1c2014dffebec1c32` and AVD configuration hash `268f6b7b6d8ed87a09892c40cb4823adc266e1ef69989c782d20214e1df18906` matched before and after. No app install, source change, playback setting change, push, or merge. This tests the A55 emulator and these AVC clips only; it does not establish physical-device performance or APK release readiness.

## Reproduce the standalone run

1. Copy `StackPictureAlign.kt` and `StackPictureMath.kt` from the pinned revision into a scratch folder. Copy the `StackAlignment` data class declaration from that revision's `StackAlignmentMath.kt` into a separate file in the same package.
2. Apply the following patch to **the scratch copy** with `git apply --unidiff-zero` and add the three runner sources below.
3. Compile with cached Kotlin 2.4.21, JVM target 17, Android API 36 stubs, Kotlin stdlib 2.4.21, and `kotlinx-coroutines-core-jvm` / `kotlinx-coroutines-android` 1.10.2. Use Android SDK build-tools 37.0.0 D8 with `--min-api 26 --lib android.jar` and include the compiled probe classes and runtime JARs as inputs.
4. Verify the six local fixture hashes, transfer the DEX ZIP to a new path in the exact A55 AVD's `/data/local/tmp`, and make it read-only. For each cap in the schedule above, run `CLASSPATH=<read-only-DEX-ZIP> timeout 75s app_process /system/bin --nice-name=ga-fps-ab com.local.listentomusic.playback.A55SamplingBenchmark <20-or-30> <six-quoted-paths>` through `adb -s <A55-serial> shell`. Launch a new process every time. Preserve failed runs.

### Acquisition instrumentation patch

```diff
--- a/StackPictureAlign.kt
+++ b/StackPictureAlign.kt
@@ -10,0 +11 @@ import android.os.Build
+import android.os.SystemClock
@@ -24 +25,5 @@ import java.nio.ByteBuffer
-internal class StackPictureAlign(private val context: Context) {
+internal class StackPictureAlign(
+    private val context: Context,
+    private val sampleFps: Long,
+    private val probe: SamplingMetrics,
+) {
@@ -39,0 +45 @@ internal class StackPictureAlign(private val context: Context) {
+            val refBegin = SystemClock.elapsedRealtimeNanos()
@@ -40,0 +47 @@ internal class StackPictureAlign(private val context: Context) {
+            val refDecoded = SystemClock.elapsedRealtimeNanos()
@@ -41,0 +49 @@ internal class StackPictureAlign(private val context: Context) {
+            probe.finishTake(0, refDecoded - refBegin, SystemClock.elapsedRealtimeNanos() - refDecoded, reference.frames.size)
@@ -44,0 +53 @@ internal class StackPictureAlign(private val context: Context) {
+                val takeBegin = SystemClock.elapsedRealtimeNanos()
@@ -45,0 +55 @@ internal class StackPictureAlign(private val context: Context) {
+                val takeDecoded = SystemClock.elapsedRealtimeNanos()
@@ -48,0 +59 @@ internal class StackPictureAlign(private val context: Context) {
+                probe.finishTake(index + 1, takeDecoded - takeBegin, SystemClock.elapsedRealtimeNanos() - takeDecoded, frames.size)
@@ -119,0 +131 @@ internal class StackPictureAlign(private val context: Context) {
+                probe.codecs.add(decoder.name)
@@ -153,0 +166 @@ internal class StackPictureAlign(private val context: Context) {
+                    probe.windows++
@@ -226,0 +240 @@ internal class StackPictureAlign(private val context: Context) {
+                probe.queued++
@@ -241 +255,2 @@ internal class StackPictureAlign(private val context: Context) {
-                val bucket = (time - clipStartUs) * 30L / 1_000_000L
+                probe.decodedInWindow++
+                val bucket = (time - clipStartUs) * sampleFps / 1_000_000L
```

### Runner sources

`ProbeMediaAccess.kt`

```kotlin
package com.local.listentomusic.data

import android.content.Context
import android.media.MediaExtractor
import java.io.File
import kotlinx.coroutines.sync.Mutex

object OfflineAnalysisBudget { val mutex = Mutex() }

// This isolated runner accepts only validated local fixtures; same call as the app's LocalFile branch.
internal fun MediaExtractor.openMedia(@Suppress("UNUSED_PARAMETER") context: Context?, location: String) {
    require(File(location).isAbsolute)
    setDataSource(location)
}
```

`ProbeMediaFile.kt`

```kotlin
package com.local.listentomusic.model

internal data class MediaFile(
    val path: String,
    val durationMs: Long = 188_000L,
    val sourcePath: String = path,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long? = null,
)
```

`A55SamplingBenchmark.kt`

```kotlin
package com.local.listentomusic.playback

import android.content.ContextWrapper
import android.os.Build
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.local.listentomusic.model.MediaFile
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

internal class SamplingMetrics {
    data class Take(
        val index: Int, val acquireNs: Long, val scoreNs: Long,
        val kept: Int, val queued: Int, val decodedInWindow: Int,
        val windows: Int, val codecs: List<String>,
    )

    var queued = 0
    var decodedInWindow = 0
    var windows = 0
    val codecs = ArrayList<String>()
    val takes = ArrayList<Take>()

    fun finishTake(index: Int, acquireNs: Long, scoreNs: Long, kept: Int) {
        takes += Take(index, acquireNs, scoreNs, kept, queued, decodedInWindow, windows, codecs.toList())
        queued = 0
        decodedInWindow = 0
        windows = 0
        codecs.clear()
    }
}

object A55SamplingBenchmark {
    private val expectedDecoded = intArrayOf(576, 1080, 864, 864, 1080, 864)

    @JvmStatic
    @Suppress("DEPRECATION")
    fun main(args: Array<String>) {
        require(args.size == 7) { "Expected sample fps followed by six fixture paths" }
        val cap = args[0].toLong()
        require(cap == 20L || cap == 30L)
        check(Build.VERSION.SDK_INT == 36 && Build.HARDWARE == "ranchu" &&
            Build.SUPPORTED_ABIS.firstOrNull() == "x86_64") { "Wrong emulator type" }
        val tracks = args.drop(1).map { path ->
            check(File(path).isFile && File(path).canRead()) { "Fixture unavailable" }
            MediaFile(path)
        }
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper()
        check(Looper.myLooper() == Looper.getMainLooper())
        Thread({
            val code = try {
                runBlocking { runScan(cap, tracks) }
                0
            } catch (failure: Throwable) {
                failure.printStackTrace()
                1
            }
            exitProcess(code)
        }, "ga-picture-sampling").start()
        Looper.loop()
    }

    private suspend fun runScan(cap: Long, tracks: List<MediaFile>) {
        val pid = Process.myPid()
        val metrics = SamplingMetrics()
        val progress = ArrayList<Int>()
        val analyzer = StackPictureAlign(ContextWrapper(null), cap, metrics)
        println("BEGIN cap=$cap pid=$pid")
        val started = SystemClock.elapsedRealtimeNanos()
        val result = analyzer.estimateAll(tracks.first(), tracks.drop(1), StackPictureSearch.QUICK) { index ->
            progress += index
        }
        val elapsedNs = SystemClock.elapsedRealtimeNanos() - started
        val offsets = tracks.drop(1).map { result.matches[it.path]?.offsetUs }
        for (take in metrics.takes) {
            System.out.printf(Locale.ROOT,
                "TAKE index=%d acquireMs=%.3f scoreMs=%.3f kept=%d queued=%d decoded=%d windows=%d codecs=%s%n",
                take.index, take.acquireNs / 1e6, take.scoreNs / 1e6, take.kept, take.queued,
                take.decodedInWindow, take.windows, take.codecs.joinToString(","))
        }
        val offsetText = offsets.joinToString(",") {
            it?.let { us -> String.format(Locale.ROOT, "%.3f", us / 1_000.0) } ?: "rejected"
        }
        System.out.printf(Locale.ROOT,
            "RESULT cap=%d pid=%d elapsedMs=%.3f timedOut=%b progress=%s offsetsMs=%s%n",
            cap, pid, elapsedNs / 1e6, result.quickTimedOut, progress.joinToString(","), offsetText)
        check(!result.quickTimedOut && elapsedNs <= 45_000_000_000L) { "Quick deadline failed" }
        check(progress == (0..5).toList() && metrics.takes.size == 6) { "Incomplete corpus" }
        check(metrics.takes.withIndex().all { (index, take) ->
            take.index == index && take.windows == 3 && take.decodedInWindow == expectedDecoded[index] &&
                take.queued >= take.decodedInWindow && take.kept >= 24
        }) { "Incomplete video input/output; a rejection from missing data is not a pass" }
        check(offsets[0] == null) { "Different-artwork take must be rejected" }
        check(listOf(1, 2, 4).all { i -> offsets[i]?.let { abs(it) <= 2_000.0 } == true }) {
            "Same-artwork take exceeded +/-2 ms"
        }
        check(offsets[3]?.let { it in 12_000.0..28_000.0 } == true) { "Mixed-cadence take outside +12..+28 ms" }
        println("PASS cap=$cap pid=$pid")
    }
}
```

## A55 receipt

```text
Selected stdout from the completed A55 session. ADB transfer output and stack frames omitted.
PROFILE GreaterArt_A55_API36 API=36
Physical size: 1080x2340
CONFIG_BEFORE 268F6B7B6D8ED87A09892C40CB4823ADC266E1EF69989C782D20214E1DF18906
INSTALLED_APK_BEFORE 8799240c33a38b1a96d1c8aabb79e28611cb06c865c9c7b1c2014dffebec1c32
DEX_SHA256 00A562A62D69C0DB426C1C134BB73D94D7D03D4AF2650721EB443DC69F1210CA
FIXTURE index=0 bytes=43057797 sha256=530c4f8c57ff6a56d92b4d459da63ec2d2124a2616d63de2d03799d0484552ca
FIXTURE index=1 bytes=42942829 sha256=ba399a4ad3c080f3a1a476d61c2fd5bcde8928ce977cc9753523d7fc8802a3a2
FIXTURE index=2 bytes=40492410 sha256=21735ed1ce37165c9d20e2a9feb94a79680f6fbddfb931f83127528343aab2c8
FIXTURE index=3 bytes=49564177 sha256=2b912aa8c3f5c632f0a62c98d34f5591d9996c786e3e04815d449d7c85912a6f
FIXTURE index=4 bytes=34355525 sha256=4ebbcca41deb929b5c79d6549978d84a9b80b6c8346790ea86f5f671792bd60f
FIXTURE index=5 bytes=58375278 sha256=6e9c7d7bb1b04989328ee477cb4d3b2b62570a98fece5c7cc5ffa9e452142471
RUN order=1 pair=1 cap=30 hostFreeMiB=184 commitHeadroomGiB=7.37 utc=2026-11-06T09:08:16.0981082Z
BEGIN cap=30 pid=9317
TAKE index=0 acquireMs=6673.600 scoreMs=3.209 kept=575 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=7217.955 scoreMs=3.497 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5813.352 scoreMs=29.905 kept=863 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=4951.970 scoreMs=8.926 kept=863 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=6508.725 scoreMs=7.474 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=4440.997 scoreMs=10.943 kept=863 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=30 pid=9317 elapsedMs=35711.127 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=30 pid=9317
PROCESS order=1 cap=30 exit=0
RUN order=2 pair=1 cap=20 hostFreeMiB=419 commitHeadroomGiB=7.31 utc=2026-11-06T09:08:57.1104726Z
BEGIN cap=20 pid=9408
TAKE index=0 acquireMs=3969.897 scoreMs=3.165 kept=479 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=6508.987 scoreMs=2.503 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5307.894 scoreMs=26.926 kept=719 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=4638.040 scoreMs=9.793 kept=719 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=7102.932 scoreMs=10.073 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=4632.333 scoreMs=6.660 kept=719 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=20 pid=9408 elapsedMs=32246.732 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=20 pid=9408
PROCESS order=2 cap=20 exit=0
RUN order=3 pair=2 cap=20 hostFreeMiB=513 commitHeadroomGiB=7.75 utc=2026-11-06T09:09:30.5030654Z
BEGIN cap=20 pid=9500
TAKE index=0 acquireMs=3813.739 scoreMs=3.964 kept=479 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=7462.453 scoreMs=2.231 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5276.807 scoreMs=24.076 kept=719 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=4742.463 scoreMs=12.221 kept=719 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=6724.211 scoreMs=9.625 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=4520.357 scoreMs=9.864 kept=719 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=20 pid=9500 elapsedMs=32631.209 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=20 pid=9500
PROCESS order=3 cap=20 exit=0
RUN order=4 pair=2 cap=30 hostFreeMiB=679 commitHeadroomGiB=7.55 utc=2026-11-06T09:10:04.5222112Z
BEGIN cap=30 pid=9587
TAKE index=0 acquireMs=4974.453 scoreMs=3.861 kept=575 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=9634.448 scoreMs=2.984 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5921.017 scoreMs=27.716 kept=863 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=4963.409 scoreMs=8.639 kept=863 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=6009.294 scoreMs=7.846 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=4465.952 scoreMs=7.880 kept=863 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=30 pid=9587 elapsedMs=36067.258 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=30 pid=9587
PROCESS order=4 cap=30 exit=0
RUN order=5 pair=3 cap=30 hostFreeMiB=859 commitHeadroomGiB=7.41 utc=2026-11-06T09:10:42.2532303Z
BEGIN cap=30 pid=9672
TAKE index=0 acquireMs=4102.322 scoreMs=3.487 kept=575 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=6797.396 scoreMs=2.181 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5496.465 scoreMs=27.999 kept=863 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=4924.896 scoreMs=11.755 kept=863 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=7294.257 scoreMs=7.482 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=4726.936 scoreMs=8.353 kept=863 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=30 pid=9672 elapsedMs=33435.875 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=30 pid=9672
PROCESS order=5 cap=30 exit=0
RUN order=6 pair=3 cap=20 hostFreeMiB=840 commitHeadroomGiB=7.35 utc=2026-11-06T09:11:16.8675135Z
BEGIN cap=20 pid=9761
TAKE index=0 acquireMs=3965.803 scoreMs=3.779 kept=479 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=7473.017 scoreMs=2.622 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5892.917 scoreMs=25.527 kept=719 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=6493.811 scoreMs=12.631 kept=719 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=8955.813 scoreMs=9.879 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=5458.266 scoreMs=8.431 kept=719 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=20 pid=9761 elapsedMs=38336.414 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=20 pid=9761
PROCESS order=6 cap=20 exit=0
RUN order=7 pair=4 cap=20 hostFreeMiB=413 commitHeadroomGiB=7 utc=2026-11-06T09:11:56.7048220Z
BEGIN cap=20 pid=9847
TAKE index=0 acquireMs=4588.213 scoreMs=4.315 kept=479 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=7616.402 scoreMs=5.277 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=5747.428 scoreMs=26.235 kept=719 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=5159.559 scoreMs=11.215 kept=719 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=9098.719 scoreMs=12.107 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=5 acquireMs=8255.169 scoreMs=15.795 kept=719 queued=902 decoded=864 windows=3 codecs=c2.android.avc.decoder
RESULT cap=20 pid=9847 elapsedMs=40584.818 timedOut=false progress=0,1,2,3,4,5 offsetsMs=rejected,0.000,0.000,26.000,0.000
PASS cap=20 pid=9847
PROCESS order=7 cap=20 exit=0
RUN order=8 pair=4 cap=30 hostFreeMiB=626 commitHeadroomGiB=6.39 utc=2026-11-06T09:12:40.7574345Z
BEGIN cap=30 pid=9934
TAKE index=0 acquireMs=6108.535 scoreMs=4.627 kept=575 queued=772 decoded=576 windows=3 codecs=c2.android.avc.decoder
TAKE index=1 acquireMs=10010.383 scoreMs=6.059 kept=720 queued=1403 decoded=1080 windows=3 codecs=c2.android.avc.decoder
TAKE index=2 acquireMs=8316.930 scoreMs=30.017 kept=863 queued=1073 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=3 acquireMs=7605.476 scoreMs=19.422 kept=863 queued=904 decoded=864 windows=3 codecs=c2.android.avc.decoder
TAKE index=4 acquireMs=10085.654 scoreMs=13.169 kept=721 queued=1395 decoded=1080 windows=3 codecs=c2.android.avc.decoder
RESULT cap=30 pid=9934 elapsedMs=45029.429 timedOut=true progress=0,1,2,3,4 offsetsMs=rejected,0.000,0.000,26.000,rejected
java.lang.IllegalStateException: Quick deadline failed
PROCESS order=8 cap=30 exit=1
INSTALLED_APK_AFTER 8799240c33a38b1a96d1c8aabb79e28611cb06c865c9c7b1c2014dffebec1c32
CONFIG_AFTER 268F6B7B6D8ED87A09892C40CB4823ADC266E1EF69989C782D20214E1DF18906
RUNS_COMPLETE count=8 passed=7
```
