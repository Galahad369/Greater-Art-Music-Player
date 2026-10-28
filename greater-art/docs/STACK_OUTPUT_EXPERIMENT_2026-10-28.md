# Stack output experiment — 28 October 2026

## Scope and decision

User approved baseline measurement and an isolated shared-output experiment.
This is not approval to replace production, install a new DSP library, change
media quality, rewrite history, publish a release or remove existing functions.

Production engine baseline: main `84db4e20a53af35044523fabbb8fcc4bf64baf1d`,
Android 1.21.80/code303. The 1.21.81/code304 changes instrumentation, mandatory
version metadata and a quality guard in the unrouted experimental PCM reader.
No production engine, manifest, resources, dependencies or decoder settings
changed. The existing PCM prototype still
requires an explicitly enabled debug build; normal builds remain default-off.

**Adoption decision: NO-GO until every required feature/fidelity gate passes.**
Successful generated-tone timing alone cannot change this decision.

## Separate measurements

1. Alignment estimation: source-time correspondence against checked musical
   landmarks. The tone experiment has exact zero offsets and does not test v8.
2. Output synchronization: inter-voice spread/drift in actually captured audio,
   not `currentPosition` equality.
3. Transport latency: command to emitted sound, pause, seek, resume and loop.
   Accurate absolute latency additionally needs capture timestamp calibration.
4. Video/UI performance: decoder drops, frame times and surface continuity.
   These cannot be inferred from an audio-only emulator test.

## Predefined acceptance checks

These checks were written before the new engine-capture results were analyzed.

| Gate | Required evidence before adoption |
|---|---|
| Probe validity | Synthetic signed-delay recovery <0.1ms; same-path single-file calibration; no missing/ambiguous voice association silently treated as zero |
| Generated-PCM timing | Three runs per engine; report complete-burst count and analyzed duration. Candidate p95 all-voice spread <=0.1ms after calibration, with >=95% of detected reference bursts complete |
| Stability | No engine error, lost lane or observed underrun in the bounded comparison. Report peak mix time and decoder waits without treating a short zero-underrun snapshot as a deadline/thermal pass |
| Fidelity | Actual decoded/output format, no silent truncation/downsampling/channel loss; headroom/clipping, resampler error, stereo and high-resolution checks; listening and longer target-hardware testing |
| Mapping | Fractional signed offsets and nonidentity tempo maps must work; preserve uncertain decisions/manual edits and independent pitch behavior |
| Transport | Live speed/pitch, mute/solo/gain, primary change, seek/reset/preroll, existing unequal-length loop/EOF semantics, saved state and error recovery |
| Integration | SAF sources, audio focus/routes/noisy events, primary native-quality video and all existing presentations; no duplicate legacy audio |
| Performance | Release-like physical-device traces and sustained CPU/thermal/underrun testing, including 6–8 compressed takes and relevant video load |

Missing features disqualify adoption regardless of a better timing number.
The shared-output prototype currently refuses nonidentity scales and has no live
speed/pitch or production-video adapter. Those remain blocking gaps, not passes.

## Reproduction

- Use the dedicated existing API36 `GreaterArt_Scroll_QA36` AVD. Its configuration
  reports 2GB RAM and software graphics. Do not describe this as an A55 acoustic,
  real-GPU or 8GB physical performance result.
- Preserve the verified 1.21.80 APK. Do not overwrite any versioned release.
- Freeze source/build writers. Never measure while Gradle, another test or
  another playback experiment is running.
- Generate the six 60-second 44.1kHz frequency-isolated WAV fixtures and their
  one-file calibration with the user's supplied probe. Keep raw WAV/CSV/logs
  outside the repository under `App/output/stack-output-baseline-20261028/`.
  The supplied analyzer SHA-256 is
  `f5aa5075252af61cd2a64d5985a769c63e67e683f4e886742ff133483801b40e`.
  It is not redistributed or installed as a production dependency.
- Install the ordinary debug app and its instrumentation APK. Grant RECORD_AUDIO
  only to `com.local.listentomusic.test`, never the production package.
- Start a single `StackOutputProbeTest` method with `probeCaptureName` set to a
  fresh filename matching `[a-z0-9_-]{1,40}.wav`. Await its exact READY log.
- Launch `StackOutputCaptureActivity` in the test APK with the same `name`.
  Explicitly choose **one app -> Greater Art**, then press the measurement
  button. Denied consent is a failed measurement, not permission to bypass it.
- The test waits for the recording-start acknowledgement before playing. A
  missing acknowledgement fails after 120 seconds. Both engines use the same
  foreground/quiet-start preferences, restored afterward. Production invokes
  the actual service Stack command, not a hand-built primary player.
- Wait for both instrumentation completion and capture DONE before another
  invocation. Starting instrumentation mid-play terminates the target process.
  Match DONE and the exact destination on the **same log line**, and verify the
  finalized RIFF header and byte count. Joining log lines can falsely match an
  older DONE followed by a new START, causing an incomplete transfer.
- Analyze calibration and save its per-frequency bias; analyze every run with
  that bias and retain CSV plus complete-burst count, spread and drift metrics.
- After pre-calibration, build only with `-PstackPcmPrototype=true` for measurement.
  Alternate production1/shared1/production2/shared2/production3/shared3 on this
  same app/test binary, then repeat calibration. The production method still
  calls the actual unchanged service coordinator, never the prototype. This
  avoids a different-binary and grouped-order comparison; it is still not a
  randomized physical-device performance study. Mark the binary as a test
  variant, not a distributable release. Restore the ordinary default-off build
  afterward.
- Revoke the test capture permission and stop the separate capture service.

Calibration uses `playSingleMixedCalibration`; baseline uses
`playSixZeroOffsetProbeVoices`. Default playback duration is 60 seconds;
`probeDurationMs` permits 10–60 seconds for bounded functional checks, not
silently shortening the repeated timing comparison.

The supplied analyzer searches +/-150ms around reference bursts spaced 500ms
apart. Skew outside that window, missing bursts, clock resets and ambiguous
matching need explicit investigation. It cannot establish nanosecond accuracy.
Its mono PCM16 capture at 44.1kHz measures relative timing only; it cannot certify
the float/stereo/high-resolution quality of the real playback path.

## Results

- Synthetic control: all six injected signed delays recovered; maximum median
  error 0.0049187ms. In-memory fixture, no Android output involved.
- Unchanged 1.21.80 baseline: unit/lint/debug app/test build succeeded on Java21;
  Gradle reported 6m26s, 8 executed/77 up-to-date tasks. Cached unit results are
  not represented as newly executed tests.
- Versioned harness and native float precision checks are exercised below.
- Default-off final gate: 448 JVM tests, no failures/errors; lint 0 errors,
  46 warnings/4 hints; debug app/test assembly succeeds. Three focused API36
  cases pass in 1.213s: native mono/stereo clipping/cancellation, safe float
  refusal and disabled-gate isolation. The float result is refusal, not a
  native high-resolution playback pass.
- Pre-calibration: 116/116 bursts for every voice across 57.5s; all-voice spread
  rounds to 0.00ms. Per-frequency median biases are below 0.0024ms. Capture
  completed with 3,080,192 samples. System UI showed an ANR during consent setup
  and recovered after Wait; no audio had started. Record this setup fault, do
  not classify it as a Greater Art crash or include it as a player timing result.
- Post-calibration: 115/116 expected reference bursts detected, with all six
  voices present in each of those 115; useful span 57.0s, 500ms reference cadence.
  Corrected spread rounds to 0.00ms and per-frequency median offsets remain at
  the calibration floor. One missing reference burst is recorded, not counted
  as zero error. This passes the 95% completeness threshold but is not proof
  of a lossless capture clock. All eight analyzed WAVs match the finalized
  device originals by SHA-256, including the re-pulled third shared recording.
- Enabled API36 functional coverage: two cases pass in 17.591s, including eight
  PCM voices, pause/seek/solo/loop/interruption-handler resume and pre-lease float
  refusal. The eight-voice steady snapshot has **3 underruns** and peak mix
  **141,719us** for a 5,333us output block. Later loop/reset counters return to
  zero; those final zeros do not erase the earlier failures. This is a functional
  pass but a stability NO-GO. No six-compressed-take log was emitted by the
  optional fixture section, so it is not counted as exercised real-take coverage.

### Calibrated repeated comparison

All rows below use the same enabled debug app/test pair. The production method
uses only the existing service route; there is still no production prototype
adapter. Capture duration means useful detected bursts, not the test sleep.

| Pair | Production median / p95 / max spread (ms) | Shared median / p95 / max spread (ms) | Complete bursts |
|---|---|---|---|
| 1 | 51.22 / 67.01 / 73.20 | rounds to 0.00 / 0.00 / 0.00 | 116/116 in both |
| 2 | 54.82 / 95.96 / 107.81 | rounds to 0.00 / 0.00 / 0.00 | 116/116 in both |
| 3 | 24.64 / 50.13 / 92.54 | rounds to 0.00 / 0.00 / 0.00 | 116/116 in both |

Shared pair1 logs 0 underruns/0 decoder waits and peak mix19,706us; a buffered
short run is not a worst-case deadline pass. Samples captured varied between
runs; analysis uses the actual complete-burst window. These are generated native
PCM probes, not multi-singer alignment or physical acoustic validation.
The displayed sub-sample estimates are not literal timing accuracy: the capture
sample interval is about 22.7us. Report shared timing conservatively as below
0.01ms in this matched-filter experiment, never as nanosecond or zero error.

| Shared repeat | Reported underruns | Peak mix (us) | Useful burst span (s) | Largest reference interval (ms) |
|---|---:|---:|---:|---:|
| 1 | 0 | 19,706 | 57.5000 | 500.0 |
| 2 | 28 | 247,304 | 58.4810 | 1,351.9 |
| 3 | 0 | 13,310 | 57.5000 | 500.0 |

These are debug-emulator runs, not a controlled hardware benchmark. Light source
inspection/CSV summarization occurred on the host between and during some runs;
no concurrent Gradle build or second playback experiment ran during capture.
Host scheduling and capture-clock effects are unmeasured; neither best-case
timing nor the worst stall is generalized to a physical phone.

**Relative spread is not continuity.** Shared pair2 logs **28 underruns**,
0 decoder waits and peak mix **247,304us**. Its reference-burst span is 58.48s,
with a maximum adjacent interval of
**1,351.9ms** instead of 500ms. All six voices can pause together while retaining
near-zero inter-voice spread. The captured interval alone cannot attribute every
gap to the engine rather than the emulator/capture path, but the independent
underrun counter already fails the stability gate. Production pair2 also has a
523.3ms maximum interval. Inspect cadence and counters, not only the spread table.

The original pair2 setup attempt (`pcm02_1028`) timed out waiting for capture;
recording began after its 120s gate had expired, so no playback was measured.
Its failed instrumentation log is retained and excluded. The fresh `pcm02b_1028`
run above is the valid repeat, not an overwritten failure. No production failure
or successful engine test is inferred from the setup timeout.

The first local transfer of shared pair3 had a zero/unfinalized WAV header. An
outside-repository runner had joined log lines before matching DONE, allowing an
older completion and newer START to satisfy the check. The analyzer rejected it.
The runner now checks one exact completion line. After actual DONE, the same
device recording was pulled as `pcm03_1028-finalized.wav` and analyzed successfully;
no audio or header was reconstructed. The incomplete copy and analyzer failure
remain preserved. Pair3 uses only the finalized original recording.

### Additional source-level parity gaps

- The prototype stops/loops at the primary duration. Current production can
  promote a longer surviving take and uses mapped session duration. Unequal-length
  EOF/loop behavior needs an explicit parity test and product decision, not an
  accidental change during adoption.
- Prototype gain normalization uses `max(1, sum(audible gains))`, while production
  divides each slot gain by audible-track count. With six gains at0.5, the
  prototype doubles each lane's effective gain relative to production. Unity-gain
  probes match, but fractional-level semantics are not qualified. No gain policy
  was changed in this experiment.

### Native float failure and fail-closed correction

The first native 96kHz stereo IEEE-float WAV test failed twice: a source sample
of 0.00001 became 0.0. Direct block inspection ruled out a lane/timeline error.
The device reported `bits-per-sample=32`, `pcm-encoding=2` (PCM16), rate96000 and
two channels. Android's platform extractor had already reduced the samples.
[AOSP source](https://android.googlesource.com/platform/frameworks/av/+/e1368e4257fa747e78eee204f136e67e176fbef9/media/extractors/wav/WAVExtractor.cpp)
documents app-UID float-output restrictions and this conversion.

The experimental reader now checks source precision against extractor output
both in raw-source preflight and before delivering decoded PCM. The observed
float-WAV conversion and unknown raw-PCM16 precision are refused before acquiring
legacy output. Compressed decoder output is checked again before delivery; its
actual encoding may be unavailable until decoding starts. This is not an
implementation of high-resolution WAV support. The device regression
must either observe exact native float samples or explicit refusal, and enabled
prototype builds must prove refusal occurs without acquiring the audio lease.
Passing safe-refusal coverage leaves fidelity/format parity **blocked**.

Media3 already has a source-preserving WAV extractor, so future support should
evaluate that existing component rather than immediately inventing a new WAV
parser. No extractor replacement or library adoption is part of this experiment.

## Final restoration and artifacts

- Ordinary build restored without `-PstackPcmPrototype=true`; pinned Gradle9.8.2
  wrapper via Java21 completed unit/lint/debug app/test tasks in 41s (4 executed,
  10 from cache, 71 up-to-date). The 448 passed JVM results were retained; the
  final restoration did not freshly execute all 448. Lint: 0 errors, 46 warnings,
  4 hints. Earlier exact-executable-source default-off gates ran successfully.
- Fresh API36 ordinary-build verification: **6/6 passed in 6.51s**. Native
  mono/stereo clip/cancellation, explicit float refusal, disabled gate, production
  paused seek/resume, allocation-failure retention and unequal-length survivor EOF.
  This is bounded coverage, not proof that every app function is bug-free.
- Test capture service stopped; test-APK RECORD_AUDIO permission revoked and
  `granted=false` confirmed. No recording permission was added to production.
- Package `com.local.listentomusic`, version1.21.81/code304; v2 signing verified
  against pinned certificate SHA-256
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.
- Internal APK (not a release):
  `App/output/stack-output-baseline-20261028/GreaterArt-1.21.81-default-off-final-test.apk`;
  SHA-256 `0b8e3359740d7b287f8b362b5b016048850663f6d582dcf4c450f5bd8f599450`.
  The enabled measurement APK is separately labeled and must not be delivered
  as a production upgrade.
- Preserved local evidence includes raw WAVs/CSVs, individual instrumentation and
  analysis logs, `capture-summary.json`, `verified-capture-hashes.json` and
  `capture-and-prototype-logcat.log`. The incomplete transfer and failed setup
  logs remain separate. Local filenames are not public release URLs.
- Version consistency and changed-file credential/workstation-path pattern
  checks pass. This is not a full historical privacy or runtime security audit;
  blocked privacy work is untouched. Remote main remained baseline84db4e2 at
  final inspection. No PR, push, release replacement or history rewrite performed.

## Decision and next gate

The shared-clock architecture is worth continuing: unlike separate outputs,
it kept the six generated lanes coincident in all three measured runs. It is
**not ready for production replacement**: bounded stability failed, high-resolution
support is incomplete and mapping, live tempo/pitch, gain/EOF parity, SAF and
native-quality video integration remain unqualified. Fix and measure those gaps
inside the isolated prototype before seeking production adoption approval.

No real-song backing alignment, audible multi-singer improvement, release
readiness or physical-device performance is claimed. The production engine and
all existing presentations remain unchanged; no resolution, bitrate, FPS or
source-quality cap was added. No new release artifact is promoted.

## References

- [Android playback capture](https://developer.android.com/media/platform/av-capture)
- [AudioTrack timestamp and underrun APIs](https://developer.android.com/reference/android/media/AudioTrack)
- [Prior prototype and acceptance gaps](agent-handoff/146-shared-clock-stack-mixer.md)
- [Previous backing-alignment/output measurements](STACK_BACKING_ALIGNMENT_1.21.27.md)
