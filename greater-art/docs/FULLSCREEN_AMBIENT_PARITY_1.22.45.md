# Fullscreen Ambient and Now Playing control parity — 1.22.45/code366

## User reports and confirmed source difference

Expanded Now Playing can use the same-decoder, same-frame SDR GL light compositor.
Fullscreen deliberately had `!immersiveVideo` in the compositor admission
condition, so it could only use the existing 900ms PixelCopy/artwork palette
and the Compose reflected-glow fallback even with Android Power saving off.
Both paths already shared the same sampled Ambient backdrop, but that is
not visually equivalent to a frame-synchronous edge beam.

The original Android Media3 SurfaceView/native path remains necessary for HDR,
unsupported formats and low-power rendering. No unconditional GL takeover.

The user also reports that Now Playing button colors and spacing do not follow
the active theme consistently. Normal transport was using unequal outer spacing
and its speed label inherited the surrounding content color. Fullscreen foreground
video chrome was deliberately fixed off-white for contrast over moving frames.
The correction offers the selected theme accent only when it remains readable
on the existing per-button black plate; otherwise the original neutral glyph
stays. Do not paint a themed opaque plate over the whole video.

## Code changes

- Admit the existing frame-synchronous Ambient player also in fullscreen for
  selected and supported SDR video, with an explicit Power-saving gate and the
  existing native/unsupported-format fallback. There is still only one primary
  playback decoder and one surface owner.
- Use the actual aspect-preserving **Fit video rectangle** as the GL light
  boundary, leaving bars to reflect light; Crop fills the viewport and has no
  artificial outside bars.
- Pass Crop, pinch scale and pan into the same GL rectangle. Gesture state is
  reconciled with the stage; no independent X/Y scaling or playback restart.
- Fullscreen loads current artwork through the expanded player's normal
  source path rather than leaving a cache-only miss indefinitely flat.
- Keep high-contrast video overlay plates and fixed neutral fallback, with
  theme-aware glyphs where both bright/dark backing tests pass.
- Give standard-width Now Playing top actions even center intervals without a
  wide artificial central gap. The pull mark remains visual-only above the
  glyph row, and narrow toolbars retain existing wrapping.
- Evenly space normal bottom transport and apply the theme to the speed caption.
- Preserve 48dp touch targets, Lock, gesture ownership, player toolbar fade,
  2.5-second auto-hide, Fit/Crop, video resolution, HDR/unknown-format
  native path, PiP and mini/fullscreen transitions.

## Tests and acceptance boundary

Source-level added/updated regressions:

- Pure Java Fit/Crop and uniform GL zoom/pan rectangle transforms.
- Android fullscreen fitted-video margin geometry across portrait/landscape,
  different source aspects, and invalid metadata.
- Accessible theme fallback contrast against bright and dark video.
- Rendered icon color test for the actual themed foreground.
- Normal Now Playing toolbar measured six-action center intervals and fixed
  Lock bounds without reintroducing hit-stealing handle overlays.

**Not yet verified by this source PR:** Gradle/JVM/lint/build and emulator
tests must run on the exact final head. A debug APK compile does not prove the
per-frame GL output or real fullscreen geometry. On an A55 or equivalent
emulator inspect original SDR and HDR test clips in portrait and landscape,
Fit/Crop with pinch/pan, Pause/Resume/Seek, color themes, Power saving on/off,
and expanded-to-fullscreen-to-mini/PiP handoffs. Verify no decoder reload or
new black screen, no stretching, and unchanged first-frame readiness. Include
screen captures with the same clip, source/time and settings in both modes.
Physical HDR panel output, acoustic/thermal performance, full hardware
acceptance and original-public signing remain separate gates.

This change is **SOURCE_ONLY**. Do not mark a verified APK, rewrite source
history, promote the old public 1.21.93 artifact, touch Nodes or Stack
alignment, merge automatically or modify original signing settings.
