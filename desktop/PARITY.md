# Desktop parity and acceptance

Desktop **0.1.2/code3** is an original native Windows **development checkpoint**, not full Android parity. The Android source/runtime remains independently versioned.

| Area | Implemented now | Still needed |
| --- | --- | --- |
| Workspace | IDE rail, resizable Library/Stack/Nodes, collapsed Settings, fixed dock | Small-layout, mixed-DPI and multi-monitor acceptance |
| Library/lists | Local chosen folders, Unicode, favorites, sort/search/reset, separate M3U8 playlists, recoverable triple-confirm deletion | Multi-select, reorder/editor, smart lists, backups, exclusions, CUE and metadata overrides |
| Playback | Native video/audio; bundled source-built audio decoder and original WASAPI fallback; seek/rate/volume | Bundled video-renderer output, format matrix, endpoint reconnection, EQ/ReplayGain |
| Stack | Up to eight sources, gain/mute/solo, saved offsets, readiness barrier, drift trim, confidence-gated RMS/onset alignment | Shared-sample mixing, instrumental/chroma time-warp, different-cover acoustic tests, primary switching, recommendations |
| Presentations | Stable renderer dock/expanded/fullscreen/Mini; same compact metrics and direct transfers | Touch/pull-down, YouTube seek feedback, lock in fullscreen, tray and all monitor scenarios |
| Artwork/cache | Selected artwork/metadata, source-keyed disk waveform cache, bounded worker queue and pressure pause | All-row artwork parity, user cache management, complete memory-pressure/performance acceptance |
| Background | Artwork-color Ambient, custom image Crop(default)/Fit, no Stretch | Dynamic video color sampling, CURRENT_VIDEO and tiled Stack wallpaper |
| Extras | Local LRC, A–B, sleep, resume, history opt-in/burn, audio disc, shuffle | Full Android advanced controls and settings integration |
| Sharing/system | Unified Windows Share options and system media transport | Actual target-app transfer and all media-key scenarios |
| Accessibility | MSAA names/roles/bounds, focus/hit-test alignment, keyboard buttons/sliders, high contrast | Native Narrator/UIA acceptance, graph-node semantics, complete dialog flow |
| Localization/design | Partial English/ZH-TW/JA/DE/FR/YUE labels, fonts/themes, rectangular rows, current-only tap effect | All dialogs/services translated, full appearance/token parity and native-reader review |
| Installer/security | Offline per-user setup/shortcuts, replaceable codec DLLs with exact source/licenses, corruption checks, local crash report | Publisher signing, clean machine, independent license/security review; no certification claim |

## Evidence boundary

Core tests use generated local fixtures. The waveform test detects a known 500 ms delay, and WASAPI tests validate the real endpoint clock/controls. The own-window video probe checks rendered frames, six voices, fullscreen bounds, named control hit tests and dock/Mini transfers. None proves phase-perfect real-world Stack sound.

Final artifact hashes, exact source commit and installed checks are in RELEASE_0.1.2.md when completed. Previous RELEASE_0.1.1.md remains immutable historical evidence. Do not confuse implementation with acceptance or promote this development checkpoint to full-parity 1.0.0.
