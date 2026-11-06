# Refined A55 picture-decoder experiment

## Result

All eight raw timestamp/decoded-count or sampled-luma parity checks passed.
Sync-start windows had zero decoded preroll. Direct flush and EOS were
still measured in milliseconds. The tight no-luma loop delivered **125.5 fps**
on Miku and **133.1 fps** on Yoshino.

## Scope

- Unchanged `GreaterArt_A55_API36`, API 36, 1080×2340. Same two local 1080p
  AVC fixtures as the original acceptance run, verified by SHA-256 after
  the probe. Another emulator was listed at preflight. Windows free RAM
  at the start of measurement was 1,171 MiB and commit headroom 4.25 GiB.
- Standalone DEX based on the synchronous loop at `fe12a84`. The installed
  app was not used and its APK hash stayed identical. No app change or
  new build. These are diagnostic timings, not an installed-candidate
  test or a physical-device performance limit.
- All variants ran in A55 PID 6789. Each file/mode used a fresh
  extractor and decoder. `c2.goldfish.h264.decoder` reported
  `isSoftwareOnly=false` and `isHardwareAccelerated=true`.
- The quick runs reproduce production acquisition for the reference and
  one companion: three 8-second Miku windows and three 12-second Yoshino
  windows at the existing early/middle/late positions. No forced cache
  purge, no matcher, no app mutex, and no UI. Mutex wait is **not
  measured**, not assumed zero in the app.

## 1. Quick acquisition: first versus repeat

| Take | First (s) | Repeat (s) | Change (s) | Decoded frames per run |
| --- | ---: | ---: | ---: | ---: |
| Miku | 7.674 | 16.544 | 8.870 | 730 |
| Yoshino | 23.167 | 12.263 | -10.904 | 1288 |
| Both | 30.841 | 28.807 | -2.034 | 2018 |

The repeat was 6.6% faster in total, but the clips moved in opposite
directions. This single first/repeat comparison does not support blaming
most of the earlier gap on process or I/O warm-up. The actual six-take
45-second gate remains red; it was not rerun or edited.

## 2. Sync-aligned split control

The same 36-second span per clip ran continuously and in three adjacent
sections with both cuts snapped to nearby sync samples. The section
lengths are therefore not exactly 12 seconds. The [MediaExtractor sync
seek API](https://developer.android.com/reference/android/media/MediaExtractor#SEEK_TO_CLOSEST_SYNC)
selected these timestamps; the probe asserted every window started at
the requested sync PTS and decoded zero earlier frames. It kept the luma
bucket across adjacent sections so the boundary itself could not
resample a bucket. Input still includes the same 250 ms end-padding rule.

| Take | Sync starts (s) | Span end (s) | Continuous (s) | Split (s) | Split minus continuous (s) |
| --- | --- | ---: | ---: | ---: | ---: |
| Miku | 87.416666, 97.083333, 111.666666 | 123.416666 | 8.137 | 7.735 | -0.402 |
| Yoshino | 87.833333, 97.000000, 111.666666 | 123.833333 | 9.125 | 10.454 | 1.329 |

| Take / mode | Codec startup (ms) | First decoded frame after each seek (ms) | Flushes (ms) | EOS drains (ms) | Input + output dequeue (s) | Decoded / in range |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| Miku / contiguous | 36.368 | 121.102 | 0.000 | 3.238 | 2.996 | 870 / 864 |
| Miku / split | 40.025 | 39.798, 24.057, 22.978 | 7.906 | 7.419 | 3.621 | 880 / 864 |
| Yoshino / split | 40.175 | 42.222, 25.846, 26.628 | 7.613 | 7.624 | 4.917 | 1104 / 1080 |
| Yoshino / contiguous | 42.358 | 42.738 | 0.000 | 2.610 | 3.590 | 1088 / 1080 |

Ordered raw PTS and sampled luma match exactly. The split paths had
10 extra tail frames for Miku and
16 for Yoshino, no preroll.
One split was faster and the other slower. Direct reset/drain time
does not explain a multi-second difference on these spans. EOS timing
overlaps dequeue timing and should not be added to it.

## 3. Tight-loop throughput

The tight loop drained/fed available buffers without blocking and used
one bounded 1 ms output wait when neither side progressed. Both modes
decoded and released the same raw PTS/count; only the luma variant
opened and read output images. With-luma samples also matched the
original synchronous loop byte-for-byte on the same spans. Order was
reversed across the two clips.

| Take / mode | Decode loop (s) | Total run (s) | Loop fps | All-in fps | Image sampling (ms) |
| --- | ---: | ---: | ---: | ---: | ---: |
| Miku / no_luma | 6.930 | 7.075 | 125.5 | 123.0 | 0.000 |
| Miku / with_luma | 7.294 | 7.404 | 119.3 | 117.5 | 64.248 |
| Yoshino / with_luma | 8.149 | 8.228 | 133.5 | 132.2 | 50.914 |
| Yoshino / no_luma | 8.173 | 8.257 | 133.1 | 131.8 | 0.000 |

FPS counts decoded output frames, including the small EOS tail. All-in
includes extractor and codec startup/release but not app matching.
This is measured throughput for this loop and host load, **not a hard
decoder ceiling**. Neither no-luma run reached the reviewer's approximate
140 fps target here. On these continuous spans, tight-with-luma loop time
was 9.3% and 9.7% lower than the old loop; these are
single A/B observations per take, not a demonstrated whole-quick gain.
Read, queue, advance, wait and timeout counters are in the raw log.

## 4. A55 guest CPU

Scoped `top -H` to the probe plus A55 media services. Capture started
during the ceiling/sync phase, not the first quick pass. There were
20 active snapshots after excluding the initial sample.

| Guest thread | Samples shown | Median % of one vCPU | Peak % |
| --- | ---: | ---: | ---: |
| CodecLooper | 16 | 6.8 | 10.5 |
| Goldfish decoder thread | 15 | 67.5 | 72.0 |
| main | 20 | 32.0 | 39.5 |
| MediaCodec_loop | 15 | 32.5 | 35.5 |

No sampled guest decoder thread reached 100%. That does not measure
the host Goldfish backend or prove that parallel codecs would scale.

## Review boundary

The data weakens warm-up or direct flush/EOS as the primary explanation.
Frame-volume reduction remains worth investigating, but no shorter
confidence windows, non-reference skipping, or parallel policy changes
were made. The public docs, candidate, acceptance assertion, and main
stay untouched. Raw timings and the scoped thread trace remain local.

## Standalone source

This is a documentation-only checkpoint on the local probe branch.
The probe was compiled with Java 21 targeting Java 17 against Android
API 36, then dexed with D8 (min API 26). It ran with Android
`app_process` on the A55 with the two fixture paths, Miku followed by
Yoshino, without installing or updating an APK.

```java
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Standalone A55 experiment: identical input windows, no playback or app changes. */
public final class RefinedPictureDecodeProbe {
    private static final String[] NAMES = {"Miku", "Yoshino"};
    private static final long REFERENCE_US = 188_000_000L;
    private static final long SECTION_US = 12_000_000L;
    private static final long SEEK_NEAR_US = 88_000_000L;
    private static final long PROD_WAIT_US = 10_000L;
    private static final int GRID = 16;

    private static final class Plan {
        final String group, mode;
        final long[][] windows;
        final boolean sync, tight, sample;

        Plan(String group, String mode, long[][] windows, boolean sync, boolean tight, boolean sample) {
            this.group = group;
            this.mode = mode;
            this.windows = windows;
            this.sync = sync;
            this.tight = tight;
            this.sample = sample;
        }
    }

    private static final class Window {
        long startUs, endUs, syncUs, seekNs, flushNs, wallNs, firstOutputNs, firstInNs, eosNs;
        long inNs, outNs, inTimeoutNs, outTimeoutNs, readNs, queueNs, advanceNs, sampleNs, releaseNs;
        int inCalls, outCalls, inTimeouts, outTimeouts, queued, decoded, preroll, tail, inRange, sampled;
    }

    private static final class Run {
        long openNs, createNs, configureNs, startNs, releaseNs, wallNs, lastBucket = -1;
        String decoder;
        boolean software, hardware;
        final List<Window> windows = new ArrayList<>();
        final List<Long> raw = new ArrayList<>();
        final List<Long> sampledPts = new ArrayList<>();
        final List<byte[]> grids = new ArrayList<>();
    }

    public static void main(String[] paths) throws Exception {
        if (paths.length != 2) throw new IllegalArgumentException("Expected Miku and Yoshino fixture paths");
        System.out.printf(Locale.ROOT, "PROCESS pid=%d epochMs=%d mutex=not_measured%n",
            Process.myPid(), System.currentTimeMillis());
        boolean passed = true;
        var first = new Run[2];
        for (int round = 0; round < 2; round++) {
            long pairNs = 0;
            for (int take = 0; take < 2; take++) {
                long radius = take == 0 ? 4_000_000L : 6_000_000L;
                long[][] windows = new long[3][2];
                for (int part = 0; part < 3; part++) {
                    long center = REFERENCE_US * (2 * part + 1) / 6;
                    windows[part] = new long[]{center - radius, center + radius};
                }
                String mode = round == 0 ? "first" : "repeat";
                Run run = decode(paths[take], NAMES[take], new Plan("quick", mode, windows, false, false, true));
                pairNs += run.wallNs;
                if (round == 0) first[take] = run;
                else passed &= compare("quick", NAMES[take], first[take], run, true, true);
            }
            System.out.printf(Locale.ROOT, "QUICK_PAIR mode=%s acquisitionMs=%.3f pid=%d%n",
                round == 0 ? "first" : "repeat", ms(pairNs), Process.myPid());
        }

        for (int take = 0; take < 2; take++) {
            long[][] split = syncWindows(paths[take]);
            long[][] whole = {{split[0][0], split[2][1]}};
            var contiguous = new Plan("sync", "contiguous", whole, true, false, true);
            var sectioned = new Plan("sync", "split", split, true, false, true);
            Run a, b;
            if (take == 0) {
                a = decode(paths[take], NAMES[take], contiguous);
                b = decode(paths[take], NAMES[take], sectioned);
            } else {
                b = decode(paths[take], NAMES[take], sectioned);
                a = decode(paths[take], NAMES[take], contiguous);
            }
            passed &= compare("sync", NAMES[take], a, b, true, false);
            var noGrid = new Plan("ceiling", "no_luma", whole, true, true, false);
            var grid = new Plan("ceiling", "with_luma", whole, true, true, true);
            Run plain, sampled;
            if (take == 0) {
                plain = decode(paths[take], NAMES[take], noGrid);
                sampled = decode(paths[take], NAMES[take], grid);
            } else {
                sampled = decode(paths[take], NAMES[take], grid);
                plain = decode(paths[take], NAMES[take], noGrid);
            }
            passed &= compare("ceiling", NAMES[take], plain, sampled, false, true);
            passed &= compare("tight_vs_legacy", NAMES[take], a, sampled, true, true);
        }
        if (!passed) throw new AssertionError("At least one parity check failed; inspect the individual comparisons");
        System.out.println("PASS all acquisition, sync-split, and tight-loop parity checks");
    }

    private static MediaFormat selectVideo(MediaExtractor ex) {
        for (int track = 0; track < ex.getTrackCount(); track++) {
            MediaFormat format = ex.getTrackFormat(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                ex.selectTrack(track);
                return format;
            }
        }
        throw new IllegalArgumentException("Missing video track");
    }

    private static long[][] syncWindows(String path) throws Exception {
        var ex = new MediaExtractor();
        try (var file = new FileInputStream(path)) {
            ex.setDataSource(file.getFD());
            MediaFormat format = selectVideo(ex);
            ex.seekTo(SEEK_NEAR_US, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            long start = ex.getSampleTime();
            long[] cuts = {start, 0, 0, start + 3 * SECTION_US};
            for (int part = 1; part < 3; part++) {
                ex.seekTo(start + part * SECTION_US, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                cuts[part] = ex.getSampleTime();
            }
            if (start <= 0 || cuts[1] <= start || cuts[2] <= cuts[1] || cuts[3] <= cuts[2] ||
                format.getLong(MediaFormat.KEY_DURATION) <= cuts[3] + 250_000L) {
                throw new IllegalStateException("Could not select a complete 36-second sync-start span");
            }
            return new long[][]{{cuts[0], cuts[1]}, {cuts[1], cuts[2]}, {cuts[2], cuts[3]}};
        } finally {
            ex.release();
        }
    }

    private static Run decode(String path, String take, Plan plan) throws Exception {
        System.out.printf(Locale.ROOT, "BEGIN group=%s take=%s mode=%s epochMs=%d%n",
            plan.group, take, plan.mode, System.currentTimeMillis());
        var run = new Run();
        long began = System.nanoTime();
        long deadline = began + 90_000_000_000L;
        var ex = new MediaExtractor();
        MediaCodec decoder = null;
        try (var file = new FileInputStream(path)) {
            ex.setDataSource(file.getFD());
            MediaFormat format = selectVideo(ex);
            int rotation = format.containsKey(MediaFormat.KEY_ROTATION) ? format.getInteger(MediaFormat.KEY_ROTATION) : 0;
            long duration = format.getLong(MediaFormat.KEY_DURATION);
            if (plan.group.equals("quick") && take.equals("Miku") && duration != REFERENCE_US) {
                throw new IllegalArgumentException("Reference duration differs from the acceptance corpus");
            }
            format.setInteger(MediaFormat.KEY_ROTATION, 0);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            run.openNs = System.nanoTime() - began;
            long t = System.nanoTime();
            decoder = MediaCodec.createDecoderByType(Objects.requireNonNull(format.getString(MediaFormat.KEY_MIME)));
            run.createNs = System.nanoTime() - t;
            run.decoder = decoder.getName();
            run.software = decoder.getCodecInfo().isSoftwareOnly();
            run.hardware = decoder.getCodecInfo().isHardwareAccelerated();
            t = System.nanoTime();
            decoder.configure(format, null, null, 0);
            run.configureNs = System.nanoTime() - t;
            t = System.nanoTime();
            decoder.start();
            run.startNs = System.nanoTime() - t;
            for (int index = 0; index < plan.windows.length; index++) {
                var w = new Window();
                w.startUs = plan.windows[index][0];
                w.endUs = plan.windows[index][1];
                if (index > 0) {
                    t = System.nanoTime();
                    decoder.flush();
                    w.flushNs = System.nanoTime() - t;
                }
                t = System.nanoTime();
                ex.seekTo(w.startUs, plan.sync ? MediaExtractor.SEEK_TO_CLOSEST_SYNC : MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                w.seekNs = System.nanoTime() - t;
                w.syncUs = ex.getSampleTime();
                if (w.syncUs < 0 || w.syncUs > w.startUs || (plan.sync && w.syncUs != w.startUs) ||
                    (ex.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) == 0) {
                    throw new IllegalStateException("Unexpected sync sample");
                }
                if (!plan.sync) run.lastBucket = -1; // Production resets its bucket at each gapped window.
                new Pump(ex, decoder, rotation, plan.sample, run, w).run(plan.tight, deadline);
                if (plan.sync && w.preroll != 0) throw new IllegalStateException("Sync-start output contained preroll");
                run.windows.add(w);
            }
        } finally {
            long t = System.nanoTime();
            try {
                if (decoder != null) decoder.release();
            } finally {
                ex.release();
            }
            run.releaseNs = System.nanoTime() - t;
        }
        run.wallNs = System.nanoTime() - began;
        System.out.printf(Locale.ROOT,
            "RUN group=%s take=%s mode=%s decoder=%s software=%s hardware=%s wallMs=%.3f openMs=%.3f " +
            "createMs=%.3f configureMs=%.3f startMs=%.3f releaseMs=%.3f raw=%d sampled=%d ptsSha=%s lumaSha=%s epochMs=%d%n",
            plan.group, take, plan.mode, run.decoder, run.software, run.hardware, ms(run.wallNs), ms(run.openNs),
            ms(run.createNs), ms(run.configureNs), ms(run.startNs), ms(run.releaseNs), run.raw.size(),
            run.sampledPts.size(), digest(run.raw, null),
            plan.sample ? digest(run.sampledPts, run.grids) : "none", System.currentTimeMillis());
        for (int i = 0; i < run.windows.size(); i++) {
            Window w = run.windows.get(i);
            System.out.printf(Locale.ROOT,
                "WINDOW group=%s take=%s mode=%s part=%d startUs=%d endUs=%d syncUs=%d wallMs=%.3f seekMs=%.3f flushMs=%.3f " +
                "firstOutMs=%.3f firstInMs=%.3f eosMs=%.3f inputMs=%.3f outputMs=%.3f inTimeoutMs=%.3f outTimeoutMs=%.3f " +
                "inCalls=%d outCalls=%d inTimeouts=%d outTimeouts=%d readMs=%.3f queueMs=%.3f advanceMs=%.3f " +
                "sampleMs=%.3f releaseBufferMs=%.3f queued=%d decoded=%d preroll=%d tail=%d inRange=%d sampled=%d%n",
                plan.group, take, plan.mode, i + 1, w.startUs, w.endUs, w.syncUs, ms(w.wallNs), ms(w.seekNs), ms(w.flushNs),
                ms(w.firstOutputNs), ms(w.firstInNs), ms(w.eosNs), ms(w.inNs), ms(w.outNs), ms(w.inTimeoutNs),
                ms(w.outTimeoutNs), w.inCalls, w.outCalls, w.inTimeouts, w.outTimeouts, ms(w.readNs), ms(w.queueNs),
                ms(w.advanceNs), ms(w.sampleNs), ms(w.releaseNs), w.queued, w.decoded, w.preroll, w.tail, w.inRange, w.sampled);
        }
        return run;
    }

    private static final class Pump {
        final MediaExtractor ex;
        final MediaCodec codec;
        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        final int rotation;
        final boolean sample;
        final Run result;
        final Window w;
        long began, inputEos;
        boolean done;

        Pump(MediaExtractor ex, MediaCodec codec, int rotation, boolean sample, Run result, Window w) {
            this.ex = ex; this.codec = codec; this.rotation = rotation; this.sample = sample;
            this.result = result; this.w = w;
        }

        void run(boolean tight, long deadline) {
            began = System.nanoTime();
            int first = result.raw.size();
            while (!done) {
                if (System.nanoTime() > deadline) throw new IllegalStateException("90-second diagnostic deadline");
                if (!tight) {
                    feed(PROD_WAIT_US);
                    drain(PROD_WAIT_US);
                } else {
                    boolean progressed = false;
                    while (drain(0)) progressed = true;
                    while (!done && feed(0)) progressed = true;
                    if (!done && !progressed) drain(1_000); // Bounded idle wait, not a busy spin.
                }
            }
            w.wallNs = System.nanoTime() - began;
            if (w.inRange < 24 || result.raw.get(first) > w.startUs + 125_000 ||
                result.raw.get(result.raw.size() - 1) < w.endUs - 125_000 || (sample && w.sampled < 24)) {
                throw new IllegalStateException("Incomplete window coverage");
            }
        }

        boolean feed(long waitUs) {
            if (inputEos != 0) return false;
            long t = System.nanoTime();
            int index = codec.dequeueInputBuffer(waitUs);
            long elapsed = System.nanoTime() - t;
            w.inCalls++; w.inNs += elapsed;
            if (index < 0) {
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) { w.inTimeouts++; w.inTimeoutNs += elapsed; }
                return false;
            }
            t = System.nanoTime();
            long pts = ex.getSampleTime();
            int size = pts < 0 || pts >= w.endUs + 250_000L ? -1
                : ex.readSampleData(Objects.requireNonNull(codec.getInputBuffer(index)), 0);
            w.readNs += System.nanoTime() - t;
            t = System.nanoTime();
            codec.queueInputBuffer(index, 0, Math.max(size, 0), Math.max(pts, 0),
                size < 0 ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            long queuedAt = System.nanoTime();
            w.queueNs += queuedAt - t;
            if (size < 0) inputEos = queuedAt;
            else {
                w.queued++;
                ex.advance();
                w.advanceNs += System.nanoTime() - queuedAt;
            }
            return true;
        }

        boolean drain(long waitUs) {
            if (done) return false;
            long t = System.nanoTime();
            int index = codec.dequeueOutputBuffer(info, waitUs);
            long ready = System.nanoTime();
            w.outCalls++; w.outNs += ready - t;
            if (index < 0) {
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    w.outTimeouts++; w.outTimeoutNs += ready - t;
                    return false;
                }
                return index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED;
            }
            try {
                done = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                if (done) {
                    if (inputEos == 0) throw new IllegalStateException("Output EOS preceded input EOS");
                    w.eosNs = ready - inputEos;
                }
                if (info.size == 0) return true;
                w.decoded++;
                if (w.firstOutputNs == 0) w.firstOutputNs = ready - began;
                long pts = info.presentationTimeUs;
                if (pts < w.startUs) { w.preroll++; return true; }
                if (pts >= w.endUs) { w.tail++; return true; }
                w.inRange++;
                result.raw.add(pts);
                if (w.firstInNs == 0) w.firstInNs = ready - began;
                if (!sample) return true;
                long bucket = pts * 30L / 1_000_000L;
                if (bucket <= result.lastBucket) return true;
                result.lastBucket = bucket;
                t = System.nanoTime();
                try (Image image = codec.getOutputImage(index)) {
                    byte[] luma = image == null ? null : sampleLuma(image, rotation);
                    if (luma == null) throw new IllegalStateException("Expected usable YUV image");
                    result.sampledPts.add(pts);
                    result.grids.add(luma);
                    w.sampled++;
                } finally {
                    w.sampleNs += System.nanoTime() - t;
                }
                return true;
            } finally {
                t = System.nanoTime();
                codec.releaseOutputBuffer(index, false);
                w.releaseNs += System.nanoTime() - t;
            }
        }
    }

    private static byte[] sampleLuma(Image image, int rotation) {
        if (image.getFormat() != ImageFormat.YUV_420_888) return null;
        var plane = image.getPlanes()[0];
        var crop = image.getCropRect();
        var buffer = plane.getBuffer();
        byte[] grid = new byte[GRID * GRID];
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
            int max = GRID - 1;
            int sx = rotation == 90 ? y : rotation == 180 ? max - x : rotation == 270 ? max - y : x;
            int sy = rotation == 90 ? max - x : rotation == 180 ? max - y : rotation == 270 ? x : y;
            int px = crop.left + (2 * sx + 1) * crop.width() / (2 * GRID);
            int py = crop.top + (2 * sy + 1) * crop.height() / (2 * GRID);
            int offset = buffer.position() + py * plane.getRowStride() + px * plane.getPixelStride();
            if (offset >= buffer.limit()) return null;
            grid[y * GRID + x] = buffer.get(offset);
        }
        return grid;
    }

    private static boolean compare(String group, String take, Run a, Run b, boolean compareGrids, boolean sameDecodeCount) {
        boolean samePts = a.raw.equals(b.raw);
        boolean grids = true;
        if (compareGrids) {
            grids = a.sampledPts.equals(b.sampledPts) && a.grids.size() == b.grids.size();
            for (int i = 0; grids && i < a.grids.size(); i++) grids = Arrays.equals(a.grids.get(i), b.grids.get(i));
        }
        int decodedA = a.windows.stream().mapToInt(x -> x.decoded).sum();
        int decodedB = b.windows.stream().mapToInt(x -> x.decoded).sum();
        boolean ok = samePts && grids && (!sameDecodeCount || decodedA == decodedB);
        System.out.printf(Locale.ROOT,
            "PARITY group=%s take=%s result=%s rawPts=%s luma=%s decodedA=%d decodedB=%d%n",
            group, take, ok ? "PASS" : "FAIL", samePts, compareGrids ? grids : "not_compared", decodedA, decodedB);
        return ok;
    }

    private static String digest(List<Long> timestamps, List<byte[]> grids) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < timestamps.size(); i++) {
            long pts = timestamps.get(i);
            for (int shift = 56; shift >= 0; shift -= 8) hash.update((byte) (pts >>> shift));
            if (grids != null) hash.update(grids.get(i));
        }
        var hex = new StringBuilder(64);
        for (byte b : hash.digest()) {
            hex.append(Character.forDigit((b & 255) >>> 4, 16));
            hex.append(Character.forDigit(b & 15, 16));
        }
        return hex.toString();
    }

    private static double ms(long nanos) { return nanos / 1_000_000.0; }
}
```
