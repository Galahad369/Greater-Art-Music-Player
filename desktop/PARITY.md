# Desktop parity and acceptance

Desktop 0.1.1/code2 is an original native Windows development application, not a finished port of all Android features.

| Area | Implemented | Acceptance boundary |
| --- | --- | --- |
| Workspace | IDE activity rail, resizable Stack/Library/Nodes, collapsed Settings, fixed dock | Owned-window screenshot review at this workstation; mixed DPI and all small layouts not accepted |
| Local Library | Selected folders, cancellation, duplicates, Unicode, favorites, sort/filter | Native tests; no URI/Android permission equivalence claim |
| Playback | Windows Media Foundation primary video/audio, seek/rate/volume | Generated 1280×720/30fps fixture rendered; source quality uncapped |
| Stack | Up to eight independent sources, gain/mute/solo, save/load/delete | Six copies of generated fixture played; six different covers and acoustic synchronization NOT verified |
| Nodes | Offline filename graph, sparse edges, caching, gestures and reveal | Math/cache tests; full physical interaction matrix pending |
| Presentations | Dock, expanded, whole-frame fullscreen, borderless detached Mini | Native transition probe passed without media reload |
| Installer | Per-user self-contained setup EXE, Desktop and Programs-group shortcuts | Local install/hash/shortcut and uninstall checks; clean host and signing pending |
| Persistence | Atomic settings, separate saved Stacks, bounded input/cache | Unicode, malformed input and invalidation tests passed |
| Accessibility | Native edit/dialog controls, shortcuts, scalable fonts | Custom GDI controls need UIA/Narrator and complete keyboard-navigation acceptance |
| Localization | Unicode filenames | English interface only; translation parity pending |
| Metadata/artwork | File format/size and placeholders | Embedded artwork, artist extraction and waveform pending |
| Extras | Clipboard path sharing, recoverable exact-file recycling | Lyrics, Ambient, history, full playlists, A–B/sleep and advanced features pending |
| Security | No application networking, selected local sources, static runtime, checked MSI payload | Unsigned; not audited/certified; system-codec and clean-host tests pending |

Do not promote this to full-parity 1.0.0 or claim perfect Stack synchronization. Read the recorded artifact receipt and handoff rather than assuming Android tests validate this implementation.
