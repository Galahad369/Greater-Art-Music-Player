# Desktop 0.1.12 — verified local development delivery

October 16, 2026 · **0.1.12/code13** · exact native build source **22590bf2d04aba8474fa7c5b0e3fbd748f048e04**. Integration/checks are tracked in [PR #176](https://github.com/Galahad369/Greater-Art-Music-Player/pull/176); this receipt records local installed/portable acceptance, not a guessed CI result. Android **1.21.39/code262** is unchanged. **Not full Android parity, guaranteed 180FPS, production signing or universal acoustic synchronization.**

## Immutable local artifacts

These repository-relative files are ignored local deliveries, not an announced public GitHub release. Never overwrite them or any older release.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| desktop/releases/GreaterArt-Desktop-Setup-0.1.12.exe | 33,447,424 | 5158a426a90fc3197b137faafc0ccd8cc10a44d07461d620ae4461513112b688 |
| desktop/releases/GreaterArt-Desktop-Portable-0.1.12.zip | 32,704,275 | ede5cce62c4a94d56ef06dea562f9b4065070ebefbbc247da53011d128a00e29 |

App EXE: **1,479,168 bytes**, ProductVersion 0.1.12, SHA-256 **a02c9dad5aba61ac915573b4d84c4c923d3d2e95d5ea33d71a74c8e086926fc0**.

Embedded MSI SHA-256: **1cea4cf8594169b61a0ccf79e9f7624caf36042258202f1126c681f6f4214974**. Packaged codec source ZIP SHA-256: **2f42e5e92c8c5eecca024f0df73b0db98d831a22350b500b73df7de842b10d09**.

## Run it

Double-click Setup, keep Create desktop shortcut selected, then Install. The per-user installer creates Desktop and Start menu shortcuts. This workstation was upgraded successfully from 0.1.10 → 0.1.11 → 0.1.12; the installed EXE hash, registry version and all six DLL hashes match the package. Preferences and original media were retained.

Installed executable: `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`. Portable: extract **the entire ZIP**, then run GreaterArt.exe beside its six codec DLLs and Codecs evidence directory. It is not a standalone single EXE. Setup/install/playback are offline. No VLC, Java, Electron, external player or runtime download.

Unsigned development binaries: integrity hashes do not establish publisher identity. Do not disable Windows protections.

## Actual acceptance

- Installed native core **62/62**, including Unicode/persistence/cache/graph/alignment, shared-clock PCM, preserved explicit Solid preference and vector raster tests. All **155** icon drawings at 100–400% DPI pass; contact sheets visually inspected. Portable independently extracted and passed **62/62**, with the same executable hash and complete payload.
- Installed single-source WASAPI controls **9/9**, six-source controls **11/11**: play/pause/paused seek/resume, timeline continuity, 0.5×/2× and invalid commands. Actual endpoint **48 kHz stereo**, zero Stack underruns. No artificial output/source quality cap.
- Own-process WASAPI capture: six independent deterministic signals match a calibrated sum with normalized error **1.56285e-7**, after a global capture-latency shift of **−480 frames**. Deliberate **one-sample** error in one voice produces **0.550466**, so the test detects it. Calibration/Stack underruns **0/0**. No microphone or other-app audio captured. A 120ms primary loops as a group and stops correctly when Repeat is disabled, zero loop underruns.
- Installed real-video/window probe passes: six sources, 1280×720 primary, **194 rendered frames**, seek-to-eight-seconds then rapid additions preserve position, monitor-bound fullscreen after minimization, direct Mini/dock/expanded transfers, named MSAA controls. One audible sample-clock bus; companions do not create duplicate media-output clients.
- Installed live Ambient observes real decoded GPU red→blue→green, **5** palette publications / **141** tap frames / **55** rendered frames; same primary identity and source dimensions, paused/disabled sampling holds. Explicit optional-effect fault recovers native video with identity/position/size preserved. No second decoder, screen capture, source proxy, resolution/bitrate/FPS limit.
- Installed enabled-motion test: compositor **180.002 Hz**, accepted visual events **120.08/s**, owned paints **114.115/s**, process CPU **0.6834 cores** in this short fixture run. Reduced-motion draw shutdown passes; Windows preferences were not changed. These are window-event/paint counters, **not proof of physical 180FPS scanout or all-scenario frame pacing**.
- Desktop version/history guard and its **6/6** tests pass. Android runtime/APK is unchanged; Android emulator testing was not rerun for this desktop-only delivery.

Local evidence: native/build/installed-013-*, native/build/portable-013-core, native/build/package-receipt.json. Captures contain owned HWNDs only. Diagnostic artifacts remain local; they were not uploaded as private reports.

## Repairs and retained failures

0.1.9 adds same-sample live-video color analysis. 0.1.10 replaces loose audible voices with one bounded PCM mixer/endpoint; its first speed test caught 17 underruns. Event-driven batching, prebuffer and MMCSS repaired that failure. 0.1.11 fixes rapid-add position loss, stream-local gain, primary-bounded loop stop, icon scaling and high-refresh painting.

Visible high-refresh acceptance caught timer starvation and repeated radial backdrop rasterization. Initial output was only 8.2 events / 4.7 paints per second. A native vector mesh reached 60/56; native-resolution layer caching and monitor-rate waitable scheduling reached the final rates above. Sparse dirty regions and reused buffers avoid rebuilding the full UI. Primary media quality was never lowered.

The first 0.1.11 installed reduced-motion assertion failed: the callback disabling motion and a queued callback were counted despite drawing nothing. 0.1.12 gates the accepted-visual counter before incrementing. Old failed records and source identities are retained; no 0.1.11 artifact was frozen or relabelled.

## Scope and provenance

Original C++20/Win32 UI, original WASAPI output, system Media Foundation primary video. Approved FFmpeg 9.0.2/port9.0.2#1 is source-built at pinned baseline e456309491fd875487bf02b8a0dca76c1b1b00cc; default/GPL/nonfree/external codec features disabled. Dynamically replaceable LGPL libraries, exact corresponding source/configuration, licenses and receipt accompany both packages. Visual Studio/SDK and WiX are build-only dependencies. Upstream FFmpeg conversion and deliberate per-user MSI ICE91 warnings are documented; no build error was ignored.

Shared-clock output aligns supplied offsets on one sample schedule. It does **not** make different singers' performances musically identical; automatic instrumental/chroma/time-warp matching remains incomplete. Also pending: full bundled video-format output, complete Android Adaptive controls/tiled video, queue editing/translations/cache settings, primary switching/recommendations, Narrator/Share targets, endpoint reconnect, clean-machine/mixed-DPI/physical high-refresh acceptance and owner-approved signing. See [control inventory](docs/ANDROID_WINDOWS_PARITY.md). Security PR #174 remains blocked by owner direction; no history rewrite, protection bypass or certification claim.
