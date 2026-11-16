# Android1.22.30/code351 — bounded PCM pipeline experiment

SOURCE_ONLY; local work only. The normal release keeps shared output disabled.
The parallel experiment also requires its separate `stackPcmParallel=true`
build property; it stays off even in ordinary prototype-enabled builds.
No push, merge, PR or publication. Preserve the exact29 APK and all earlier
artifacts. The full original-six backing-music goal is NOT complete.

## Why this experiment

The29 per-block traces show serial preparation/mixing plus tempo processing
exceeding the4.2667ms source-block budget at192k/1.25×. Full ART compilation
does not make the original gate pass. A bounded two-stage pipeline tests that
specific serial-work hypothesis; it does not change source quality or backing
matching. See [29 evidence and exact artifact](STACK_PCM_PIPELINE_1.22.29.md).

One audio-priority producer owns decoded windows, gains and the same32-tap
mix kernel. The existing output worker alone owns Sonic, AudioTrack, hardware
clock, writes and callbacks. A FIFO holds one ready block; three reusable
arrays cover consumer1/ready1/producer1. Extra premixing is capped at two
blocks/25ms; slow and lower-rate runs stay serial. Live levels are captured
once per produced block, not claimed to respond before queued audio plays.

Only measured queued AudioTrack headroom can cover a short ready wait. Keep
half the unchanged output buffer as guard and bound that wait at50ms. Atomic
select chooses either the block or the timer, not both. If the budget expires,
pause before the original2s data wait. A local data timeout is now IOException,
not owner cancellation, so failure cleanup can restore legacy audio. Parent
cancellation/outer timeout still propagates normally. Join producer and
decoders before returning the audio lease; drain partial/tempo/hardware EOF.

No filter, precision, source rate, output buffer, hardware backend, source
video/HDR/surface, matching threshold or production enablement change.

## Preserve the failed first candidate

The first candidate's six fractional half-second samples report0underruns but
advance only0.8004185of requested speed and add195ready pauses. Its later2×
transport fails42underruns. The other six original device cases pass. This is
NOT a continuity fix. Do not delete this failure or count pause-masked zero
hardware underruns as acceptance. The revised candidate adds requested-speed/
no-extra-pause and high-rate partial/tempo EOF checks; all original gates remain.

## Verification stages

- Timeout regression: fails on the old helper, passes after failure/cancel
  separation. FIFO/precision/ownership/error/backpressure/cancellation checks
  preserve actual samples and original causes, including coroutine recovery.
- Revised private-source JVM:591tests,0failures/errors/skips. This does not
  substitute for actual device acceptance. Final default-flag JVM also passes
  all591tests with0failures/errors/skips; lint0errors/54warnings/6hints and
  debug app/test assembly pass. Generated default BuildConfig independently
  shows both prototype and parallel flagsfalse, version1.22.30/code351.
- Read-only source review: no blocking issue found; the headroom reserve is
  not a guarantee against arbitrary OS stalls. Runtime gates still decide.
- Revised private A55 original fractional gate: FAIL3underruns at its first
  failing sample. The additive cadence check: FAIL0.8544304requested progress,
  17underruns and461additional output pauses in6.003s. The other six original
  precision/ordinary-cadence/transport cases pass. No performance improvement
  or production replacement is claimed.
- One bounded revised-pipeline trace:1127rows, ending28hardware underruns.
  Mean producer wall3169.60us/CPU2582.81us and tempo wall2683.20us/CPU2598.87us.
  Tempo p95wall11811us. Stages overlap across blocks; their summed work is NOT
  serial dequeue→write latency or a throughput proof. Mean-stage arithmetic
  alone does not justify another queue/backend/buffer variation.
- High-rate partial/tempo/hardware EOF: the first new test incorrectly treats
  pre-start inactive state as completion and fails at the seek frame432000.
  Require actual active advancing output first; then the same exact EOF and
  restored-position checks pass at576000frames/3000ms with0underruns. This
  corrects a test precondition; it does not repair or waive the failed stress
  and cadence gates.
- Optimized assembly from commit75ae3c6: preflight and `assembleRelease` PASS.
  App-input SHA-256 is
  `e7904de5b991c9f268ab62067970d70b8c6d5a4c4def4399eccd1cf41c5daaa0`.
  The retained private APK is1.22.30/code351,5,579,536bytes, SHA-256
  `2929ef6f567db1af5c614986aa60b8f91947b3da8959a31f1f7f08f8a1d80c05`.
  Package `com.local.listentomusic`, nondebuggable/not test-only, existing
  maintenance certificate
  `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
  Exact-file identity and zip alignment PASS; no new key is created.
- Those exact bytes pass actual A55 normal Library→Now Playing advancing
  playback, pause, seek67s, resume and Home→Library. Installed hash is checked
  before/after; playing and paused screenshots are visually inspected. This
  is a normal playback smoke, NOT Stack/backing/full-app or physical-HDR proof.
- Original public signer: unavailable. Existing maintenance signing is a
  separate private-test identity, not a public update-compatible release.
- Original-six backing/output, nonidentity scales, production host/MediaSession
  parity, full video/focus/routes and physical phone/HDR acceptance: incomplete.

Private evidence root: `App/output/overnight-alignment-20261114-0049/`.
Protocols, original failures, exact debug identities and future final receipts
remain separate. Do not infer acceptance from a successful build or one chart.

Exact APK: `optimized-local-30/GreaterArt-1.22.30.apk`. Separate chronological
receipts are `release30-build/build.json`, `optimized-local-30/identity.json`,
`optimized30-smoke-01/report.json` and `a55-final-restore.json`. The initial
build receipt correctly says identity/device not yet run; the later receipts
record the private maintenance-identity check and actual normal smoke. They
do not unblock the original public signer or promote a verified public release.

Final restoration: both temporary app-ops default, test microphone permission
denied, music volume5/15 and original stored AVD profile hashes unchanged.
Original test APK hash723825e70d25840217f5f27859ff21ad940d4059a8d4701abb8b5cedec487731
is restored. The owned A55 exits gracefully at06:51:16Taipei on14November;
the exact optimized30 remains installed. No phone or desktop app is changed.

Retain `prototype30-device-01/02`, the first0.8004185cadence observation,
`prototype30-trace-02/`, `prototype30-eof-03/` and every exact private identity.
`STACK_PCM_PROTOTYPE` and `STACK_PCM_PARALLEL` must both befalse in the normal
build. Do not re-enable either experiment to make this document sound complete.

Primary design references: [Android priority inversion guidance](https://source.android.com/docs/core/audio/avoiding_pi),
[reduced-latency design](https://source.android.com/docs/core/audio/latency/design)
and [atomic select](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.selects/select.html).
These support design review, not a claim of measured performance.
