# Android 1.22.25 / code 346 — OLED, video colors and transparent transport

State: **SOURCE_ONLY**. No public release or original-signer promotion.

## Owner-requested behavior

- Black OLED is the default when no color preference is stored or after Appearance reset. Keep existing explicit choices and persisted enum names.
- Follow video colors is the second Settings choice. Its control accents use cached artwork immediately when available, then one matching sample from the existing video-color collector. Audio uses cover art. Neutral media keeps the neutral base. Do not create another decoder/readback loop or recolor the entire app on every frame.
- This explicit color choice does not depend on Library wallpaper, Ambient intensity or the Ambient video-reaction switch. Ambient itself remains independent. OLED stays black; other explicit Light/Dark choices remain available.
- Remove the rounded fill and outline around the normal Now Playing timeline/transport area. Keep seek track/thumb, elapsed/duration, repeat, previous, play/pause, next and speed. Keep fixed geometry, touch targets and callbacks, including temporary 2× hold. Do not remove fullscreen contrast protection or change video surfaces.

## Acceptance and preservation

The prepared checks cover default/order, all saved options, wallpaper-independent activation, contrasting video palettes, readable accent text, translations and existing JVM regressions. The six compact-player device cases also check actual blank transport pixels against the surrounding background while retaining control/layout assertions across audio/video and small/large layouts.

Optimized assembly, exact-file identity, signing and installed-device smoke are pending. Source checks or compiled instrumentation are not screenshots or runtime acceptance. Keep the exact 1.22.24 APK and original-signer 1.21.93 release unchanged. The existing separate maintenance signer cannot update an original-signer installation.

Task evidence: `App/output/oled-video-theme-20261113/`. Preflight passed before source changes; the final build must use the committed 1.22.25 inputs. Peak build space is estimated at 2–3 GiB; about28 GiB was free before starting.
