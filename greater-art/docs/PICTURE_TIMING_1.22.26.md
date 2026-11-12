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

## Remaining acceptance

Run the current-source JVM/lint/test-compilation suite, optimized release gate,
maintenance-signer identity and actual installed A55 analysis/review/Apply smoke.
Keep original-signer release, physical HDR/performance and acoustic acceptance
separate. Backing-only sound isolation, its known false matches, Forte's possible
key difference and emitted six-voice sync remain open. Do not mark the thread
goal complete or claim1000% accuracy from this picture repair.
