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
  and optimized assembly pass.
- The exact optimized34 normal-entry A55 smoke passes. Four actual screenshots
  show Home, Fullscreen, Favourite, Search and Share matching the filled control's
  changing accent while the real video advances. Pause works and Share presents
  both original options. Screenshots were visually inspected.
- The later355 metadata check confirms the original appId/data inode, exact
  original preferences, volume, app-ops and test-package absence. After the smoke,
  original preferences/settings are restored and the exact optimized34 remains
  installed, without a downgrade. Guest shutdown completes; no emulator remains;
  stored A55 profile/GPU hashes are unchanged. Earlier false aggregates remain.

## Retained optimized artifact and follow-up

- Source commit: `bfed54eaa00d0ef58d3622af50a46fb54e079fe7`.
- App-input SHA-256: `f4e0def5c1371bbdc4257ec5ebdc0d954f6a5332ce030ff28d0dd6a055e9252e`.
- APK: `App/output/icon-theme-20261115/optimized34-maintenance/GreaterArt-1.22.34.apk`.
- Version/code:1.22.34/355; optimized/non-debug/non-test;5,595,920bytes.
- APK SHA-256: `ce3c239a146417919b7bd13506954b0f364a4818382ce837699dbb2c1b57e350`.
- Existing maintenance certificate:
  `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
- Private identity, ZIP CRC and16KiB alignment pass. Original-public certificate
  verification FAILS; that missing-key gate is unchanged, not waived.
- Exact smoke receipt: `optimized-smoke34-01/report.json`. Earlier failures and
  the two compact fixtures remain separate from this focused normal-entry pass.

The actual screenshot also shows neutral queue overflow icons. The follow-up
[1.22.35 source correction](PLAYER_ICON_THEME_1.22.35.md) completes that normal
player coverage. The tested34APK does NOT contain that later queue correction.

Private evidence: `App/output/icon-theme-20261115/`. The content baseline and
previous release hashes are retained. Existing user changes remain outside the
commit. No push, merge, branch/worktree, signing-key replacement or publication.

## References checked

- [Material3 IconButtonDefaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconButtonDefaults)
  supports explicit content colors without changing click targets or callbacks.
- [Earlier live-video theme verification](VIDEO_THEME_1.22.32.md) describes the
  unchanged shared palette, Power saving behavior and original signing limit.
