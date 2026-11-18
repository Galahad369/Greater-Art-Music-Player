# Fullscreen Ambient and Now Playing controls — 1.22.47/code368 review repair

## Review basis

PR #246's 1.22.46/code367 first commit passes required Android, CodeQL,
Windows, privacy and version CI but has not undergone native visual acceptance.
The independent PR review recorded two presentation regressions, not a broken
APK identity:

1. `videoOverlayAccent()` checks the selected color over the 56%-black local
   `GaVideoOverlay.control` plate. Fullscreen speed text and the seek slider used
   that selected color without the same plate. Indigo dark secondary
   `#C5C9E7` passes about 3.04:1 on the local plate over a white frame, but
   only about 1.98:1 on the standalone 44%-black fullscreen scrim. Do not make
   contrast assertions about a background that the control does not render.
2. Expanded Now Playing's six evenly spaced toolbar targets were moved across
   the center 72dp/52dp pull-down gesture activation lane. This replaced the
   previous explicit clear-lane invariant. An active pointer detector can admit
   vertical gestures starting in that lane. Button-center taps may still work,
   but non-overlap with the pull zone must be mechanically guaranteed.

## Corrective implementation

- Keep the current theme accent if it passes the existing contrast check on
  actual `GaVideoOverlay.control` plates; keep the neutral fallback otherwise.
- Put a small 44x30dp dark capsule inside the existing 48dp fullscreen speed
  touch target. Preserve click/hold, label, timing and speed menu.
- Only the immersive video seek slider opts into a local 12dp-high dark track
  backing and 24dp thumb backing around the same 3dp track/18dp filled thumb.
  Normal audio and Stack slider rendering remain unchanged. Backing is part of
  transient controls, not a filter on video source pixels.
- Give the fullscreen timestamp labels a bounded local dark backing so their
  existing white text stays readable over bright footage.
- Restore the centered 72dp pull lane in the expanded toolbar. Keep 48dp
  touch targets outside that region. Distribute the left and right three-action
  groups evenly within symmetric widths, including small-window wrapping.
  Six uniformly spaced centers with a protected 72dp gap are geometrically
  incompatible in ordinary 384dp-wide layouts. A balanced, functional toolbar
  has priority over uniform cross-gap intervals.
- Do not modify the 52dp starting region of
  `expandedPlayerPullInput`, Lock/unlock, actual pull activation or resistance.
- Retain all previous PR #246 fullscreen SDR Ambient changes, native HDR and
  Android Power-saving fallbacks, aspect/pixel-ratio policy, zoom/pan, source
  frame quality, first-frame handoffs and media surface ownership.
- Do not modify Nodes, Stack, storage, permissions, desktop, signing keys or
  historical verified APKs.

## Regression coverage

1. Existing and expanded fullscreen contrast JVM checks across all user
   themes, dark/light and black/white backing extremes.
2. Rendered instrumentation tests for fullscreen speed plate and active seek
   track backing on a white video-surface fixture.
3. Expanded toolbar tests: centered 72dp gesture lane, no intersecting
   action targets, symmetrical intervals within both groups, stable Lock
   bounds, all actions still executable.
4. Real Compose pointer events: tap the five actionable button centers and
   swipe the central handle, verifying drag and click ownership separately.

## Required acceptance before merge

- Re-fetch the exact PR branch head. Run version guard, Android Gradle
  testDebugUnitTest/lintDebug/assembleDebug/assembleDebugAndroidTest/
  assembleRelease; review CodeQL, dependency and privacy checks.
- **Actually execute instrumentation tests** on Android emulator/device:
  `NowPlayingToolbarChromeTest`,
  `FullscreenTransportIconColorTest`,
  `FullscreenAmbientBoundsTest`, related fullscreen rotation/gestures,
  frame-Ambient and surface handoff tests.
- Inspect same-media/same-frame A55 portrait/landscape screenshots for
  Expanded/immersive fullscreen. Include light/dark/theme switching, bright
  video, speed/seek controls, Fit/Crop, pinch/pan, seek/pause/resume and direct
  fullscreen/Mini/PiP return. Include SDR frame-synchronous and HDR/
  unsupported/power-saving native fallback.
- Run the repository local optimized-APK gate against exact committed source.
  Record actual APK SHA-256, original-authorized signing certificate and
  installed-device smoke separately. A Linux CI release assembly by itself is
  not that acceptance.
- If SDK/emulator/authorized local signer unavailable, mark the corresponding
  gate BLOCKED and retain a draft PR. Do not assert screenshots were examined
  if no device ran.
- No new release, promotion, tag rewrite, forced push or original-signer
  reconfiguration. Preserve immutable verified1.21.93 and 46/47 source states.

## State at submission

**SOURCE_ONLY.** This handoff describes new code and tests prepared for PR CI.
No local Android SDK, emulator or signing key is available in the connected
GitHub source-editing environment. No physical-device visual, thermal, acoustic
or production-signing acceptance has been established.
