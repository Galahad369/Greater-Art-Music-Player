# Fullscreen rendering QA — 1.21.50 / code 273 (SOURCE_ONLY)

## Scope / release boundary
Do **not** infer a device pass from JVM CI, lint, CodeQL, or an assembled APK.
Latest verified personal sideload is **1.21.39/code262**; this checklist applies
only to a newly built 1.21.50/code273 artifact. Do not overwrite the immutable
verified artifact or release a CI-signed debug APK as a sideload update.

## Automated regressions
- `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon`
- `Release1114Test`: FIT-frame pan clamps, letterbox axes at 1×/1.2×/2×,
  zero-size/NaN/invalid inputs, toolbar width thresholds.
- `FullscreenRotationTest.narrowFullscreenToolbarRetainsRotateExitAndClose` and
  `ultraNarrowFullscreenToolbarStacksActions`: no fixture required; run as
  instrumented Compose tests on a supported emulator/device.
- `FullscreenRotationTest.portraitEntryAndExplicitRoundTripKeepPlayerAndSurface`
  requires the owned 1280×720 fixture at
  `/sdcard/Download/GA-Ambient-Test/ga-ambient-scenes.mp4`.
  Do not claim it passed if the fixture or emulator is absent.

## Real-device matrix (record device model, API level, screenshot, result)
- Android **14 / API34**: open full-screen video, rotate twice, pinch zoom,
  one-finger drag, lock/unlock, hide/reveal chrome; inspect SurfaceView
  flicker/crop/black frames and verify persistent first frame/player identity.
- Android **15 / API35**: repeat; the Media3 API34 synchronization workaround
  does not guarantee a corresponding fix here.
- Android **16 / API36**: repeat, including landscape side cutout.
- If available, test foldable / multi-window widths ~300dp, 224dp, 200dp,
  160dp and 110dp. Essential touch targets must remain >=48dp, never clip,
  and Rotate, Lock, Exit, Close must remain reachable.
- Audio fullscreen in both orientations: verify toolbar against top/side
  camera cutouts, check controls/queue are visible and not masked.
- Video fullscreen: verify toolbar, Prev/Play/Next row, timeline, timestamps
  and 2× gesture banner clear unsafe side/top/bottom cutout regions. Video
  itself should stay edge-to-edge.
- FIT letterboxed 4:3 or vertical media in a wide viewport: at 1× no pan;
  at 1.2× horizontal pan remains zero if video stays narrower than viewport;
  at 2× movement clamps to actual video borders, not black PlayerView bounds.
  Test two-finger pinch/pan and one-finger pan while zoomed. A tap reveals
  chrome; while zoomed a drag must not side-seek or trigger 2× hold.
- Pause/resume, PiP entry/exit, floating mini window/fullscreen return and
  media switch: no stale surface, flicker, content leak or unbounded zoom.

## Evidence / gate
For each row record PASS/FAIL, exact 1.21.50 build SHA and device model/API,
portrait and landscape screenshots, screen recording of rotation and
zoom/pan, surface lifecycle logs, and any failing trace.

**Not yet verified** here: exact-version device screenshot/video, complete
instrumentation run, signed physical-phone sideload, audio/video codecs across
devices, and product-release approval. API35/36 SurfaceView behavior requires
physical/emulator observations, not a source-only assertion.
