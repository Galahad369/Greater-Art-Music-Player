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
All 11 native decoder/review checks pass in 41.312 s. The real Riona comparison
uses distinct `c2.android.avc.decoder` and `c2.goldfish.h264.decoder` implementations,
with no local fallback: timestamps, edges and all normalized luma samples agree
for 360 analysis frames. Cancellation releases the budget for the next read;
missing references and audio-only input produce the expected typed failures.

The unchanged original-six corpus passes five Quick scans at 31.282, 28.684,
28.534, 28.197 and 28.308 s. Each returns companion offsets in microseconds
`[null, 0, 0, 12500, 0]`. Wide passes at 224.854 s with
`[null, 0, 0, 20500, 0]`. Both retain the original 2 ms/12–28 ms ranges and
different-video rejection; no hidden read/decoder failures occur in corpus logs.
These are fresh app processes on the same guest/filesystem cache, not five cold
VMs or a controlled speedup comparison. Per-window failure recovery has pure
regression coverage; the successful corpus does not claim every native failure
was injected. Physical-phone performance and backing-music sync remain unverified.

Optimized assembly passes in 4m 2s from commit
`a5bf26e730015c940514ffc57a034dbbde3f05ce`; Android input SHA-256
`a40272cfcfb29c3f90f3d396fb1db415aed0a7bd7a90ad637577dcb62c15447d`.
The exact existing-maintenance-key APK is
`App/output/picture-reliability-20261116/optimized43-maintenance/GreaterArt-1.22.43.apk`,
5,620,696 bytes, SHA-256
`f453f0bedf9ead91e891d363c8539f23af7395b0196184520e9046e74f867f77`.
Actual package/version/signer, ZIP CRC, non-debug flags and 16 KiB alignment pass.
The first private verifier invocation hit Windows' old Java 8 launcher timeout;
`artifact-verification-jdk21.json` checks the unchanged signed bytes with the
pinned JDK 21 first on PATH. No rebuild, re-sign or replacement key was used.

`a55-43-01/` retains passed native/corpus results and an exact-smoke failure:
rotation entered landscape, but an untimed toggle tap hid the already-visible
toolbar and the script could not find Rotate. `a55-43-02/` retains the retry on
the same APK: wait for the existing 2.5 s auto-hide, reveal normally, and retain
the same Rotate-label, actual-orientation and return assertions. Normal Library
entry, real video advancement, pause/resume, fullscreen/landscape and return all
pass. Actual screenshots are inspected; the landscape capture can show chrome
part-way through its normal fade. Original preferences restore byte-for-byte;
app ID/data inode, app-op modes and AVD profile hashes are preserved. The guest
filesystem is synced before shutdown. No root, GPU or permanent RAM change.

This is a self-reviewed local maintenance candidate, not public-release approval.
The original public signer remains unavailable and the original public identity
gate fails as expected. Retain SOURCE_ONLY, prior verified artifacts and all
failed evidence. No push, merge, public download replacement or sound claim.

## Prevention

Test failure paths, not only successful offsets. A failed analysis is not evidence
of different pictures. Keep completed evidence separate from a failed retry's
partial output. Native decoder changes need frame-evidence comparisons; emulator
timing does not justify changing a phone decoder or playback settings.

References: [MediaExtractor sample size](https://developer.android.com/reference/android/media/MediaExtractor#getSampleSize()),
[MediaCodec discontinuities/flush](https://developer.android.com/reference/android/media/MediaCodec).
