# Desktop parity and acceptance

Desktop **0.1.16/code17** is an original native Windows **verified local development delivery**, not full Android parity. Frozen installer/portable evidence is in [RELEASE_0.1.16.md](RELEASE_0.1.16.md). Android source/runtime remains independently versioned.

| Area | Implemented now | Still needed |
| --- | --- | --- |
| Workspace | IDE rail, resizable Library/Stack/Nodes, collapsed Settings, fixed dock | Small-layout, mixed-DPI and multi-monitor acceptance |
| Nodes | Background springs/repulsion, weighted hubs/collision spacing, hover neighbors, middle mouse/Space+drag pan, cursor zoom, force controls and idle/reduced-motion handling; real 209-file interaction and 2,000-node numerical checks | Touch gestures, graph-node accessibility, broader large-library/mixed-DPI acceptance and remaining group/display options |
| Library/lists | Local chosen folders, Unicode, favorites, sort/search/reset, separate M3U8 playlists, recoverable triple-confirm deletion | Multi-select, reorder/editor, smart lists, backups, exclusions, CUE and metadata overrides |
| Playback | Native video/audio; bundled source-built audio decoder and original WASAPI fallback; seek/rate/volume | Bundled video-renderer output, format matrix, endpoint reconnection, EQ/ReplayGain |
| Stack | Up to eight sources on one PCM sample clock and one WASAPI endpoint; fractional offsets, gain/mute/solo, group loop, confidence-gated RMS/onset alignment | Instrumental/chroma time-warp matching, different-cover acoustic tests, primary switching, recommendations |
| Presentations | Stable renderer dock/expanded/fullscreen/Mini; same compact metrics and direct transfers | Touch/pull-down, YouTube seek feedback, lock in fullscreen, tray and all monitor scenarios |
| Artwork/cache | Selected artwork/metadata, source-keyed disk waveform cache, bounded worker queue and pressure pause | All-row artwork parity, user cache management, complete memory-pressure/performance acceptance |
| Background | Original-decoder live-video/artwork Classic/Adaptive Ambient, custom image Crop(default)/Fit, no Stretch | Complete Android Adaptive controls, CURRENT_VIDEO and tiled Stack wallpaper |
| Extras | Local LRC, A–B, sleep, resume, history opt-in/burn, audio disc, shuffle | Full Android advanced controls and settings integration |
| Sharing/system | Unified Windows Share options and system media transport | Actual target-app transfer and all media-key scenarios |
| Accessibility | MSAA names/roles/bounds, focus/hit-test alignment, keyboard buttons/sliders, high contrast | Native Narrator/UIA acceptance, graph-node semantics, complete dialog flow |
| Localization/design | Partial English/ZH-TW/JA/DE/FR/YUE labels, fonts/themes, rectangular rows, current-only tap effect; 31 original vector icons and compositor-paced visuals | All dialogs/services translated, full appearance/token parity, native-reader review and physical 180 Hz/mixed-DPI acceptance |
| Installer/security | Offline per-user setup/shortcuts, replaceable codec DLLs with exact source/licenses, corruption checks, local crash report | Publisher signing, clean machine, independent license/security review; no certification claim |

## Evidence boundary

Core tests use generated local fixtures. The waveform test detects a known 500 ms delay; WASAPI tests validate the real endpoint clock/controls. Process-specific digital loopback compares six signals against calibration and catches one-sample sabotage. The owned-window video probe checks rendered frames, six voices, fullscreen bounds, named control hit tests and dock/Mini transfers. None proves perfectly matched real-cover musical timing or physical acoustic output.

Final hashes/source/installed checks are in [RELEASE_0.1.16.md](RELEASE_0.1.16.md); the [source/control inventory](docs/ANDROID_WINDOWS_PARITY.md) records individual parity gaps. Previous versioned receipts remain historical evidence. Do not confuse implementation with acceptance or promote this development checkpoint to full-parity 1.0.0.
