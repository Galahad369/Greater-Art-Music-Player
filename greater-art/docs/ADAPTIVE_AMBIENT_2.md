# Adaptive Dynamic Ambient — review evidence

Target 1.21.36/code259, branch `feat/ambient-2-adaptive-dynamic`, based on main
df7f8de. PR #168 is separate and untouched; its consumed1.21.33–35 identities are
not reused. Latest verified APK remains1.21.32; this feature is SOURCE_ONLY.

## Architecture

- Classic keeps the original ArtworkBackdrop renderer. Its Settings label becomes
  Classic Ambient, translated into all six languages.
- Adaptive draws four simultaneous layers: three-color Classic gradient, broad
  radial Aurora, brightness/chroma-weighted Bloom and diagonal Flow. Intensity
  zero removes their color contribution; Motion Off keeps the static combination.
- Existing24×24 compositor pixels are reused at about900ms. A copied frame must
  still match both media ID and source View. No primary Surface assignment changes.
- Analysis on Dispatchers.Default uses at most576 video or144 artwork pixels.
  RGB representatives, approximate luminance/chroma and palette variation are
  bounded, not HDR metadata, camera motion or beat detection. Scene metrics smooth
  over about1.9s; palette colors interpolate over1.6s.
- Current artwork uses existing caches once per media/override change. Two shared
  numeric snapshots preserve palette continuity; they retain no frame bitmaps.
- Deterministic monotonic phase preserves geometry between hosts. State is read
  during drawing, not by controls. No blur, full-size per-frame bitmap, extra
  decoder, Surface, audio stream, FFT/DSP or source-quality constraint.
- Hidden/unresumed/fullscreen phase work stops. Scrolling and battery saver stop
  analysis and motion. System animator-duration zero disables decorative movement
  and snaps finite fades. Observers/receivers detach on disposal.
- Muted theme fallback, capped tint and fixed veil protect contrast independently
  of intensity. Existing AppBackground dimming remains; expanded player applies
  its chosen dim level as well.

## Settings and integration

Existing background selection/default is preserved. Adaptive defaults to intensity
50%, React On and Motion Subtle. DataStore persists changes. Nonfinite intensity
becomes50%; values clamp0–1; unknown motion becomes Subtle. Reset clears new keys.
React Off retains media colors but uses steady lighting parameters. Motion offers
Off/Subtle/Normal. Intensity saves at drag release, not every drag pixel.

All new labels/descriptions cover English, Traditional Chinese, Japanese, German,
French and Cantonese. Switch row and motion chips use48dp targets and existing
spacing tokens. Library/Nodes/Stack share AppBackground; expanded player uses
the same modifier and existing artwork. Fullscreen suppresses its hidden backdrop.

## Verification boundary

Automated evidence covers palette extremes/fallback, stale-video rejection,
media reset, smoothing, nonfinite clamping, all-layer weights, Motion Off/React
Off, lifecycle gates and bounded shared cache. Android tests cover DataStore
reload, artwork sampling, measured localized targets and actual composed Canvas
color changes without moving the title.

Owned video fixture: generated24s red/blue/green scenes,1280×720 at30fps, with
generated sine audio. These are fixture properties, NOT playback constraints.
Real compositor/player test evidence and final measurements are recorded below.
Physical-device/API26 execution, long Stack/thermal testing, GPU allocation traces
and exhaustive TalkBack/orientation acceptance are not claimed. A sparse analysis
benchmark is not a GPU frame-time or universal playback-smoothness guarantee.

## Recorded emulator results — October 12

- API36 A55-sized emulator:1080×2340, density450; not a physical A55.
- 352 JVM tests pass (15 new Ambient regressions). Android build and lint pass:
  zero lint errors,41 warnings and3 hints on the measured build.
- Seven Android tests passed on the final build (58.95s): preference/artwork/analysis
  tests, three composed UI tests including the zero-viewport guard, and real-video
  integration. Actual scene colors changed red→blue; Classic→Adaptive
  preserved controller identity and surface generation. Pause held the sample.
- Two video-backed Stack voices ran together, errors0, primary1280×720 unchanged.
  This is not acoustic sync or a six/eight-video capacity claim. Recorded primary
  decoder was c2.goldfish.h264.decoder, codecError=none, droppedFrames=0.
- Sparse576-pixel analysis means across200 iterations were0.564ms,2.801ms and
  4.304ms in three runs. Variance reflects emulator/host load; not a phone claim.
- Short gfxinfo windows: Classic81 frames/35.80% jank/p90 frame36ms; Adaptive447
  frames/10.74% jank/p90 frame29ms; cumulative Adaptive+Stack708 frames/10.59%
  jank/p90 frame29ms. GPU p90:16/23/21ms. Heap snapshots:24.1/21.5/15.6MB.
  These are unequal windows including transport/scene changes, NOT proof of a
  speedup or regression. GPU p99 reported an anomalous4950ms bucket. Jank remains;
  real-device GPU/thermal profiling is required before promising smooth60Hz.
- Raw measurements: [Classic](ambient-evidence/classic-gfx.txt),
  [Adaptive](ambient-evidence/adaptive-gfx.txt),
  [Adaptive with Stack](ambient-evidence/adaptive-stack-gfx.txt).
- Actual Library screenshots: [warm](ambient-evidence/adaptive-library-warm.png),
  [cool](ambient-evidence/adaptive-library-cool.png). Two-track Stack active while
  Library is visible: [screenshot](ambient-evidence/adaptive-stack-active.png).
- Composed artwork fixture (not full-app/audio-playback footage):
  [warm](ambient-evidence/adaptive-audio-warm.png),
  [cool](ambient-evidence/adaptive-audio-cool.png). Fixed title retained position.
- App-specific crash buffer contained no Greater Art fatal in the checked run;
  an emulator Bluetooth-system crash was unrelated. No universal crash-free claim.
- Final test-only debug APK SHA-256:
  `f331621de2b297a574be011d0901dbae0a576b2fcea235a985d2db8b9a662096`.
  It is not promoted as a verified release. Version guard and public-repository
  security audit passed; GitHub CI is a separate PR gate.

## Implementation lessons

- Serialize verification. An overlapping older lint run stalled in UAST; only its
  positively identified daemon/wrapper were stopped. Final checks were rerun.
- JUnit runBlocking tests must return Unit explicitly if the final expression is
  Log.i (which returns Int). The integration test initially failed initialization,
  not player execution.
- Continuous polling cooperates with Compose infinite-animation policy so test
  clocks do not chase a new1.9s fade every simulated900ms forever.
- Screenshot Text must explicitly use the theme foreground. MaterialTheme alone
  can leave an inherited black color on a dark test background.
- Unrelated historical-APK deletions remain local user work, outside this PR.
