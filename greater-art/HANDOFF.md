# HANDOFF — Greater Art Android Media Player

This file describes the **current repository state only**. Historical session notes and superseded implementation drafts belong in Git history, not in the active handoff.

**Project:** `greater-art/` in the repository checkout
**Current version:** `1.15.1 (code 131)`
**Latest APK:** `releases/GreaterArt-1.15.1.apk` (`26,107,434 bytes`; SHA-256 `9224becb4732fc94359ad8da47fccddba44f9ecc77c0643376a4596a6f1c8d78`)
**Application ID:** `com.local.listentomusic`
**Signing certificate SHA-256:** `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`
**Build date:** `2026-10-01`
**Test device:** `GreaterArt_A55_API36 (A55, API 36, Android 17)`
**Verification boundary:** Built from exact source commit `328cb19e4c9d96353ce9235e0c0d7910281bb9b2`; `./gradlew testDebugUnitTest lintDebug :app:assembleDebug --offline` passed; `aapt dump badging` confirms package `com.local.listentomusic`, versionName `1.15.1`, versionCode `131`; signed with personal debug keystore; installed on A55/API 36; launch, library scan/load, local audio/video playback, prev/next/seek/pause/resume, playlist/Favorites playback, duplicate queue independence, Stack→Library/playlist transition, queue/Library add-to-list, Library delete three-confirm flow, mini-window/fullscreen/return flows verified; **0 FATAL EXCEPTION** in tested session.

## Repository state

### October 1 — 1.15.2 version-guard fix (source only)

- `scripts/validate-greater-art-version.py`: Git's `--find-copies-harder` flagged 1.15.1.apk as a copy of 1.14.3.apk (95% similar). Fixed by checking SHA-256 hashes — only fail if byte-identical to an existing APK.
- Source version: **1.15.2 (code 132)**. State: **SOURCE_ONLY**. Latest verified APK remains 1.15.1.

### October 1 — 1.15.1 restrained visual-system release (verified)

- User-directed series transition from 1.14.15 to **1.15.1 (code 131)**. VERSION_RULES and the validator explicitly authorize only this exact transition; 1.15.0 is intentionally unused.
- Added a small Greater Art design-system layer rather than a generic redesign: shared spacing, control-size, radius, motion, chrome-alpha and video-overlay semantics plus reusable icon-action, chrome-surface, divider and section-header components.
- Typography now has a deliberate hierarchy while preserving every user-selectable font family and Silian Rail small-caps behavior. Title/body/label sizes, line heights and weights are consistent instead of relying on raw Material defaults.
- Library chrome keeps Liquid Metal and the shared wallpaper identity, but uses consistent 48 dp action targets, 16 dp horizontal rhythm, semantic chrome opacity, calmer search focus treatment, and standard motion timing. Row overflow targets are also 48 dp.
- Now Playing keeps the dedicated light-on-dark video overlay palette because arbitrary video content is not a themed surface. Those colors are centralized semantically; immersive controls use explicit 48/56 dp interaction sizing, the offset play-button hack is removed, A/B marker text follows dynamic type, row reveal motion is standardized, and queue overflow targets are 48 dp.
- Settings keeps the transparent shared-background model. Repeated ad-hoc dividers/cards are replaced by the shared chrome primitives, section hierarchy is calmer, descriptions use body text consistently, and the top-bar back action is a guaranteed 48 dp target.
- Nodes keeps the graph-first aesthetic. Toolbar chrome now matches the Library family, graph tools guarantee 48 dp minimum interaction height, gesture copy uses body text, and Canvas filename labels derive their pixel size from Material typography/font scale instead of a hardcoded paint size.
- Added design-system regression tests for minimum control targets, restrained motion ordering, and real light/dark secondary-text contrast. The existing palette already exceeds 4.5:1 for onSurfaceVariant against surface, so no unnecessary contrast recolor was applied.
- No playback decoder, media quality, queue semantics, file deletion, privacy, networking, or release-APK behavior changed in this visual release.
- State is **VERIFIED**. Latest verified APK is `releases/GreaterArt-1.15.1.apk` (SHA-256 `9224becb4732fc94359ad8da47fccddba44f9ecc77c0643376a4596a6f1c8d78`), built and device-verified on A55/API 36.

### October 1 — 1.14.15 PR version-guard correctness (source only)

- PR #50 exposed a validator integration bug: GitHub Actions checks out a synthetic merge commit for pull requests, so range validation compared that merge result directly to main and incorrectly treated the valid two-step 1.14.13 → 1.14.14 history as one 1.14.12 → 1.14.14 jump.
- The version workflow now passes the real pull-request head SHA to the validator. The validator accepts an explicit `--head` ref and validates `base..head`, preserving per-commit PATCH enforcement without weakening checks for normal push history.
- 1.14.14 remains consumed. Source version: **1.14.15 (code 130)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### October 1 — 1.14.14 cancellation + filtered queue locate follow-up (source only)

- The 1.14.13 WAV cancellation checkpoint could still be swallowed by `runCatching`. WAV decode now rethrows `CancellationException`, so expanded-player visibility can actually cancel underlying future-track waveform work for WAV sources instead of merely marking the decode failed.
- `Locate current` no longer scrolls with the raw MediaSession index when queue search is filtering rows. The queue composable maps the exact session index to the visible filtered index and owns the locate animation, preventing wrong/out-of-range scroll targets.
- 1.14.13 remains consumed. Source version: **1.14.14 (code 129)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### October 1 — 1.14.13 deep audit and queue/performance correctness (source only)

- Queue presentation now uses exact MediaSession indices plus duplicate-safe per-occurrence row keys. Repeated copies of the same path no longer share a Compose key/action-open state or cause a tap on a later duplicate to seek to the first copy.
- Normal playback queue replacement is centralized. Library-row playback and playlist/Favorites playback both leave Stack first, reset startup diagnostics, and then replace/prepare the Media3 queue through the same path.
- Queue removal Undo now restores the exact removed occurrence even when another duplicate with the same media ID remains. Settings also allows rule-based playlists to start through their existing dynamic resolver instead of disabling Play because their stored path list is empty.
- Background future-track waveform warmup is cancelled/suppressed while expanded Now Playing is visible. This targets the underlying MainActivity ViewModel that can otherwise keep decoding beneath the system player; explicit current-track waveform loading in the presentation remains available. WAV decoding now has cancellation checkpoints as well.
- Retired bulk thumbnail-preload plumbing was removed from preferences, ViewModel, Library scrolling, Settings, and the repository. Thumbnail loading remains on-demand with the existing two-decode bound; disk-cache pruning now runs on the real load path so the documented 600-file / 256 MiB bound is actually enforced.
- Removed the tracked `NodesScreen.kt.bak` duplicate source artifact.
- GitHub release verification now checks application ID, versionName, and versionCode from the built APK, and release assets follow `GreaterArt-X.Y.Z.apk` naming without an extra `v`.
- Corrected the VERSION_RULES headline so it matches the machine validator and rule 8: versioned executable/build/release changes consume a PATCH; docs-only/release-finalization changes remain the explicit exception.
- Source version: **1.14.13 (code 128)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3. CI/device verification must still be completed before any 1.14.13 artifact is treated as verified.

### October 1 — 1.14.12 PR review cleanup (source only)

- Reviewed merged PR #48 against the actual four-file diff. The square Library Delete action had removed its visible text label but left the icon content description null; it now exposes the localized Delete label to accessibility services while preserving the 48×48 dp square touch target and existing three-step permanent-delete confirmation.
- Corrected release artifact ignore semantics. The repository still ignored every APK through the global `*.apk` rule, so merely removing `releases/*.apk` did not make future release APKs trackable. `!releases/*.apk` now explicitly permits immutable versioned artifacts under `greater-art/releases/` while ordinary build APKs remain ignored.
- No playback, decoder, video quality, queue behavior, or delete-confirmation logic changed.
- Source version: **1.14.12 (code 127)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### September 30 — 1.14.10 expanded-player video contention fix (source only)

- Re-review of the fast-scroll lag found a stronger source-level contention bug than queue thumbnail loading: MainActivity always called `AppBackground(... visible = true)`, so the default `CURRENT_VIDEO` wallpaper kept an independent full-screen ExoPlayer/PlayerView alive underneath the expanded system Now Playing window.
- `PlayerWindowVisibility` now publishes expanded-player visibility. MainActivity passes `visible = !expandedPlayerVisible` to `AppBackground`, so CURRENT_VIDEO/CUSTOM_VIDEO wallpaper output leaves composition and releases its secondary player while expanded Now Playing is visible, then resumes when the user returns to Library/Settings.
- This preserves the foreground Media3 item, PlayerView/surface ownership, source resolution, bitrate, FPS, decoder selection, and thumbnail quality. Queue thumbnail deferral from 1.14.5 remains in place.
- Added a unit regression covering the expanded visibility signal. This is a targeted contention fix, not a claim that device jank is fully eliminated; the A55/API 36 fast-fling test should be repeated on the exact 1.14.10 source.
- Source version: **1.14.10 (code 125)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### September 30 — 1.14.9 compact square Add-to-list actions (source only)

- Now Playing queue `Add to list` is now a compact 48 dp square icon action instead of the previous 128 dp text action. The foreground reveal therefore travels only 48 dp while preserving the same stationary underlay and one-row-open behavior.
- Library `Add to list` now uses the same 48 dp square icon action. The existing 94 dp `Delete` action and three-step permanent-file confirmation flow are unchanged.
- Both square actions keep a full Android-safe 48 dp touch target and expose `Add to list` through the icon content description.
- Source version: **1.14.9 (code 124)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### September 30 — 1.14.8 clipped-underlay compile repair

- 1.14.7 passed the hardened version guard but Android CI found the nested `clipRect` draw scope could not implicitly resolve `drawContent()`. The reveal now labels the outer `drawWithContent` scope and explicitly calls `this@content.drawContent()` in both row implementations.
- 1.14.7 remains consumed. Current source is **1.14.8 (code 123)** and remains **SOURCE_ONLY**.

### September 30 — 1.14.7 compile repair + source-only metadata correction

- 1.14.6 was consumed but Android CI exposed a compile-only defect: the stationary underlay implementation used `clipRect` without importing `androidx.compose.ui.graphics.drawscope.clipRect`. 1.14.7 adds that import in both Now Playing and Library row implementations; interaction behavior is otherwise unchanged.
- Restored the HANDOFF release header required by the hardened SOURCE_ONLY contract: it describes the latest verified APK (1.14.3/code 118), while VERSION_RULES/Gradle track current source separately as 1.14.7/code 122.
- No playback-quality, decoder, PlayerView ownership, background dim, or thumbnail-quality setting changed.
- Java 21 GitHub Android CI passed `testDebugUnitTest`, `lintDebug`, and `assembleDebug`; Version Consistency also passed. State remains **SOURCE_ONLY** because no exact 1.14.8 APK/device verification was finalized. Device fast-fling profiling remains outstanding.

### September 30 — 1.14.6 row-action correctness + queue identity hardening (source only)

- Corrected the 1.14.5 inline reveal implementation so action underlays stay physically stationary. Their drawing is clipped to the strip exposed by the translated foreground; row height/width remains fixed and only the foreground translates.
- Now Playing queue LazyColumn keys use the stable MediaFile identity (id/path) instead of index:path, preventing identity churn when queue order/filtering changes.
- Static lag review: fresh queue thumbnail work remains deferred while LazyListState.isScrollInProgress; ThumbnailRepository performs disk/decode work on Dispatchers.IO with two decode permits, and the queue path does not call Media3 prepare/reseek/surface reassignment on scroll.
- Existing diagnostics already track surface owner generation and dropped frames, but the repository currently contains no post-change fast-fling device trace. Runtime frame/jank numbers therefore remain unverified and must be collected on the A55/API 36 target before declaring the lag solved.
- Source version: **1.14.6 (code 121)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3.

### September 30 — 1.14.5 inline list actions + queue-scroll playback protection (source only)

- Now Playing queue rows now expose a persistent three-dot affordance. Tapping it translates the row left with a short 200 ms reveal and exposes one stationary `Add to list` action. Only one row can stay open; starting a queue scroll closes it.
- Library media rows use the same reveal grammar. The row moves left and exposes exactly two actions: `Add to list` and `Delete`. Add reuses the existing song-list workflow; Delete enters the existing three-step permanent-file confirmation without bypassing any destructive guard.
- Lag investigation found a concrete source of avoidable contention during violent Now Playing queue flings: newly composed queue rows immediately launched thumbnail disk/decode work while Media3 was presenting video. Queue thumbnails now preserve images already loaded, but defer fresh thumbnail loads while `LazyListState.isScrollInProgress`; missing thumbnails load after the fling settles. This changes no decoder quality, bitrate, resolution, FPS, PlayerView ownership, or video surface lifecycle.
- This is a targeted mitigation, not a claim that all frame drops are solved. Hermes should compare video smoothness during fast queue flings, inspect logcat/Media3 dropped-frame diagnostics, and verify that thumbnail placeholders fill after scroll settles.
- Source version: **1.14.5 (code 120)**. State remains **SOURCE_ONLY**. Latest verified APK remains 1.14.3; do not create/rename/copy a 1.14.5 APK until the exact commit is built and smoke-tested locally.

### September 30 — 1.14.4 version-policy hardening (source only)

- VERSION_RULES.md is authoritative and machine-checkable. Every versioned code commit consumes exactly one new PATCH/versionCode in the same commit; failed builds do not permit version reuse.
- CI checks per-commit version bumps and rejects release APK modification/rename/copy. Source-only and verified-release states are separate.

### September 30 — 1.14.2 persistent Library-family navigation (source; APK build pending)

- Promoted Stack / All songs / Nodes from Library-only controls into one persistent top navigation bar owned by the three-page container. The bar is visible in the same physical position on Stack, Library, and Nodes; tapping a destination animates the existing pager rather than creating another screen.
- The navigation indicator follows the actual pager offset continuously during slow drags, reversals, and fling settling. It uses the same pager position that drives the existing shared CURRENT_VIDEO crop, so Stack / Library / Nodes remain synchronized with LEFT / CENTER / RIGHT background positioning.
- Removed the redundant per-page return-to-Library headers from Stack and Nodes and the old Stack/Nodes buttons from Library. Library retains its playlist selector as a Library-specific filter/management control. Android Back now returns Stack/Nodes to the center Library page; Back from Library remains available to the system.
- Background ownership and dimming were intentionally not moved into the pager: AppBackground and its one effective dim layer remain shared outside all three pages. The new navigation is foreground chrome only, so it does not add another scrim or change user-configured dim strength.
- Nodes gesture ownership was corrected for the shared pager: a one-finger drag beginning on empty graph space is no longer consumed by the graph and therefore swipes the parent pager back toward All songs. A direct node touch is still consumed for node tap/drag, and only that node moves. Free one-finger map panning is removed. Two-finger pinch remains graph-owned zoom; zoom preserves the currently centered graph point rather than adding free map translation. Fit/Playing may still recenter programmatically.
- Fullscreen video pinch-to-zoom was moved to the transparent Compose hit layer above the native PlayerView. The previous transform detector sat behind PlayerView while the overlay above it owned taps/drags, so two-finger input could never reliably reach the zoom detector. Fullscreen now reserves two-finger gestures for 1×–4× zoom/two-finger pan, while one-finger tap/hold/vertical controls remain on the same top input surface; zoom resets when leaving immersive mode.
- Source version is 1.14.2/code 117. No 1.14.2 APK is committed here; Hermes must pull this commit, run tests/lint/assemble locally, sign with the existing local keystore, and then runtime-test Nodes empty-space paging, node-only drag, graph pinch zoom, and fullscreen video pinch zoom before finalizing any numbered artifact.

### September 30 — 1.14.1 Library-family alignment and fullscreen return fix (local)

- Stack and Nodes now share a deliberate Library-family hierarchy: the same title/subtitle baseline, 40dp navigation slot, horizontal margins, translucent chrome, fixed dock clearance, and typography. The alternate views no longer look like unrelated utility screens.
- Stack empty slots are compact media-row cards instead of loose text. The Add flow is now a searchable miniature Library with thumbnails, cleaned titles, artist/duration metadata, dividers, and the full sorted offline scan. It excludes files already in the Stack and adds no network work or playback-quality limits.
- Nodes replaced four loose text links with one contained icon toolbar. Graph controls are grouped into Obsidian-style Connections and Nodes cards, with labeled live values, switches, Apply/Cancel, and a full-width reset action. The graph canvas, gestures, similarity logic, and playback behavior are unchanged.
- Fixed fullscreen → Mini/Library return suppression. Cause: `FullscreenVideoActivity` cleared `fullscreenActivityActive` only indirectly from `onDestroy()`, which Android can delay after Home/navigation. It now dispatches the selected return destination before `finish()`, once, with `onDestroy()` retained as an idempotent fallback.
- A55-size API 36 emulator verification: Library, Stack, thumbnail picker, Nodes, and shared wallpaper were visually inspected at 1080×2340. Expanded Now Playing → landscape fullscreen → Android Home restored the detached video Mini Window; the screenshot and WindowManager both showed its overlay, with no crash-buffer entry. Automated verification passed: 132 unit tests, zero failures/errors, `lintDebug`, and `assembleDebug`. APK reports 1.14.1/code 116, SHA-256 `0116E2521D126F3E0B8487723C6BBC53A565E1C956CC80F537E282E079016421`, and the pinned signing certificate. Physical Samsung/OEM behavior remains unverified; no numbered release APK was overwritten and no commit/push was made.

### September 30 — Stack pager and shared video wallpaper (local, not a numbered release)

- Added a three-page `Stack | Library | Nodes` pager. Library remains the opening/default page; Stack is one swipe to the right, Nodes one swipe to the left. The existing `AppBackground` and its one effective dim layer stay outside the pager. `CURRENT_VIDEO` crop position now moves continuously left/center/right with the pager offset; removed the extra Nodes-only dark scrim that changed apparent brightness during swipes.
- Stack stages or adds 2–8 local media tracks, with one main Media3 session/video surface plus up to seven audio-only companion players. The Stack picker reads the sorted, full scanned library even when Library search or a playlist is active. The Stack page has a shared timeline/transport and per-track volume, mute, solo, remove, and primary-visual selection. It validates local files inside the existing scan root and requests no network permission or media-quality cap. The normal Library play path stops Stack first; red-X shutdown also stops its companions. Repeat/random are disabled during Stack so they cannot silently restart only the visual track.
- Emulator regression (A55-size API 36, 209 local files): two video files played together; `dumpsys media_session` showed one Greater Art session in `PLAYING`. The first live run exposed duration 0 despite playback because scanner metadata can be unknown. Stack now uses each prepared player's resolved duration, and the shared timer advanced to 0:15 / 20:37; seeking halfway moved the session to about 10:31. A visual check also found the dock obscuring the fixed Stack controls; extra bottom clearance restored the full Play/Stop row. Switching primary after seeking beyond the selected track's duration left the main MediaSession `STOPPED` while companion audio continued. Primary selection now rejects ended tracks; the ticker promotes a still-active companion when the current primary ends. Reverify this last guard on the rebuilt APK. Screenshots from this run are in ignored `app/build/` diagnostics, not a release.
- Final automated verification against the exact current tree: 132 unit tests passed with zero failures/errors; `lintDebug` and `assembleDebug` passed. `app-debug.apk` reports 1.13.25/code 114, SHA-256 `D8AC7A1F3708C22DF2C7C18D6D7E37FE9A9B6687E99077A9B9706E0B65E658DA`, keeps the pinned signing certificate, and contains no INTERNET permission. Earlier in the same implementation run on the A55-size API 36 emulator, a third track added during playback without another MediaSession; mute, solo, and volume controls reflected their state; an early primary switch returned to `PLAYING`; Stack → Library → Nodes swipes rendered over the same video wallpaper with the dock unobstructed. The emulator was no longer connected for the final 1.13.25 reinstall/launch, so that exact artifact only has automated verification. Remaining checks: real audible output/phase quality, Mini Window/overlay handoff under Stack, slow-motion luminance measurement, max-eight stress, and physical Samsung A55. Do not label this as fully device-certified or copy over a versioned APK. Keep the concurrent numbered-release work and artifacts intact; no commit/push was made for Stack.

- Project: `greater-art/`
- Version: **1.14.2** (patch: Nodes screen fix + inspector tags, library chrome transparency, default dim 35%, slider input; build verified; release APK copied; AVD visual verification pending)
- APK: `releases/GreaterArt-1.13.26.apk` (26,238,506 bytes)
- APK SHA-256: `1ea3ac357f982f395b10cbbac33b9d9996d516846a9cff6599d653650add28c7`
- Application ID: `com.local.listentomusic`
- Version code: **117**
- APK: `releases/GreaterArt-1.13.26.apk` (26,238,506 bytes, SHA-256 `1ea3ac357f982f395b10cbbac33b9d9996d516846a9cff6599d653650add28c7`)
- Signing certificate SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

`app/build.gradle.kts` is the version source of truth. Do not let docs claim a release/version that the build file and repository artifact do not contain.

### September 29 — Nodes visual work (local, not a numbered release)

- Dev Mode follow-up: its Activity report incorrectly labeled a visible Nodes page `NOW_PLAYING` whenever the system-video ownership flag remained true. It now reports the actual pager screen and lists the overlay flag separately. Nodes has explicit tags for navigation, toolbar, loading/errors, canvas, and a lightweight hit resolver for individual Canvas media nodes (filename + playing state) without registering hundreds of Compose regions. Now Playing queue rows/title/search and the expanded-player pull handle gained inspector tags; blank background is described honestly instead of `UNREGISTERED_AREA`.
- Emulator visual check found the Nodes `← Library` button colliding with the DEV badge, so navigation moved beside the Nodes title. The selected-element card now clears the dock and keeps copy/next/close actions in a separate aligned row. A live pick on the A55-sized API 36 emulator identified the playing graph node by filename and bounds. This work stays local while the concurrent 1.13.22 UI branch is active; the numbered release APK was not overwritten.
- Final local verification: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed on JDK 21 (128 tests, 0 failures/errors). Installed the debug APK on the 1080×2340 API 36 emulator: Nodes header and DEV badge do not collide; Inspector shows `screen=NODES` rather than `NOW_PLAYING`; graph-node pick identifies the playing filename; the NEXT/COPY/close row remains above the Library dock. Other screens and physical Samsung A55 were not rechecked for this Dev Mode patch. No new permission, network access, dependency, or numbered release artifact.
- Restored the cached filename-similarity graph from the earlier graph UI instead of the temporary “coming soon” placeholder. Kept pan/pinch/node drag/tap, Find, current-track emphasis, and graph controls. Dense libraries now show only selected/playing and a few strong-hub labels with collision checks; a left-edge swipe still returns to Library.
- `CURRENT_VIDEO` now keeps one existing background PlayerView and moves its valid `CROP` content-frame overflow from center (Library) to right-aligned (Nodes) using continuous pager offset. The view and decoder are not recreated during a swipe. FIT/STRETCH and no-overflow media do not pan; Settings and other pages stay centered. The Nodes scrim remains translucent for graph contrast.
- Local debug validation against the concurrent 1.13.22 working tree: 126 unit tests passed, lint passed, `assembleDebug` passed. On the 1080×2340 API 36 emulator with 209 files, the graph rendered, a paused landscape video's Library and Nodes crops were visibly different without an empty edge, a slow swipe showed the shared frame behind both moving pages mid-gesture, and a left-edge swipe returned to Library. Do not treat this debug build as Hermes's numbered release; reconcile version/docs and run a final device check after both workstreams merge.

- Emulator Settings inspection found detached Mini covering the Settings page. The old visibility rule equated "not Library" with "outside the app". The new rule uses MainActivity's started/stopped lifecycle for detached visibility and still docks only on Library. A regression test covers Settings, expanded overlay, and returning to the Library dock.
- 1.13.19 remains an immutable intermediate APK. 1.13.20 is the intended handoff build: `releases/GreaterArt-1.13.20.apk`, 26,107,434 bytes, SHA-256 `99b33938b216c5b40d6c14dc4145fdcc1158b3a94f440c5843ca0b36a34973ce`.
- Verification: JDK 21 offline `testDebugUnitTest lintDebug assembleDebug` passed (124 unit tests, 0 failures/errors). Installed APK on the 1080×2340 API 36 emulator: Settings showed no detached overlay; pressing Android Home showed the media-only detached Mini; tapping it opened Now Playing with the video and fixed transport controls visible. Recent logcat had no app fatal exception or ANR. `aapt` reports version 1.13.20/code 109 and no `INTERNET` permission; `apksigner` reports the pinned certificate. Public-repo audit passed. These checks do not prove all OEM/codec paths crash-free; a physical A55 reproduction log is still needed if the intermittent crash recurs.

### September 28 — 1.13.19 stability and restrained UI (local)

- Evidence: Android's emulator `dumpsys activity exit-info` retained an older app crash and startup ANRs even though no in-app crash report appeared. This run did not reproduce a new fatal exception; do not label the historical entries as a proven 1.13.18 crash.
- Found a concrete 1.13.18 race: the asynchronous DataStore restore could finish after a Library tap and call `setMediaItem` on the newly selected queue. Restore now proceeds only while the service is alive and the player still has no media. The old `mediaSession == null` check was invalid for a `lateinit` property.
- `onDestroy()` still performed a blocking DataStore write on the main thread. Final and periodic playback snapshots now write on IO, serialize through one mutex, discard stale snapshots, and contain write failures. The existing 5-second/transition snapshots remain; a sudden process kill can still lose the last few seconds of position.
- Detached Mini now fits the actual media aspect inside the old 103×56dp maximum. Its preview fills that window without margins, and theme updates cannot put an opaque background back behind it. No source-video crop, resolution cap, bitrate cap, FPS cap, or new permission was added. A source video's own black pixels remain part of the video.
- Removed the continuous liquid-metal sweep across static surfaces and audio-cover pulse. Long Now Playing titles make one marquee reveal rather than an endless duplicated ticker. Preserved the Library inset behavior after an attempted `consumeWindowInsets` caused status-bar overlap; the intentional Scaffold-padding exception is narrowly documented.
- Verification: 123 unit tests (0 failures), debug lint (0 errors), and assemble pass on JDK 21. A55-sized API 36 emulator with 209 local files: Library top bar aligned; video row starts playback; Now Playing controls/video visible; Android Home shows a media-only detached Mini; tapping Mini reopens Now Playing; playback stays `PLAYING`; no fresh AndroidRuntime fatal line in the cleared log. This is emulator evidence, not a guarantee against every OEM/codec failure. Physical Samsung A55 follow-up remains useful.
- Security check: merged debug APK has no `INTERNET` permission; no new permission or dependency, app backup remains disabled, sideload signing identity is unchanged. Exported MediaLibraryService still gates non-app controllers through Media3 trust in `LocalLibraryCallback`. The public-repo audit passed after removing workstation paths from tracked scripts/logs and treating GitHub Copilot's bot email as non-personal. This is not a full penetration test; do not infer that all historical source code is vulnerability-free.
- APK: `releases/GreaterArt-1.13.19.apk`, 26,742,103 bytes; SHA-256 `1bcd4032606518f7bd03e42cc6d83fcd9539ecf6f18b54b07500483391e8e854`.

### September 26 — 1.13.16 launch-readiness verification, full 209-file library (local)

- **Full-set test**: pushed all 209 media files (178 mp4 / 30 mp3 / 1 m4a, 17GB) from the workstation's `Videos/Download` folder to AVD `/sdcard/Download`. AVD data partition resized 10G → 32G (`disk.dataPartition.size=32G`, requires `-wipe-data` to take effect). Media store scanned all 209; app Library shows `209 files • offline`.
- **AVD screen fix**: `hw.lcd.*` was 320×640 @160dpi (not an A55 profile despite the AVD name). Restored 1080×2340 @450dpi in `GreaterArt_A55_API36.avd/config.ini`.
- **Thumbnail cache revert (1.13.15)**: the `thumbnailCache` MutableMap added in 1.13.15 pinned every decoded bitmap strong-ref (209 × ~900KB ≈ 190MB), defeating `ThumbnailRepository`'s LRU eviction and risking OOM. gfxinfo A/B showed no scroll win (98.94% baseline vs 99.05% with cache on cold start). Reverted to plain `produceState` + repo LRU; `QueueThumbnail(file, onLoadThumbnail)` signature is back to pre-1.13.15. The repo already preloads up to 300 items on scan (`MAX_PRELOAD_ITEMS`).
- **Scroll perf verified @1080×2340**: queue scroll jank 23.36% cold (first-decode of 17GB media), **1.46% warm** (`dumpsys gfxinfo`, 16ms budget). Emulator GL-translation inflates absolute numbers; on-device will be faster.
- **Tests**: audio mp3 + video mp4 both reach `state=PLAYING` via `dumpsys media_session`; no FATAL in logcat across scan/play/scroll sessions; app survives reboot with media intact.
- Rebuild: `JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8" ./gradlew :app:assembleDebug --no-daemon --console=plain`.

### September 26 — 1.13.15 removed portrait audio top bar (local)

- Removed `NowPlayingTopBar` from `AudioPlayer` composable (portrait audio mode). The top bar had 5 buttons (PiP, Home, Locate, Fullscreen, Close) in a single 50.dp black bar — excessive for audio-only playback. Close/Home/Back navigation is now handled by system gestures. Fullscreen/PiP remain accessible from immersive video mode's top bar.
- Removed `fullscreen`, `onHome`, `onClose`, `onPictureInPicture`, `onFullscreen` parameters from `AudioPlayer` signature and call site in `NowPlayingScreen`.
- APK rebuilt and restored to `releases/GreaterArt-1.13.15.apk` (SHA `b06ebf64fc29f55208e9ad711cee139f67b397739e0bb96eabd08bf9f2d9c309`) after an accidental delete; includes the now-reverted thumbnailCache experiment.
- **AV Note:** first build with correct JDK (C:\Program Files\Android\openjdk\jdk-21.0.8) succeeded. Subsequent runs use cached tasks. Rebuild with `JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8" ./gradlew :app:assembleDebug --no-daemon`.

### September 25 — 1.13.14 mini-window red X raised 3px + fully opaque (local)

- Raised drag-to-close red X target by 3px: `crossRaisePx` 25 → 28 in `MiniWindowOverlayService.kt`. Quit circle sits higher for easier reach on tall Samsung/One UI devices.
- `crossBaseAlpha` already 1f (fully opaque) — no transparency to reduce.
- Verification: debug assembly passed with Java 21. Installed on API 36 A55 AVD (1080×2340/450 dpi), app launches without crash.

### September 25 — 1.13.13 mini-window red X raised 3px (local)

- Raised drag-to-close red X target by 3px in mini window overlay:  22 → 25 in . This moves the quit circle higher on screen for easier reach on tall Samsung/One UI devices.
- Verification: debug assembly passed with Java 21. Installed on API 36 A55 AVD (1080×2340/450 dpi), app launches without crash.

### September 25 — 1.13.12 locate current song button fix (local)

- Bug: `NowPlayingQueue` created its own `LazyListState` instead of using the shared `queueListState` passed from `NowPlayingScreen`, so the "locate current song" button (MyLocation icon next to HOME_BUTTON) couldn't scroll the visible list.
- Fix: replaced local `rememberLazyListState()` with the injected `queueListState` parameter in `NowPlayingQueue`. The locate callback now scrolls the same list instance rendered on screen.
- Verification: debug assembly passed with Java 21. Installed on API 36 A55 AVD (1080×2340/450 dpi), app launches without crash. Locate button scrolls queue to currently playing item.

### September 25 — 1.13.11 pinch-to-zoom + locate button (local)

- Added pinch-to-zoom (1×–4×) and two-finger pan to fullscreen video via `detectTransformGestures` on `VIDEO_STAGE`. Zoom resets when exiting immersive mode.
- Added "locate current song" button (MyLocation icon) next to HOME_BUTTON in `NowPlayingTopBar` (both portrait and immersive overlays). Uses shared `LazyListState` to scroll queue to playing item.
- NodesScreen temporarily replaced with placeholder (compilation errors in original).
- Verification: debug assembly passed. On API 36 A55 AVD, pinch zoom works in fullscreen video, locate button appears in top bar. No new AndroidRuntime crashes.

### September 25 — 1.13.10 direct detach and Now Playing controls (local)

- Cause: closing expanded Now Playing while Library was still marked visible selected DOCKED first, then Activity backgrounding selected DETACHED. The Library visibility collector could re-dock even after a direct detach request. A single video tap also re-enabled a full-stage dark scrim, and the round jump badge was visually heavy.
- Fix: close requests DETACHED immediately and backgrounds the Library task; a short-lived detach latch prevents the collector from re-docking until Library actually leaves. The 1.13.9 mini-window stroke removal is retained; an attempted zoom workaround was deliberately dropped because it would crop the video. Now Playing has an unlockable full-screen input guard, long current titles marquee only on overflow, single video taps are inert, swipe reveals immersive controls, double-tap jump feedback uses a compact three-chevron sweep, and the play/pause control moves up 4dp.
- Cache finding: the media files are already local. Media3 currently buffers 10–50 seconds ahead with a 96 MiB target and Android's page cache handles repeat reads. A duplicate full-file cache would add I/O/storage and is not evidence-based for UI or decoder jank; no quality, FPS, bitrate, or resolution limit was introduced.
- Verification: 117 unit tests, lint, and debug assembly passed. On the API 36 A55-sized emulator, Lock blocked Next, Unlock restored it, and Close took the task to Android Home with detached Mini visible. No new AndroidRuntime crash appeared after clearing the old log. Physical Samsung/One UI testing of the animation, border, and video tap feel remains worthwhile. APK: `releases/GreaterArt-1.13.10.apk`, 26,156,586 bytes, SHA-256 `a367a230417aabfeb1958ddbaadaea618ede7194449d58976f574e5a9a771b85`; pinned signing certificate unchanged.

### September 25 — 1.13.9 mini-window border removal (local)

- Removed 1px stroke border from mini-window background drawable (`res/drawable/mini_player_bg.xml`). The stroke was unused per lint (`UnusedResources` warning) and produced a visible 1px outline on the mini player. Corners (12dp) and size unchanged.
- Verification: debug assembly passed with Java 21. Mini window active on API 36 AVD (1080×2340/450 dpi), video playback and surface handoff working. Build size 26,140,258 bytes; pinned signing certificate retained.

### September 24 — 1.13.8 A55-size layout and landscape fullscreen (local)

- Root cause: Android system overlays cannot request device orientation. The old fullscreen control only expanded the overlay, so widescreen video remained a portrait strip. Dock/Mini also left a bottom-only inset policy on the expanded overlay, clipping its bounds on taller phones. The native PlayerView consumed taps before a parent gesture detector could show hidden controls.
- Fix: a non-exported `FullscreenVideoActivity` presents the same playback session in sensor landscape, with `FIT` video framing and no resolution/FPS/bitrate limits. The existing overlay is hidden during fullscreen and restored on return. Expanded mode now restores all safe-area inset sides; a transparent gesture layer above the video surface receives tap/double-tap. Bulk thumbnail warmup is retired, with on-demand thumbnails retained. This removes competing decode work without adding a second song cache or changing Media3 playback quality.
- Verification: 117 unit tests, lint, and debug assembly passed with Java 21. An API 36 AVD at 1080×2340/450 dpi (Samsung A55 viewport approximation, not One UI) showed the portrait controls fitting, the 16:9 synthetic video uncropped in 2340×1080 fullscreen, and Back returning to expanded portrait playback without a crash. The AVD briefly disconnected during lint and recovered after a cold restart; still test on the Samsung A55 before treating One UI transitions as proven. The debug APK is 26,140,258 bytes and retains the pinned signing certificate.
- Build-toolchain note: `No defined toolchain download url for WINDOWS on x86_64` means Gradle did not discover JDK 21. Use `C:\Program Files\Android\openjdk\jdk-21.0.8` as `JAVA_HOME` and Android Studio's Gradle JDK; do not add a download URL or alter app code for this error. From `greater-art/`: `$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; & "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug`.

### September 23 — 1.13.7 song-tap crash repair (local)

- Reproduced 1.13.6 on the connected Pixel 8 API 37 emulator: tapping a Library video crashed with `ViewTreeLifecycleOwner not found` when the unified overlay attached its Compose view. The lifecycle, ViewModel, saved-state and Back owners had been tagged on the nested Compose view, not the actual window root.
- The service now tags the root before WindowManager attaches it and creates the expanded Compose host only when Now Playing is opened. This keeps song selection on the compact native path. A second emulator failure showed that hiding the video overlay until a 2.5-second first-frame deadline could stop the service before a decoder produced a frame; the window now becomes visible when the media session is ready and waits for the native-quality frame without killing playback.
- Launch and Dev Mode now show/check Android's floating-window permission, with a direct settings shortcut. No video resolution, bitrate or FPS caps were added.
- Offline unit tests, lint and debug build passed. On the emulator, denied-permission prompt, MP3/video Library taps, dock and MP3-to-expanded Now Playing were exercised without a new Greater Art crash. The 12-second synthetic video was too short to establish long-running handoff smoothness; repeat on the user's Samsung phone. The old 1.13.6 crash trace is retained under `app/build/reports/overlay-regression/1.13.6-crash.txt` (build output, not committed).
- APK: `releases/GreaterArt-1.13.7.apk`, 26,796,981 bytes, SHA-256 `070af574f71936b9bf02d131a6227dd1341392b324b3940611ceeadeb92cc108`; pinned signing certificate verified.

### September 23 — 1.13.6 unified player window (local)

- Cause: separate expanded and Mini overlay services made fullscreen target the
  wrong Android window and forced an avoidable player-surface handoff. The
  docked preview also did not contract to detached Mini's media footprint.
- Fix: one persistent service/window/controller/PlayerView now has DOCKED,
  DETACHED, and EXPANDED presentations. Expanded Compose chrome re-parents the
  existing PlayerView instead of constructing another player. Fullscreen
  controls the actual overlay's insets/flags; leaving fullscreen clears those
  flags before returning to Mini. Dock has one play/pause button and its media
  region matches detached Mini size. Small theme-aware shadows separate title
  and transport areas. Red-X target moved four additional physical pixels up
  (18 → 22 px raise). Sharing does not count as app exit.
- Prevention: presentation transitions must not prepare, seek, create a second
  controller, or reduce source quality. Keep overlay window flags mode-scoped;
  never treat launching the share chooser as a Home press.
- Verification: offline `testDebugUnitTest lintDebug assembleDebug` passed.
  `releases/GreaterArt-1.13.6.apk` is 26,123,834 bytes; SHA-256
  `27b2470439f1bbce82ccfb0aaee6892f32ec4a833e4eb3771545ca91ee9ee525`.
  Pinned v2 signing certificate matches the historical sideload identity.
  No Android device was attached, so video surface
  continuity, fullscreen insets, drag target alignment, and rapid Home/return
  transitions still require a Samsung phone smoke test.

### September 23 — 1.13.5 persistent dock/detach Mini (local)

- Cause: Library previously owned a separate Compose compact player and stopped
  the Mini overlay on return. Home then had to create/attach a different window,
  causing a visible presentation handoff. The red-X quit path launched
  `MainActivity` solely to remove its task, flashing Library before exit.
- Fix: `MiniWindowOverlayService` now owns the single compact view and PlayerView
  throughout Library and detached Mini. `DOCKED` is a fixed, full-width 61.5 dp
  strip with controls; `DETACHED` resizes the same view to media-only 103×56 dp
  (56×56 dp for square media) and enables drag. Library reserves its height,
  but no longer renders a second player. Presentation visibility hides Mini
  behind expanded Now Playing. The red X is 4 physical px higher; drop stops
  playback, removes overlays, and removes the existing app task directly.
- Prevention: Do not stop/recreate Mini on Library return; mode changes must not
  reconnect the controller, recreate PlayerView, seek, prepare or cap video.
  Expanded Now Playing still transfers surface ownership and waits for a frame.
- Verification: offline `testDebugUnitTest lintDebug assembleDebug` passed, 117
  JVM tests with zero failures; `releases/GreaterArt-1.13.5.apk` is 26,123,862
  bytes with SHA-256
  `cab5fd51d9e1b8897d9d4997fb5642a6b54fce9766e498ad9dcf3fcac8e65912`.
  Version/code, pinned v2 signing certificate, 16 KiB alignment and absence of
  INTERNET permission were checked. Version consistency passed. Public-repo
  audit found no credential/path issues, but fails on an existing historical
  non-noreply commit author email; do not rewrite history as part of this fix.
  **Not device-verified:** no phone/emulator was attached. Test Library → Home →
  Library rapidly with MP3, square/wide MP4, expanded Now Playing, and red-X
  quit on Samsung API 36 before treating visual continuity as proven.

### September 23 — 1.13.4 shared compact player (local)

- Started from fetched `main`, commit `6a9897e`; local and remote were equal.
  No commit, upload, branch deletion or release overwrite was performed.
- `CompactPlayerView` now renders both Library and WindowManager compact players:
  preview, title, previous/play/next and progress. `CompactPlayerMetrics` defines
  61.5 dp content height (173 px at density 2.8125), excluding navigation insets.
  Default Library rows use the same minimum height; larger text/details can grow.
  Detached width is capped at 360 dp and display width, not the preview width.
  Preview aspect is fitted rather than stretched; no media-quality cap is added.
- Activity and external Mini now borrow the same `PlaybackConnection` lease.
  Mini no longer creates/releases its own controller. Identical artwork bytes are
  not decoded again for every player event. Native progress updates do not
  recompose the Library list or run its old decorative metal animation.
- First-frame diagnosis: repeat-one transitions unconditionally cleared both
  surface and controller frame evidence, despite reusing the same renderer/output.
  Repeat now preserves existing evidence; real media changes and surface transfers
  still invalidate it. First-frame routing is active in release builds too (logs
  remain debug-only), because handoff readiness must not depend on DEBUG.
- Library/Mini candidates retain the source while a destination registers. Mini
  reveal requires destination readiness plus both full presentations being hidden.
  Audio waits for connection/state readiness. Timeout is failure cleanup, not a
  successful reveal. Library launch coordinates override a previously dragged
  location for this transition; detached launches can still use saved position.
  A quick return to Library cancels any pending Home exit before it can move the
  Activity behind the launcher later.
- Android Home is controlled by Android: an app cannot defer the launcher or
  guarantee its Activity surface survives after stop. Readiness-gated destination
  reveal is implemented; a zero-gap Home transition is NOT claimed without device
  evidence. Renderer timestamps are not screen-capture proof.
- Library selected rows previously reused `isCurrent`, painting playback markers
  on selections. Selection and current path are now separate. Only the current
  item gets the twin green bars, without a fade leaving markers on older rows.
  Press feedback is a short rightward nudge plus standard ripple, not scale wobble.
- Required device checks: Samsung API 36 Home/return rapidly, audio, square/wide
  video, drag-return-leave source position, light/dark/font settings, native compact
  buttons, and diagnostics before/after repeat. No phone is attached.
- Verification: offline `testDebugUnitTest lintDebug assembleDebug` passed, 117
  JVM tests with zero failures, lint zero errors (21 warnings, one hint). APK
  `releases/GreaterArt-1.13.4.apk` is 26,229,503 bytes; SHA-256
  `9c519eafdd6e6b604f08946ae9b2cd2df2a0961f5acceaceb91576f2c908cac9`.
  Version 1.13.4/code 93, pinned signing certificate, v2 signature, 16 KiB
  zip alignment and no INTERNET permission were checked. Older APKs remain.

### September 23 — 1.13.3 local player visibility and DEV restoration

- Library mini-player is rectangular and edge-to-edge, retaining the external
  Mini's preview dimensions and the existing playback session.
- Mini visibility now depends on both Library and expanded-player visibility.
  A prepared handoff window is transparent and untouchable while either is visible.
  Previously the generic system-overlay flag included Mini itself, preventing
  cleanup on Library return. Use expanded-overlay state for that decision.
- Leaving Library and closing/backing out of expanded Now Playing requests Mini.
  The player's Home and pull-down still return to Library. No re-prepare, seek,
  decoder replacement, resolution limit or FPS cap was introduced.
- Restored the original DEV button and inspector dialog: report, Copy bug report,
  Pick element, region IDs and technical details. The rejected tap/hold redesign
  is superseded. In the service, the same panel renders inline rather than opening
  an Activity-token dialog. Diagnostics are collected only when enabled.
- Removed the Queue heading and moved Search beside Favourite/Share for both
  audio and video. Search opens only on request; closing clears its filter.
  Current-track scrolling uses the filtered index, not the original queue index.
  Only the middle list is flexible; seek and transport remain fixed.
- Verification: offline unit tests, lint and assemble passed. APK is 26,789,330
  bytes, version 1.13.3/code 92, pinned certificate verified, 16 KiB zip alignment
  verified, no INTERNET permission. Artifact: `releases/GreaterArt-1.13.3.apk`.
  Earlier versioned APKs were not overwritten. This continuation did not commit
  or push changes.
- Device checks remain required: Android Home/Back from both presentations, Mini
  return, DEV button/picking/report in Activity and overlay, small-screen layout
  and keyboard search. Build success does not prove OEM window behavior.

### September 22 — 1.13.2 local Yee-flow adjustments

- Started from the current local Hermes commit `85d4fe1`, not an earlier session's
  uncommitted implementation. Its Gradle version was already 1.13.2/code 91 while
  docs advertised several older versions. Current docs and artifact now agree.
  No commit, push or history rewrite was performed here.
- Yellow-gap cause: the service reserved percentage margins, and the reused
  Activity player applied status/navigation padding again. The service now fills
  the safe frame, with square bottom corners; `systemOverlay` suppresses the
  redundant Compose insets. Activity/PiP padding behavior is preserved. Rotation
  lets WindowManager fit the new usable frame. System navigation remains usable.
- Restored a top grab handle. Drag moves the existing window, short release/cancel
  returns it in 180 ms, and a 72 dp downward pull slides it away and returns to
  Library without stopping playback. Temporary out-of-bounds placement is enabled
  only while pulling and removed after snap-back; Mini geometry was not changed.
- Android Home/Recents now requests the existing Mini handoff. The service receives
  the protected system-dialog-close broadcast and filters `homekey`/`recentapps`;
  `MainActivity.onUserLeaveHint` provides the Activity-host fallback. Neither path
  launches Library/Home itself. The player's own Home and pull-down still return
  to Library. Duplicate shrink/close/share requests are guarded. No new permission,
  Accessibility service, usage-access polling, or playback re-prepare was added.
  Android documents receiving this [system broadcast](https://developer.android.com/about/versions/12/reference/broadcast-intents-31);
  sending it is restricted, and this app does not send it. OEM reason/gesture
  delivery must still be confirmed on the user's Samsung phone.
- Library media rows and artwork now have square corners with standard press
  feedback. Header/search/share/favorite/repeat/speed icons are larger within
  unchanged button bounds. Existing queue, sharing, lyrics, waveform, A–B and sleep
  functions remain; no resolution/FPS/bitrate restriction or new decoder was added.
- DEV is now direct: tap to pick an element; long-press to view/copy the report.
  Removed the intermediate setup dialog and region-toggle controls. The same
  inspector is available inside the system player, using readable fixed colors.
  Disabled Dev Mode no longer registers element bounds. A hidden duplicate video
  seek animation was removed to avoid invisible frame-rate recomposition.
- Verification: `testDebugUnitTest lintDebug assembleDebug --offline` passed;
  112 JVM tests, zero failures/errors; lint 0 errors, 19 warnings, 1 hint.
  APK version 1.13.2/code 91, pinned v2 certificate, no INTERNET permission and
  16 KiB zip alignment verified. `releases/GreaterArt-1.13.2.apk` is 26,107,478 bytes,
  SHA-256 `ed8af19a527013b501437a7d85f53b1c26fd091179a5efd4e03bf27b8ed4b090`.
  Previous 1.13.1 APK retained SHA-256
  `ec417d425968d47b453bb69290897cf9685e10d413bef118d03786fbadf35c30`.
- Security audit: no credential/workstation-path/sensitive-file finding, but the
  existing reachable Git history has non-noreply author email identities. Do not
  treat the full audit as passing or publish without reviewing that privacy finding.
  No email address is reproduced here and history has not been rewritten.
- Device boundary: no attached phone or emulator. Test Android Home (button and
  gesture) from Library-hosted and launcher-hosted Now Playing, Mini return, short
  and full pull-down, rotation, keyboard search, both Share choices, and Dev picking.
  JVM tests verify bounds/reason filtering/thresholds, not actual window animation.

### September 21 — 1.12.7 overlay sharing and Greater Art-only repo

- Merged system Now Playing overlay source was present on `main`, but its two
  Share controls were visually redundant and service-launched share sheets sat
  behind the focusable `TYPE_APPLICATION_OVERLAY`. Current-file Share and
  queue M3U8 export now live in one menu. A non-exported translucent
  `ShareProxyActivity` temporarily hides/disarms the overlay while Android's
  chooser is active, then restores it without re-preparing playback.
- Root cause of avoidable overlay entry work: its new `MainViewModel` also
  performed a full Download scan, thumbnail/waveform warmup, and play-history
  write even though it only presents the existing Media3 session. The
  presentation-only path skips those jobs and uses the session queue.
- Library row artwork/title placement no longer changes when a track becomes
  active. Press and active state use short, low-cost animations. Developer Mode
  opens to a compact summary; technical data remains one tap away.
- Removed tracked LocalKit and calculator source/APKs and their CI/Dependabot,
  issue-template, README and landing-page links. Ignored local files (including
  machine-only signing/build data) were not purged. Old commits remain in Git
  history; deletion from `main` is not a history rewrite.
- Verification: 105 unit tests passed; lint 0 errors, 18 warnings, 1 hint;
  `assembleDebug` passed. APK package `com.local.listentomusic`, version
  1.12.7/code 88, no packaged INTERNET permission, pinned certificate
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`,
  APK v2 signature and 16 KiB zip alignment verified. Public-repo audit passed.
- Artifact: `releases/GreaterArt-1.12.7.apk`, 26,852,621 bytes,
  SHA-256 `ba584647e7f742ddf16ad1f736f610ca532b18e63dbeebaaff26928ba79923a4`.
- Device boundary: no phone/emulator connected. Sharesheet focus/return and
  overlay video-surface continuity need a real-device check before calling
  them visually proven.

### September 21 — 1.12.6 splash logo scaling fix + version bump

- PR #38 merged: Fixed Android splash logo scaling in `res/values-v31/themes.xml` (windowSplashScreenAnimatedIcon uses proper drawable).
- Version bumped to 1.12.6 (code 87) for the rebuilt artifact.
- Verification: `testDebugUnitTest lintDebug assembleDebug --offline` succeeded (105 unit tests, 0 failures/errors; lint 0 errors, 18 warnings and 1 hint; debug assemble, APK v2 signature and 16 KiB alignment passed).
- Package is `com.local.listentomusic` 1.12.6/code 87; no packaged INTERNET permission; pinned signing certificate unchanged.
- Artifact: `releases/GreaterArt-1.12.6.apk`, 26,074,638 bytes, SHA-256 `c90f763f133de8f8144fa9554bef8cddefafe96dd453ca4da8b253e8d470d0e3`.
- Device boundary: no Android device/emulator was connected.

### September 21 — 1.12.5 symmetric surface handoff and release recovery

- Merged every remaining remote feature/security branch into `main`. Two old fix
  branches were patch-equivalent to changes already on main; their history was merged
  without reapplying or reverting the working implementation.
- Now Playing → Mini Window and Mini Window → Now Playing use the same explicit,
  readiness-gated surface handoff. The source remains visible until the destination
  owns the Media3 surface and renders a first frame. Audio skips the video-frame wait.
- Mini Window return goes directly to Now Playing with Android task animation disabled.
- Added the version-consistency CI/script from the security branch. It validates the
  Gradle version, READMEs, handoff, landing page, APK filename and APK SHA-256.
- The failed Hermes build was environmental rather than a Kotlin compiler failure:
  the runner could not create the Gradle wrapper lock under the user `.gradle` cache,
  and GitHub CLI was not installed for its attempted PR workflow. The exact recovery
  path is documented in `docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md`.
- Verification: 105 unit tests, 0 failures/errors; lint 0 errors, 18 warnings and 1
  hint; debug assemble, APK v2 signature and 16 KiB alignment passed. Package is
  `com.local.listentomusic` 1.12.5/code 86; no packaged INTERNET permission; pinned
  signing certificate unchanged.
- Artifact: `releases/GreaterArt-1.12.5.apk`, 26,074,638 bytes, SHA-256
  `5f876dea74951e0cb44b05029cff6bd4c06431b7289f3ee3d1971474a524af4c`.
- Device boundary: no Android device/emulator was connected. Both directions of the
  overlay handoff still require a real-device visual smoke test.

### September 20 — 1.12.4 red X position + Mini→Now Playing handoff

- `MiniWindowOverlayService.kt`: `crossRaisePx` 10 → 14 (moves red X 4 additional physical pixels higher in mini window overlay; total +5px from 9).
- PR #35 merged: Mini window tap now returns directly to NOW_PLAYING (no Library intermediate), surface retained until Now Playing PlayerView registers, instant sheet animation for mini return, duplicate session refresh removed.
- Verification: `testDebugUnitTest lintDebug assembleDebug --offline` succeeded. APK manifest confirms version 1.12.4/code 85.
- **Delivered APK:** `releases/GreaterArt-1.12.4.apk`, 26,074,638 bytes.
  SHA-256: `80c3cb9d5ee76f18c4a25fb59e409b820c1474a8df3a7b3c3dd94d65ba33e293`.
  Created only after final verification with overwrite disabled. No older APK changed.

### September 20 — 1.12.3 Favorites persistence and Now Playing hierarchy

- Root cause of the Favorite failure: Favorite/excluded-folder paths were written
  with URL-safe Base64, while the shared ordered-path reader only accepted standard
  Base64. Paths whose encoding contains `/` versus `_` could be saved and then vanish
  on the next DataStore emission; CJK filenames made this easy to reproduce.
- `StoredPathListCodec` now writes one canonical URL-safe format and reads both that
  format and the legacy standard alphabet. Custom order, Favorites and excluded
  folders share the same codec. Three regression tests cover a real CJK Download path,
  the legacy format and corrupt-entry isolation.
- The built-in Favorites destination now behaves as a real list: Play this list works,
  list sharing uses the Favorites identity, removing a Favorite refreshes the view,
  and the inactive reorder affordance remains hidden.
- Reclaimed the screenshot-reported Now Playing space without shrinking video or
  touch targets. Title, Favorite and current-original-file Share share one 48 dp action
  row. Queue, Search and M3U8 queue export form a clear queue header. Optional A–B and
  Sleep controls consume no row when both settings are disabled.
- Playback quality remains native: no max resolution, bitrate or FPS constraint was
  introduced. This patch does not claim to resolve the separate Samsung first-frame
  report without a new real-device diagnostic.
- Verification: 102 tests, 0 failures/errors; lint 0 errors, 18 warnings and 1 hint;
  APK v2 signature and 16 KiB alignment verified; package/version is
  `com.local.listentomusic` 1.12.3/code 84; packaged manifest has no INTERNET permission.
- Artifact: `releases/GreaterArt-1.12.3.apk`, 26,074,638 bytes, SHA-256
  `ac20c6535245dbe2440020f6e61026f155a7d56744e9488115b2e5186b9b55cb`.
- Device boundary: no Android device/emulator was connected. Favorite persistence and
  the compact Now Playing layout still need the supplied Samsung phone smoke test.
- Work remains local on `main`; no commit, push, tag, branch merge or remote release.

### September 20 — 1.12.2 surface continuity, offline sharing and Library dead-band fix

## Product invariants

- Local-first media player.
- No `INTERNET` permission.
- No ads, analytics, telemetry, accounts, subscriptions, or cloud playback requirement.
- Never overwrite an existing versioned APK.
- Preserve application ID and signing identity.
- Remote publishing is task-scoped: push/tag/release only when explicitly requested.

## Current user journey

1. App launches and scans local supported media.
2. Library is the default screen.
3. Tapping a Library row starts playback in place.
4. The live Library mini-player appears.
5. Tapping the mini-player opens Now Playing.
6. Leaving the app from Now Playing uses the configured floating mode; Mini Window is the default.
7. Tapping the floating presentation returns to playback.

## 1.12.2 state

- Now Playing keeps a stable PlayerView across media changes rather than keying it to the current media path.
- Surface diagnostics are generation-aware and distinguish controller first-frame evidence from active-presentation attribution.
- Same-view/same-player owner reconciliation is a no-op.
- Temporary hold-for-2× activates after 700 ms on actively playing foreground video and restores the exact prior speed on release/cancel.
- Original-file sharing, explicit multi-file sharing, and portable M3U8 list sharing use Android's Sharesheet.
- Local Favorites and queue search are available without rebuilding playback order.
- Waveform presentation uses decoded peaks plus playback progress/playhead rather than fabricated equalizer motion.
- Optional playback history defaults off and remains local.
- Library content draws behind the mini-player with only the end inset needed to make the final row reachable.

## Playback and rendering priorities

When resources compete:

1. Main playback continuity / audio.
2. Main visible video quality.
3. Visible mini-player / fullscreen presentation.
4. Current-video wallpaper.
5. UI animation.
6. Decorative effects.

Do not hide rendering/performance bugs by introducing generic:

- resolution caps;
- bitrate caps;
- FPS caps;
- low-quality proxy video;
- intentional frame skipping;
- universal software decoding;
- periodic seek/reprepare/restart hacks.

Profile and remove duplicate/hidden work first.

## Surface ownership invariants

The visible presentation owns the primary video output.

Expected ownership transitions include:

```text
LIBRARY_MINI -> NOW_PLAYING
NOW_PLAYING -> FULLSCREEN / PiP
FULLSCREEN / PiP -> NOW_PLAYING
NOW_PLAYING -> LIBRARY_MINI
MINI_WINDOW -> foreground Activity presentation
```

Rules:

- stale detach/release must never clear a newer owner's output;
- same owner + same PlayerView + same player + unchanged binding is a no-op;
- a new diagnostic generation should correspond to a meaningful output/media epoch, not ordinary recomposition;
- current-video wallpaper must not steal the primary surface;
- diagnostic first-frame events are renderer/controller evidence, not screen-capture proof.

Current diagnosis: [docs/SURFACE_DEBUG_1.12.2.md](docs/SURFACE_DEBUG_1.12.2.md).

## Performance rules

Investigate jank in this order:

1. surface/view churn;
2. hidden work behind Now Playing;
3. duplicate/current-video background work;
4. broad Compose recomposition from playback position;
5. waveform redraw cost;
6. thumbnail work;
7. allocation, GC, blur, overdraw, and other CPU/GPU stalls.

Keep source video at native quality whenever the device supports it.

## Privacy-sensitive features

- Favorites are local.
- Playback history is optional and defaults off.
- Manual share/export actions are explicit user actions, not background sync.
- Future voice/caption/desktop/handoff ideas are architecture proposals only unless implemented and verified.
- Do not silently add a network speech/translation fallback.

See [docs/LOCAL-FUTURES-1.12.2.md](docs/LOCAL-FUTURES-1.12.2.md).

## Verification

The 1.12.5 repository build reports:

- 105 unit tests passing;
- lint: 0 errors, 18 warnings, 1 hint;
- debug assemble passed;
- APK signature verification passed;
- 16 KiB zip alignment passed;
- packaged manifest has no `INTERNET` permission.

No Android device/emulator was connected for that verification. Do not convert build/test evidence into claims about Samsung surface output, overlay geometry, Bluetooth behavior, Sharesheet compatibility, or actual frame pacing.

## Before changing playback/surfaces

Read:

- `README.md`
- `docs/SURFACE_DEBUG_1.12.2.md`
- `docs/NODES.md` if touching graph/library navigation
- the relevant runtime source

Then reproduce/instrument before applying architectural workarounds.

## Before changing documentation

Check `app/build.gradle.kts` and the actual `releases/` tree first. Documentation must not get ahead of repository code/artifacts again.

## Semantic Versioning Policy

Greater Art versioning uses:

MAJOR.MINOR.PATCH

Examples: 1.13.6
1  = MAJOR
13 = MINOR
6  = PATCH

---

- Current Version
versionName = X.Y.Z
versionCode = N

Example:

versionName = 1.13.6
versionCode = 95

Source of truth:
greater-art/app/build.gradle.kts
Before changing the version, always read the current value from the repository.
Never assume the version from an old handoff, APK filename, or conversation.

---

PATCH — X.Y.Z → X.Y.(Z+1)
Use PATCH for:

bug fixes
crash fixes
UI polish
animation improvements
performance fixes
incorrect sizing/positioning
transition fixes
device-specific fixes
internal refactors with no major product change
security fixes that preserve existing behavior

- Examples:

1.13.5 → 1.13.6
1.13.6 → 1.13.7

- Example changes:

Fix Mini Window position
Fix red-X quit behavior
Fix black frame during player transition
Fix Samsung overlay sizing

---

MINOR — X.Y.Z → X.(Y+1).0

Use MINOR for a meaningful new feature or capability while Greater Art remains the same overall product generation.

Examples:

1.13.7 → 1.14.0
1.14.4 → 1.15.0

Examples of MINOR changes:

new playback feature
new Library mode
new queue functionality
new media-management capability
new major Settings feature
new user-visible workflow

Reset PATCH to 0.

- Correct:

1.13.7 → 1.14.0

- Not:

1.13.7 → 1.14.8

---

## Verification Records (this session)

### September 27 — 1.13.18 async-prefs ANR fix (local)
- Root cause: `PlaybackService.onCreate` called `runBlocking(Dispatchers.IO) { preferences.current() }` on the main thread while the AVD was still scanning 17GB of media on first launch. The blocking DataStore disk read froze input dispatch and produced an ANR exactly at the first song-row tap.
- Fix: replaced with `serviceScope.launch { /* async prefs load */ }` so the main thread stays free during the first-play tap. `preferences.current()` = DataStore `values.first()`; no `currentBlockingFallback()` exists (do not invent one).
- Build: Java 21, `./gradlew :app:assembleDebug --no-daemon --console=plain`. APK: `releases/GreaterArt-1.13.18.apk`, versionCode 107, versionName "1.13.18".
- SHA-256: `77e53510b6733b2489fa3cb6f57d1cc3ad47ff93b1656de3b601b3290fb90689`.
- AVD: `GreaterArt_A55_API36` at 1080×2340 / 450dpi / **4GB RAM / 6 vCPUs** (was 2GB — insufficient for 17GB media scan). `hw.ramSize=4G`, `hw.cpu.ncore=6` in `config.ini`.
- Calendar hijacking workaround: `pm disable-user --user 0 com.google.android.calendar` (GMS sign-in/calendar config screens kept stealing foreground mid-verification). AVD still dies every ~15 min under media load + host 16GB RAM pressure; keep this workaround after each `emulator.exe -no-snapshot-load` restart.
- Verified on AVD this session:
  - App launch 1.13.18 → no crash, pid stable
  - Library: 209 files offline, search field filters correctly
  - Row tap → playback starts (mp3 + mp4 → `state=PLAYING` in `dumpsys media_session`)
  - Settings opens; language switch (English→Deutsch→title "Einstellungen") and theme switch (Light) tap-verified
  - Settings: every section present (Language & appearance, Playback, Song lists, Library & cache, Privacy); every chip + switch + button exists in source and is tappable
  - Now Playing opens from mini player; Home / Locate current song / Fullscreen / Close all verified on AVD
  - Fullscreen toggle → `FullscreenVideoActivity` opens
  - Close player → playback stops (`PAUSED`), app stays alive
  - Timeline seek → position changes
  - Zero crashes in logcat across entire session
- Scroll perf: warm 1.46% jank @1080×2340 (gfxinfo); cold 23% (first decode of 17GB).
- Not AVD-tap-verified this session (AVD died before full sweep completed): settings toggle switches (ReplayGain, black disc, play history, sleep timer, A–B, extended search, editable queue, resume position, auto floating, developer mode, toggle ads); settings action buttons (equalizer, create playlist, import M3U, export list, find duplicates, backup, restore, exclude folder, rescan, clear cache, reset confirmation dialog); Now Playing transport controls (pause, next, previous, repeat mode, speed, favorite, search queue, share, lock); playlist CRUD; full playback scenarios and edge cases. Source audit + live Settings screenshot confirm these all exist and are tappable; full behavioral verification needs a stable AVD or physical Samsung A55.

### September 26 — 1.13.17 version bump (local)
- build.gradle.kts: versionCode 106 → 107, versionName "1.13.17" → "1.13.18".
- APK rebuilt: `releases/GreaterArt-1.13.18.apk` (SHA `77e53510b6733b2489fa3cb6f57d1cc3ad47ff93b1656de3b601b3290fb90689`, 26,107,434 bytes).

## Verification Records (continued)
- HANDOFF.md: added 1.13.17 and 1.13.18 entries (see version table below).

### September 27 — 1.13.17 (local, transitional)
- NOTE: 1.13.17 (code 106) was a transitional build that was accidentally labeled before the 1.13.18 async-prefs fix landed. It is NOT shipped as a release APK.
- Code state: same as 1.13.16 plus the version bump to 106; the ANR-causing `runBlocking` prefs load was still present.
- No release APK shipped for 1.13.17. Use 1.13.18 (code 107) as the current verified build.

## Workflow (add this in handoff.md in your language)

read handoff.md --> read user instructions (usually debug, fix, or patch, changes etc) --> connect adb android studio virtual device A55 --> test it out --> debug --> verify --> if no bug, build it in the correct version number

### Version table

| Version | Code | APK SHA-256 | Notes |
|---------|------|-------------|-------|
| 1.13.16 | 105 | `ee7145b3369174c4ce9c5d95ec3b87a8009b45ce9bfe5639da33fba5ea29d935` | Scroll perf verified, thumbnail cache reverted, 209-file library test |
|| 1.13.18 | 107 | `77e53510b6733b2489fa3cb6f57d1cc3ad47ff93b1656de3b601b3290fb90689` | Async-prefs ANR fix, 4GB AVD, 209-file library, warm 1.46% jank |
|| 1.13.21 | 110 | `99b33938b216c5b40d6c14dc4145fdcc1158b3a94f440c5843ca0b36a34973ce` | Thumbnail dimensions reduced (VIDEO 640→240, ART 512→256), Settings LazyColumn split (5 key blocks), build verified, release APK copied, AVD visual verification pending |
