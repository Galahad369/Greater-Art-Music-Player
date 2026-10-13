# Greater Art Desktop — native Windows edition

Desktop **0.1.1 / code2**, development build. Original C++ application with an IDE/Codex-style workspace: activity rail, resizable Stack pane, Library editor area, Nodes pane and fixed playback dock. Expanded Now Playing and detached Mini use the same native video renderer.

## Install and run

Use the locally delivered **GreaterArt-Desktop-Setup-0.1.1.exe**. Double-click it, keep **Create desktop shortcut** selected and press Install. Setup creates a Start menu entry too. It installs for the current user without an app-runtime download. Add a local folder in the app to populate Library.

Installed executable: `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`.
Settings and graph cache: `%LOCALAPPDATA%/GreaterArtDesktopNative/`.
Uninstall through Windows Installed apps; your settings and media are retained.

The separately delivered **GreaterArt-Desktop-0.1.1.exe** is also directly runnable. It stores preferences in LocalAppData; “single EXE” does not mean it writes settings beside itself. Do not run an obsolete prototype at the same time.

## Dependencies and offline boundary

The application uses Windows SDK/system components only: Win32/GDI, Windows Media Foundation, COM, shell and DWM. Static C++ runtime. **No VLC, Java, Electron, browser shell, NuGet, accounts, telemetry, updater or URL playback.** Only selected local folders are scanned; UNC/network roots and reparse-point traversal are rejected.

It still relies on Windows codecs. Unsupported media shows an error; it is never silently transcoded, proxy-played or quality-capped. Windows N installations without media components are not supported by this build. No resolution, bitrate or FPS limit is added.

Build tools are different from runtime dependencies: Visual Studio C++ desktop workload + Windows SDK build the app; WiX 3 builds the embedded MSI. WiX is not an installed app dependency.

## Working features

- Rectangular Library rows, search/reset, favorites, name/newest/oldest sort, chosen-folder recursive scan and Unicode filenames.
- Primary video/audio plus up to seven independent audio-only companions; master and per-voice volume, mute/solo, play/pause, seek, speed and whole-Stack loop.
- Saved Stacks separate from the Library, add-track picker, restore/delete saved Stacks, copy current/Stack paths.
- Exact-file Recycle Bin action behind three confirmations; never a blanket delete.
- Offline filename-similarity Nodes graph, sparse edges, strength-aware sizes, pan/zoom/drag, Fit and wand reveal; cached by media identity, size and modification time.
- Fixed transport, direct speed control, expanded player, monitor-bound fullscreen preserving the entire video frame, and borderless Mini on minimize.
- Collapsed settings groups, three restrained palettes, row/Mini sizing and persistent pane widths.
- Ctrl+Space playback, Ctrl+O folder, Ctrl+, settings, F11 fullscreen, Escape exit fullscreen.

## Build and check

```powershell
./desktop/native/build.ps1
./desktop/native/package.ps1 -WixPath 'C:/path/to/wix3'
python desktop/packaging/validate_version.py
python desktop/packaging/test_version.py
./desktop/native/build/GreaterArt.exe --self-test 'C:/owned/test-output'
./desktop/native/build/GreaterArt.exe --verify 'C:/owned/video-fixture.mp4' 'C:/owned/probe-output'
```

The interactive verification probe uses an isolated settings directory and captures only its owned windows. Never substitute desktop screen capture. It checks real rendered video, six playing media engines, fullscreen monitor bounds and dock/expanded/Mini handoff; it does **not** prove acoustic alignment.

## Honest limitations

Not full Android parity. Independent Media Foundation voices are not sample/phase-locked, and no offline arrangement aligner is offered yet. Current interface labels are English; Unicode media titles work. Artwork/metadata extraction, waveform/lyrics, Ambient, history, deeper queue features and UIA/Narrator acceptance remain unfinished. Mixed-DPI/multi-monitor, unsupported-codec and clean-machine coverage is incomplete.

Binaries are unsigned. Embedded MSI SHA-256 validation detects payload mismatch, not publisher authenticity. Do not disable Windows security protections to run them. See [handoff](HANDOFF.md), [parity](PARITY.md) and [version policy](VERSION_DESKTOP.md). This local development delivery is not a public production release.
