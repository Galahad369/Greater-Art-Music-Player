# Issue #146 — Shared-clock Stack mixer prototype

This branch is the implementation workspace for GitHub issue #146.

## Objective

Prototype Stack playback around one output clock:

A -> decoder -> PCM -> delay/resampler \
B -> decoder -> PCM -> delay/resampler  -> float mixer -> ONE AudioTrack
C -> decoder -> PCM -> delay/resampler /

Current independent player paths cannot guarantee sample/frame-level lock.

## Prototype requirements

- Decode each Stack source to PCM without changing the normal single-track player.
- Feed one shared mixer/output clock.
- Support positive and negative per-track delay.
- Preserve mute, solo and gain semantics.
- Handle small drift through bounded resampling/time-stretch rather than repeated hard seeks.
- Define buffering, underrun and recovery policy.
- Bound CPU/memory for the supported Stack track count.
- Preserve source quality where supported.
- Fall back safely for unsupported codecs/sources.
- Add measurable synchronization benchmarks/tests.

## Non-goals for the first prototype

- No requirement to replace production Stack immediately.
- No UI redesign.
- No “nanosecond” accuracy claim.
- Do not merge until there is a clear performance/correctness decision.

## Implemented source — 1.21.23 / code 246, SOURCE_ONLY

Depends on PR #152 / integration `303d82a` (1.21.22), preserving initial `49a3cd6`.
Main concurrently consumed 1.21.19–21; prototype test builds before integration
were uncommitted, not consumed releases. Production Stack remains unchanged.
`SharedClockPcmPrototype` requires both a debug build and explicit
`-PstackPcmPrototype=true`; ordinary builds cannot acquire its audio host lease.
There is deliberately no production service/UI adapter yet.

`StackPcmStream` decodes native-rate mono/stereo into four queued 1,024-frame
chunks per lane, with backpressure, clipped source timestamps and cancellation.
Compressed decoding requests float. AAC/MP3 may retain codec-native PCM16 when
float is ignored, converting those samples losslessly into mixer float; this is
not a forced PCM16/rate/bitrate setting. Lossless higher-precision sources must
honor float. Unsupported precision/channel/rate formats are refused, not reduced.
`PcmClockMath` maps every voice from
one output frame using signed fractional offsets, converts differing native rates
with bounded windowed-sinc interpolation, and preserves stereo. Coherent-sum
headroom, gain/mute/solo and negative-offset delayed entry are explicit.

One stereo-float AudioTrack supplies the wrapping frame clock. Pause parks the
played frame, seek cancels and joins old decoders before rebuilding, and looping
restarts all lanes at the primary boundary. A generation guard rejects obsolete
completion callbacks. Starvation pauses the shared sink and waits a bounded time;
failure releases the sink/decoders before asking the host to restore legacy output.
Audio-focus loss and becoming-noisy share a pause handler; resume requests focus
again. Bounded output prefill avoids starting an empty AudioTrack, and inlined
sample callbacks avoid boxing on every voice/sample. The host must silence
every legacy voice before granting the exclusive audio lease; video ownership and
native video quality stay outside this prototype.

### Unsupported / unproven

- Non-identity tempo maps are refused: a qualified pitch-preserving stretcher is
  required before adopting them. Sample-rate conversion is not a tempo solution.
- Mono/stereo only; no multichannel downmix or source-quality fallback.
- No physical-device listening, acoustic alignment or perfect-synchrony claim.
- Production host integration, real-device CPU/thermal limits and video-follow
  behavior remain adoption gates, not completed features.
- Main finalized owner-confirmed 1.21.18 provenance; the unchanged version guard
  must independently pass tracked SHA/manifest checks. All older artifacts remain
  untouched, not retroactively certified; this source is not a verified release.

### Evidence

Pure tests cover eight coherent voices, signed offsets, gain/mute/solo,
44.1/48 kHz conversion, long timelines, invalid inputs and unsigned head wrap.
The 1 kHz conversion fixture measured RMS error `0.000017575736188555596`.
Final enabled API 36 instrumentation passed native mono/stereo clips plus the
transport harness; the disabled-build test is intentionally skipped in that
variant. Ordinary default-off instrumentation passed native decoding and the
gate test, intentionally skipping enabled transport. These are complementary,
not three exercised methods per variant.

- Eight PCM voices: pause/seek/solo/primary loop and interruption-handler resume
  pass. Cold-run snapshot: 3 underruns, peak mix 40,270 us per 5,333 us block.
  Earlier boxed-sample runs measured 133–535 ms peaks. Inlining reduced the
  observed peak, but this is not a controlled benchmark or an eight-voice
  performance pass. Keep the route experimental.
- Six actual local compressed takes, clipped 30–32 seconds, native 44.1 kHz:
  concurrent output, pause/seek/resume pass; steady short snapshot 0 underruns,
  0 decoder waits, peak mix 3,532 us per 5,805 us block, Java heap 15,128,704 bytes.
  Heap is a snapshot, not peak/native memory, and this is not full-song listening.
- Protected noisy broadcasts cannot be emitted by a test app; direct handler
  coverage is not physical headset-disconnect/system-delivery verification.
- Unsupported non-identity maps are refused before acquiring the host. An earlier
  float-only AAC failure exercised output retirement; native AAC PCM16 handling
  now passes without forcing a lower decoder format.
- Integrated default-off gate: 278 JVM tests pass; lint 0 errors / 28 warnings;
  Android app/test APK build pass. Unchanged version guard passes source 1.21.23
  plus current main's owner-confirmed 1.21.18 hash/manifest metadata.
