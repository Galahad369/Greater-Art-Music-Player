# Greater Art Desktop — native Windows edition

Current review source: **0.1.8/code9**, SOURCE_ONLY. Latest verified development installer: **0.1.4/code5**; its immutable links below remain valid. Original C++/Win32 application: activity rail, resizable Stack / Library / Nodes panes, collapsed Settings, fixed dock. Expanded Now Playing and detached Mini transfer the same primary renderer without reloading media.

## Install and run

[Artifact hashes and installed test receipt](RELEASE_0.1.4.md) · [Android-to-Windows source/control inventory](docs/ANDROID_WINDOWS_PARITY.md).

Use **GreaterArt-Desktop-Setup-0.1.4.exe**. Double-click, leave **Create desktop shortcut** selected, then Install. The per-user setup also creates a Start menu entry. Installation and playback are offline. Add a local folder to populate Library.

Installed app: `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`.
Settings/cache: `%LOCALAPPDATA%/GreaterArtDesktopNative/`.
Uninstall through Windows Installed apps; original media and preferences are retained.

The portable edition is a **ZIP**, not an isolated EXE: extract the whole folder and run GreaterArt.exe beside its six codec DLLs. The previous 0.1.1 single-EXE build stays preserved; do not replace or relabel it.

## Original, offline playback

No VLC, external player, Java, Electron, browser shell, accounts, telemetry, updater or runtime download. Native Windows Media Foundation remains the primary video renderer. Approved FFmpeg 9.0.2 libraries are built from pinned source, dynamically linked in-process for audio decoding/analysis and WASAPI output. Compatible modified libraries are replaceable. Exact corresponding source and LGPL notices accompany the package; see [codec boundary](native/CODECS.md).

No source resolution, bitrate or FPS constraints, proxies, thumbnail-video substitution or transcoding. Unsupported native video remains an explicit limitation; bundled video rendering is not yet wired. Windows N still needs its system media components. Selected local folders only; UNC/network roots, URL playback, alternate streams and reparse traversal are rejected.

Build-time dependencies are not installed app dependencies: Visual Studio C++/Windows SDK, source-build vcpkg and WiX 3.

## Working scope

- Rectangular Library rows, current-only playing marker/tap nudge, search/reset, favorites, chosen folders, Unicode, sort; local M3U8 import/export and separate playlists.
- Up to eight simultaneous sources, per-voice gain/mute/solo, master volume, coordinated start/seek/loop, drift trim, confidence-gated offline sound alignment and saved offsets. This is structural alignment, **not sample-perfect mixing**.
- Filename-similarity Nodes graph, sparse weighted edges, natural clusters, pan/zoom/drag/tap, wand reveal, neighbor/link threshold controls and versioned cache.
- Fixed seek/transport, first-class speed, lock, whole-frame monitor fullscreen, borderless Mini, direct Mini↔expanded transfer.
- Selected-media metadata/artwork, bounded waveform disk cache, local synchronized LRC, A–B repeat, sleep timer, resume, opt-in history/burn, optional spinning audio disc.
- Artwork-color Ambient and aspect-preserving custom image Crop(default)/Fit; no Stretch. Continuous video-derived Ambient and tiled Stack video wallpaper remain pending.
- One native Share button offers current original file, queue M3U8 or queue files. Windows Share/media keys are implemented; full target-application acceptance remains pending.
- Native accessible button names/roles/bounds and keyboard transport/slider navigation; high contrast, fonts, partial six-language UI. Ctrl+click inspects a drawn control when Developer mode is enabled.

## Build and verify

```powershell
./desktop/native/build-codecs.ps1 -VcpkgExe 'C:/path/to/vcpkg.exe'
./desktop/native/build.ps1 -RequireCodecs
./desktop/native/package.ps1 -WixPath 'C:/path/to/wix3'
python desktop/packaging/validate_version.py
python desktop/packaging/test_version.py
./desktop/native/build/GreaterArt.exe --self-test 'C:/owned/test-output'
./desktop/native/build/GreaterArt.exe --verify-audio 'C:/owned/audio-fixture.wav' 'C:/owned/audio-output'
./desktop/native/build/GreaterArt.exe --verify 'C:/owned/video-fixture.mp4' 'C:/owned/probe-output'
```

Tests isolate all generated settings. The video probe captures **only owned HWNDs**, checks real rendering, six voices, fullscreen bounds and renderer handoffs. The audio probe checks actual WASAPI timeline behavior; neither proves what six real covers sound like.

## Remaining gates

Not full Android parity or production release. Bundled video-renderer coverage, shared-sample mixing/time-warp alignment, all translations, full queue editing/smart playlists/recommendations, ReplayGain/EQ, URI-platform equivalents, continuous video/tiled backgrounds, touch/gesture parity, Narrator, endpoint changes, clean-machine and mixed-DPI acceptance remain open.

Unsigned: embedded payload hashes detect corruption, not publisher identity. Never disable Windows protections. See [handoff](HANDOFF.md), [parity matrix](PARITY.md), [version policy](VERSION_DESKTOP.md) and the versioned artifact receipt. Android source/APKs are independent and unchanged by this desktop checkpoint.
