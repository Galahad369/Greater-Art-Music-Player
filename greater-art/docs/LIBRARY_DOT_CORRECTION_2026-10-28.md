# Library dot correction —1.21.90/code313

## Scope and cause

Baseline: main `5be845d008bbdda9173d859dc9ee2d61f65224c0`.
The previous24dp PlayArrow badge was a design misunderstanding.
Its test incorrectly required stationary artwork. Passing that test did not prove the requested design.

Use the existing Compose layout, not a new dependency. Compose's [offset modifier](https://developer.android.com/develop/ui/compose/modifiers) changes placement without changing measurement. Reserve8dp at the artwork slot's end to keep the text column fixed.

- Artwork leading spacer:12dp to4dp.
- Ordinary artwork:8dp left of the previous position.
- Current artwork:8dp right of the new ordinary position.
- Dot:6dp diameter,2dp clear of the current artwork; no PlayArrow or separate badge background.
- Paused dot:45% alpha. Row semantics retain translated playing/paused descriptions.
- Thumbnails off:dot before the unchanged title position, no artwork reservation.

Keep highlights, tap animations, selection, reorder and row actions. The keyboard-safe dock source is unchanged. Nodes, Stack, desktop and security tooling are unchanged. Version/docs/HTML status metadata follows the new source identity; no release is promoted.

## Local checks

Java21, Gradle9.8.2: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` passes.
471 JVM tests pass. Lint:0 errors,47 warnings,4 hints.
The geometry test uses pixel-rounded offsets, not unrounded dp products.

Owned device:GreaterArt_Scroll_QA36, API36,1080x2340,450dpi.
12 instrumentation cases pass in132.598s:

- LibraryPlayingMarkerTest:4 cases. Small/medium/large row and Mini sizes; thumbnails/details on/off; current-song switching; fixed title/action columns;6dp dot and non-overlap; six translated states; tap/reorder and theme checks.
- LibraryDotVisualTest:1 full-Activity capture matrix with discovered local files.
- LibraryRowWorkTest:5 cases, including RAM artwork seeding and action touch targets.
- LibraryImeDockTest:2 real audio/video search cases with CURRENT_VIDEO wallpaper. Preserve playback, controller and surface identity while the keyboard hides/restores the dock.

The app and tests were installed on the emulator. Only the test APK was ART-precompiled for functional-test startup. No speed, cold-start or physical-device claim follows from this.

## Visual inspection

All24 full-screen matrix images were opened and inspected:
2 themes x3 row sizes x2 thumbnail settings x2 playback states.
The Library used413 discovered local files, including QA videos with solid-colour frames and an audio placeholder. Current item0001 sits between ordinary items0000 and0002. The active artwork is indented, its dot is separate, and titles/actions align. Paused dots are muted. No current PlayArrow badge exists.

Two additional manual captures show actual cover songs, including Bitter Choco Decoration, with ordinary covers above and below. The playing capture also shows CURRENT_VIDEO wallpaper and a MediaSession state of PLAYING. The paused capture shows the muted dot and dock Play control. These confirm the layout is not limited to synthetic row-composable screenshots.

Evidence remains local in the sibling workspace `output/library-dot-20261028/`:
`build-90.log`, `build-90-tests.log`, `device-tests.log`,24 `dot-real-*.png` images, `cover-playing.png` and `cover-paused.png`.
Do not upload private media or screenshot contents to the public repository.

This does not prove physical-phone, TalkBack, all-wallpaper contrast or performance acceptance. It does not fix or enable the unfinished shared-output engine.

## Internal artifact and review gate

`output/library-dot-20261028/GreaterArt-1.21.90-internal-test.apk` (sibling workspace),27,055,861 bytes.
SHA-256:`3165ce7bc855f6c50781ead4997ce180a553ebb34b9420e2ae5dcbaec0abbfd2`.
Package:`com.local.listentomusic`; version1.21.90/code313.
16KiB ZIP alignment and the unchanged pinned signing certificate pass.

This is an internal test APK, not a new verified public release.
Keep the verified1.21.80 APK untouched.
Create a focused PR. Do not merge without explicit user authorization.
