# WinUI migration status

The native desktop app remains the fallback during the rebuild.
Do not replace its installer until the required feature gates pass.
Do not remove a working feature to make the new shell appear complete.

| Area | New shell | Remaining gate |
| --- | --- | --- |
| Library | Folder selection, recursive discovery, filter and current-track state | Artwork, metadata, sort modes and saved lists |
| Playback | Existing native engine, pause, seek, previous, next, repeat and speed | Broader format and audio-device acceptance |
| Presentations | Dock, expanded player, Mini and real fullscreen | Multi-monitor, multiple DPI settings and physical pointer acceptance |
| Queue | Fixed transport and separately scrollable queue | Queue editing, saved lists and unified sharing |
| Ambient | Optional colors from the existing decoded-video tap | Full Adaptive controls and broader HDR acceptance |
| Nodes | Existing filename graph and background physics; 209-file layout, weighted nodes, controls and hidden-work checks | Physical gestures, wider library sizes and broader accessibility acceptance |
| Stack | Existing C++ mixer remains in the native implementation | Full WinUI controls, persistence, analysis and shared-clock acceptance |
| Other mobile functions | Retained in the previous desktop implementation where already supported | History, lyrics, A–B, sleep, lock and developer tools in the new shell |
| Accessibility | Native controls and named transport actions | Full keyboard, Narrator and high-contrast acceptance |
| Languages | Initial English interface with Unicode filenames | Migrate and review the existing translations |
| Deployment | Self-contained local preview package | Signed installer and offline clean-machine acceptance |

Android source, package identity, signing material and APKs are not part of this migration.
