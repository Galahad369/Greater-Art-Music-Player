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
- The same frozen tests pass after repair: 555 JVM tests, no failures/errors/skips;
  lint has 0 errors, 54 warnings and 6 hints; Android-test compilation passes. The
  separate planned-count probe fails 4 of 7 assertions against retained baseline
  classes and passes 7 of 7 against the repaired compiled app. A missing-class
  harness attempt is retained separately, not reported as app behavior.
- `native-support-review-01` records a source-bound self-review, not independent
  review or release approval. It predates the commit below; do not reuse its
  source-context fingerprint as a post-commit workflow-ready receipt.

## Exact optimized candidate

- Runtime source: `7749357c803a54b39ac54e08139990721b356fab`, 1.22.27/code348.
- Preflight passed with the existing JDK21/API37 SDK. Frozen-source
  `assembleRelease` passed in 3m12s. Build receipt:
  `App/output/stack-backing-repair-20261113/native-support-release-01/build.json`.
- APK: `App/output/stack-backing-repair-20261113/optimized-local-27/GreaterArt-1.22.27.apk`.
  Size 5,579,536 bytes; SHA-256
  `d29be1d8eb9dfab75f5d1889724e6a386962bd6680ee7a5ddda270a61b88e733`.
- Existing maintenance certificate SHA-256:
  `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
  Exact package/version/signature, non-debug/non-test, ZIP and 16KiB-alignment
  checks pass. No new key was made and no previous APK was overwritten.
- The original-signer identity check remains failed and preserved. Cryptographic
  signing with the maintenance key is not an original-key update or public
  release. Keep the verified repository download at 1.21.93/code316.

## Focused installed-A55 observations

The exact candidate above was installed in place on the unchanged
`GreaterArt_A55_API36` emulator. The original six video hashes matched; no
uninstall, data wipe, renderer change or lowered media quality was used.

- Normal six-song sound review completed in 8.875s with retained feature caches
  and the new pair-map revision. This is not a cold performance benchmark.
  Riona proposes +0.650ms and unc +83.216ms. Forte, Yoshino and Morisray remain
  unchanged/rejected. These dispositions preserve the previous result; they do
  not prove backing identity or output accuracy.
- Live Apply while all six were playing regrouped 6/6. Riona Earlier/Later 1ms,
  Apply/reopen and restoration of +0.650ms passed. These checks exercise manual
  state persistence across the review, not a newly executed saved-Stack reload.
- The normal Library/dock/Now Playing flow passed pause/resume, seek and Home
  return. The observed elapsed labels advanced from 34 to 43s, and a paused 35%
  seek landed at 66s of 188.058s. The two real-video screenshots were inspected:
  frames and the seek position change, buttons remain visible, and no rounded
  top or transport backplate appears. This is not a fullscreen/physical-HDR test.
- Review screenshots are readable but have large per-track manual-button rows;
  do not call the alignment UI finished or visually optimal.
- Exact installed APK hashes matched before/after. Both temporary storage and
  overlay app-ops were restored to default, and the test microphone permission
  remains denied. A55 profile, previous26 APK and user-owned untracked work stay
  unchanged. The owned A55 was gracefully stopped with its apps and data intact;
  no other emulator was stopped. No push, merge or publication followed the tests.

Private receipts/screenshots:
`App/output/stack-backing-repair-20261113/native-support-a55-01/` and
`App/output/stack-backing-repair-20261113/native27-presentation-01/`.

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

Before release: finish saved-Stack reload and full-function regression coverage,
backing-only acceptance for all six original takes, and qualified emitted-output
tests. Keep the uncertain tracks unchanged. The focused UI checks above do not
close these gates or establish physical acoustic accuracy. A green unit count or
successful R8 assembly is not evidence of universal accuracy.
