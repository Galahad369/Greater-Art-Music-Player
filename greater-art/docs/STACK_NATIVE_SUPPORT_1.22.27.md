# Sound refinement support — Android1.22.27/code348

Status: SOURCE_ONLY. This is a focused confidence repair, not completion of
backing-only sound alignment or emitted-output synchronization. No push, merge,
publication, signer replacement or previous-APK overwrite is authorized here.

## Cause and approved scope

The owner approved stricter partial/ambiguous-match rejection and declined a
second-stage native-fit rescue. Native refinement previously filtered failures
out of its correction list, then measured support against that reduced list.
Three successful measurements could therefore approve a six-region mono map.
Optional refinement could also move a stereo-confirmed map on partial support.

Keep the existing three-region minimum and75% support requirement. Pass the
planned region count from the production analyzer through native refinement;
count failed/missing regions against that same denominator. If support is
insufficient, reject a mono-only map or retain the unrefined stereo-confirmed
map. Preserve actual coarse anchor metadata. Pair-map revision10 prevents older
decisions from bypassing the new check; feature caches remain compatible.

This is not a new matcher, separation model, output engine or quality fallback.
Sound and picture selection, one selected timing map, manual/saved offsets,
readiness-gated Apply, HDR and source resolution/FPS/bitrate are unchanged.

## Baseline and regression evidence

- Retained baseline: main `c61488aa3208e0bf4a5a5eaddd88752671d0cc4b`, runtime
  `e046dbcc1d4e4841d73af5ed8bb284649ed198bc`, exact optimized1.22.26 APK SHA
  `0f34b8467201b09e1f7d2bedc9174e506ac3476a3b56a738a4e71901343289c3`.
- A private Java probe calling the compiled production method reproduced three
  failed support assertions. It also retained valid3/3,4/5 and5/6 controls.
- The frozen Android JVM regression then reproduced the intended failures:
  missing native regions approved mono timing and moved optional stereo timing.
  Workflow challenge: `App/output/stack-backing-repair-20261113/native-support-red-02/`.
  The earlier sandbox/cache-access failure is retained separately; it is not the
  bug reproduction. No shared worktree was reset and no assertion was relaxed.
- Final unit/lint, optimized assembly, exact identity and installed-APK results
  are pending and must be recorded separately. Preflight passed with the existing
  JDK21 and API37 SDK. The original historical signer is still unavailable.

## Remaining acceptance

The current full-mix matcher can still follow vocals. Existing separation inputs
still fail a valid changed-vocal regional case; retain that failure. New external
matcher candidates were researched, not installed/adopted. Keep the owner
confirmation boundary before new production dependencies or a new engine.

The A55 known-zero premix output control passed, while independent six-source
Stack output failed the unchanged scorer. Complete capture/gain qualification
still has red checks. Those diagnostic results do not prove physical acoustic
accuracy or make all six original songs pass. See the private investigation's
`coded-stack-device-01/OUTPUT_COMPARISON.md` and retained raw recordings.

Before release: test these exact new APK bytes through normal six-song sound
review/Apply/manual/save flows on A55; retain uncertain tracks without silently
changing their saved timing; complete output and real-media acceptance. A green
unit count or successful R8 assembly is not evidence of universal accuracy.
