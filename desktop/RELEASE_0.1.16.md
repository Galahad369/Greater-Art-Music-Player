# Desktop 0.1.16 — interactive Nodes delivery

October 17, 2026 · **0.1.16/code17** · exact native build source **044a9670992027a19b2909aa609abe12d4ad5734**. This receipt records local installed and extracted-portable acceptance. Live GitHub checks and protected-main integration are separate evidence. Android 1.21.39/code262 and all older frozen deliveries are unchanged.

## Immutable local artifacts

These are ignored local deliveries, not a public GitHub release. Never overwrite a versioned artifact.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| desktop/releases/GreaterArt-Desktop-Setup-0.1.16.exe | 33,484,288 | decda7a59456a28e3078b4f2b855e2125f25e43e13624eb52bb85805792e718b |
| desktop/releases/GreaterArt-Desktop-Portable-0.1.16.zip | 32,741,606 | bcbf95e7b3ba1b9a92d591784073d0427862b1806cc4637546f4fa8f189052de |

App EXE: **1,545,216 bytes**, ProductVersion/FileVersion **0.1.16**, SHA-256 **7dfff31204f08b039b315682c996f386a1079c69c7e413fb8d36ac6a1db5f7bd**.

Embedded MSI SHA-256: **eeb1a073fdc798919d8142ddf6514862104d270e20843af2d00146de9cf72c72**. Corresponding codec source ZIP SHA-256: **1263608a647e3c9c1997a3f32eda6ca3761da9660c321847b3403f1d9e5547f0**.

Double-click Setup to install offline. Desktop and Start menu shortcuts target `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`; the successful per-user upgrade retained existing preferences/media. Portable users must extract the entire ZIP and keep its six codec DLLs and Codecs directory beside the app. No VLC or external player is needed. These remain unsigned development binaries.

## What changed

- Nodes now use background damped springs and spatial-tree repulsion. Strong links pull closer, weighted hubs appear larger, and collision spacing gives larger nodes room. Dragging a node moves its neighbors; release lets the network settle.
- Middle mouse or **Space+drag** pans even when started over a node. Empty-space drag also pans. Physical pointer precision is retained at high DPI. Wheel zoom stays anchored to the cursor; hover emphasizes immediate neighbors and playing media retains its ring.
- The gear exposes center force, repulsion, link force and link distance. Preferences append compatibly to existing settings. The wand reheats/reveals the graph without rescanning files or reloading playback. Hidden panes stop physics requests; reduced motion computes a final layout in the background.
- Native-DPI antialiased geometry is batched. Text and marks outside the actual dirty region are skipped; an unseen Ambient layer is no longer drawn underneath opaque panes. Visible pane backgrounds, source playback resolution/bitrate/FPS and the primary player are preserved.

Implementation/reference and limits: [NODES_PHYSICS.md](docs/NODES_PHYSICS.md).

## Verified behavior

- **Installed 73/73 and independently extracted portable 73/73** core checks. Includes strong/weak separation with full-size pair clearance, 60/180-update numerical equivalence, pinned drag and neighbor movement, sleeping/reheating, coincident points, all 2,000 test nodes retained, stale-worker generations, and old/new preference compatibility. Existing codec, cache, Unicode, playlist, shared-clock PCM and all 155 high-DPI icon raster checks remain included.
- User-supplied **209-file** library used without modifying original files. The installed 0.1.16 graph probe passes hover, slow drag, middle mouse/Space+drag over nodes, focus-loss reset, cursor zoom, filtered hit testing, release settling and reduced motion while the **same native player** renders **3840×2160** video (**469 frames**). The preceding installed 0.1.15 run also passes the real **1920×1080** graph case (**586 frames**); graph/input code is unchanged in 0.1.16.
- Installed 0.1.16 transfer probe reports its actual **0.1.16/code17** identity and passes six voices, **1920×1080 / 136 rendered frames**, monitor-bound fullscreen, direct Mini/dock transfers, preserved rapid-add position and named MSAA controls.
- Same rendering implementation measured on the installed 0.1.15 build: **209 nodes / 790 edges**, 180 Hz compositor, **94.4 owned paints/s**, **5.86 ms** average main paint. Controlled source profiling improved **65.2 → 105.2 paints/s**, **10.10 → 4.93 ms** main paint and **1.22 → 0.85 CPU cores**. These short window counters do not prove physical 180FPS scanout or all-machine smoothness. No source-quality limit or Windows preference change was used.
- Installed EXE and six DLLs match the package. The extracted ZIP has identical app/runtime hashes and complete corresponding-source/license evidence. Registry version and both shortcut registrations are correct. Desktop version/history guard and six validator tests pass; shared Android guard is checked with its installed inspection tool.

Local evidence: native/build/installed-017-core, installed-017-graph-4k-observed, installed-017-transfers-final, portable-017-core and package-receipt.json; earlier real-library/profile runs are retained in native/build. Captures use only owned HWNDs. Personal media, names and paths remain local and ignored.

## Retained failures and prevention

0.1.13's installed pan probe could start its next gesture off-canvas after a queued physical cursor move. 0.1.14 fits its test canvas before each gesture. Never warp the user's cursor to make a test pass. A minimized run correctly stopped simulation; capture loss canceled another drag. One 0.1.16 idle assertion saw only 0.765 seconds of graph age; its live pane state was not logged. A repeat that observed constant window size/DPI passed. Keep these reports and distinguish visible, uninterrupted test conditions from a universal no-bug claim.

Real-library profiling caught clipped text layout and redundant backdrop work after functional tests had passed. 0.1.15 removes those costs. A simultaneous build-directory test locked a codec DLL during packaging; sequential packaging succeeded. Run acceptance from installed/extracted copies after the packager finishes copying files.

The general transfer report had hardcoded 0.1.12/code13 despite correct later PE metadata. 0.1.16 reads VERSION/VERSION_CODE directly. Always compare the installed hash, PE resources, registry and test output. Every consumed source identity remains in the ledger; no failed candidate was relabelled or frozen as a delivery.

## Scope

Original Windows implementation inspired by documented Obsidian interaction principles, not its source or exact numeric tuning. No new runtime dependency, permission or network behavior. Existing reviewed, replaceable LGPL codec DLLs and pinned corresponding source are preserved. Upstream FFmpeg conversion warnings and deliberate per-user MSI ICE91 warnings remain documented; build errors are not ignored.

Full Android parity, arbitrary-cover musical alignment, physical acoustic/high-refresh testing, full Narrator/Share-target acceptance, mixed-DPI/clean-machine coverage and production signing remain separate gates. Security PR #174 stays blocked by owner direction. Paused Android branches and three named "Paused Android" stashes are preserved separately; no Android change is in this delivery.
