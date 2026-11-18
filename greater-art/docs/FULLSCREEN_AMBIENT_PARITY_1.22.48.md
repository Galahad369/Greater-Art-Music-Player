# 1.22.48/code369 — ordinary seek-track preservation after PR #246 review

This is a follow-up to [the 1.22.47 review repair](FULLSCREEN_AMBIENT_PARITY_1.22.47.md).

## Finding

The 47 fullscreen slider change introduced an opt-in dark12dp backing but also
wrapped the existing ordinary slider's 3dp track in a second
`inactiveColor.copy(alpha = 0.34f)` background. For an opaque source color,
drawing both layers can compound the resulting opacity from 0.34 to 0.5644,
altering normal audio/Stack playback visuals even though the task only asked
for fullscreen color contrast.

## Fix

In `CompactSlider` keep the outer `track` Box transparent when
`videoOverlay=false`; the single inner3dp track paints the original
34%-alpha color exactly once. When `videoOverlay=true`, retain the
12dp `GaVideoOverlay.control` local dark plate behind the inner3dp track
and the24dp thumb backing. This is a source-only rendering refinement:
no playback, media format, volume, gesture timing, surface, Ambient algorithm
or ownership modification.

The new rendered Compose test uses a white source fixture and a black
34%-alpha inactive track. A single layer leaves approximately66% channel
intensity, whereas doubled layers would leave approximately44%.
The fullscreen dark-background screenshot checks require non-transparent
pixels, but allow either opaque composed captures or captured local alpha.

## Verification boundaries

- Fresh executable commit must advance to1.22.48/code369, append its precise
  ledger entry and satisfy per-commit Android version checks.
- Run required CI test/lint/build, privacy, CodeQL, dependency and Windows
  checks on exact final head. The Android instrumentation APK being built
  is not equivalent to running its tests.
- Run `FullscreenTransportIconColorTest`,
  `NowPlayingToolbarChromeTest`, `FullscreenAmbientBoundsTest` and existing
  seek/rotation/gesture/surface/FrameAmbient tests on a real A55 or equivalent
  emulator. Inspect screenshots with bright/black source frames and normal
  audio/Stack as well as fullscreen theme transitions.
- Use the repository exact optimized APK gate for any release acceptance.
  The original public sideload signature is unavailable in this context;
  preserve immutable verified1.21.93 and label new source SOURCE_ONLY.
- Retain the protected middle pull lane, all 48dp actions, non-stretched
  fullscreen Fit/Crop, native HDR/low-power fallback, and the one-decoder
  fullscreen SDR Ambient path. Do not merge solely on a green compile.
