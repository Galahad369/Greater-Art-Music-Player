# Greater Art 1.22.10 — A55 seek and preroll follow-up

Source under review: `fe12a84`, `codex/stack-picture-alignment-1.22.10`.
This documentation-only checkpoint records the previously completed standalone
probe. It does not change the candidate's code, APK, tests, or graphics settings.
The 45 s acceptance check still fails, and the candidate stays unmerged.

## Step 0: measured on A55

Ran a standalone `MediaExtractor` probe on the existing `GreaterArt_A55_API36`
emulator (API 36, 1080×2340). The six inputs matched the SHA-256 hashes from
the earlier acceptance run. No video decoder was opened. The probe reproduced
the app's 18 planned quick windows, timed `seekTo(PREVIOUS_SYNC)`, and read the
returned sync PTS. Preroll is the difference between requested start and sync
PTS. The [Android MediaExtractor reference](https://developer.android.com/reference/android/media/MediaExtractor#SEEK_TO_PREVIOUS_SYNC)
documents the seek mode. Each seek was timed once; these are not repeatability
or physical-device measurements.

- **18 windows:** 1.348 s total seek wall time; 30.333 s of source preroll.
- **First nine windows completed by quick:** 0.192 s total seek wall time;
  22.067 s of source preroll.
- **Interpretation:** Those first three videos completed in 42.391 s in
  the earlier quick run, versus 22.561 s estimated from each video's
  own wide-scan rate and requested footage. The 19.830 s difference is
  not a directly measured fixed cost. Applying those same rates to the
  measured preroll accounts for roughly 5.278 s, plus 0.192 s
  of seeks, leaving about 14.360 s unexplained by this model. The third
  companion had already started when the 45.034 s overall timer ended;
  it is not counted in the comparison.

All six are MP4 (ISO BMFF) with H.264/AVC at 1920×1080. Codec implementation
name, decoder time and buffer waits were not measured in Step 0.

| Take | FPS (extractor metadata) | Duration (s) | Sync samples | Median / max spacing (s) |
| --- | ---: | ---: | ---: | ---: |
| 0 Hatsune Miku | 24 | 188.000 | 54 | 3.250 / 7.000 |
| 1 Riona | 30 | 188.000 | 55 | 3.600 / 7.000 |
| 2 unc | 24 | 188.084 | 52 | 3.375 / 7.000 |
| 3 Forte | 24 | 188.125 | 87 | 1.708 / 5.000 |
| 4 Yoshino | 30 | 188.267 | 59 | 2.833 / 7.000 |
| 5 Morislei | 24 | 188.000 | 85 | 1.750 / 5.000 |

| Take | Section | Start (s) | End (s) | Previous sync (s) | Preroll (s) | Seek (ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 Hatsune Miku | 1 | 27.333333 | 35.333333 | 26.875000 | 0.458333 | 38.385 |
| 0 Hatsune Miku | 2 | 90.000000 | 98.000000 | 87.416666 | 2.583334 | 16.167 |
| 0 Hatsune Miku | 3 | 152.666666 | 160.666666 | 150.000000 | 2.666666 | 25.561 |
| 1 Riona | 1 | 25.333333 | 37.333333 | 19.900000 | 5.433333 | 18.241 |
| 1 Riona | 2 | 88.000000 | 100.000000 | 84.733333 | 3.266667 | 10.738 |
| 1 Riona | 3 | 150.666666 | 162.666666 | 149.300000 | 1.366666 | 11.752 |
| 2 unc | 1 | 25.333333 | 37.333333 | 19.875000 | 5.458333 | 24.444 |
| 2 unc | 2 | 88.000000 | 100.000000 | 87.833333 | 0.166667 | 17.362 |
| 2 unc | 3 | 150.666666 | 162.666666 | 150.000000 | 0.666666 | 29.230 |
| 3 Forte | 1 | 25.333333 | 37.333333 | 25.000000 | 0.333333 | 65.047 |
| 3 Forte | 2 | 88.000000 | 100.000000 | 88.000000 | 0.000000 | 26.061 |
| 3 Forte | 3 | 150.666666 | 162.666666 | 150.000000 | 0.666666 | 56.601 |
| 4 Yoshino | 1 | 25.333333 | 37.333333 | 19.900000 | 5.433333 | 75.071 |
| 4 Yoshino | 2 | 88.000000 | 100.000000 | 87.833333 | 0.166667 | 67.286 |
| 4 Yoshino | 3 | 150.666666 | 162.666666 | 150.000000 | 0.666666 | 115.069 |
| 5 Morislei | 1 | 25.333333 | 37.333333 | 25.000000 | 0.333333 | 516.999 |
| 5 Morislei | 2 | 88.000000 | 100.000000 | 88.000000 | 0.000000 | 42.389 |
| 5 Morislei | 3 | 150.666666 | 162.666666 | 150.000000 | 0.666666 | 191.857 |

## Original A55 logcat timestamps

```text
1793936358.105  6640  6640 I GA-StackPictureQA: QUICK analysed 0 / 5
1793936375.124  6640  6640 I GA-StackPictureQA: QUICK analysed 1 / 5
1793936388.201  6640  6640 I GA-StackPictureQA: QUICK analysed 2 / 5
1793936390.844  6640  6654 I GA-StackPictureQA: QUICK offsetsUs=[null, 0.0, null, null, null] elapsedMs=45034
1793936438.406  6710  6710 I GA-StackPictureQA: WIDE analysed 0 / 5
1793936486.921  6710  6710 I GA-StackPictureQA: WIDE analysed 1 / 5
1793936526.377  6710  6710 I GA-StackPictureQA: WIDE analysed 2 / 5
1793936566.573  6710  6710 I GA-StackPictureQA: WIDE analysed 3 / 5
1793936617.591  6710  6710 I GA-StackPictureQA: WIDE analysed 4 / 5
1793936661.849  6710  6710 I GA-StackPictureQA: WIDE analysed 5 / 5
1793936661.851  6710  6724 I GA-StackPictureQA: WIDE offsetsUs=[null, 0.0, 0.0, 17000.0, 0.0] elapsedMs=268246
```

Times above are device epoch seconds. Quick reached 2/5 companions, wide
reached 5/5 and passed. Offsets in the log are microseconds.

## Source answers and next check

- The reference is decoded once per attempt; each file uses one codec instance.
  Split windows seek to the previous sync sample and drain to EOS. The decoder
  is flushed between sections. Contiguous wide starts at zero on this corpus,
  so it skips the explicit initial `seekTo`.
- `OfflineAnalysisBudget` is a shared coroutine `Mutex` used by picture/audio
  analysis, waveform and thumbnail generation. The 45 s quick budget includes
  waiting. The decoder runs on `Dispatchers.IO` with no thread priority set in
  this path. The timed corpus invoked the engine without opening Stack; the
  old logs do not measure lock contention.
- `nearest()` tolerates 35 ms. The wrong short-window Yoshino results were
  emitted as confident (e.g. +9 ms at the centered 3.5 s configuration), not
  rejected. Do not weaken the test or use those windows as a drop-in default.
- The best next diagnostic is a 36-second span decoded continuously versus
  the identical span split into three adjacent 12-second sections, with
  counters for codec startup, time-to-first-frame,
  flush, EOS drain, dequeue waits and mutex waits. Hold the matcher and sampled
  PTS/luma sequence constant while comparing decoder scheduling on the
  production windows. Only try an
  early end-of-window exit if it preserves that sequence. Step 0 does not
  establish whether flush/EOS, refill, or queue waits are the dominant cost.
- Keeping only unmatched tracks in a wider retry is a separate change.
  Actual quick had one match, one checked without an offset, and three
  incomplete companions. The two-file wide estimate applies only after a
  complete quick run finds the other four. A partial retry also would not
  recheck competing offsets on already matched tracks.

Keep the A55 quick assertion red until the unchanged 45 s gate actually
passes. Do not describe the candidate as a verified release.

## Standalone diagnostic source

Save the following as `PictureSeekProbe.java` outside the app tree. The
recorded run compiled it with Java 21 targeting Java 17 against the API 36
`android.jar`, then used D8 with min API 26 to generate a DEX ZIP. The
ZIP ran with Android `app_process` on the existing A55 without installing
an APK. Pass the six local fixture paths in the order shown above and
verify that they match the acceptance corpus. No decoder is needed.

```java
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** Standalone extractor-only diagnostic; no application, decoder, or media writes. */
public final class PictureSeekProbe {
    private static final long MAX_ANALYSIS_US = 210_000_000;
    private static final long QUICK_SECTION_US = 8_000_000;
    private static final long QUICK_PADDING_US = 2_000_000;

    public static void main(String[] paths) throws Exception {
        if (paths.length != 6) throw new IllegalArgumentException("Expected six files in corpus order");
        long referenceDuration = 0;
        long totalPreroll = 0;
        long firstThreePreroll = 0;
        long totalSeekNs = 0;
        long firstThreeSeekNs = 0;
        int windowCount = 0;
        for (int take = 0; take < paths.length; take++) {
            MediaExtractor extractor = new MediaExtractor();
            try (FileInputStream source = new FileInputStream(paths[take])) {
                byte[] head = new byte[8];
                int length = source.read(head);
                String container = length == 8 && head[4] == 'f' && head[5] == 't' &&
                    head[6] == 'y' && head[7] == 'p' ? "ISO-BMFF" : "unknown";
                source.getChannel().position(0);
                extractor.setDataSource(source.getFD());
                int video = 0;
                while (video < extractor.getTrackCount()) {
                    String mime = extractor.getTrackFormat(video).getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) break;
                    video++;
                }
                if (video == extractor.getTrackCount()) throw new IllegalArgumentException("No video track: " + take);
                MediaFormat format = extractor.getTrackFormat(video);
                long actualDuration = format.getLong(MediaFormat.KEY_DURATION);
                long duration = Math.min(actualDuration, MAX_ANALYSIS_US);
                if (take == 0) {
                    referenceDuration = duration;
                    if (duration <= 3 * QUICK_SECTION_US) throw new IllegalArgumentException("Reference is shorter than expected");
                }
                extractor.selectTrack(video);
                Number fps = format.containsKey(MediaFormat.KEY_FRAME_RATE) ? format.getNumber(MediaFormat.KEY_FRAME_RATE) : null;
                System.out.printf(Locale.ROOT, "TAKE %d container=%s mime=%s width=%d height=%d fps=%s durationUs=%d%n",
                    take, container, format.getString(MediaFormat.KEY_MIME), format.getInteger(MediaFormat.KEY_WIDTH),
                    format.getInteger(MediaFormat.KEY_HEIGHT), fps, actualDuration);
                for (int region = 0; region < 3; region++) {
                    long center = referenceDuration * (2 * region + 1) / 6;
                    long radius = QUICK_SECTION_US / 2 + (take == 0 ? 0 : QUICK_PADDING_US);
                    long start = Math.max(center - radius, 0);
                    long end = Math.min(center + radius, duration);
                    if (start >= end) throw new IllegalArgumentException("Missing region " + region + " on take " + take);
                    long before = System.nanoTime();
                    extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    long seekNs = System.nanoTime() - before;
                    long sync = extractor.getSampleTime();
                    if (sync < 0 || sync > start || (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) == 0) {
                        throw new IllegalStateException("Seek failed to land on a preceding sync sample: " + take + "/" + region);
                    }
                    long preroll = start - sync;
                    System.out.printf(Locale.ROOT,
                        "WINDOW take=%d region=%d startUs=%d endUs=%d syncUs=%d prerollUs=%d seekMs=%.3f%n",
                        take, region + 1, start, end, sync, preroll, seekNs / 1e6);
                    windowCount++;
                    totalPreroll += preroll;
                    totalSeekNs += seekNs;
                    if (take <= 2) {
                        firstThreePreroll += preroll;
                        firstThreeSeekNs += seekNs;
                    }
                }
                // Report GOP spacing after the timed seeks so enumeration does not warm them.
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                long previousSync = -1;
                int syncCount = 0;
                var spacing = new ArrayList<Long>();
                do {
                    long time = extractor.getSampleTime();
                    if (time < 0) break;
                    if ((extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        if (previousSync >= 0 && time > previousSync) spacing.add(time - previousSync);
                        previousSync = time;
                        syncCount++;
                    }
                } while (extractor.advance());
                if (spacing.isEmpty()) throw new IllegalStateException("Cannot establish keyframe spacing for take " + take);
                Collections.sort(spacing);
                System.out.printf(Locale.ROOT, "SYNC take=%d count=%d spacingMedianUs=%d spacingMaxUs=%d%n",
                    take, syncCount, spacing.get(spacing.size() / 2), spacing.get(spacing.size() - 1));
            } finally {
                extractor.release();
            }
        }
        System.out.printf(Locale.ROOT,
            "PASS extractor only: windows=%d prerollUs=%d seekMs=%.3f firstThreePrerollUs=%d firstThreeSeekMs=%.3f%n",
            windowCount, totalPreroll, totalSeekNs / 1e6, firstThreePreroll, firstThreeSeekNs / 1e6);
    }
}
```
