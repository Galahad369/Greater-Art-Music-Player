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
    private val maps = File(context.applicationContext.cacheDir, "stack-align-v8-maps")
    companion object { private val mutex = Mutex(); private val mapMutex = Mutex() }

    suspend fun estimate(primary: MediaFile, companion: MediaFile): StackAlignment = withContext(Dispatchers.Default) {
        mapMutex.withLock {
            val target = withContext(Dispatchers.IO) {
                fun identity(file: MediaFile): String {
                    val source = File(file.sourcePath)
                    require(source.isFile && source.canRead() && MediaScanner.isInsideTarget(source))
                    return "${source.canonicalPath}|${source.length()}|${source.lastModified()}|${file.clipStartMs}|${file.clipEndMs}|${file.durationMs}"
                }
                val key = MessageDigest.getInstance("SHA-256").digest(
                    "8|${identity(primary)}\u0000${identity(companion)}".toByteArray())
                    .joinToString("") { "%02x".format(it) }
                maps.mkdirs()
                File(maps, "$key.bin")
            }
            val cached = withContext(Dispatchers.IO) {
                runCatching { DataInputStream(target.inputStream().buffered()).use { readStackAlignmentMap(it, target.length()) } }
                    .getOrNull()?.also { target.setLastModified(System.currentTimeMillis()) }
            }
            if (cached != null) return@withLock cached
            val result = calculate(primary, companion)
            coroutineContext.ensureActive()
            withContext(Dispatchers.IO) {
                val atomic = AtomicFile(target)
                var output: java.io.FileOutputStream? = null
                try {
                    output = atomic.startWrite()
                    val data = DataOutputStream(output.buffered())
                    writeStackAlignmentMap(data, result); data.flush(); atomic.finishWrite(output)
                    maps.listFiles()?.filter { it.extension == "bin" }?.sortedByDescending { it.lastModified() }
                        ?.drop(96)?.forEach { it.delete() }
                } catch (_: java.io.IOException) { output?.let { atomic.failWrite(it) } }
                finally { output?.let { runCatching { it.close() } } }
            }
            result
        }
    }

    internal suspend fun region(file: MediaFile, start: Long, length: Long): StackInstrumentViews = withContext(Dispatchers.IO) {
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

    private suspend fun calculate(primary: MediaFile, companion: MediaFile): StackAlignment = withContext(Dispatchers.Default) {
        val duration = primary.durationMs
        val starts = stackRegionStarts(duration)
        if (starts.isEmpty()) return@withContext StackAlignment(0, 0.0, false)
        val monoAnchors = ArrayList<List<StackInstrumentAnchor>>()
        val sideAnchors = ArrayList<List<StackInstrumentAnchor>>()
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
            val monoMatch = stackInstrumentCandidates(a.mono, b.mono) { active.ensureActive() }
            monoAnchors.add(monoMatch)
            val attack = (20 until a.mono.percussion.size - 20).maxByOrNull { a.mono.percussion[it] }
            if (attack != null) transientTimes += start + attack * STACK_INSTRUMENT_HOP_MS
            if (a.side != null && b.side != null) {
                val sideMatch = stackInstrumentCandidates(a.side, b.side) { active.ensureActive() }
                sideAnchors.add(sideMatch)
                if (com.local.listentomusic.BuildConfig.DEBUG)
                    android.util.Log.d("StackAlignV7", "anchor=$start mono=$monoMatch side=$sideMatch")
            } else allSide = false
        }
        val mono = stackFitInstrumentCandidates(monoAnchors, duration)
        val side = if (allSide) stackFitInstrumentCandidates(sideAnchors, duration) else mono
        if (com.local.listentomusic.BuildConfig.DEBUG)
            android.util.Log.d("StackAlignV7", "map mono=$mono side=$side sideRequired=$allSide")
        // A confident side map must agree. A weak side channel is not evidence of
        // disagreement, but replacing it requires strong distributed mono evidence
        // AND independent native-rate attack confirmation below.
        val nativeMonoRequired = allSide && !side.confident && mono.confident && mono.anchorCount >= 4 &&
            mono.correlation >= .6 && mono.residualMs <= 5
        if (!mono.confident || (!nativeMonoRequired && (!side.confident || abs(mono.offsetUs - side.offsetUs) > 40_000 ||
            abs((mono.timeScale - side.timeScale) * duration) > 40))) return@withContext StackAlignment(0, 0.0, false)
        val coarse = if (nativeMonoRequired) mono else side
        // Each refinement uses <=400ms original-rate PCM, released before the next anchor.
        val corrections = ArrayList<Double>()
        val supportedTimes = transientTimes.filterIndexed { index, _ ->
            monoAnchors[index].any { abs(it.companionMs - (coarse.timeScale * it.primaryMs + coarse.offsetUs / 1000)) <= 25 }
        }.take(6)
        for (time in supportedTimes) {
            val companionTime = coarse.timeScale * time + coarse.offsetUs / 1000
            val aStart = time - 200
            val bStart = companionTime.toLong() - 200
            if (aStart < 0 || bStart < 0 || aStart + 400 > duration || bStart + 400 > companion.durationMs) continue
            val correction = try { OfflineAnalysisBudget.mutex.withLock {
                val a = withContext(Dispatchers.IO) { decodeStackPcmWindow(primary, aStart, 400, null) }
                val b = withContext(Dispatchers.IO) { decodeStackPcmWindow(companion, bStart, 400, null) }
                val rate = minOf(a.sampleRate, b.sampleRate)
                val aSamples = if (allSide && !nativeMonoRequired) a.side else a.mono
                val bSamples = if (allSide && !nativeMonoRequired) b.side else b.mono
                val active = coroutineContext
                stackNativeLagUs(
                    stackNativeAttack(stackResampleAnalysis(aSamples, a.sampleRate, rate), rate),
                    stackNativeAttack(stackResampleAnalysis(bSamples, b.sampleRate, rate), rate), rate,
                ) { active.ensureActive() }?.plus((bStart - aStart - (companionTime - time)) * 1000)
            } } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Optional refinement preserves a dual-confirmed map. For mono-only
                // evidence, missing native confirmation instead makes us abstain.
                null
            }
            if (correction != null) corrections += correction
        }
        val refined = stackValidateNativeCorrections(coarse, corrections, nativeMonoRequired)
        if (com.local.listentomusic.BuildConfig.DEBUG)
            android.util.Log.d("StackAlignV8", "nativeRequired=$nativeMonoRequired correctionsUs=$corrections result=$refined")
        refined
    }
}

internal fun writeStackAlignmentMap(output: DataOutputStream, value: StackAlignment) {
    output.writeInt(8); output.writeLong(value.offsetMs); output.writeDouble(value.correlation)
    output.writeBoolean(value.confident); output.writeDouble(value.timeScale); output.writeDouble(value.offsetUs)
    output.writeInt(value.anchorCount); output.writeDouble(value.residualMs)
}

internal fun readStackAlignmentMap(input: DataInputStream, length: Long): StackAlignment {
    require(length == 49L && input.readInt() == 8)
    val value = StackAlignment(input.readLong(), input.readDouble(), input.readBoolean(), input.readDouble(),
        input.readDouble(), input.readInt(), input.readDouble())
    require(value.correlation.isFinite() && value.correlation in 0.0..1.0 && value.timeScale in .985..1.015)
    require(value.offsetUs.isFinite() && abs(value.offsetUs) <= 30_000_000 && abs(value.offsetMs.toDouble()) <= 30_000)
    require(abs(value.offsetUs / 1000 - value.offsetMs) <= .501 && value.anchorCount in 0..6)
    require(value.residualMs.isFinite() && value.residualMs in 0.0..25.0 && (!value.confident || value.anchorCount >= 3))
    return value
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
