# Android → Windows parity tracker

October 13, 2026. Initial source inventory, **not an acceptance certificate**.
References are Android `MainViewModel.kt`, `data/AppPreferences.kt`, graph source
and current Settings UI; a complete screen-by-screen acceptance audit is pending.
"Implemented" below means source exists. It does not imply all scenarios passed.

| Capability | Android reference | Windows 0.1.0 | Acceptance still required |
|---|---|---|---|
| Explicit local discovery | `rescan`, scoped tree, exclusions | Chosen filesystem roots; no Android URI migration needed | Cancellation during deep walk, changed/deleted files, watcher, large-library limits |
| Library search and filtering | query, sort mode, custom order, details | Name search and favorites filter | Sort modes, reorder, metadata, extended search, details and viewport performance |
| Media identity | typed source/reference, Unicode paths | Normalized full-path identity; duplicate names retained | Moves, inaccessible files, long paths, mixed-case filesystem behavior |
| Favorites | toggle and persisted favorites playlist | Persisted membership and filtered view | Favorites queue behavior and rapid concurrent edits |
| Local playlists | create, seed, rename, delete, rules, import/export M3U | Missing | All playlist operations and migration/export safety |
| Playback | play, pause, next, previous, stop, seek | Implemented with direct native backend | Repeated transitions, invalid media, audio-only artwork, resume policy |
| Repeat and speed | repeat mode, playback cycle, temporary 2× | Basic loop, rate and volume | Full repeat/shuffle semantics, temporary speed, end conditions |
| Queue editing | queue item play, move/remove, Locate | Library queue only | Editing, search, Locate, active-list identity and queue sharing |
| Stack transport | primary plus ≤7 companions, gain/mute/solo | Real concurrent companion audio and basic controls | Readiness barrier, drift control, primary replacement, repeated loops |
| Stack alignment | backing features, timing maps, manual offsets | Missing; no sync promise | Port pure analysis, native PCM decode, caching and acoustic validation |
| Stack persistence | saved Stack, delete Stack, offsets/scale | Missing | Separate Stack/list saves, restore, errors and missing sources |
| Graph generation | filename similarity, layout/options | Shared pure Android algorithms | Large libraries, persisted graph cache/options |
| Graph interaction | pan/zoom, node drag/play, fit/find, controls | Canvas rendering and zoom slider only | Pan, drag, node play, controls, motion and keyboard alternatives |
| Workspace | screen-based Android navigation | Simultaneous three panes with persisted dividers | DPI/resize stress, narrow layouts, keyboard focus, screen readers |
| Player presentations | docked, overlay, Mini/PiP/fullscreen | Library video and expanded desktop window share primary | Borderless fullscreen, mini/tray and transfer continuity |
| Artwork/metadata | thumbnail repository, current art, overrides | Missing | Native metadata extraction, bounded cache, invalidation and fallback |
| Waveforms | waveform repository and timeline | Simple seek slider only | Decode/cache/render and source-quality-preserving analysis |
| Lyrics and timed controls | lyrics, sleep timer, A–B | Missing | Correct cancellation, timing, secondary panels and localized feedback |
| Appearance | themes, font, row/mini sizes, black disc | One development dark palette | All palette/font choices, accessibility, coherent design tokens |
| Background/Ambient | Crop/Fit, current/custom video, Ambient/Adaptive | Missing | Full-resolution compositor, no Stretch, analysis without duplicate decode |
| Languages | English, Traditional Chinese, Japanese, German, French, Cantonese | English development UI only | Shared/localized strings, long-label layouts, service messages |
| History and audio features | opt-in history/burn, ReplayGain | Missing | Preference privacy defaults, persistence, gain semantics |
| File operations | share media/queue, deletion confirmations | Missing | Safe selected-file scope, confirmations, Windows share/export equivalent |
| Settings | grouped preferences, backup/restore/reset | Roots/favorites/dividers only | Settings groups, backup schema, validation, undo, cache management |
| Developer diagnostics | inspector, crash/report paths | Visible native error message and explicit test probes | Bounded diagnostic panel, crash recovery and sanitized export |
| Windows integration | platform-specific equivalent | Missing | Tray, shortcuts, media keys, taskbar, notifications, single-instance policy |
| Distribution/security | Android source/verified APK ledger | Independent version guard, no public artifact | Bundled JRE/runtime, license review, signing, clean-host startup, installer/uninstaller |

Do not remove missing rows merely because the application compiles. Full parity
requires working UI, persisted state where applicable, negative-path tests and
measured runtime evidence, not placeholder controls or success text.
