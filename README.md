# Greater Art

Greater Art is a local-first Android audio and video player. This repository contains the app, its documentation, release APKs, and the checks used to build and audit it: `Galahad369/Greater-Art-Music-Player`.

> **Vibe-coded disclosure:** Greater Art was built through iterative work with AI coding agents. Human direction, product decisions, device feedback, and acceptance guide the work; substantial code and documentation are AI-assisted.

- Verified build: **1.15.77 (code 207)** — preserves the reviewed Stack alignment/readiness work and adds guarded decoder-pressure recovery so stale recovery jobs cannot interfere with a replacement Stack. The recorded API 36 verification covers build/lint/tests, package/version/signature checks, playback and Stack transitions. Emulator state is not acoustic synchronization proof; see the handoff.
- APK: [GreaterArt-1.15.77.apk](greater-art/releases/GreaterArt-1.15.77.apk)
- [App documentation](greater-art/README.md) · [Handoff and verification](greater-art/HANDOFF.md)
- [User demonstration](greater-art-user-demo.html) · [Technical demonstration](greater-art-technical-demo.html)

Current source: **1.21.20 / code 243 — SOURCE_ONLY**, instrument-first Stack alignment
with distributed timing anchors, confidence-gated linear drift maps and bounded
original-rate transient refinement. Library Performance v2
stages 1–2. Shared viewport-priority artwork work pauses during fling; RAM hits stay
immediate, allocation budgets respond to playback/Stack pressure, and verified
identical covers reuse assets. No playback-quality constraint or network permission.
See the [staged Stack/performance plan](greater-art/docs/PERFORMANCE_AND_STACK_UPGRADE_PLAN.md).
Timing maps are implemented in source, not a verified release. The shared-clock
mixer is a separate prototype stage, not the production audio path.

Greater Art has no Internet permission, advertisements, accounts, analytics, telemetry, or cloud playback dependency. Library files stay on the device. Playback history is optional, off by default, and can be burned locally.

Stack plays up to eight local tracks together. Choose **Now-playing video → Fit** in Background settings to tile Stack videos behind Library, Nodes and Stack. **Cut to screen size (Crop)** remains the default; Stretch is removed. Optional video previews need device decoder/GPU capacity and never reduce source quality. The single primary video surface serves the wallpaper while docked artwork remains visible; expanding Now Playing transfers that surface directly.

## Navigation structure (ASCII)

Greater Art uses a **three-page, swipeable navigator** directly below its shared header (not a conventional bottom navigation bar). The initial page is **All songs**.

### App hierarchy

```text
GREATER ART
|
+-- MAIN PAGES (horizontal swipe / navigation icons)
|   |
|   +-- STACK
|   |   +-- Choose up to 8 local tracks
|   |   +-- Individual volume / mute / solo / offsets
|   |   +-- Align sound
|   |   +-- Saved Stacks / offline recommendations
|   |   +-- Shared seek / play / pause / loop / stop
|   |
|   +-- ALL SONGS [default]
|   |   +-- Search / sort local media
|   |   +-- Playlist selector
|   |   |   +-- All songs
|   |   |   +-- Favorites
|   |   |   +-- Created playlists
|   |   +-- Multi-select and track action menus
|   |   +-- Play a song or video
|   |
|   +-- NODES
|       +-- Interactive filename-similarity graph
|       +-- Find / fit / locate playing track
|       +-- Graph controls / tap node to play
|
+-- SETTINGS (top-right header icon)
|   +-- Language & appearance
|   +-- Background
|   +-- Playback
|   +-- Library & lists
|   +-- Privacy & data
|   +-- Developer
|
+-- PLAYER (persistent dock / floating overlay)
    +-- Docked mini player
    +-- Expanded Now Playing
    |   +-- Artwork or video
    |   +-- Timeline and playback controls
    |   +-- Queue or Stack track list
    |   +-- Queue search / synced lyrics (when available)
    |   +-- Video fullscreen
    +-- Detached floating mini player
```

### Main page switching

```text
      swipe right <----------> swipe left
+----------------+----------------+----------------+
|     STACK      |   ALL SONGS    |     NODES      |
|                |   (default)    |                |
+----------------+----------------+----------------+
       page 0           page 1           page 2

Tap a navigation icon or swipe horizontally to switch pages.
The page indicator tracks the swipe; Back returns to All songs.
```

### Playback navigation

```text
ALL SONGS / STACK / NODES
           |
           | Play a track (or a graph node)
           v
     AUDIO / VIDEO PLAYBACK
           |
           v
       DOCKED MINI
           |
           | Tap player
           v
      NOW PLAYING <---------------------+
           |                            |
           +-- Queue / Stack track list |
           +-- Search / lyrics          |
           +-- Transport / seek         |
           |                            |
           +-- Video fullscreen --------+
           |     (exit fullscreen)
           |
           +-- Home / return to Library -> MAIN PAGES
           |
           +-- Close / collapse --------> DETACHED MINI
                                              |
                                              | Expand
                                              +----> NOW PLAYING
```

### Settings navigation

```text
MAIN PAGES
    |
    +-- Settings icon
           |
           +-- Language & appearance
           |     +-- Language / theme / palette / font
           +-- Background
           |     +-- Mode / image or video / fit / dim
           +-- Playback
           |     +-- Speed / repeat / sleep timer
           |     +-- Floating player preferences
           +-- Library & lists
           |     +-- Playlists / M3U import-export
           |     +-- Duplicate tools
           +-- Privacy & data
           |     +-- Backup / restore / local data controls
           +-- Developer
           |
           +-- Back -> Main pages (previous pager position)
```

### Back and menu behavior

```text
STACK ------------------ Back --> ALL SONGS
NODES ------------------ Back --> ALL SONGS
SETTINGS --------------- Back --> MAIN PAGES
VIDEO FULLSCREEN ------- Back --> NOW PLAYING
NOW PLAYING ------------ Close -> DETACHED MINI
NOW PLAYING ------------ Home --> LIBRARY
PLAYLIST / SORT MENU --- Select or dismiss -> CURRENT PAGE
```

Navigation patterns: **horizontal paging + icon tabs**, contextual dropdowns/dialogs, a separate Settings page, and a persistent expandable player. There is **no primary drawer menu**. Some workflows (such as playlist deletion and permission granting) use confirmations or external Android settings rather than a multi-step in-app wizard.

Implementation: [GreaterArtApp.kt](greater-art/app/src/main/java/com/local/listentomusic/ui/GreaterArtApp.kt), [LibraryPageChrome.kt](greater-art/app/src/main/java/com/local/listentomusic/ui/LibraryPageChrome.kt), and [MiniWindowOverlayService.kt](greater-art/app/src/main/java/com/local/listentomusic/playback/MiniWindowOverlayService.kt).

## Build and verify

See [greater-art/README.md](greater-art/README.md) for the Java 21/Android SDK build command. The repository's [version check](scripts/validate-greater-art-version.py) and [public-repo security audit](scripts/audit-public-repo.ps1) help keep the APK, docs, and public files aligned. Public APKs are experimental sideload builds; review source and permissions before installation.

The former LocalKit and calculator projects have been removed from this repository's current tree. Their old commits remain in Git history; any private or local-only files were not included in this cleanup.

Security reports: [SECURITY.md](SECURITY.md).
