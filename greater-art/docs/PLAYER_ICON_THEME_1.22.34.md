# Now Playing action colors — 1.22.34 / code355

Status: SOURCE_ONLY. This is a focused icon-tint repair, not a Stack alignment
fix or public release. Keep assembly, original-public signing and exact-APK
device acceptance separate.

## Cause and scope

The normal toolbar selected fixed white whenever frame Ambient was active.
Otherwise it selected neutral text color. Search, Share and Previous/Next also
inherited neutral IconButton content. Those paths bypassed the shared video
accent even while Repeat, Speed and the Play/Pause background changed correctly.

Normal toolbar actions now use the existing Material secondary accent, as do
current-media actions and transport IconButtons. Favourite keeps its outlined/
filled glyphs and accessible labels. Stack loop-off remains visibly subdued in
the same hue. Material3 still owns disabled-state contrast and input behavior.
No holder, target, icon size or action was added or removed.

Controls directly over foreground fullscreen video deliberately keep the
existing light-on-dark palette and scrim. Filled Play/Pause keeps its on-secondary
symbol; tinting that symbol to its own background would hide it. These contrast
roles are not missing video samples.

No sampler, palette cadence/cache, Power saving policy, decoder, resolution,
frame rate, bit depth, HDR, seek, speed, surface ownership or Stack engine changed.

## Verification

- JDK21/API37 and the existing optimized-build preflight pass.
- The new actual-component screenshot check fails on the retained 1.22.33 debug
  APK for the intended reason: zero pixels in the current accent for the first
  toolbar action. It is not a missing API, compilation failure or timeout.
- The private first build runner passed a scalar as native arguments; Gradle
  rejected the task name before testing. Its failed log is retained. A proper
  argument array fixes only that runner.
- The baseline A55 run restores original preferences, app data and optimized33.
  Its immediate final receipt still sees Android's transient shutdown helper;
  a later read-only check finds no emulator. Keep that initial false aggregate.
- Both new rendered-icon tests pass on the A55: toolbar and current-media/
  transport icons follow two palettes, and their callbacks still work. Both
  existing transparent-toolbar/fullscreen-scrim checks also pass.
- The broader batch remains FAILED: two existing 320dp compact fixtures fail.
  The audio fixture expects a transport-area tag not present in the audio path;
  the video fixture toggles only `isPlaying`, while the current UI uses the
  separately stored `playRequested` field. These are source findings, not a
  baseline-device proof. Keep both failed assertions and do not call the whole
  batch green or change their gates as part of this icon repair.
- The private green restoration assertion still expected versionCode354 after
  the authorized355 upgrade. Preference bytes restore exactly; the final
  installed355 identity/settings check remains pending. No downgrade is allowed.
-604 JVM tests pass; lint reports zero fatal/errors and54 warnings. Diagnostic
  assembly passes. Optimized assembly and exact-file smoke are pending.

Private evidence: `App/output/icon-theme-20261115/`. The content baseline and
previous release hashes are retained. Existing user changes remain outside the
commit. No push, merge, branch/worktree, signing-key replacement or publication.

## References checked

- [Material3 IconButtonDefaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconButtonDefaults)
  supports explicit content colors without changing click targets or callbacks.
- [Earlier live-video theme verification](VIDEO_THEME_1.22.32.md) describes the
  unchanged shared palette, Power saving behavior and original signing limit.
