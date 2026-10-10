# Backing alignment and actual output — 1.21.27 / 250

Local SOURCE_ONLY work, 10 October 2026. No APK release is promoted, no user
recording is required, and no production recording permission is added.

## Two different failures

1. **File correspondence:** v7 rejected repetitive local matches before comparing
   the whole timeline. One incompatible outro could also spoil five good anchors.
   Stereo-side uncertainty sometimes vetoed a strong mono map.
2. **Output timing:** separate AudioTracks and pitch-preserving speed correction
   can emit audio at different times even if player positions agree. Improving
   file offsets alone cannot solve this second failure.

## Implementation

- Preserve v7 HPSS/percussion/bass/rotated-chroma descriptors and bounded decoding.
- Keep up to four strong candidates per region. Fit distributed maps using one
  vote per region, at least three anchors, 40% timeline coverage, 75% support
  among populated regions, and the existing residual/scale/rotation limits.
- Reject similarly supported conflicting maps rather than selecting a repeated
  chorus arbitrarily. Standalone local matching remains strict.
- If stereo-side matching is uncertain, require at least four strong mono
  anchors, correlation >=0.6 and residual <=5 ms, then at least three consistent
  native-rate attack confirmations. A confident conflicting side map still vetoes.
- Store accepted and abstained decisions atomically in `stack-align-v8-maps`,
  bounded to 96 pairs. Identity includes source paths/statistics, clip endpoints,
  durations and algorithm version. Malformed cache data recomputes.
- No production floor reduction: the Baka diagnostic's 0.4 candidate floor is
  deliberately test-only; normal matching remains 0.5.

## Real-file evidence

The initial unchanged production diagnostic rejected its first tested pair in
each requested family. The subsequent full corpus exercised 16 pairs and accepted
seven with distributed consensus. These tests log decisions and validate finite
maps; they **do not assert ground-truth audible correctness**.

With native confirmation enabled, the six-take 孤独毒毒 corpus uses the Miku take
as primary and produces:

| Companion | Offset | Scale | Decision |
| --- | ---: | ---: | --- |
| モリスレイ | +18.953 ms | 1.0000337521 | accepted, 5 anchors |
| unc / あんく | +83.216 ms | 0.9999995224 | accepted, 6 anchors |
| ふぉるて | unchanged | 1 | native confirmation insufficient |
| 響咲リオナ | +0.650 ms | 1.0000031311 | accepted, 6 anchors |
| 吉乃 | +16.185 ms | 0.9999897646 | accepted, 4 anchors + native confirmation |

This five-pair run passed in 13.071 seconds with descriptors already warm.
New aligner instances reloaded decisions in 31–48 ms. Cold corpus comparisons
took roughly 18–86 seconds on the emulator; do not describe cold analysis as
instant. Baka self/Ado remains uncertain: shared body hits and a different intro
do not yet provide a sufficiently confirmed map. Several live/alternate
arrangements also abstain. These are unresolved requirements, not completed fixes.

## Output measurement without asking the user for a recording

`StackOutputProbeTest` plays six frequency-isolated WAV voices through the actual
Stack coordinator. `StackOutputCaptureActivity` and `StackOutputCaptureService`
live exclusively in the instrumentation APK. Android consent is **Share one app
→ Greater Art**; capture additionally fixes `addMatchingUid` to Greater Art.
No microphone input, unrelated-app capture, or production permission is used.

The user's supplied Python probe generated six 60-second files and a calibration
file. A synthetic known-delay check recovered all injected signed offsets within
0.1 ms; that validates the measuring tool only.

Actual API36 emulator internal capture:

- Six independent players: 60 useful bursts over 30 seconds; calibrated all-voice
  spread median **72.92 ms**, p95 **111.58 ms**, max **116.51 ms**.
- Single-file calibration: 70 useful bursts over 35 seconds; all-voice spread
  rounds to **0.00 ms**. Constant per-frequency biases were below 0.003 ms.
- The first Stack probe was interrupted after its useful window by another
  instrumentation invocation. Do not call it a full uninterrupted minute.
- Raw captures and CSV live outside the repository under
  `App/output/stack-sync-probe-20261010/`; no user media was overwritten.

This proves a real **emulator output** problem, not acoustic performance on a
Samsung phone. It does not justify another dead-zone tweak or an unsupported
claim of sample accuracy. The default-off shared-clock prototype comparison
measured **44 useful bursts over 22 seconds**, with median/p95/max all-voice
spread rounding to **0.00 ms** through the same calibrated internal-capture path.
Its playback test passed in 64.098 seconds; snapshot: zero underruns, zero
decoder waits, peak mix 29,772 us for a 5,805 us block. A buffered run without
underruns is not a worst-case CPU/thermal pass. This is six generated native-rate
PCM probes, not aligned compressed cover vocals or physical-phone listening.
Production-host adoption and pitch-preserving non-identity tempo support remain
explicit gates. The ordinary build has been restored to default-off.

### Reproduction

1. Install the app and `app-debug-androidTest.apk`; grant RECORD_AUDIO only to
   `com.local.listentomusic.test`. Push generated fixtures to
   `/sdcard/Download/GreaterArtSyncProbe`.
2. Start the test capture Activity with a unique `name` ending in `.wav`.
   Select **Share one app**, then Greater Art. Wait for the measurement button.
3. Run `StackOutputProbeTest#playSixZeroOffsetProbeVoices`, then press the visible
   **Measure Greater Art output** button. Do not start another instrumentation
   run until this playback test finishes.
4. Wait for `GreaterArtOutputCapture`'s DONE entry before pulling the WAV.
5. Repeat with `playSingleMixedCalibration` and a fresh consent/name; run the
   supplied Python `analyze --save-bias`, then analyze Stack with `--bias`.
6. `playSixSharedClockProbeVoices` requires the explicit debug property
   `-PstackPcmPrototype=true`. It is not a production route or release build.

Standalone capture initially failed because Kotlin runtime classes are supplied
by the target APK only during instrumentation, and because capture attempted to
start from the background after the chooser. Framework-only Java components and
a foreground start button fixed those test-harness failures. They were test APK
failures, not production crash evidence. JUnit diagnostics must explicitly return
Unit; `Log.i` otherwise makes an expression-bodied test return Int.

## Verification boundary

Final default build: 305 JVM tests pass; lint has zero errors, 30 warnings and two
hints; app and instrumentation APK builds pass. Cache-corruption plus six-take
seek/resume/loop passed together (2 tests, 68.417 seconds). The enabled shared-clock
probe is a separate variant, not silently adopted into production.
The final installed default-off gate plus six-take transport regression passed
(2 tests, 17.129 seconds). Test capture permission was revoked afterward.

The unchanged version guard currently fails because the pre-existing untracked
`releases/GreaterArt-1.21.26.apk` is newer than the owner-verified 1.21.18 artifact.
It is preserved, not overwritten, deleted, relabeled or retroactively verified.
Source metadata is 1.21.27/250; no release is promoted. No perfect-sync,
crash-free, or physical-device claim is made.

## Next required engineering work — not completed by this patch

1. Integrate one exclusive production Stack audio sink without restarting or
   reducing the primary video, preserving transport, seek, loop, clips, gains,
   mute/solo, focus and graceful codec failure.
2. Qualify pitch-preserving per-lane tempo processing at native precision/rates;
   the prototype refuses non-identity scales instead of shifting pitch or
   reducing precision. Do not simply turn the debug flag on in ordinary builds.
3. Repeat calibrated output tests for start, seek, resume, loop and eight lanes,
   with output-based assertions, then real compressed cover listening/performance.
4. Improve uncertain cover correspondence separately: Baka's intro/body mismatch
   and the Forte native-confirmation failure need evidence, not invented offsets.
   Different live arrangements may need constrained piecewise maps, not one line.
