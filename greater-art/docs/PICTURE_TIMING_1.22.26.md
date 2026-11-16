# Android 1.22.26 / code 347 — picture timing repair

State: **SOURCE_ONLY**. App integration, not a universal alignment or acoustic pass.
Current-version assembly and exact installed-app acceptance are separate stages.
Preserve the exact1.22.25 candidate and verified original-signer1.21.93 release.

## Causes and implementation

1. The analysis sampler floored integer microsecond timestamps into30Hz buckets.
   Quantized30fps timestamps could discard about a third of the pictures. Native
   Riona and Yoshino each decoded1,080 Quick pictures but selected only721.
   Use a window-local nearest-frame grid with one copied luma buffer. Preserve
   original timestamps, flush the pending nearest frame at the window end, and
   release codec buffers normally. The existing analysis budget is not a playback
   FPS or quality limit; primary decoding/output is unchanged.
2. With mixed cadences, several offsets can have exactly the same top score.
   The old nearest-zero tie-break changed the answer when only timestamps moved:
   the real native feature replay gained an erroneous7ms and a synthetic scene
   gained15ms. Center only the contiguous, exactly equal top-score interval.
   Keep exact same-cadence delta snapping and run the same regional-consistency
   check. No confidence, scene, ambiguity, time or corpus gate is weakened.
3. Review text now states the actual limits: pictures are frame-limited; sound
   estimates can be affected by vocals. This corrects the earlier categorical
   backing-only claim. It does not substitute for completing the backing matcher.

## Preserved behavior

Sound and picture are independent choices for one active map per track. Keep
manual Earlier/Later/editing, saved Stack timing, Apply readiness, cancellation,
unmatched tracks, queue/library behavior, all existing controls, Ambient, native
HDR, Fit/Crop, signing identities and source media quality unchanged. No model,
transcode, new decoder, playback-frame skipping or primary track limit is added.

## Evidence before app integration

Evidence root: `App/output/stack-picture-sampling-20261113/`.

- The old sampler adapter fails the frozen1,080-picture test with720 retained.
  The streaming implementation passes, preserves source PTS, matches714 varied
  timestamp/window cases against an independent offline grid, and passes60
  known-shift/edited/repeated-motion safety assertions.
- Reject the simple rounding prototype: it passes the real host corpus but moves
  a60fps negative-offset control by16.7ms. Reject nearest-only integration too:
  native A55 returns9ms for Yoshino, outside the frozen12–28ms range. Keep both
  failures; never loosen the range to make a candidate green.
- Native feature export reproduces9ms on the host, unlike FFmpeg's area-scaled
  luma. A synthetic24/30fps regression reproduces the same zero-prior defect.
  The actual production-math/JUnit challenge fails with
  `PICTURE_TIMELINE_TRANSLATION`, expected−991000us versus−976000us.
- With nearest selection and equal-peak centering, two fresh A55 processes pass
  all six videos and18 windows in33.813s and33.895s. All5,328 in-window decoded
  pictures are selected before legitimate flat-image rejection. Offsets in take
  order are Riona rejected, unc0, Forte0, Yoshino12.5 and Morisray0ms.
- Native Wide passes in256.345s: all29,341 decoded pictures selected, with the
  same rejections/zero offsets and Yoshino20.5ms. These are frame-based estimates,
  not sample-accurate measurements. Do not claim Quick and Wide prove an exact
  unique instant or that this is faster in the actual app.
- The45s Quick limit, ±2s/±30s ranges, completeness checks, Riona rejection,
  ±2ms zero-offset corpus gates and Yoshino12–28ms range are unchanged. Tests
  are process-cold, not storage-cold; Windows file cache was not flushed.
- Installed1.22.25 SHA-256 `15d61935631169f9b9e88965cb9106278bf794428c08aa4f3d4dd0206cc3bf4f`
  and the A55 profile stayed unchanged throughout these isolated probes. No app
  install, permission change, push or merge occurred during them.

## Workflow lessons

- Host area-scaled pictures are not native16-point luma evidence. Export exact
  native features when they disagree; do not tune acceptance to the host result.
- Check timestamp-translation invariance, source-frame retention and explicit
  false matches, not merely a correct-looking positive offset or green build.
- Use absolute paths for the local workflow runner's evidence. The retained
  relative-path failure is a runner invocation issue, not an app test failure.
- SDK36's standalone D8 is too old for Kotlin2.4 metadata. The existing cached
  AGP9.5.1 builder supplies D8 9.5.29; no toolchain upgrade/download was needed.
- Use the fixture ownership manifest, not retyped package paths. The first
  native export failed from a path typo; no missing-file result counted as a
  meaningful picture rejection.

## Executed current-build checks

Runtime commit: `e046dbcc1d4e4841d73af5ed8bb284649ed198bc`.

- All552 JVM tests pass with no failures, errors or skips. Lint reports0 errors,
  54 warnings and6 hints. Android test compilation passes; compilation is not
  instrumentation execution. The unchanged translation challenge fails on the
  old production math and passes after the repair. The source-bound checks were
  refreshed after committing because the runner also binds Git HEAD/status.
- Preflight and optimized `assembleRelease` pass from committed inputs,
  SHA-256 `866220aa1153ee90c24d3a9c5fbf0cb58db1aea12a1f3345488bf2337cab23ab`.
  The first sandbox attempt cannot open Gradle's existing cache lock; the native
  retry uses that cache normally. Both logs remain. The original-certificate
  identity check still fails correctly; its pin was not changed.
- The separate local APK uses the existing maintenance certificate
  `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
  Package/version/code, non-debuggable/non-test flags, ZIP CRC and16KiB alignment
  pass. Exact installed hashes match before and after the actual A55 tests.
  The update from the exact25 APK succeeds without clearing data or changing keys.
- Actual Settings/Library/dock/Now Playing passes OLED/video-color choice,
  pause/resume, seeking and Home return. The screenshot was visually inspected:
  the timeline and transport have no surrounding rounded fill or outline.
  Control positions, touch targets, speed access and functions remain intact.
- Through the normal six-track Stack UI, Quick finishes without its timeout flag
  in40.325s and Wide finishes in285.770s. These are observed UI intervals including
  automation, not storage-cold decoder benchmarks. Both reviews propose four
  maps, reject Riona, keep unc/Forte/Morisray at0ms and put Yoshino at12.5ms Quick
  or20.5ms Wide, inside the unchanged12–28ms corpus range. The full review list is
  observed and captured. Applying four maps preserves paused state; resume reaches
  `6/6 tracks playing` and pause works again. This is not acoustic sync proof.
- The six-track adapter subsequently fails to locate an off-screen Yoshino row;
  retain that failure and do not call the whole script a pass. A separate actual
  two-track manual-control sequence passes Earlier1ms, Later1ms, Apply, reopen,
  restore the original0ms, reopen and Cancel. It selects the observed track's own
  button instead of guessing between multiple identically named controls. No app
  change or APK rebuild was made for either private adapter repair.
- OLED and both temporary app-ops are restored. The final crash buffer is empty
  in this test, not evidence of universal crash freedom. The unchanged owned A55
  is stopped after testing. Profile SHA-256 remains
  `0387c814ba4ce319ceb37d88b5f48ee3fecaa3f027d03ccbb00381118edd1ae9`.
  Exact24/25 APKs, verified1.21.93 and the user-owned untracked CodeGraph file
  retain their hashes. No branch/worktree, push, merge or public release is added.

**Exact local APK:**
`App/output/stack-picture-sampling-20261113/optimized-local-26/GreaterArt-1.22.26.apk`
—5,579,536bytes, SHA-256
`0f34b8467201b09e1f7d2bedc9174e506ac3476a3b56a738a4e71901343289c3`.

Actual Now Playing screenshot:
`App/output/stack-picture-sampling-20261113/native-theme-26/now-playing-video-colors.png`.
Evidence: `checks-26-committed/`, `release-26-native/build.json`,
`optimized-local-26/identity.json`, `native-theme-26.log`, `native-six-26.log`,
the per-track Quick/Wide review JSON/screenshots under `native-six-26/`, and
`native-manual-26.log`. All are private local evidence under the task root above.

## Remaining acceptance

This is a maintenance-key local candidate, not a promoted original-key release.
The unchanged workflow's protected-release review requires an owner review;
self-review is recorded honestly and does not bypass it. The historical private
key remains unavailable, so the original-signer APK gate is not accepted. The
maintenance key cannot update an original-signer installation. Preserve that
distinction even though the focused exact-APK A55 checks above pass.

Physical HDR/performance, full-app regression and acoustic acceptance remain
separate. Backing-only sound isolation, its known false matches, Forte's possible
key difference and emitted six-voice sync remain open. Do not mark the thread
goal complete or claim1000% accuracy from this picture repair.
