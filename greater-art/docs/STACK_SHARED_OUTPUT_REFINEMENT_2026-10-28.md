# Shared-output refinement — 28 October 2026

## Release gate

The user chose **finish full shared-output parity before making a release**.
Do not ship a default-off maintenance build as completion of this request.
Production remains on the existing engine until the candidate passes every gate.
No existing verified artifact, signing identity or source-quality setting changes.

Baseline: `bed9c0b`, 1.21.81/code304. Current work: 1.21.82/code305, SOURCE_ONLY.
The [previous experiment](STACK_OUTPUT_EXPERIMENT_2026-10-28.md) measured better
relative timing but observed 28 shared-output underruns. It is not erased by a
later passing run.

## Implemented candidate changes

- Use the already-depended-on Media3 1.11.1 `WavExtractor`/`SampleQueue`, not a
  homegrown parser or Android's truncating app-UID WAV path. Bounded samples,
  native rate/stereo, frame-aligned clips/seeks and local/content sources. No
  whole-file decoded cache, new dependency, network loader or quality fallback.
- Exact integer PCM8/16/24 conversion and float32 samples. Integer PCM32 and
  unsupported precision still fail closed before the output lease, rather than
  pretending a float32 mixer preserves every source integer bit.
- Dedicated audio-priority render worker instead of the shared coroutine pool.
  Reuse output/gain/native-window arrays; avoid per-sample deque lookups and
  redundant full PCM chunk copies. Batch 1,024 frames instead of 256 and reuse
  the exact native fractional-delay kernel. Keep all 32 sinc taps, stereo and
  double accumulation; batching has a sample-for-sample equivalence test.
- Native-rate decode-ahead is bounded to about 250ms per lane (rather than four
  chunks: only 21ms at 192kHz), roughly 3MiB for eight stereo 192kHz voices.
- 100ms bounded output capacity, roughly 83ms prefill for scheduling headroom.
  This trades a bounded startup buffer for resilience, not source precision,
  resolution, frame rate or bitrate. Pause/seek cancellation must stay bounded.

## Observed verification so far

- Explicit enabled build: unit/lint/debug app/test assembly succeeded in 4m58s;
  451 JVM tests passed. Final lint counts to be recorded after review.
- API36 native decoding: 4/4 pass in 1.77s. Strict 96kHz stereo float retains
  sub-PCM16 values; 192kHz stereo PCM24 retains exact low bits across clip/seek,
  including an odd-length final-frame count. Native PCM16 clip/cancellation still
  passes. Unsupported integer PCM32 does not acquire the audio host lease.
- First dedicated-worker attempt failed at 48kHz with 8 underruns. Decode-ahead
  then passed 20s at 48kHz but failed at 192kHz with 1 underrun. Larger batches
  passed 20s at each native rate with zero reported underruns (49.992s test).
- Fractional-delay stress then passed its 48kHz stage but failed at 192kHz with
  20 underruns; peak mix21,007us versus a 5,333us output block. It retains the
  zero-underrun assertion. The next candidate gathers bounded native windows
  before mixing to remove the repeated lookup cost. That next candidate passed
  the same 20s-per-rate test at 48/192kHz with zero underruns/decoder waits
  (52.433s total). This is bounded native-WAV emulator coverage, not a phone pass.
- The exact cached-kernel test initially compared unclipped interpolation to a
  clipped mix near full scale. Keep the clipping contract; rerun the high-frequency
  comparison at 0.3 amplitude, unchanged 1e-6 tolerance. All 453 JVM tests pass.
- Existing desktop graph headers compiled in an isolated reference harness:
  9/9 checks pass, including 2,000 nodes and obsolete-generation rejection.
  No desktop source, installed executable or release was changed.
- Scoped source: API36 test passes with All Files Access denied. Native float
  values remain exact through a MediaStore `content://` URI; deleting that owned
  row prevents audio-lease acquisition. Original storage mode is restored.
  This tests deletion, not every SAF-provider grant-revocation behavior.

### Full-minute repeated capture

Same frozen candidate app/test pair, 44.1kHz six-lane native PCM fixtures, one-app
capture consent, pre/post calibration, and SHA-256 comparison with device originals.
All five recordings recover 116/116 complete bursts over a 57.5s useful span.
The capture sample interval remains 22.7us; sub-sample estimates are not literal
nanosecond accuracy.

| Candidate run | p95 spread (ms) | Largest burst interval (ms) | Underruns / decoder waits | Peak mix / prepare / render (us) |
|---|---:|---:|---|---|
| A | below 0.01 | 500.0 | 0 / 0 | 8,852 / 45,088 / 53,273 |
| B | below 0.01 | 500.0 | 0 / 0 | 6,523 / 43,744 / 50,279 |
| C | below 0.01 | 500.0 | 0 / 0 | 11,062 / 25,413 / 36,489 |

Unlike the earlier failed run, no extra shared pause appears in the detected
reference cadence. These are repeated generated probes, not an independent
physical-device performance study or real backing-music alignment. B/C WAVs are
byte-identical, but distinct app/capture process events, filenames and modification
times confirm separate recordings of the deterministic fixture. All copied WAV
hashes match their device originals. Failed earlier candidates remain retained.

### Core restoration (not release)

- Final ordinary/default-off build: unit/lint/debug app/test tasks pass in 2m40s;
  453 JVM tests, 0 failures/errors. Lint 0 errors / 47 warnings / 4 hints. The
  extra warning is a `String.toUri` style suggestion, not a suppressed safety error.
- Final enabled pause/seek/solo/loop/interruption-handler test passes 1/1 in
  9.983s. Final ordinary-build precision/isolation plus production pause/seek,
  allocation-failure retention and survivor-EOF cases pass 8/8 in 5.997s.
- Recording permission is revoked (`granted=false`); storage app-op restored to
  its original allow mode after the scoped-only test. Existing release unchanged.
- Internal default-off core APK:
  `App/output/stack-refinement-20261028/GreaterArt-1.21.82-default-off-core-test.apk`;
  SHA-256 `23795636cec048ab51be0cbf9507906ed5b88479b8059c92f2ee9c85b4624aeb`.
  Version1.21.82/code305 and pinned certificate verified. This is **not a release**;
  the user explicitly requires complete parity first. Do not put it in releases.

## Android parity checkpoint — 1.21.83/code306

Scope is mobile only. Do not control, change or package the desktop app. Existing
compiled-reference numbers are used only by Android JVM tests.

- Partial gain, mute and solo now match the actual production normalization,
  including an unmuted zero-gain lane. Canonical slot level state is updated too.
- The existing Media3 float Sonic processor passes component tests at48/96/192kHz:
  stereo/native rates, quiet1e-7 signals, independent pitch and0.999/1.001 tempo.
  It is **not integrated into shared playback** yet; real-time cost, dynamic
  transport and source/master mapping are not certified by these tests.
- Native WAV analysis reuses the same extractor as playback, with no platform
  float/PCM24 truncation. Decoder revision2 invalidates only generated descriptor
  and alignment-decision cache keys, not saved offsets. The Android quiet-float
  window/coarse-analysis test passes1/1 in0.522s.
- Default-off unit/lint/app/test build passes in2m26s. Final unit rerun including
  Android graph checks against captured compiled-reference values passes457 tests,
  zero failures/errors in1m33s. No new release or production-route switch.

### Actual local backing corpus

Twelve original files were found in the user's existing local media library and
copied to the owned API36 emulator, not uploaded. Originals were unchanged. The
existing four-family audit completed in301.321s; every persisted warm result was
equal to the cold decision (19–61ms warm lookup). Neither instrumentation success
nor a confident map proves audible synchronization.

| Pair / companion | Decision | Offset ms | Scale | Anchors / maximum fit residual ms |
|---|---|---:|---:|---|
| Bitter Choco: self cover → 宮下遊 | confident |283.436883|0.9999940703|3 /0.740397|
| Baka: self cover → Ado | abstain |—|—|0 /—|
| Gambling: syudou → YuNi | abstain |—|—|0 /—|
| 孤独毒毒: Miku → モリスレイ | confident |18.952560|1.0000337521|5 /1.511745|
| 孤独毒毒: Miku → uncあんく | confident |83.215981|0.9999995224|6 /0.731348|
| 孤独毒毒: Miku → ふぉるて | abstain |—|—|0 /—|
| 孤独毒毒: Miku → 響咲リオナ | confident |0.650085|1.0000031311|6 /0.816009|
| 孤独毒毒: Miku → 吉乃 | confident |16.184925|0.9999897646|4 /0.745373|

These are decoder-derived affine fits, not human annotations or acoustic output
measurements. Do not silently treat an abstention as aligned at zero. Retain the
confidence gates and investigate the missing correspondence evidence first.
Private local logs: `App/output/stack-refinement-20261028/real-corpus-analysis-device.log`
and `real-corpus-analysis-results.log`; do not commit original recordings or local paths.

## Remaining full-parity gates

1. Sustained 6–8-lane compressed/high-resolution output, cadence and underruns;
   physical CPU/thermal and acoustic testing remain separate from an emulator.
2. Accepted nonidentity alignment scales with pitch preserved; independent global
   tempo/pitch and dynamic changes without hidden PCM16 conversion.
3. Match production gain/solo/mute, primary rebasing, unequal-length survivors,
   loop/EOF, add/remove/restore and error semantics.
4. Primary native-quality video, all surface owners, SAF revocation, focus/routes,
   lifecycle, saved Stack and interrupted transport with no duplicate legacy audio.
5. Backing-alignment corpus validation and uncertainty/manual-edit preservation.
6. Android Nodes presentation/interaction evidence against desktop, not only
   identical physics constants. Baseline Nodes device interaction passed 1/1;
   desktop staggered reveal is absent from the phone toolbar and needs review.

Original MP4 takes are now available on the owned emulator. Earlier generated
PCM captures must still not be relabeled as real-singer, compressed-take or
physical acoustic validation. Native WAV stress is not compressed playback proof.

## Existing components researched

- [Media3 1.11.1 WAV extractor](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/extractor/src/main/java/androidx/media3/extractor/wav/WavExtractor.java)
- [AudioTrack buffers and underruns](https://developer.android.com/reference/android/media/AudioTrack)
- [Media3 1.11.1 Sonic float PCM support](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/common/src/main/java/androidx/media3/common/audio/SonicAudioProcessor.java)

Sonic is a candidate for the next tempo/pitch parity stage, not adopted or
qualified just because its API accepts float. Validate duration, pitch, channels,
precision, transition state and CPU cost before wiring it into the candidate.
