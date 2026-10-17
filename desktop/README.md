# Greater Art Desktop — native Windows edition

Current source **0.1.16/code17**, interactive Nodes update undergoing final installed acceptance. Latest frozen delivery is **0.1.12/code13** until the new receipt is recorded. Original C++/Win32 application: activity rail, resizable Stack / Library / Nodes panes, collapsed Settings, fixed dock. Expanded Now Playing and detached Mini transfer the same primary renderer without reloading media. Older frozen artifacts are preserved.

## Install and run

[Artifact hashes and installed test receipt](RELEASE_0.1.12.md) · [Android-to-Windows source/control inventory](docs/ANDROID_WINDOWS_PARITY.md).

Use **GreaterArt-Desktop-Setup-0.1.12.exe** from the local desktop/releases directory. Double-click, leave **Create desktop shortcut** selected, then Install. The per-user setup also creates a Start menu entry. Installation and playback are offline. Add a local folder to populate Library.

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
- Up to eight sources mixed on one sample clock and WASAPI output, fractional-sample offsets, gain/mute/solo, primary-bounded group loop and fine nudges. Calibrated process-specific output tests detect a one-sample error. Automatic musical matching remains simpler, confidence-gated RMS/onset alignment—not a guarantee that different performances align perfectly.
- Filename-similarity Nodes graph with live damped springs, node-size collision spacing and weighted hubs. Middle mouse or Space+drag pans over nodes; empty-space drag also pans. Wheel zoom stays under the cursor, dragged nodes pull their neighbors, hover highlights links, and physics settles at idle. The gear exposes center/repel/link force and link distance. Wand reveal, neighbor/link threshold controls and versioned cache remain available. See [physics and verification](docs/NODES_PHYSICS.md).
- Fixed seek/transport, first-class speed, lock, whole-frame monitor fullscreen, borderless Mini, direct Mini↔expanded transfer.
- Selected-media metadata/artwork, bounded waveform disk cache, local synchronized LRC, A–B repeat, sleep timer, resume, opt-in history/burn, optional spinning audio disc.
- Live-video/artwork Classic and Adaptive Ambient, using the original decoder with no second video player. Aspect-preserving custom image Crop(default)/Fit; no Stretch. Complete Android Adaptive controls and tiled Stack video wallpaper remain pending.
- One native Share button offers current original file, queue M3U8 or queue files. Windows Share/media keys are implemented; full target-application acceptance remains pending.
- Native accessible button names/roles/bounds and keyboard transport/slider navigation; high contrast, fonts, partial six-language UI. Ctrl+click inspects a drawn control when Developer mode is enabled.
- Original antialiased vector icons at physical DPI, multi-size application icon, compositor-paced marquee/control presentation and reused paint buffers. [Render checks and 180 Hz acceptance boundary](docs/HIGH_DPI_ICONS.md).

## Build and verify

```powershell
./desktop/native/build-codecs.ps1 -VcpkgExe 'C:/path/to/vcpkg.exe'
./desktop/native/build.ps1 -RequireCodecs
./desktop/native/package.ps1 -WixPath 'C:/path/to/wix3'
python desktop/packaging/validate_version.py
python desktop/packaging/test_version.py
./desktop/native/build/GreaterArt.exe --self-test 'C:/owned/test-output'
./desktop/native/build/GreaterArt.exe --verify-audio 'C:/owned/audio-fixture.wav' 'C:/owned/audio-output'
./desktop/native/build/GreaterArt.exe --verify-stack-audio 'C:/owned/video-fixture.mp4' 'C:/owned/stack-output'
./desktop/native/build/GreaterArt.exe --verify-stack-output 'C:/owned/calibrated-output'
./desktop/native/build/GreaterArt.exe --verify-ambient 'C:/owned/red-blue-green.mp4' 'C:/owned/ambient-output'
./desktop/native/build/GreaterArt.exe --verify 'C:/owned/video-fixture.mp4' 'C:/owned/probe-output'
./desktop/native/build/GreaterArt.exe --verify-graph 'C:/owned/local-video.mp4' 'C:/owned/graph-output'
```

Tests isolate all generated settings. The video probe captures **only owned HWNDs**, checks real rendering, six voices, fullscreen bounds and renderer handoffs. The audio probe checks actual WASAPI timeline behavior; neither proves what six real covers sound like.

## Remaining gates

Not full Android parity or production release. Bundled video-renderer coverage, instrumental/time-warp matching, all translations, full queue editing/smart playlists/recommendations, ReplayGain/EQ, URI-platform equivalents, full Adaptive controls/tiled backgrounds, touch/gesture parity, Narrator, endpoint changes, clean-machine, mixed-DPI and physical 180 Hz acceptance remain open.

Unsigned: embedded payload hashes detect corruption, not publisher identity. Never disable Windows protections. See [handoff](HANDOFF.md), [parity matrix](PARITY.md), [version policy](VERSION_DESKTOP.md) and the versioned artifact receipt. Android source/APKs are independent and unchanged by this desktop checkpoint.
