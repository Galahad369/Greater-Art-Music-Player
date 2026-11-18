# Issue #147 — Hosted Android API36 fullscreen component smoke (source 1.22.51/code372)

**Follow-up:** The first hosted run failed before testing. See the
[1.22.52 runner repair and evidence](ISSUE_147_HOSTED_EMULATOR_1.22.52.md).

## Exact scope

A **non-release, disposable** GitHub Actions emulator job supplements the
existing Android compile-only workflow with actual on-emulator instrumentation
for existing **fixture-free** classes:

- `NowPlayingToolbarChromeTest` — 48dp targets, protected 72dp pull lane,
  clicked button centers and synthetic Compose pull/tap behavior.
- `FullscreenTransportIconColorTest` — high-contrast theme-icon rendering,
  dark control plates and ordinary-seek-track pixel regression.
- `FullscreenAmbientBoundsTest` — Fit rectangle bounds and invalid-metadata fallback geometry.
- `FullscreenToolbarTest` — visible speed selector and compact/narrow layout.

This is a headless x86_64 **API36 virtual Pixel profile**. It is **not**
the owned `GreaterArt_A55_API36` Windows virtual device, an actual Samsung
Galaxy A55, or a physical HDR-certified panel.

## Mechanism, security and scope

New optional workflow:
`.github/workflows/android-api36-fullscreen-smoke.yml`.

- Runs on PRs changing its own workflow file, or through manual
  `workflow_dispatch` on the default branch after an approved merge.
- SHA-pinned `reactivecircus/android-emulator-runner` v2.38.0
  commit `a421e43855164a8197daf9d8d40fe71c6996bb0d`, plus existing
  SHA-pinned checkout, Java21 and Gradle actions.
- Ubuntu KVM, API36 Google APIs x86_64, headless SwiftShader, fixture-free
  `connectedDebugAndroidTest` scoped to four named classes.
- Minimal `contents: read` permission, no persisted checkout token,
  no secrets or keystore, no user media and no uploading APKs or screenshots.
- Workflow is intentionally **not** a new required status check; it cannot
  leave unrelated PRs pending on a path-filtered required check.
- Official Android instrumentation CLI contract:
  `-Pandroid.testInstrumentationRunnerArguments.class=...`.
- Uses a disposable runner so a mismatched owner-retained test APK cannot
  contaminate or be overwritten in the local A55. This is environmental
  isolation, **not** a repair of that local test-APK blocker.

Upstream action documentation:
https://github.com/ReactiveCircus/android-emulator-runner
Android test CLI:
https://developer.android.com/studio/test/command-line

## Limitations — do not claim PASS prematurely

Before this new PR's workflow completes, all test results are **NOT RUN**.
A hosted emulator passing does **not** verify:

- Optimized **1.22.50 maintenance-signed** candidate, SHA-256
  `6d35cc4e824574eeb8fc7f9537e808971d6670637d09ac57a19535bb1363c073`.
  This exact file is retained in the owner's local Windows workspace and is
  not available in this GitHub-only integration.
- Frame-synchronized SDR visual equivalence between real Now Playing and
  fullscreen, real-media seeking/pause and orientation, HDR/PQ native fallback,
  Power Saver, GPU-accurate directional beam or 1:1 pixel identity.
- Physical device touch/audio/headset/thermal/acoustic QA.
- The user's different retained local test APK (the cause was not established here): do **not** uninstall the local
  app, change device permissions or replace retained test APKs.
- Production signing, release identity, Stage D SAF permissions,
  native Stack output or security certification.

## Exact A55 work still needed (separate)

With the owned local signed 1.22.50 maintenance APK and fixtures:

1. Verify installed **APK bytes** match the recorded candidate, not a
   different build or an instrumented debug APK.
2. Record real SDR footage in Now Playing and immersive fullscreen at the
   same media position/time. Compare video bounds, light shaft, background
   sampling and empty Fit margins on portrait and landscape.
3. Switch Fit/Crop, test pinch/pan/seek/pause/rotation and verify no stretching.
4. Force Battery Saver ON/OFF and compare sampled-Ambient/native route;
   preserve user settings after tests.
5. Test an authorized HDR/PQ fixture. Distinguish unavailable emulator HDR
   support from an observed app failure; physical HDR still requires hardware.
6. Repeat Mini→Expanded→fullscreen→Mini return and overlay Lock/pull gestures;
   check first-frame readiness, player identity and audio continuity.
7. Only with owner authorization resolve the conflicting local *test* APK.
   Prefer safe side-by-side/emulator user profile or known-certificate reinstall
   if it preserves the original app, prefs, grants and signed artifact.
8. Report fixture identity, OS, actual app hash, screenshot/log labels,
   PASS/FAIL/BLOCKED for each case. Never rewrite previous evidence.

**Status:** SOURCE_ONLY with new optional CI capability; no audio, visual,
renderer, storage, permissions or desktop app code changed. Full #147
remains OPEN even if the virtual component suite passes.
