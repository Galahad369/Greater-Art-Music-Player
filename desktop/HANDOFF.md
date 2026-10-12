# Desktop continuation — October 13, 2026

## Scope and owner decisions

- Independent Windows edition, full Android parity is the eventual goal.
- Default three-pane workspace: Stack | Library | Nodes.
- Desktop branch, no automatic merge; preserve Android behavior and artifacts.
- Owner approved independent desktop version validation and the necessary one-time
  Android tooling-policy bump. Local source now carries 1.21.39/code262; it has
  not been committed or claimed as a new verified Android APK.
- Owner did not select a desktop licensing route. Do not infer permission to
  relicense Android or publish a bundled installer from that uncertainty.

## Current state

Branch `feat/greater-art-windows-desktop`, based on Android main
`884a2d5d86aa268cffac0200b37adf925b7ed5b3` (1.21.38/code261 SOURCE_ONLY).
Desktop identity: 0.1.0/code1, DEVELOPMENT. Work remains local and uncommitted.
Android's verified APK remains 1.21.37; no Android APK was replaced by this work.

Local dependencies: JDK 21.0.8, Kotlin 2.2.21, Compose 1.9.3, JNA 5.17.0,
coroutines 1.10.2, installed native VLC 3.0.24. Direct JNA C bindings are used;
choosing them does not settle distribution obligations of bundled native plugins.

## Actual checks

`test` and `backendProbe` passed after the latest Kotlin edits. Three JVM tests
cover root boundaries/Unicode/deduplication, settings persistence and the shared
Android similarity/layout math. Native probe exercised real generated local media:
primary video plus one audio companion, pause, seek, resume, speed, volume,
minimized continuation and cleanup. Original-size decoded-frame PNGs are in
`app/build/native-probe/before-seek.png` and `after-seek.png` (ignored build output).

The latest local app image built successfully with `test createDistributable`.
Its actual `GreaterArtDesktop.exe --backend-probe <fixture>` exited 0 with
`GREATER_ART_VLC`, `JAVA_HOME` and `JAVA_TOOL_OPTIONS` cleared. Thus this probe
used the packaged JRE and bundled runtime, not the development environment's
configured VLC location. Output: `app/build/native-probe/packaged-stdout.log` and
`packaged-stderr.log`. This machine still has development tools installed; it is
not a clean-machine acceptance test. The full Compose workspace was not visually
accepted by this backend-only probe.

Local development launcher (keep the entire adjacent app-image directory):
`app/build/compose/binaries/main/app/GreaterArtDesktop/GreaterArtDesktop.exe`.
No signed installer or public distribution was produced. Three desktop JVM tests,
three desktop-policy tests and four Android merge-policy tests pass; both current
version guards pass. Complete Android runtime tests were not rerun for the tooling
metadata change. No commits, pushes or automatic merges were performed.

The native probe reported D3D11/avcodec warnings. Successful snapshots prove frames
were decoded, not that all frames were smooth or that six singers were audibly
aligned. Do not mark playback acceptance complete from position counters.

## Fixes and mistakes to avoid

- Java TLS failed PKIX validation although Windows HTTPS worked. Diagnosed via
  Java, then used process-local Windows-ROOT trust. Never disable TLS verification.
- Removed obsolete `--plugin-path`; VLC 3 reported it no longer exists.
- Companion media now uses per-media `:no-video`: no stray native video windows,
  while primary video and companion source audio quality remain untouched.
- Media startup waits for a registered native canvas instead of starting an
  unhosted output. All presentations still share the same primary player.
- Failed companion loads release their native player. Queue transitions release
  old companions and restore primary volume/rate rather than leaving stale voices.
- Dividers convert physical-pixel drag distances to dp and use current callbacks.
- Graph node degrees are precomputed rather than rescanning every edge per node
  during each Canvas draw.
- Native polling stops when shutdown begins, preventing timer dereferences after
  player release. Executor shutdown is scheduled before awaiting native cleanup.
- Loop restarts all current sources, but it is not a sample-synchronized barrier.

## Next work, in order

1. Run and inspect the actual Compose workspace with real media; test preview ↔
   expanded canvas transfer, repeated source changes and clean window closure.
2. Investigate decoder warnings and frame pacing without adding quality caps.
3. Add integration coverage for primary/companion lifecycle, end/loop and failures.
4. Inventory Android commands/preferences into a complete parity matrix; implement
   missing functionality incrementally. Alignment, saved Stacks, playlists,
   translations, metadata/artwork, lyrics, history, Ambient, true fullscreen,
   shortcuts and tray behavior are not complete.
5. Complete/test independent desktop CI/version governance. Current local edits
   to shared workflow/Android guard require the authorized one-time Android patch:
   main was rechecked at 884a2d5; local 1.21.39/code262 is free and the current-state
   guard passes. Recheck before committing; do not overwrite an APK or claim a
   newly verified Android binary for this source-only tooling change.
6. Package and test a self-contained local development app image with reviewed
   runtime/JRE. Installer/public release additionally require licensing, signing
   and acceptance gates. Do not publish automatically.

Official VLC archive downloaded, resumed after a premature connection close,
checksum-verified and extracted into ignored `app/runtime/windows-x64/vlc`.
The complete vendor tree, including plugins and license files, is retained.
No archive was extracted until checksum validation passed. Metadata:
`https://download.videolan.org/pub/videolan/vlc/3.0.24/win64/vlc-3.0.24-win64.zip`
Vendor SHA-256:
`fcf30850371ad10c9373cc4f0f4501e7dee49e3e9ae9f20c72fb2661a1ca6323`.
Verify archive path safety and checksum before extracting; preserve license files.
