# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video. It scans supported media under Android's `Download` folder and is intentionally local-first: **no Internet permission, ads, accounts, analytics, telemetry, or cloud playback dependency**.

## Current release

- Verified release: **1.15.43**, code **173** (see HANDOFF.md for artifact hash).
- Current source: **1.15.50**, code **180** — verification pending; not a released APK.
- Application ID: `com.local.listentomusic`
- APK: `releases/GreaterArt-1.15.43.apk`
- APK SHA-256: `77e37b747f4122f289c936bc1f06e156575b102398fbc24f7ea4daab4b96cb61`
- Signing certificate SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`
- v1.13.20 keeps detached Mini hidden over Settings and other in-app pages while preserving Library's dock and Android Home's detached presentation.
- v1.13.19 guards delayed session restore from replacing a newly selected song, moves final playback saving off the main thread, fits detached Mini to the media aspect without an opaque edge, and keeps theme updates from restoring its background. Metal highlights are now static and the audio cover no longer pulses continuously; long Now Playing titles make one restrained reveal. No media quality limit or new permission was added.
- v1.13.10 closes Now Playing directly to detached Mini, adds a reversible control lock and scrolling long-title marquee, keeps single video taps inert, replaces the bulky jump cue with a compact three-chevron animation, and raises the center play/pause control slightly. The 1.13.9 mini-window stroke removal remains in place. No new file-copy cache or media quality cap was added.
- v1.13.8 gives video fullscreen a landscape Activity so Android can rotate the whole frame; the normal expanded player remains a floating overlay. It restores expanded safe-area insets after the dock/mini state, lets taps reach video controls above the native surface, and disables bulk thumbnail preloading to avoid competing with playback. The A55-sized API 36 emulator showed a fitted portrait player and an uncropped landscape video; real Samsung A55 transition testing is still required.
- v1.13.7 fixes the 1.13.6 Library song-tap crash in the unified floating player and checks floating-window permission at launch and in Dev Mode. An emulator smoke test covered song selection and expanded Now Playing; Samsung phone verification remains important.
- v1.13.6 uses one persistent WindowManager player for Library-docked,
  detached Mini, and expanded Now Playing. The dock keeps a single play/pause
  control and a media preview sized exactly like detached Mini. Expanded mode
  reuses the same PlayerView; fullscreen changes the overlay window itself.
  Subtle title/control shadows clarify boundaries, and the red quit target is
  four physical pixels higher. Phone-level transition checks are still needed.
- v1.13.5 docks the actual persistent Mini Window overlay at Library's bottom,
  then resizes that same window and PlayerView into a draggable media-only player
  on Home. The separate Compose mini-player is gone. Dropping Mini on the red X
  stops playback and removes the existing task without launching Library; the X
  sits four physical pixels higher. No media-quality cap was added. Device
  transition checks remain pending because no Android device was connected.
- v1.13.4 uses one compact-player view and shared playback connection inside
  Library and outside the app. Both have a 61.5 dp content height with title,
  preview, progress and controls. Only the current Library track has green bars;
  taps use a brief directional nudge. Repeat-one no longer erases valid frame
  evidence. Mini reveal is readiness-gated; real-device Home transitions still
  require verification. No source-quality reduction was introduced.
- v1.13.3 makes the Library mini-player edge-to-edge and rectangular, using the
  same preview dimensions as Mini Window. Detached Mini is hidden and untouchable
  whenever Library or expanded Now Playing is visible. Leaving Library or closing
  expanded Now Playing hands playback to Mini; the player's Home/pull-down returns
  to Library instead. The original DEV button, report, copy, inspection and technical
  controls are restored, including diagnostics inside the expanded player.
  Search sits beside Favourite and Share; the redundant Queue heading is removed,
  leaving more room for the scrollable list without moving the fixed controls.
- v1.13.2 fills the usable screen with Now Playing: no side/bottom percentage
  margins and no duplicate system-bar padding. The top grab handle follows a
  downward drag; a short pull springs back, a completed pull returns to Library
  without stopping playback. Android Home/Recents requests Mini Window instead.
  Library media rows and thumbnails are rectangular. Action icons are larger
  inside their unchanged buttons. DEV now selects elements immediately on tap;
  hold DEV to view/copy the local report. No video-quality limit was added.
- v1.12.7 introduced Now Playing in a focusable system overlay. Its single Share button
  offers the current original media file or an M3U8 export of the queue; a
  transient internal Activity lets Android's share sheet receive input above the
  overlay. The queue header now keeps Search without a duplicate Share control.
  Library rows reserve a stable artwork/title alignment slot and respond subtly
  to press and active-track changes. Developer Mode shows a short status first,
  with the full report behind Technical details. The overlay's presentation-only
  ViewModel no longer triggers a second library scan, thumbnail/waveform warmup,
  or duplicate play-history recording.
- v1.12.6 moves Now Playing into a system overlay rather than an in-Activity
  sheet. Mini Window can expand directly back to that overlay.
- v1.12.5 makes the Now Playing ↔ Mini Window surface handoff symmetric and
  readiness-gated. The working source surface stays alive until the destination owns
  the player and renders its first frame; audio transitions do not wait on a video
  frame. Mini Window returns directly to Now Playing without a task animation.
- The release also adds a repository version-consistency gate so the Gradle version,
  READMEs, handoff, landing-page link, release APK and APK SHA-256 cannot silently
  drift apart again. See the
  [ChatGPT → Hermes build playbook](docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md).
- v1.12.3 fixes Favorites for filenames whose persisted path uses URL-safe Base64,
  including affected CJK paths. The reader accepts both legacy standard Base64 and
  the current URL-safe format. Favorites can now be played and shared as a real list.
  Now Playing also places the title, Favorite and current-file Share actions in one
  compact row, with Queue/Search/queue export grouped directly above the queue.
- v1.12.2 keeps the playback journey library-first: tapping a Library row starts playback in
  place, while tapping the live Library mini-player deliberately opens Now Playing.
  The list now draws behind that player instead of reserving a permanent blank band;
  only its scroll-end inset keeps the final row reachable.
- Hold an actively playing video for 700 ms for temporary 2× playback. Releasing
  restores the exact prior speed without changing the saved preference. Queue search,
  local Favorites, original-file sharing, multi-file sharing and portable M3U8 list
  sharing are available without adding network access.
- Video surface ownership now keeps the same PlayerView through media transitions and
  cross-checks Media3's current-media first-frame signal. This removes the unnecessary
  surface recreation behind misleading `READY_VIDEO_NO_FRAME` warnings; see
  [the 1.12.2 surface note](docs/SURFACE_DEBUG_1.12.2.md).
- The mini-window preserves the established visible `103×56dp` widescreen footprint,
  switches to `56×56dp` for square art/video, reaches screen edges without a hidden
  gutter, shows embedded audio artwork, and applies a strong transparent red tint
  only while it overlaps the real close target. The target is another three physical
  pixels higher than 1.11.7.
- Library search now uses a focused liquid-metal treatment and a clear action; the
  top bar uses denser controls. Initial loading uses the real app mark. Now Playing
  keeps side-only double-tap seeking with one restrained YouTube-style cue, adds an
  optional pause-aware black-disc presentation for audio, and leaves the center
  double-tap zone inert.
- Optional play history is disabled by default. When enabled, successful playback
  transitions are stored locally (up to 500 entries), never included in settings
  backups, and can be permanently burned from the Library. Resetting app settings
  also clears it. Developer inspection can cycle through overlapping tagged elements.
- Two standalone phone films document the exact journey: the
  [user film](../greater-art-user-demo.html) and the
  [technical journey](../greater-art-technical-demo.html).

Versioned APKs are never overwritten.

## Core experience

### Library and playback

- Recursive local media discovery under `Download`.
- Media3 playback through a `MediaLibraryService` with background/system media controls.
- Library search, sorting, playlists, custom ordering, M3U/M3U8 import/export, and local metadata overrides.
- Tapping a Library row starts playback in place; tapping the live Library mini-player opens Now Playing.
- Repeat Off / One / All / Random, playback speed, seek, next/previous, optional A–B practice, and optional sleep controls.
- Local Favorites and queue-only search without rebuilding playback order.
- Optional local play history is off by default and remains device-local.
- Bluetooth output removal pauses playback before audio can spill to the phone speaker.

### Video and floating playback

- Full local video playback, rotation, fullscreen, and Android picture-in-picture.
- Hold an actively playing foreground video for 700 ms for temporary 2× playback; release/cancel restores the exact prior speed.
- Three floating modes:
  - `COMPACT` — Android-controlled compact PiP.
  - `FOLLOW_VIDEO` — PiP following video aspect ratio.
  - `MINI_WINDOW` — tiny draggable system overlay.
- Shared video-surface ownership coordinates Library, Now Playing, fullscreen/PiP, and the mini-window without intentionally lowering source resolution or frame rate.

### Local text, waveform, and library tools

- Matching local LRC plus embedded lyric fallback.
- CUE virtual tracks and local SRT/TTML support where applicable.
- Cached decoded waveform peaks with playback coloring and a precise playhead; the waveform does not fabricate equalizer motion.
- Filename-similarity **Nodes** graph with pan, pinch, drag, find, and tap-to-play.
- Natural sorting, multi-selection, rule playlists, duplicate tools, and local title/cover overrides.

### Sharing

- Share an original media file through Android's system Sharesheet.
- Explicit multi-file sharing without loading the files into RAM.
- Portable M3U8 sharing for the current Library, playlist, or queue.
- Temporary share access is URI-based; raw private filesystem paths are not the sharing API.

### Appearance

- Dark/light liquid-metal backgrounds.
- User-selected image or muted looping video background.
- Current-video background mode.
- Multiple text styles and bundled open-licensed fonts.
- English, Traditional Chinese, Japanese, German, French, and Cantonese interface options.

### Developer mode

Developer Mode is optional and local. It exposes playback state, cache/waveform facts, surface ownership/generation diagnostics, and tagged UI regions for troubleshooting. Diagnostics are evidence about application events; they are not a substitute for observing pixels on a real device.

## Privacy

The packaged manifest intentionally contains **no `INTERNET` permission** and removes Media3's transitive network-state permission.

Media files, preferences, playlists, Favorites, optional history, thumbnails, waveform cache, and selected backgrounds remain on-device unless the user explicitly invokes an Android share/export action.

This personal sideload build uses `MANAGE_EXTERNAL_STORAGE` to scan media under `Download`. A Play Store distribution would need a different storage strategy such as a persisted Storage Access Framework folder grant.

## Build

Requirements:

- JDK 21
- Android SDK / API 37

From `greater-art/`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug
```

Current release artifact:

```text
releases/GreaterArt-1.13.10.apk
```

Versioned APKs are never overwritten. Builds remain signed by the pinned personal
debug keystore so a newer APK can update the existing installation.

The repository does not contain the personal release keystore.

## Architecture

```text
app/src/main/java/com/local/listentomusic/
├── MainActivity.kt
├── MainViewModel.kt
├── data/
│   ├── AppPreferences.kt
│   ├── MediaScanner.kt
│   ├── MetadataIndex.kt
│   ├── ThumbnailRepository.kt
│   └── WaveformRepository.kt
├── graph/
│   ├── FilenameSimilarity.kt
│   ├── GraphBuilder.kt
│   └── GraphLayout.kt
├── model/
├── playback/
│   ├── PlaybackService.kt
│   ├── ParallelPlayback.kt
│   ├── MiniWindowOverlayService.kt
│   └── NowPlayingOverlayService.kt
└── ui/
    ├── GreaterArtApp.kt
    ├── LibraryScreen.kt
    ├── NowPlayingScreen.kt
    ├── ShareProxyActivity.kt
    ├── SettingsScreen.kt
    └── components/
```

## Important engineering invariants

- Playback/audio and the visible primary video have priority over decorative effects.
- Do not cap source video resolution, bitrate, or frame rate to hide rendering problems.
- A stale video view must never clear a newer owner's active output.
- Same-owner/same-view/same-player reconciliation must remain a no-op.
- Hidden/completely covered UI should not continue expensive decorative rendering.
- Do not rebuild playback queues merely because search/filter presentation changes.
- Keep the application ID and signing certificate stable for install/update continuity.
- No network, account, analytics, or telemetry dependency should be introduced casually.

## Verification boundary

For 1.12.7 (code 88): 105 unit tests passed; lint reports 0 errors, 18 warnings and 1 hint;
the debug build, APK v2 signature and 16 KiB alignment checks passed. The application
ID and pinned signing certificate are unchanged, and the packaged manifest has no
INTERNET permission. No Android device was connected for this review build.

APK: `releases/GreaterArt-1.12.7.apk` (26,852,621 bytes)

SHA-256: `ba584647e7f742ddf16ad1f736f610ca532b18e63dbeebaaff26928ba79923a4`.

Real-device checks remain required for visible surface output, overlay geometry, Sharesheet receivers, Bluetooth behavior, hold-to-2× cancellation, and device-specific performance.

## Current technical notes

- [HANDOFF.md](HANDOFF.md) — concise continuation state and non-negotiable invariants.
- [docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md](docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md) — exact recovery and release procedure when ChatGPT/GitHub work must be pulled and built by Hermes.
- [docs/NODES.md](docs/NODES.md) — filename graph behavior and limits.
- [docs/SURFACE_DEBUG_1.12.2.md](docs/SURFACE_DEBUG_1.12.2.md) — current surface-warning diagnosis.
- [docs/LOCAL-FUTURES-1.12.2.md](docs/LOCAL-FUTURES-1.12.2.md) — architecture-only future possibilities.

Public demonstrations:

- [User demonstration](../greater-art-user-demo.html)
- [Technical demonstration](../greater-art-technical-demo.html)

Historical drafts and superseded release briefs are intentionally left to Git history rather than kept in the active documentation tree.
