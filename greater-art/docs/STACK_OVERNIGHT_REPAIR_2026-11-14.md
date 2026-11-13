# Android1.22.28/code349 — focused overnight repairs

Status: SOURCE_ONLY. Local work only; no push, merge, PR or publication.
The latest verified public APK remains1.21.93/code316. Preserve every prior
artifact and signing identity. This report is not a release certificate.

## Implemented scope

- A cancelled analysis keeps ownership until decoder cleanup finishes. Starting
  a replacement while the old job is inactive but incomplete could otherwise
  let old cleanup replace the new state or playback ownership.
- Mono/side timing maps must agree at both ends of the song. Individually
  permitted30ms offset and30ms rate errors must not silently add to60ms.
  The original40ms individual guards remain; pair map revision is11.
- A seek beyond a short primary promotes a healthy longer take using the
  requested master time before Media3 clamps the old file to its EOF. An
  unsupported map rebase must retain the old bounded fallback, not swallow
  the seek or silently clamp saved timing.
- The disabled shared-output prototype pauses before awaiting missing decoder
  data. Its old200ms wait exceeded the100ms sink capacity. Buffered/closed
  channels take a nonblocking path; timeout, cancellation, sample identity and
  decoder errors remain intact. This is not prototype enablement or a claim
  that its high-resolution/transport parity now passes.

All source quality, default correction constants/backend, manual/saved maps,
separate sound/picture methods and existing app functions are preserved.

## Evidence and limits

Original coroutine and composed-endpoint regressions fail before the repairs.
The actual A55 reproduces15s seeking becoming9.999s with a10s primary and20s
companion, paused and playing; both cases pass after the repair. The review UI
suite passes5/5 with two stale text assertions corrected to existing UI copy.
The controlled missing-data ordering test fails the old policy and passes the
repair. Full JVM suite:560 tests,0failures/errors/skips at that checkpoint.

The qualified raw-device route passes the unchanged timing, gain, isolation and
physical-absence controls twice. It uses no microphone or desktop audio, keeps
all native frames and does not repair PCM. Qualification is not real-song or
physical-speaker acceptance.

Exact optimized1.22.27 independent-code output fails: a quiet repeat reaches
17.501ms, while its premixed positive control passes at0.0906ms. Controlled
debug traces show substantial rate actuation and asynchronous position
plateaus. Unity actuation preserves code correlations above.999 but leaves
fixed offsets up177.414ms. Native AudioTrack parameters fail more severely.
Static scheduling improves later windows but still fails2ms and early-window
correlation. No variant is adopted in production. Actual six-song backing,
whole-song output and full transport/picture acceptance remain open.

Private evidence root: `App/output/overnight-alignment-20261114-0049/`.
See notebookE21 onward for frozen hashes, failed attempts, protocols and limits.

## Build and artifact stages

- Versioned source commit/review: pending final local checkpoint.
- Optimized assembly: mandatory; pending this source checkpoint.
- Original public signer: unavailable. Do not replace it or claim that the
  existing maintenance signer can update original-signer installations.
- Exact optimized1.22.28 APK and normal-entry A55 smoke: pending.
- Full six-song backing/output acceptance: FAILED/INCOMPLETE, not verified.
- Publication: not authorized and not attempted.

Update these stages from actual receipts after assembly. Never infer runtime
or release acceptance from unit tests, player positions or a signed file alone.
