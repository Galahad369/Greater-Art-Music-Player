package com.local.listentomusic.playback

import android.content.Context
import android.util.AtomicFile
import com.local.listentomusic.data.OfflineAnalysisBudget
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

internal data class StackInstrumentViews(val mono: StackInstrumentRegion, val side: StackInstrumentRegion?)

internal class StackInstrumentAlign(context: Context) {
    private val cache = File(context.applicationContext.cacheDir, "stack-align-v7-instruments")
    companion object { private val mutex = Mutex() }

    private suspend fun region(file: MediaFile, start: Long, length: Long): StackInstrumentViews = withContext(Dispatchers.IO) {
        mutex.withLock {
            val source = File(file.sourcePath)
            require(source.isFile && source.canRead() && MediaScanner.isInsideTarget(source))
            val identity = "7|${source.canonicalPath}|${source.length()}|${source.lastModified()}|${file.clipStartMs}|${file.clipEndMs}|$start|$length"
            val key = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
            cache.mkdirs()
            val target = File(cache, "$key.bin")
            val cached = runCatching {
                DataInputStream(target.inputStream().buffered()).use { readStackInstrumentViews(it, target.length(), start) }
            }.getOrNull()
            if (cached != null) { target.setLastModified(System.currentTimeMillis()); return@withLock cached }
            val active = coroutineContext
            val decoded = OfflineAnalysisBudget.mutex.withLock {
                val pcm = decodeStackPcmWindow(file, start, length)
                val mono = stackInstrumentFeatures(pcm.mono, start) { active.ensureActive() }
                val monoPower = pcm.mono.sumOf { it.toDouble() * it }
                val sidePower = pcm.side.sumOf { it.toDouble() * it }
                val side = if (stackPreferStereoSideSignal(monoPower, sidePower, pcm.stereo))
                    stackInstrumentFeatures(pcm.side, start) { active.ensureActive() } else null
                StackInstrumentViews(mono, side)
            }
            active.ensureActive()
            val atomic = AtomicFile(target)
            var output: java.io.FileOutputStream? = null
            try {
                output = atomic.startWrite()
                val data = DataOutputStream(output.buffered())
                writeStackInstrumentViews(data, decoded); data.flush(); atomic.finishWrite(output)
                cache.listFiles()?.filter { it.extension == "bin" }?.sortedByDescending { it.lastModified() }
                    ?.drop(48)?.forEach { it.delete() }
            } catch (_: java.io.IOException) { output?.let { atomic.failWrite(it) } }
            finally { output?.let { runCatching { it.close() } } }
            decoded
        }
    }

    suspend fun estimate(primary: MediaFile, companion: MediaFile): StackAlignment = withContext(Dispatchers.Default) {
        val duration = primary.durationMs
        val starts = stackRegionStarts(duration)
        if (starts.isEmpty()) return@withContext StackAlignment(0, 0.0, false)
        val monoAnchors = ArrayList<StackInstrumentAnchor>()
        val sideAnchors = ArrayList<StackInstrumentAnchor>()
        val transientTimes = ArrayList<Long>()
        var allSide = true
        for (start in starts) {
            coroutineContext.ensureActive()
            val searchStart = (start - STACK_SEARCH_MS).coerceAtLeast(0)
            val searchEnd = minOf(companion.durationMs, start + STACK_REGION_MS + STACK_SEARCH_MS)
            if (searchEnd - searchStart < STACK_REGION_MS) continue
            val a = region(primary, start, STACK_REGION_MS)
            val b = region(companion, searchStart, searchEnd - searchStart)
            val active = coroutineContext
            val monoMatch = stackMatchInstrumentRegion(a.mono, b.mono) { active.ensureActive() }
            monoMatch?.let(monoAnchors::add)
            val attack = (20 until a.mono.percussion.size - 20).maxByOrNull { a.mono.percussion[it] }
            if (attack != null) transientTimes += start + attack * STACK_INSTRUMENT_HOP_MS
            if (a.side != null && b.side != null) {
                val sideMatch = stackMatchInstrumentRegion(a.side, b.side) { active.ensureActive() }
                sideMatch?.let(sideAnchors::add)
                if (com.local.listentomusic.BuildConfig.DEBUG)
                    android.util.Log.d("StackAlignV7", "anchor=$start mono=$monoMatch side=$sideMatch")
            } else allSide = false
        }
        val mono = stackFitInstrumentMap(monoAnchors, duration)
        val side = if (allSide) stackFitInstrumentMap(sideAnchors, duration) else mono
        if (com.local.listentomusic.BuildConfig.DEBUG)
            android.util.Log.d("StackAlignV7", "map mono=$mono side=$side sideRequired=$allSide")
        // Preserve v6's independent same-domain second opinion, now over the entire clipped timeline.
        if (!mono.confident || !side.confident || abs(mono.offsetUs - side.offsetUs) > 40_000 ||
            abs((mono.timeScale - side.timeScale) * duration) > 40) return@withContext StackAlignment(0, 0.0, false)
        // Each refinement uses <=400ms original-rate PCM, released before the next anchor.
        val corrections = ArrayList<Double>()
        for (time in transientTimes.take(4)) {
            val companionTime = side.timeScale * time + side.offsetUs / 1000
            val aStart = time - 200
            val bStart = companionTime.toLong() - 200
            if (aStart < 0 || bStart < 0 || aStart + 400 > duration || bStart + 400 > companion.durationMs) continue
            val correction = try { OfflineAnalysisBudget.mutex.withLock {
                val a = withContext(Dispatchers.IO) { decodeStackPcmWindow(primary, aStart, 400, null) }
                val b = withContext(Dispatchers.IO) { decodeStackPcmWindow(companion, bStart, 400, null) }
                val rate = minOf(a.sampleRate, b.sampleRate)
                val aSamples = if (allSide) a.side else a.mono
                val bSamples = if (allSide) b.side else b.mono
                val active = coroutineContext
                stackNativeLagUs(
                    stackNativeAttack(stackResampleAnalysis(aSamples, a.sampleRate, rate), rate),
                    stackNativeAttack(stackResampleAnalysis(bSamples, b.sampleRate, rate), rate), rate,
                ) { active.ensureActive() }?.plus((bStart - aStart - (companionTime - time)) * 1000)
            } } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Optional native-rate refinement must not erase a valid distributed map.
                null
            }
            if (correction != null) corrections += correction
        }
        if (corrections.size >= 3 && corrections.maxOrNull()!! - corrections.minOrNull()!! <= 4000) {
            val offsetUs = side.offsetUs + corrections.sorted()[corrections.size / 2]
            if (abs(offsetUs) <= 30_000_000)
                side.copy(offsetMs = kotlin.math.round(offsetUs / 1000).toLong(), offsetUs = offsetUs)
            else side
        } else side
    }
}

internal fun writeStackInstrumentViews(output: DataOutputStream, views: StackInstrumentViews) {
    output.writeInt(7); output.writeInt(views.mono.percussion.size); output.writeBoolean(views.side != null)
    fun write(region: StackInstrumentRegion) {
        region.percussion.forEach(output::writeFloat); region.bass.forEach(output::writeFloat)
        region.chroma.forEach { it.forEach(output::writeFloat) }
    }
    write(views.mono); views.side?.let(::write)
}

internal fun readStackInstrumentViews(input: DataInputStream, length: Long, start: Long): StackInstrumentViews {
    require(input.readInt() == 7)
    val n = input.readInt(); val hasSide = input.readBoolean()
    require(n in 150..1800 && length == 9L + n * 14L * 4 * (if (hasSide) 2 else 1))
    fun read(): StackInstrumentRegion {
        fun floats(count: Int) = FloatArray(count) { input.readFloat().also { require(it.isFinite() && abs(it) < 1e9) } }
        return StackInstrumentRegion(start, floats(n), floats(n), Array(n) { floats(12) })
    }
    return StackInstrumentViews(read(), if (hasSide) read() else null)
}
