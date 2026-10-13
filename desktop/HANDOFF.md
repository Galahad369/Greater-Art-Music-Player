# Greater Art Desktop handoff

## Current state — October 13, 2026

Desktop **0.1.1/code2**, DEVELOPMENT. Native Windows implementation replaces the 0.1.0 Compose/VLC prototype, which is preserved in Git checkpoint **702976a**. Android source on the desktop branch remains 1.21.39/code262 SOURCE_ONLY; verified Android APK 1.21.37/code260 is untouched.

User direction: VS Studio/Codex-style workspace, original application, offline, few dependencies, self-contained setup EXE that creates a shortcut. Implemented using original C++/Win32 UI and Windows Media Foundation, not a VLC wrapper.

## Architecture

- native/Core.h: bounded chosen-folder discovery, Unicode identity/similarity, sparse force graph, atomic settings/Stack persistence, disk graph cache.
- native/App.cpp: application model, GDI layout/painting/hit testing, dialogs and controls.
- native/Window.inl: native window hosting, dock/expanded/Mini/fullscreen transitions and isolated verification probe.
- native/Entry.inl: entrypoint, application icon, self-tests and single-instance guard.
- native/Media.cpp/.h: one MTA actor serializes Media Foundation engines; a stable primary renderer moves between hosts.
- native/Setup.cpp: original offline installer front-end; checksum-verifies embedded MSI before using Windows Installer.
- native/Product.wxs: per-user app and shortcuts; no elevated install or runtime download.
- native/build.ps1 + package.ps1: fail-fast C++/SDK compilation and build-only WiX packaging.
- packaging/validate_version.py: independent desktop identity/resource/package/history validation.

No Java/VLC/Electron/NuGet is used by the native build. Do not resurrect the obsolete prepare-runtime download script or vendor bundle. Unrelated DLL commits on main were not blindly merged or published. No extra Git worktree was created.

## Verification recorded

- Native self-tests: **18/18**, covering Latin/CJK similarity, master/gain/mute/solo, hour durations, Unicode roots/favorites, saved Stack separation, scoped discovery/cancellation, sparse graph, atomic disk cache/invalidation, canonical paths and malformed settings.
- Actual installed native video probe: **PASS**, six media engines playing generated fixture, 1280×720 source, 249 renderer frames in that run, exact fullscreen monitor bounds, dock → expanded → fullscreen → dock → Mini → expanded → dock.
- Owned-window workspace and Now Playing captures inspected; rectangular rows/current marker, fitted centered video, fixed controls and pane hierarchy confirmed.
- Setup/shortcuts checked against exact target and SHA-256. A clean uninstall/reinstall check removed only development registrations; no user media/settings were deleted.
- Final commit/artifact hashes and post-integration checks are in RELEASE_0.1.1.md when finalized.
- No audible claim: six identical generated fixture copies are NOT six different real covers, and player-position/probe evidence is NOT acoustic synchronization.
- GitHub CI is configured but not claimed run for this local unpushed commit.

## Workflow and mistakes — prevention

1. **Concurrent checkout mutation:** another process reset/switched checkout and erased uncommitted prototype edits. Native implementation was staged in an ordinary non-Git workspace folder, then applied to the existing desktop branch. Inspect status/ref before every commit. Do not reset, discard or auto-merge unrelated DLLs.
2. **Cross-thread renderer handoff deadlock:** MTA reparenting can synchronously send a UI message while UI waits for the actor. Wait pumps SENT messages only; never dispatch queued clicks/timers reentrantly. Skip no-op host transfers.
3. **Fit looked left-aligned:** HWND presentation ignores a screen-relative destination offset. Center the renderer child inside its host and submit origin-relative frame bounds. Never fix it with Stretch, source resizing or quality limits.
4. **Current-row marker missed equivalent paths:** slash differences made a playing file look unrelated. Use canonical absolute preferred-path identities, preserving Unicode.
5. **Native single-node graph placement:** center the singleton explicitly. Distinguish node drag from tap so dragging never starts playback.
6. **Win32 compile hazards:** RPC headers define small; undefine before the app font field. Use pointer-width HMENU casts and DWORD native video dimensions.
7. **Capture privacy:** initial hidden-window desktop BitBlt test captured unrelated background; those generated images were removed. Verification now uses PrintWindow on OWNED HWNDs only. No desktop capture or unrelated app imagery in delivered evidence.
8. **MSI directory identifier collision:** StartMenuFolder is a reserved installer property. Use GreaterArtProgramGroup under ProgramMenuFolder; verify both actual shortcut paths after install.
9. **Stale same-version development installs:** repeated 0.1.1 test MSIs can leave multiple product registrations. Uninstall only known development product codes before acceptance; then compare installed EXE hash to package-receipt.json. Never call a successful installer exit sufficient verification.
10. **Locked MSI rebuild:** generate each payload in a unique build subdirectory; fail fast on tool errors and produce a receipt. Do not silently deliver old output. Exact lock holder was not established.
11. **Save dialog race:** snapshot Stack tracks/gain/mute/solo when opening Save, not after the primary may have advanced during typing.
12. **Speed rejection:** only publish the new speed after all engines accept; restore previous speed if any source rejects it.
13. **Immutable delivery:** commit source first, build from that commit, validate resource identity, freeze version-named EXEs without overwrite, record hashes/source commit. Generated objects/logs/MSIs/runtime bundles stay ignored.

## Remaining acceptance gates

Full Android feature parity, accurate shared-sample Stack mixing/alignment, real-cover acoustic verification, UIA/Narrator semantics and keyboard navigation, translations, metadata/artwork/waveforms/lyrics/Ambient/history, unsupported codecs, clean-machine testing, mixed-DPI/multi-monitor and trusted signing. Embedded checksum is not publisher authentication. Do not claim bug-free, security-certified or production-ready.

No automatic public push, release, branch deletion or main merge is part of this local native delivery.
