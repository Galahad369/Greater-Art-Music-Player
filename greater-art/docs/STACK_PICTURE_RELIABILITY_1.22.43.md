# Picture decoder reliability — 1.22.43 / code 364

## Approved scope

The owner approved a focused response to the older Claude picture-alignment audit,
not another sound matcher or a new playback engine. No library/model is added.
Keep sound and picture separate. Preserve all original corpus assertions, the
45-second Quick limit, frame evidence windows, 30fps nearest-frame sampling and
confidence rules. Playback/renderer/quality, the 42 icon fixes and Stack manual
offsets remain unchanged. No push, merge, signing-key replacement or public release.

## Cause and repair

- A 512 KiB analysis packet probe could reject a valid larger IDR. Use format
  metadata and the actual sample size where API 28+ provides it, with a hard
  16 MiB analysis allocation bound. Oversized/unusable analysis falls back; it
  never changes the primary video's resolution or decoding quality.
- One local-decoder failure repeated every window. Stage a window's frames,
  retain completed windows and retry only the failed window on a fresh platform
  decoder. Use that decoder for subsequent windows. Cancellation is not a retry.
- IO, unsupported-video, decoder and wait/decode timeouts previously collapsed
  to null/no-match. Carry typed failures to Review, including reference failures.
  A timed-out Wide scan offers an explicit retry. No offset is applied for failures.
- Keep output order AND multiplicity checks, rather than sorting returned frames
  and hiding an out-of-order stream from the sequential sampler. Log actual codec
  names, window timing and fallback reasons without media paths.

## Acceptance and evidence

Evidence lives in `App/output/picture-reliability-20261116/`.
Snapshot the original tree and 42 APK first. Extract the existing gate, IDR scan,
whole-take retry policy and 512 KiB allocation into the production-used pure policy.
The frozen new assertions fail for the intended oversize/repeated-window defects
before repair; retain `policy-red.xml`. A preliminary generic-annotation compile
failure is retained but is NOT counted as regression reproduction.

Run host tests/lint and diagnostic assembly. On the owned A55 compare actual
local/platform timestamp and normalized-luma hashes on a short original Riona
window, check cancellation/budget reuse and typed missing/unsupported failures,
and run unchanged original-six Quick/Wide corpus gates. Do not loosen offsets,
rejections or deadlines. Device evidence does not establish physical-phone speed
or backing-music synchronization. Run optimized assembly, existing maintenance
signing/exact-file checks and normal-entry playback on those bytes separately.

Preflight and 629 JVM tests pass. Lint has zero errors/54 existing warnings;
debug and instrumentation APKs compile. Preserve the failed host JVM C2 compiler
crash in `jdk-crash-13704.log`; the unchanged retry (`checks-all-04.log`) passes.
Native and optimized results are pending. The original public signer remains
unavailable; retain the existing verified public artifact.

## Prevention

Test failure paths, not only successful offsets. A failed analysis is not evidence
of different pictures. Keep completed evidence separate from a failed retry's
partial output. Native decoder changes need frame-evidence comparisons; emulator
timing does not justify changing a phone decoder or playback settings.

References: [MediaExtractor sample size](https://developer.android.com/reference/android/media/MediaExtractor#getSampleSize()),
[MediaCodec discontinuities/flush](https://developer.android.com/reference/android/media/MediaCodec).
