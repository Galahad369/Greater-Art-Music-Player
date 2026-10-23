# Reviewed consolidation — October23,2026

## Accepted and retained

- PR199 / a4318eb: green Android, version, privacy, dependency, CodeQL and
  Windows checks; merged through normal protected-main merge as801dd16.
  Keep the single-request DISK→GENERATION state machine rather than layering
  the local69 two-request implementation on top.
- Claude d0fe253: accept lazy Ambient composition, no-surface sampler backoff,
  inactive lyric memoization, M3U indexing and paused Stack idle/wake logic.
  Credit Claude; retain the complete original patch alongside this review.
- Preserve active Stack50ms synchronization, gate behavior, native source quality,
  row/viewport optimizations, cache capacity/epochs and consumer cancellation.
- Carry forward the local real-disk scroll instrumentation test; defer pruning
  when a disk probe begins during scrolling. Add M3U behavior comparisons and
  paused seek/resume coverage.

## Not imported blindly

| Proposal | Disposition and reason |
| --- | --- |
| Split resume DataStore | Deferred: path/position writes become non-atomic; initial async collector may resume at0; reset/backup/restore not migrated |
| ViewModel idle2s polling | Deferred: a resumed player can retain its slow sleep despite immediate state publication |
| Waveform thumb2Hz stepping | Rejected: draw-phase optimization must not trade away smooth thumb motion |
| Original69 version metadata | Consumed/collided; integration uses next free71/code294 |
| Privacy PR174 | Blocked draft/failing privacy check; no history rewrite or forced merge |

## History and release recovery

Local main's complete bc3453b history is preserved by private Git ref
refs/archive/local-verified-1.21.69-20261023 and a verified self-contained bundle,
local-verified-69.bundle, in the sibling integration-backups/merge-20261023-bc3453b
directory. It includes the code, release receipt and immutable69 artifact.
Separate exact65/69 APK copies and the original Claude patch are retained there.
Do not upload private recovery bundles publicly: old history may contain paths.

The local69 sideload remains emulator-verified at its documented source; it is
not canonical71, and is not relabelled or retroactively rebuilt. Its SHA-256 is
229ed961c1528103afdcae2966d6291f995434388376763a236708183dae7940.
The historical c3761c6 handoff commit adds an APK65 under source68; importing
that commit directly fails strict APK provenance. Preserve, do not rewrite it.

Canonical source71 is SOURCE_ONLY with repository verified39 unchanged. No
physical battery/R8 performance/BT route/acoustic guarantee follows from a merge.

## Verification

Local unit, lint, build and API36 regression verification pending. New integration
must pass required GitHub checks before ordinary protected merge. Record outcomes
without borrowing PR199's passing checks for this new code.
