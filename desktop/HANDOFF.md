# Greater Art Desktop handoff

## Interactive Windows Nodes — October 17, 2026

Current source candidate **0.1.15/code16**. Latest frozen delivery remains 0.1.12 until installed acceptance and a new immutable receipt are recorded. Windows-only work; concurrent Android edits and local branches belong to their author and are excluded from this change. No new worktree. Security PR #174 remains blocked by owner direction.

- Replaced the static layout renderer with a background spring/repulsion simulation. Strong edges shorten and stiffen springs; weighted hubs grow and receive stronger centering. Node-size collision clearance keeps dense clusters readable. A spatial tree retains every node and bounds repulsion work.
- Middle mouse and Space+drag pan even over nodes; empty-space drag also pans. Physical pointer coordinates preserve fractional logical movement on high-DPI screens. Cursor-anchored zoom, hover-neighbor highlighting and node drag capture share stable world/camera coordinates. Slow drags cannot accidentally play a song. Keyboard focus and capture loss clear gestures safely.
- Physics uses fixed numerical steps independent of display pacing, coalesced worker requests, generation cancellation and cooling to idle. Hidden panes stop requests; reduced motion computes a final layout in the background. Equal-style edges/circles are batched into antialiased vector paths. Wand and force changes do not rescan media or recreate playback.
- Appended force preferences preserve existing settings/playlists/history. Center, repel, link force and distance are available from the Nodes gear. The six-language help text includes the new pan gestures.

Evidence so far: native build and **73/73** core checks pass, including 60/180-update physics equivalence, strong/weak separation, pinned neighbor response, idle/reheat, coincident points, 2,000-node finite geometry, stale generations and old preferences. An owned-window probe using the user's **209-file** library passed hover, slow drag, middle-mouse/Space+drag over nodes, clean release/focus-loss reset, zoom, filtering, settling and reduced motion while the same native player rendered **3840×2160** video (467 frames). Installed-package checks follow before delivery. Personal paths, filenames and captured media stay in ignored local evidence only. See [NODES_PHYSICS.md](docs/NODES_PHYSICS.md).

Mistakes and prevention: test capture against the latest input received, because Windows can deliver queued physical pointer movement after SetCapture; never warp the user's cursor to make a test pass. Keep cooling time separate from capped numerical catch-up, or a busy UI makes the graph take much longer to settle. Preserve elapsed time when a worker request is not admitted. Batch drawing as well as moving physics off-thread; per-edge graphics allocations still stall the UI. Compare actual player identity, source dimensions and frame progress during gestures. Numerical refresh equivalence is not a claim of 180FPS scanout. Check branch and changed paths before every commit when multiple agents share one checkout.

One final probe was minimized partway through and correctly stopped graph work; its settling assertion failed with only 6.157 seconds of visible simulation. The next visible-window run passed. Installed 0.1.13 core passed 73/73; its modified-pan probe then began outside the canvas after a queued physical cursor move dragged the previous anchor out of view. 0.1.14 fits the test canvas before each new pan gesture and reports precise failure bounds. Production physics/input are unchanged. Preserve failed reports and consumed versions. Paused Android work is preserved on local/1.21.40-nodes-obsidian-graph and local/1.21.41-nodes-obsidian-graph, plus three named stashes beginning "Paused Android" (Nodes, version and ledger edits). Resume those only on their original branch; they are not part of the desktop PR.

Installed 0.1.14: core **73/73**, real 1080p gestures/settling/reduced motion passed with **579** frames; source 4K probe passed with **468** frames. Its initial pacing run measured 49.8 paints/s. Profiled repeats found graph drawing about 1 ms versus 10.10 ms total paint; text outside the dirty region and an Ambient layer underneath opaque panels were redundant. 0.1.15 skips those costs without changing visible detail. The same short real-video probe improved 65.2 → 105.2 paints/s, main paint 10.10 → 4.93 ms, CPU 1.22 → 0.85 cores. This is a measured local improvement, not a guaranteed display FPS. Final installed results follow in the release receipt. Never run a build-directory test while packaging copies its codec DLLs: that caused a locked-file package failure; the sequential retry succeeded. Use installed/extracted copies for acceptance after packaging completes.

## Verified local delivery — October 16, 2026

Current **0.1.12/code13**, exact native build source **22590bf2d04aba8474fa7c5b0e3fbd748f048e04**. Frozen local setup/portable files, installed hashes and full limits: [RELEASE_0.1.12.md](RELEASE_0.1.12.md). Installed and separately extracted portable core **62/62**; 155 icon raster checks; single-source **9/9**, six-source **11/11**, zero underruns; calibrated digital output error **1.56285e-7**, one-sample sabotage **0.550466**; short group-loop stop passes. Installed 1280×720/194-frame rapid-add/fullscreen/Mini/dock/MSAA, real GPU colors and native fault recovery pass. Enabled-motion run: **120.08 events / 114.115 paints per second** on the 180.002 Hz compositor; reduced-motion draw shutdown passes. This is not a promise of 180FPS scanout, physical acoustic proof, full parity or production signing.

PR [#176](https://github.com/Galahad369/Greater-Art-Music-Player/pull/176) tracks protected-main integration and current CI; do not infer its live state from this local acceptance receipt. Security #174 remains owner-blocked; no rewrite/bypass. No new worktree. Android 1.21.39/code262 and all older frozen artifacts are unchanged. The records below preserve earlier candidates/failures, not conflicting current versions.

## Final acceptance correction — October 16, 2026

Current candidate **0.1.12/code13**, temporary protected-main integration PR #176. 0.1.11 installed core 62/62, six-source controls 11/11, single controls 9/9, digital output (error 1.56285e-7; one-sample sabotage 0.550466; zero underruns), 120ms loop stop, six-voice rapid-add/fullscreen/Mini/dock/MSAA, live GPU colors and effect recovery passed. Its enabled-motion run measured **120.8 events / 114 paints per second** on the 180 Hz compositor, not guaranteed 180FPS. Final reduced-motion assertion exposed a counter race: the callback that disables motion plus one already-queued callback were counted as new visual activity despite neither drawing. 0.1.12 increments the accepted-visual counter only after the reduced-motion guard. This is not a new audio/surface reload path. Final installed and frozen receipt follows after actual verification. No old release or Android artifact is changed.

## High-DPI icons and Stack handoff — October 16, 2026

Current source **0.1.11/code12**, local main; final installer acceptance/integration pending. Android **1.21.39/code262** and immutable deliveries are unchanged. No new worktree. Remote main's intervening #175 changes only the obsolete website repository link; preserve it during integration. Security PR #174 remains explicitly blocked, not a branch to merge/delete or a reason to bypass protection.

- Replaced all private font glyphs and aliased icon special cases with 31 original antialiased vector drawings. Same hitboxes, consistent 24-unit geometry, 30-unit main transport glyph, semantic names preserved. Windows resource has 16/24/32/48/64/128/256 sizes; titlebar requests select the current DPI size.
- Replaced fixed 30 Hz marquee/16ms decorative timers with a bounded monitor-rate high-resolution worker. Only one frame message may be pending; hidden/idle/reduced-motion states stop animation. Persistent per-window paint buffers and sparse update regions replace full-window allocations/copies. Physical fractional marquee/seek presentation reduces stepping. Native full-resolution cached vector Ambient replaces expensive repeated GDI+ radial rasterization; memory is bounded to 64 MiB. Playback clocks, source resolution/bitrate/FPS and primary decoder quality are untouched.
- Rapid voice additions retain the requested master position while a new shared source set prepares; they no longer reset to an unready bus's zero timestamp. A paused endpoint must be prebuffered before video resumes. Endpoint gain uses stream-local volume, not global session gain. Very short group loops drain correctly and stop at the primary boundary when Repeat is disabled. Explicit saved Solid preferences survive the new Adaptive default.
- Current local source checks: core **62/62**, including all **155** actual icon drawings at 100–400% DPI; six-source WASAPI controls **11/11**, actual 48 kHz stereo, **zero underruns**. Final packaged/installed/digital output/CI results belong in RELEASE_0.1.11.md once actually performed, not inferred from these tests.

Prevention: match icons optically with one path system, not a platform font plus ad-hoc aliased symbols. Test raster output, not just source dimensions. A perfectly axis-aligned rectangle can legitimately have no partially covered pixels; round-ended Pause bars now match the set. Preference roundtrip tests must create their isolated directory before saving. A ready decoder is not a prebuffered audio endpoint. Never read a still-preparing output's time as user intent, or pretend digital fixture proof guarantees arbitrary cover alignment. See [high-DPI boundary](docs/HIGH_DPI_ICONS.md) and [shared-clock evidence](docs/SHARED_CLOCK_STACK.md).

High-refresh failures were retained: hidden-test inactivity, visible WM_TIMER starvation, then 8.2/4.7 events/paints per second from backdrop rasterization. A native mesh reached 60/56; final high-resolution waits/native-size layer caching need their own measured acceptance. WM_TIMER has lower queue priority than paints; service due playback polling from presentation events too. DwmFlush is not a contractual per-monitor 180 Hz pacing primitive for GDI. Do not fix decorative cost by reducing source quality.

## Shared-clock Stack stage — October 15, 2026

Current working candidate **0.1.10/code11**, local main. The preceding live-video Ambient milestone is committed as cf1b53b. Android 1.21.39/code262 verified sideload and immutable releases are unchanged. No additional worktree. Protected-main PR/CI integration remains required; security PR #174 stays blocked by owner direction.

Windows Stack now prepares one bounded PCM producer and one WASAPI output for all voices, with fractional-sample offsets before mixing and one post-mix tempo stage. Primary video remains full-quality on the same native player; it is silent during Stack. Companions no longer create independent playback clients. Saved-Stack loads prepare once after restoring parameters. Primary-ended group looping appends the next opening on the same output stream. Details, explicit output proof and limitations: [SHARED_CLOCK_STACK.md](docs/SHARED_CLOCK_STACK.md).

Initial core 58/58 and calibrated own-process capture passed, including detecting a one-sample sabotage. Six-source speed/seek testing caught endpoint starvation (17 underruns), despite passing time/state assertions. Repair: prebuffer, batch ready PCM instead of feeding one small block per Sleep(3), event-driven WASAPI and MMCSS scheduling. Final rebuilt/installed/capture/CI acceptance is pending and must be recorded honestly. Independent timestamps or an analysis-fit residual are not acoustic proof. Full instrumental time-warp alignment and remaining parity inventory are still unfinished.

## Live-video Ambient stage — October 15, 2026

Current working source **0.1.9/code10 — implementation candidate**, local main. No new worktree. Android **1.21.39/code262 VERIFIED debug sideload** and every immutable release are unchanged. PR #172 merged normally as b4bf1ac after every check, including the complete Windows installer pipeline, passed; local main was aligned and the integration branch removed.

Original optional video pass-through MFT now reads sparse colors from the existing primary decoder. The exact input sample, payload/time/duration and native presentation remain unchanged. GPU readback is bounded, reused and nonblocking; paused/hidden/disabled/high-contrast states stop sampling. Classic/Adaptive video-or-artwork options use smooth palette transitions and localized labels. No second decoder, screen capture, FPS/resolution/bitrate cap or source proxy. Details and limits: [LIVE_VIDEO_AMBIENT.md](docs/LIVE_VIDEO_AMBIENT.md).

Final source checks passed: native compilation; core **57/57**, including detailed frame-contract, lifecycle, retained-buffer and synthetic HDR precision checks; real 1280×720 GPU red→blue→green colors with unchanged player/source, pause/disabled holds (5 GPU palette publications, 158 input frames); explicit optional-effect fault recovered native video with identity/position/size preserved; six-voice fullscreen/Mini/dock/MSAA transfer probe passed (224 rendered frames). Evidence is native/build/reviewed-019-*. Installed/CI results must still be recorded, not assumed. Complete Android Adaptive controls, real HDR display acceptance and tiled backgrounds are not claimed ported.

Prevention: advertise input formats before a type is configured; support output-first negotiation and declare retained sample ownership; create a separate tap/epoch per resource so late old callbacks cannot recolor new media; preserve tagged HDR precision. Fixture preferences must be set after persisted settings load; visible-presentation tests must use visible owned windows. A synthetic decoded-buffer test alone missed the first real integration failure. Optional decorative pipeline failure retries native playback at most once per source and exposes artwork fallback instead of sacrificing quality.

Next is one shared WASAPI sample clock for Stack, then the remaining parity inventory. Independent voices and analysis-fit residuals are not sample-output/acoustic proof. Security PR #174 stays blocked by owner direction; no history rewrite/protection bypass.

## PR review source — October 14, 2026

Current source **0.1.8/code9 — SOURCE_ONLY**, on temporary integration branch codex/desktop-integration-review, PR #172. Verified/frozen 0.1.4 artifacts remain unchanged. Hosted Windows CI passed pinned codec compilation, native compilation and core tests on 0.1.7, then exposed a source-packaging assumption: a registry at pinned HEAD builds from ports/ffmpeg, not the cached git-tree directory. Packaging now accepts the exact registry path, verifies its approved port tree and rejects dirty source. All stages must pass together before integration; no runtime codec version changed.

Review found and repaired: (1) a saturated worker queue discarded admitted operations, potentially stranding scan/alignment busy flags; admission now returns false without dropping accepted jobs, and callers clear flags/report rejection; (2) worker exceptions only logged, leaving callers without failure completion; generation-checked UI messages now clear current busy state; (3) raw FFmpeg frame allocations leaked on exceptional exits; frames are now scope-owned; (4) CI assumed vcpkg was on PATH; it also checks the runner installation root and Visual Studio tool path, with no prebuilt-codec fallback.

Five real worker-thread regression checks were added; native core tests pass **56/56**, and real Opus audio-control tests **9/9**. Final PR/CI/merge status belongs in docs/PR_INTEGRATION_REVIEW_2026-10-14.md. A successful local build is not a GitHub CI pass.

Workflow prevention: report bounded-queue rejection, preserve all admitted work, propagate exceptions to the owning UI generation, use RAII for native decoder objects, and never use bypass/admin merge to evade required checks. A temporary PR branch is deleted only after its work is preserved on main. Large legacy binary history is preserved, not silently rewritten.

## Current checkpoint — October 14, 2026

Desktop **0.1.4/code5**, DEVELOPMENT, local **main**. Existing desktop branch was merged as `dbcd06c`; no new branch or worktree was created. Obsolete tracked VLC/vendor DLLs and generated bytecode are removed from the current checkout, recoverable in Git history. Android source 1.21.39/code262 and verified APK 1.21.37/code260 are untouched by this desktop work. The section below records historical 0.1.1 evidence, not current feature limits.

### Implemented this checkpoint

- Separate local M3U8 playlists, opt-in bounded history/burn, LRC, A–B repeat, sleep, resume, lock, partial six-language labels and fonts/high contrast.
- Selected metadata/artwork and waveform work uses a bounded two-worker queue with generation cancellation. Analysis pauses under Stack pressure. Wave cache keys actual source identity/size/mtime, capped at 128 files/128 MiB; eviction recognizes only generated wave-number.bin files, never settings or original media.
- Confidence-gated RMS/onset sound alignment, saved ±offsets, readiness barrier and small drift-speed trims. Not instrumental separation, shared sample clock, time-warp or perfect real-cover alignment.
- Original WASAPI audio output with source-built in-process FFmpeg 9.0.2 codecs when native audio fails. Native video renderer stays unchanged. Failed video is not falsely reported as audio-only playback.
- One Share button with current file / queue M3U8 / queue files, Windows media keys, named/bounded MSAA controls and keyboard sliders. Full Share-target and Narrator acceptance is not established.
- Borderless Mini and dock share compact media metrics; Mini↔expanded transfers directly. Playlist order survives filtering; repeat-off stops at the end. A control-only tap nudge does not animate unrelated rows. Developer Ctrl+click reports an element without executing it.

### Tests and delivery boundary

Final local delivery: **0.1.4/code5**, source **227c7fe101bb1a8a7d4ed297ac00adc09d59e5d6**. Installed 51/51 core, 9/9 WAV + 9/9 Opus, 8/8 AIFF fallback, deliberately minimized fullscreen/six-voice transfer probe passed. Setup and frozen portable ZIP are in desktop/releases; hashes and detailed acceptance limits are in [RELEASE_0.1.4.md](RELEASE_0.1.4.md). A source/control parity inventory now lives at docs/ANDROID_WINDOWS_PARITY.md. Main is local; no public push/CI result is claimed.

Core and real-decoder tests, WAV/Opus WASAPI control tests, owned-window six-voice/fullscreen/Mini transfer probe, version/history tests and installer extraction/hash checks are required. Final results, immutable artifact paths, hashes and source commit belong in **RELEASE_0.1.4.md**, not guesses in this handoff. Current source-build evidence is in ignored native/build/codecs; distribution includes exact patched source/configuration/license files in Codecs.

### New mistakes and prevention

1. **Speed history jump:** multiplying all previously submitted frames by the new speed makes playback time jump. Account for submitted audio in rate-tagged spans, not a global elapsed-times-speed formula; test pause/seek/½×/2× on an actual WASAPI endpoint.
2. **Source-build spaces:** FFmpeg's port rejects spaces in checkout paths. Use a temporary SUBST alias, not a new checkout or worktree; remove only the unchanged mapping owned by this invocation.
3. **Broad prebuilt codec archive:** never substitute that archive for the reviewed minimal build. Disable default/GPL/nonfree features, pin the registry baseline and package corresponding patched source plus build evidence. Dynamic replacement of compatible LGPL libraries must remain possible.
4. **Header/fragment reordering:** generic formatters moved bcrypt before windows.h and Entry before Window declarations. Native .clang-format disables include sorting; preserve Windows prerequisite headers and ordered implementation fragments. Compile both app and Setup after formatting.
5. **Windows case-insensitive directories:** build/Codecs collides with build/codecs. Build documentation payload uses **build/CodecDocs**, separate from source/tool output; installer places only four evidence files under its Codecs directory.
6. **Historical version guard:** the two immutable pre-policy vendor-only user commits had no ledger. The exception is exact SHA + restricted vendor/bytecode paths, regression-tested; never exempt new source changes or silently rewrite history.
7. **Export race/partial files:** prevalidate all entries and use CREATE_NEW, not exists-then-open. Never overwrite an existing user export; remove only a newly created failed output.
8. **COM cleanup:** apartment lifetime must outlive its COM objects, including error paths. Windows Runtime initialization pairs with uninit_apartment. Worker exceptions must not strand the endpoint or lose a joinable thread.

9. **Installed-only routing race:** the state query could expose a native error before the timer started fallback. State query and timer must use the same idempotent handoff; pending fallback reports Loading, not Error. Verify the installed EXE with AIFF repeatedly, not only the raw decoder. 0.1.2 failed this acceptance; preserve its source commit and consume 0.1.3/code4 for the fix.

10. **Iconic destination bounds:** one installed 0.1.3 capture showed a minimized Now Playing window (237×39); the next run passed. Do not assume SetWindowPos restores an iconic window or silently discard the failed run. 0.1.4 explicitly restores on expanded/fullscreen requests; its regression probe deliberately minimizes before fullscreen, and two source-build runs passed. Final installed evidence is recorded separately.

### Still incomplete

Full Android parity: native output for bundled video formats, shared-sample/instrumental time-warp alignment, continuous video Ambient/tiled Stack wallpapers, complete translations/queue editing/smart playlists/recommendations, full touch gestures, EQ/ReplayGain, all cache controls, endpoint changes, clean-machine/mixed-DPI testing, Narrator/Share target acceptance and signing. Do not label this finished, phase-perfect, crash-free or production-ready. See PARITY.md.

## Historical 0.1.1 state — October 13, 2026

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
- Final source commit **be22da9**, artifact hashes and post-integration checks are recorded in [RELEASE_0.1.1.md](RELEASE_0.1.1.md). The final installed probe reports 234 renderer frames; the earlier 249-frame run was a staging-build check.
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
