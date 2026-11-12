# Android 1.22.25 / code 346 — OLED, video colors and transparent transport

State: **SOURCE_ONLY**. No public release or original-signer promotion.

## Owner-requested behavior

- Black OLED is the default when no color preference is stored or after Appearance reset. Keep existing explicit choices and persisted enum names.
- Follow video colors is the second Settings choice. Its control accents use cached artwork immediately when available, then one matching sample from the existing video-color collector. Audio uses cover art. Neutral media keeps the neutral base. Do not create another decoder/readback loop or recolor the entire app on every frame.
- This explicit color choice does not depend on Library wallpaper, Ambient intensity or the Ambient video-reaction switch. Ambient itself remains independent. OLED stays black; other explicit Light/Dark choices remain available.
- Remove the rounded fill and outline around the normal Now Playing timeline/transport area. Keep seek track/thumb, elapsed/duration, repeat, previous, play/pause, next and speed. Keep fixed geometry, touch targets and callbacks, including temporary 2× hold. Do not remove fullscreen contrast protection or change video surfaces.

## Acceptance and preservation

The prepared checks cover default/order, all saved options, wallpaper-independent activation, contrasting video palettes, readable accent text, translations and existing JVM regressions. The six compact-player device cases also check actual blank transport pixels against the surrounding background while retaining control/layout assertions across audio/video and small/large layouts.

## Executed checks

- Runtime source: `aa2240cb0631be154fa9770bf4e8a78b63e16d4c`. All545 JVM tests pass, with no failures/errors/skips. Lint reports0 errors,54 warnings and6 hints. Android test compilation passes. The compact-player instrumentation was compiled, not executed; do not report its six fixture cases as device passes.
- The mandatory optimized `assembleRelease` passes from committed1.22.25 inputs, Android-input SHA-256 `263dfb5cda781ee4c19d30859dbc7e9e47d6b4f544296da942be81f05345cad5`. The historical original-certificate identity check correctly fails because that private key remains unavailable. Its failed receipt is retained, not changed into a pass.
- A separate local candidate uses the same existing maintenance certificate as1.22.24: `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`. Package, version/code, non-debuggable/non-test flags, ZIP integrity and16KiB alignment pass. No new signer was created.
- The exact installed APK passes the A55 native Settings/Library/dock/Now Playing sequence. OLED is the observed initial choice; the popup presents OLED before video colors. Both choices can be selected. Pause/resume, seeking to roughly35% (`1:06` of `3:08`), fixed controls and Home return work. The installed SHA-256 matches before and after. Actual OLED-paused and video-color-playing screenshots were captured and visually inspected: there is no bottom rounded panel or outline.
- The same APK passes the existing native fullscreen/Home/pull-to-Library regression, including sampled margin Ambient, DEV actions and inspection. Original DEV-off state and both temporary app-ops are restored. The color selection is restored to Black OLED. A test-only initial navigation failure looked for `Appearance` instead of the actual `Language & appearance`; its log is retained and only the private adapter was corrected. No app code or APK rebuild followed.

**Exact candidate:** `App/output/oled-video-theme-20261113/optimized-local/GreaterArt-1.22.25.apk`,5,579,536bytes, SHA-256 `15d61935631169f9b9e88965cb9106278bf794428c08aa4f3d4dd0206cc3bf4f`.

**Actual screenshot:** `App/output/oled-video-theme-20261113/native-02/now-playing-video-colors.png`. Native logs: `native-02.log` and `native-fullscreen.log`; unit/lint/compilation: `checks-03/android-checks.log`; immutable build/signing reports: `release/build.json` and `optimized-local/identity.json`.

## Limits and prevention

This is focused emulator acceptance, not full-app, physical HDR, performance, acoustic Stack or original-signer release acceptance. The maintenance key cannot update an original-signer installation. Do not overwrite the exact1.22.24 APK or verified1.21.93 download, change signing pins, publish, or claim the blocked original-key gate passed.

Keep the transport container as a layout-only Column: do not reintroduce clip/background/border decoration around seek/transport. Preserve the new pixel assertion and all existing control assertions. Store color choices by their existing names, not the Settings display order. Keep media-derived accents separate from Ambient intensity and source rendering.

Windows build lesson: this repository has the POSIX `gradlew`, not `gradlew.bat`. Use the Java `org.gradle.wrapper.GradleWrapperMain` entry point already used by the release gate. Retain the private wrapper's initial script-policy/missing-batch failures as host setup evidence, not app failures.

Task evidence: `App/output/oled-video-theme-20261113/`. Preflight passed before source changes; the final build must use the committed 1.22.25 inputs. Peak build space is estimated at 2–3 GiB; about28 GiB was free before starting.
