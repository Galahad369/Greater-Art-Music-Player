# A55 AVC non-reference picture probe — quick-plan baseline 1.22.10

**Result:** 3,778 of 6,258 input pictures (60.4%) in the existing quick plan are non-reference units without protected metadata. Dropping them could bring frame work well below 45 s in a favorable model, but **does not meet 45 s if the prior unexplained overhead persists** (~51.6 s). The larger issue is that the matcher would lose 3,224 of 5,328 in-window pictures, including timing evidence across the three sections. No skip path was run.

## What was measured

- On `GreaterArt_A55_API36`, API 36, 1080×2340: header-only `MediaExtractor` scan of the exact six `孤独毒毒` fixtures. Each fixture matched both its byte length and SHA-256 from the prior A55 acceptance run. Full AVC sample counts matched independent decoded PTS totals from the earlier corpus. Every sample had a unique PTS, and every access unit classified. No decoder was opened; no media was modified or copied off the emulator.
- For each access unit, all ordinary VCL slices had to agree on `nal_ref_idc`. The candidate-drop count excludes sync units and non-reference units carrying non-filler metadata such as SEI or parameter sets. Unknown, encrypted, or partial samples cannot count as eligible. Synthetic controls covered reference and non-reference slices, multi-slice units, escaped payload, metadata, sync, malformed input, and unsupported layouts. See [ITU H.264](https://www.itu.int/rec/T-REC-H.264/en) and [FFmpeg's AVC decoder implementation](https://github.com/FFmpeg/FFmpeg/blob/master/libavcodec/h264dec.c) for the distinction between non-reference slices and other NALs carried in a packet.
- Pinned to **`fe12a84`**, the failed 1.22.10 candidate that supplied the prior timing evidence. Reproduced its three 8 s reference windows, three 12 s windows per companion, previous-sync seeks, and first input PTS at/after end + 250 ms stop rule. The probe uses input order for the stop rule and presentation order for timestamp gaps. Feed counts for Miku (730) and Yoshino (1,288) agree with prior actual decoder output on those same windows.
- Initial multi-file process exited 137 during Riona after only 1,965 of 5,640 frames; no cause established. Discarded that partial run. The successful retry used one fresh process per fixture and refused to emit full totals unless the count matched the independently known count. Six process exits were zero; 18 windows recorded. Header scans totaled 40.76 s of per-take scan time; this is **not** a decode, matcher, cold-run, or acceptance benchmark.
- AVD configuration hash stayed unchanged. Another run updated the installed app while this work was in progress; this standalone probe did not read through or install the app. No production source changes, build, release, push, or merge for this probe. The 1.22.10 acceptance failure stays recorded.

## Counts

“Eligible” describes header structure only; it does not mean the app can discard that picture without affecting alignment. Quick feed counts include preroll and tail.

| Take | Whole file: non-ref / total | Whole non-ref | Quick non-ref / feed | Quick eligible | In-window eligible / input | Max retained gap | Gaps >125 ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Miku | 2,684 / 4,512 | 59.5% | 430 / 730 | 430 | 339 / 576 | 125.0 ms | 0 |
| Riona | 3,306 / 5,640 | 58.6% | 833 / 1,403 | 833 | 639 / 1,080 | 100.0 ms | 0 |
| unc | 2,643 / 4,514 | 58.6% | 599 / 1,031 | 599 | 503 / 864 | 125.0 ms | 0 |
| Forte | 3,065 / 4,515 | 67.9% | 607 / 904 | 603 | 575 / 864 | 166.7 ms | 182 |
| Yoshino | 3,133 / 5,648 | 55.5% | 718 / 1,288 | 718 | 598 / 1,080 | 100.0 ms | 0 |
| Morislei | 3,041 / 4,512 | 67.4% | 599 / 902 | 595 | 570 / 864 | 166.7 ms | 180 |
| **Total** | **17,872 / 29,341** | **60.9%** | **3,786 / 6,258** | **3,778** | **3,224 / 5,328** | **166.7 ms** | **362** |

There were 45 protected non-reference units over the whole corpus, 8 in the quick feed. The eligible quick units contain only 17.7% of AVC payload bytes (10,500,251 of 59,428,832), despite being 60.4% of the pictures. The decoder's cost need not scale with either count or bytes. Header inspection still has a cost, too.

## 45-second estimate

| Scenario | Calculation | Estimated time |
| --- | --- | ---: |
| Drop all eligible; optimistic frame loop | 2,480 retained ÷ prior 125.5–133.1 fps tight, no-luma loop | 18.63–19.76 s before other costs |
| Drop all eligible; prorate prior wide timing | Sum each take's retained frames × its prior all-in wide seconds per source frame | 22.50 s before quick-specific overhead |
| Same, with prior quick overhead scenario | 22.50 s + 29.09 s | **51.59 s** |
| Drop only outside the evidence windows | 5,704 retained ÷ prior 125.5–133.1 fps tight, no-luma loop | 42.85–45.45 s before other costs |

The wide-rate estimate uses the prior six A55 per-take times (44.801, 48.515, 39.456, 40.196, 51.018, 44.258 s), divided by each full frame count. For the overhead scenario, the prior quick run completed the first three takes in 42.391 s. Their wide-proportional frame work is 28.33 s and their nine measured seeks cost 0.192 s, leaving 13.87 s unexplained. Doubling that residual and adding all 18 measured seeks (1.348 s) gives 29.09 s. This is a sensitivity scenario, not a measurement of fixed overhead. The wide-rate model needs quick-specific overhead below 22.50 s even if the full predicted frame savings materialize. Protecting all in-window pictures saves only 554 pictures (8.9%).

**Recommendation:** Do not drop every eligible unit in the production matcher. In particular, Forte and Morislei would have 362 within-window gaps over 125 ms combined. The current matcher needs two ordered pictures within ±35 ms of the expected times; the 125 ms cutoff applies to reference anchors, not directly to companions. Sparse companion timestamps can still remove valid matches. Before any timed skip experiment, score the retained timestamps using existing decoded fixtures and require the original offsets, ambiguous/unrelated rejections, and all three evidence regions to pass. Then measure the unchanged 45 s cold A55 gate on any surviving strategy. The header counts alone prove neither accurate offsets nor a timing pass.

**Separate candidate:** The checkout advanced independently to `86ba54f` (1.22.12/code 333). Its handoff records an unchanged A55 corpus pass in 35.534 s using local AVC decoding and IDR-based seek starts, without dropping non-reference pictures. The installed APK hash observed after this probe matches that handoff. That is separate evidence, not a rerun here, and does not change the 1.22.10 estimates above.

## Reproduction source

Standalone Java entry point. Compile against Android API 36 targeting Java 17, convert to DEX with Android build tools D8, and run one fixture at a time using `app_process` on the A55. Arguments: fixture index (0–5 in table order), independent decoded frame count, and readable fixture path. Verify the fixture hash before scanning. Do not package this code into Greater Art.

```java
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/** Read AVC access-unit headers through the Android extractor. No decoder is opened. */
public final class AvcReferenceProbeRetry {
    private static final int UNKNOWN = 0, REF = 1, NONREF = 2, PROTECTED_NONREF = 3;
    private static final long TAIL_US = 250_000L;
    private static final String[] TAKES = {"Miku", "Riona", "unc", "Forte", "Yoshino", "Morislei"};

    private static final class Sample {
        final long pts;
        final int kind, bytes;

        Sample(long pts, int kind, int bytes) {
            this.pts = pts;
            this.kind = kind;
            this.bytes = bytes;
        }
    }

    private static final class Counts {
        int total, ref, nonref, eligible, unknown;
        long bytes, eligibleBytes;

        void add(Sample s) {
            total++;
            bytes += s.bytes;
            if (s.kind == REF) ref++;
            else if (s.kind == UNKNOWN) unknown++;
            else {
                nonref++;
                if (s.kind == NONREF) {
                    eligible++;
                    eligibleBytes += s.bytes;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected fixture index, independent frame count, path");
        int take = Integer.parseInt(args[0]);
        int expectedFrames = Integer.parseInt(args[1]);
        controls();
        var buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024);
        // The pinned reference's track duration is also checked when processing take 0.
        long referenceUs = 188_000_000L;
        {
            String name = TAKES[take];
            System.out.println("BEGIN take=" + name);
            long began = System.nanoTime();
            var extractor = new MediaExtractor();
            try (var file = new FileInputStream(args[2])) {
                extractor.setDataSource(file.getFD());
                MediaFormat format = selectAvc(extractor);
                long durationUs = format.getLong(MediaFormat.KEY_DURATION);
                long analysisUs = Math.min(durationUs, 210_000_000L);
                if (take == 0 && referenceUs != analysisUs) throw new IllegalStateException("Reference duration changed");
                var samples = new ArrayList<Sample>();
                var indices = new HashMap<Long, Integer>();
                var full = new Counts();
                int syncSamples = 0;
                while (true) {
                    long pts = extractor.getSampleTime();
                    if (pts < 0) break;
                    int flags = extractor.getSampleFlags();
                    buffer.clear();
                    int bytes = extractor.readSampleData(buffer, 0);
                    if (bytes <= 0 || bytes > buffer.limit()) throw new IllegalStateException("Unreadable AVC sample");
                    if (indices.put(pts, samples.size()) != null) {
                        throw new IllegalStateException("Duplicate picture PTS; one-sample count is not reliable");
                    }
                    boolean sync = (flags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
                    if (sync) syncSamples++;
                    int kind = (flags & (MediaExtractor.SAMPLE_FLAG_ENCRYPTED | MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME)) != 0
                        ? UNKNOWN : classify(buffer, bytes, sync);
                    var sample = new Sample(pts, kind, bytes);
                    samples.add(sample);
                    full.add(sample);
                    if (!extractor.advance()) break;
                }
                if (full.total != expectedFrames || syncSamples == 0) {
                    throw new IllegalStateException("Incomplete fixture: saw " + full.total + " of " + expectedFrames + " frames");
                }
                System.out.printf(Locale.ROOT,
                    "FULL take=%s frames=%d ref=%d nonref=%d eligible=%d protected=%d unknown=%d syncSamples=%d bytes=%d eligibleBytes=%d durationUs=%d%n",
                    name, full.total, full.ref, full.nonref, full.eligible, full.nonref - full.eligible, full.unknown,
                    syncSamples, full.bytes, full.eligibleBytes, durationUs);
                long radiusUs = take == 0 ? 4_000_000L : 6_000_000L;
                for (int part = 0; part < 3; part++) {
                    long center = referenceUs * (2 * part + 1) / 6;
                    long startUs = Math.max(0, center - radiusUs);
                    long endUs = Math.min(analysisUs, center + radiusUs);
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    long syncUs = extractor.getSampleTime();
                    Integer first = indices.get(syncUs);
                    if (first == null || syncUs > startUs || startUs >= endUs ||
                        (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) == 0) {
                        throw new IllegalStateException("Could not reproduce the quick sync start");
                    }
                    var feed = new Counts();
                    var inRange = new Counts();
                    var keepPts = new ArrayList<Long>();
                    var allPts = new ArrayList<Long>();
                    int preroll = 0, tail = 0;
                    for (int index = first; index < samples.size(); index++) {
                        Sample sample = samples.get(index);
                        // The app stops at the first input PTS at or beyond its pad, in extractor order.
                        if (sample.pts >= endUs + TAIL_US) break;
                        feed.add(sample);
                        if (sample.pts < startUs) preroll++;
                        else if (sample.pts >= endUs) tail++;
                        else {
                            inRange.add(sample);
                            allPts.add(sample.pts);
                            if (sample.kind != NONREF) keepPts.add(sample.pts);
                        }
                    }
                    if (inRange.total == 0) throw new IllegalStateException("Empty quick window");
                    long[] allGaps = gaps(allPts);
                    long[] keepGaps = gaps(keepPts);
                    System.out.printf(Locale.ROOT,
                        "WINDOW take=%s part=%d startUs=%d endUs=%d syncUs=%d feed=%d preroll=%d tail=%d inRange=%d " +
                        "nonref=%d eligible=%d protected=%d unknown=%d inRangeNonref=%d inRangeEligible=%d feedBytes=%d eligibleBytes=%d " +
                        "maxGapUs=%d retainedMaxGapUs=%d retainedGapsOver125=%d%n",
                        name, part + 1, startUs, endUs, syncUs, feed.total, preroll, tail, inRange.total, feed.nonref,
                        feed.eligible, feed.nonref - feed.eligible, feed.unknown, inRange.nonref, inRange.eligible,
                        feed.bytes, feed.eligibleBytes, allGaps[0], keepGaps[0], keepGaps[1]);
                }
            } finally {
                extractor.release();
            }
            System.out.printf(Locale.ROOT, "END take=%s readOnlyWallMs=%.3f%n", name, (System.nanoTime() - began) / 1e6);
        }
        System.out.println("PASS exact fixture frame count and three quick feed windows; no decoder opened");
    }

    private static MediaFormat selectAvc(MediaExtractor ex) {
        for (int i = 0; i < ex.getTrackCount(); i++) {
            MediaFormat format = ex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null || !mime.startsWith("video/")) continue;
            if (!mime.equals("video/avc")) throw new IllegalArgumentException("Expected AVC video, got " + mime);
            ex.selectTrack(i);
            return format;
        }
        throw new IllegalArgumentException("No video track");
    }

    private static long[] gaps(ArrayList<Long> pts) {
        Collections.sort(pts);
        long largest = 0, over125 = 0;
        for (int i = 1; i < pts.size(); i++) {
            long gap = pts.get(i) - pts.get(i - 1);
            largest = Math.max(largest, gap);
            if (gap > 125_000L) over125++;
        }
        return new long[]{largest, over125};
    }

    private static int startCode(ByteBuffer b, int from, int end) {
        for (int i = from; i + 2 < end; i++) {
            if (b.get(i) == 0 && b.get(i + 1) == 0 && b.get(i + 2) == 1) return i;
        }
        return -1;
    }

    private static int classify(ByteBuffer b, int length, boolean sync) {
        int prefix = startCode(b, 0, length);
        if (prefix < 0) return UNKNOWN; // Require the extractor's observed Annex B format.
        for (int i = 0; i < prefix; i++) if (b.get(i) != 0) return UNKNOWN;
        boolean ref = false, nonref = false, idr = false, ordinary = false, protect = sync;
        while (prefix >= 0) {
            int pos = prefix + 3;
            if (pos >= length) return UNKNOWN;
            int header = b.get(pos) & 255;
            int type = header & 31;
            int idc = (header >>> 5) & 3;
            if ((header & 128) != 0 || type == 0 || type >= 24) return UNKNOWN;
            int next = startCode(b, pos + 1, length);
            if (type == 1 || type == 5) {
                if ((next < 0 ? length : next) <= pos + 1 || (type == 5 && idc == 0)) return UNKNOWN;
                ref |= idc != 0;
                nonref |= idc == 0;
                idr |= type == 5;
                ordinary |= type == 1;
            } else if ((type >= 2 && type <= 4) || (type >= 19 && type <= 21)) {
                return UNKNOWN; // Partitioned or extension VCL is outside this probe.
            } else if (type != 9 && type != 12) {
                protect = true; // Keep units carrying SEI, SPS/PPS, or other non-filler metadata.
            }
            prefix = next;
        }
        if ((idr && ordinary) || ref == nonref) return UNKNOWN; // No VCL or mixed reference flags.
        return ref ? REF : (protect ? PROTECTED_NONREF : NONREF);
    }

    private static void controls() {
        expect(REF, false, 0,0,0,1,0x65,0x80);
        expect(REF, false, 0,0,1,0x41,0x80);
        expect(NONREF, false, 0,0,0,1,0x01,0x80,0,0,3,1,0x80);
        expect(NONREF, false, 0,0,1,0x01,0x80,0,0,0,1,0x01,0x80);
        expect(PROTECTED_NONREF, false, 0,0,1,0x06,0x80,0,0,1,0x01,0x80);
        expect(PROTECTED_NONREF, true, 0,0,1,0x01,0x80);
        expect(UNKNOWN, false, 0,0,1,0x01,0x80,0,0,1,0x41,0x80);
        expect(UNKNOWN, false, 0,0,1,0x05,0x80);
        expect(UNKNOWN, false, 0,0,1,0x06,0x80);
        expect(UNKNOWN, false, 0,0,0,2,0x01,0x80);
        System.out.println("PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported");
    }

    private static void expect(int expected, boolean sync, int... input) {
        byte[] bytes = new byte[input.length];
        for (int i = 0; i < input.length; i++) bytes[i] = (byte) input[i];
        if (classify(ByteBuffer.wrap(bytes), bytes.length, sync) != expected) {
            throw new AssertionError("NAL-header control failed");
        }
    }
}
```

## A55 run receipt

```text
PROFILE GreaterArt_A55_API36 API=36
Physical size: 1080x2340
PROBE_SHA256 864B7538929E83C5405ED9F24ADCA8959C0CF6FFD50D86A975E5518789D3F0C6
FIXTURE index=0 bytes=43057797 sha256=530c4f8c57ff6a56d92b4d459da63ec2d2124a2616d63de2d03799d0484552ca independentFrames=4512
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=Miku
FULL take=Miku frames=4512 ref=1828 nonref=2684 eligible=2684 protected=0 unknown=0 syncSamples=54 bytes=39911254 eligibleBytes=6636020 durationUs=188000000
WINDOW take=Miku part=1 startUs=27333333 endUs=35333333 syncUs=26875000 feed=209 preroll=11 tail=6 inRange=192 nonref=126 eligible=126 protected=0 unknown=0 inRangeNonref=117 inRangeEligible=117 feedBytes=1780886 eligibleBytes=231708 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
WINDOW take=Miku part=2 startUs=90000000 endUs=98000000 syncUs=87416666 feed=260 preroll=62 tail=6 inRange=192 nonref=148 eligible=148 protected=0 unknown=0 inRangeNonref=109 inRangeEligible=109 feedBytes=2450563 eligibleBytes=405407 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
WINDOW take=Miku part=3 startUs=152666666 endUs=160666666 syncUs=150000000 feed=261 preroll=64 tail=5 inRange=192 nonref=156 eligible=156 protected=0 unknown=0 inRangeNonref=113 inRangeEligible=113 feedBytes=3400232 eligibleBytes=470244 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
END take=Miku readOnlyWallMs=7430.463
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=0 exit=0
FIXTURE index=1 bytes=42942829 sha256=ba399a4ad3c080f3a1a476d61c2fd5bcde8928ce977cc9753523d7fc8802a3a2 independentFrames=5640
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=Riona
FULL take=Riona frames=5640 ref=2334 nonref=3306 eligible=3306 protected=0 unknown=0 syncSamples=55 bytes=39736919 eligibleBytes=6331585 durationUs=188000000
WINDOW take=Riona part=1 startUs=25333333 endUs=37333333 syncUs=19900000 feed=530 preroll=163 tail=7 inRange=360 nonref=325 eligible=325 protected=0 unknown=0 inRangeNonref=216 inRangeEligible=216 feedBytes=3784748 eligibleBytes=559405 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
WINDOW take=Riona part=2 startUs=88000000 endUs=100000000 syncUs=84733333 feed=464 preroll=98 tail=6 inRange=360 nonref=263 eligible=263 protected=0 unknown=0 inRangeNonref=205 inRangeEligible=205 feedBytes=2635414 eligibleBytes=223960 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
WINDOW take=Riona part=3 startUs=150666666 endUs=162666666 syncUs=149300000 feed=409 preroll=41 tail=8 inRange=360 nonref=245 eligible=245 protected=0 unknown=0 inRangeNonref=218 inRangeEligible=218 feedBytes=4643738 eligibleBytes=570818 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
END take=Riona readOnlyWallMs=6334.605
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=1 exit=0
FIXTURE index=2 bytes=40492410 sha256=21735ed1ce37165c9d20e2a9feb94a79680f6fbddfb931f83127528343aab2c8 independentFrames=4514
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=unc
FULL take=unc frames=4514 ref=1871 nonref=2643 eligible=2643 protected=0 unknown=0 syncSamples=52 bytes=37344130 eligibleBytes=6241381 durationUs=188084000
WINDOW take=unc part=1 startUs=25333333 endUs=37333333 syncUs=19875000 feed=425 preroll=131 tail=6 inRange=288 nonref=254 eligible=254 protected=0 unknown=0 inRangeNonref=173 inRangeEligible=173 feedBytes=2944108 eligibleBytes=389730 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
WINDOW take=unc part=2 startUs=88000000 endUs=100000000 syncUs=87833333 feed=296 preroll=4 tail=4 inRange=288 nonref=165 eligible=165 protected=0 unknown=0 inRangeNonref=162 inRangeEligible=162 feedBytes=2652946 eligibleBytes=408781 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
WINDOW take=unc part=3 startUs=150666666 endUs=162666666 syncUs=150000000 feed=310 preroll=16 tail=6 inRange=288 nonref=180 eligible=180 protected=0 unknown=0 inRangeNonref=168 inRangeEligible=168 feedBytes=3784914 eligibleBytes=512595 maxGapUs=41667 retainedMaxGapUs=125000 retainedGapsOver125=0
END take=unc readOnlyWallMs=6550.785
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=2 exit=0
FIXTURE index=3 bytes=49564177 sha256=2b912aa8c3f5c632f0a62c98d34f5591d9996c786e3e04815d449d7c85912a6f independentFrames=4515
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=Forte
FULL take=Forte frames=4515 ref=1450 nonref=3065 eligible=3044 protected=21 unknown=0 syncSamples=87 bytes=46424692 eligibleBytes=13395318 durationUs=188125000
WINDOW take=Forte part=1 startUs=25333333 endUs=37333333 syncUs=25000000 feed=301 preroll=8 tail=5 inRange=288 nonref=204 eligible=203 protected=1 unknown=0 inRangeNonref=195 inRangeEligible=194 feedBytes=3071470 eligibleBytes=780552 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=60
WINDOW take=Forte part=2 startUs=88000000 endUs=100000000 syncUs=88000000 feed=293 preroll=0 tail=5 inRange=288 nonref=200 eligible=198 protected=2 unknown=0 inRangeNonref=197 inRangeEligible=195 feedBytes=2858820 eligibleBytes=892163 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=64
WINDOW take=Forte part=3 startUs=150666666 endUs=162666666 syncUs=150000000 feed=310 preroll=16 tail=6 inRange=288 nonref=203 eligible=202 protected=1 unknown=0 inRangeNonref=187 inRangeEligible=186 feedBytes=4703754 eligibleBytes=1153667 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=58
END take=Forte readOnlyWallMs=5235.071
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=3 exit=0
FIXTURE index=4 bytes=34355525 sha256=4ebbcca41deb929b5c79d6549978d84a9b80b6c8346790ea86f5f671792bd60f independentFrames=5648
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=Yoshino
FULL take=Yoshino frames=5648 ref=2515 nonref=3133 eligible=3133 protected=0 unknown=0 syncSamples=59 bytes=31147147 eligibleBytes=4123969 durationUs=188267000
WINDOW take=Yoshino part=1 startUs=25333333 endUs=37333333 syncUs=19900000 feed=529 preroll=163 tail=6 inRange=360 nonref=295 eligible=295 protected=0 unknown=0 inRangeNonref=194 inRangeEligible=194 feedBytes=2567820 eligibleBytes=299260 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
WINDOW take=Yoshino part=2 startUs=88000000 endUs=100000000 syncUs=87833333 feed=371 preroll=5 tail=6 inRange=360 nonref=202 eligible=202 protected=0 unknown=0 inRangeNonref=198 inRangeEligible=198 feedBytes=2122278 eligibleBytes=284437 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
WINDOW take=Yoshino part=3 startUs=150666666 endUs=162666666 syncUs=150000000 feed=388 preroll=20 tail=8 inRange=360 nonref=221 eligible=221 protected=0 unknown=0 inRangeNonref=206 inRangeEligible=206 feedBytes=3026789 eligibleBytes=316592 maxGapUs=33334 retainedMaxGapUs=100000 retainedGapsOver125=0
END take=Yoshino readOnlyWallMs=9406.712
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=4 exit=0
FIXTURE index=5 bytes=58375278 sha256=6e9c7d7bb1b04989328ee477cb4d3b2b62570a98fece5c7cc5ffa9e452142471 independentFrames=4512
PASS header controls: reference, multi-slice, escaped payload, protected, mixed, invalid and unsupported
BEGIN take=Morislei
FULL take=Morislei frames=4512 ref=1471 nonref=3041 eligible=3017 protected=24 unknown=0 syncSamples=85 bytes=55237659 eligibleBytes=13895833 durationUs=188000000
WINDOW take=Morislei part=1 startUs=25333333 endUs=37333333 syncUs=25000000 feed=301 preroll=8 tail=5 inRange=288 nonref=203 eligible=203 protected=0 unknown=0 inRangeNonref=194 inRangeEligible=194 feedBytes=4037889 eligibleBytes=972666 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=62
WINDOW take=Morislei part=2 startUs=88000000 endUs=100000000 syncUs=88000000 feed=293 preroll=0 tail=5 inRange=288 nonref=199 eligible=196 protected=3 unknown=0 inRangeNonref=196 inRangeEligible=193 feedBytes=3718969 eligibleBytes=1030202 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=63
WINDOW take=Morislei part=3 startUs=150666666 endUs=162666666 syncUs=150000000 feed=308 preroll=16 tail=4 inRange=288 nonref=197 eligible=196 protected=1 unknown=0 inRangeNonref=184 inRangeEligible=183 feedBytes=5243494 eligibleBytes=998064 maxGapUs=41667 retainedMaxGapUs=166667 retainedGapsOver125=55
END take=Morislei readOnlyWallMs=5800.518
PASS exact fixture frame count and three quick feed windows; no decoder opened
PROCESS index=5 exit=0
PASS six fixtures matched by SHA-256; exact frame totals and 18 quick windows verified on the A55
AVD_CONFIG_SHA256_UNCHANGED 268F6B7B6D8ED87A09892C40CB4823ADC266E1EF69989C782D20214E1DF18906
INSTALLED_APK_BEFORE_SHA256 6da85295829be2b6e2de0777e353461016799f5fe90c74897316703c90e50595
INSTALLED_APK_AFTER_VERSION 1.22.12 code333 SHA256 8799240c33a38b1a96d1c8aabb79e28611cb06c865c9c7b1c2014dffebec1c32 (changed outside this probe)
```
