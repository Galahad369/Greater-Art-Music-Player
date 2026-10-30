# WinUI migration status

Current source: **0.1.21/code22 — LOCALLY VERIFIED PREVIEW**. Native self-tests passed **81/81**, bridge checks **15,378 assertions**, pure-model checks **85/85** and version tests **9/9**. Both final real 1080p and 4K runs passed all **16** exploration-control, **6** transform/ABI and **4** isolated preference checks each on **209 nodes / 895 edges**, with no failed checks. The bundle remained unchanged throughout the runs; final captures were visually inspected after the Canvas and weighted-radius fixes. See [the completed verification receipt and exact hashes](../winui/VERIFICATION_0.1.21.md), with local binary/capture evidence at `desktop/winui/artifacts/nodes-0.1.21-delivery-check`. Delivery is a runnable publish folder, not a newly frozen ZIP or installer. Historical native-delivery evidence remains separate.

Visual A/B acceptance caught a bug that numeric passes missed: Grid clipped negative-world Path geometry before the camera transform even though all 209 nodes projected into the viewport. Hosting the world Paths in Canvas preserves every quadrant; owned captures confirmed all nodes visible. Keep actual rendered captures as an acceptance gate, not just projection/ABI checks.

The native desktop app remains the fallback during the rebuild.
Do not replace its installer until the required feature gates pass.
Do not remove a working feature to make the new shell appear complete.

| Area | New shell | Remaining gate |
| --- | --- | --- |
| Library | Folder selection, recursive discovery, filter and current-track state | Artwork, metadata, sort modes and saved lists |
| Playback | Existing native engine, pause, seek, previous, next, repeat and speed; local transfer runs retained original playback identity without quality caps | Broader format and audio-device acceptance; muted checks do not prove acoustic output |
| Presentations | Dock, expanded player, Mini and real fullscreen; local Mini/fullscreen/repeat transfers passed at 1080p and 4K | Multi-monitor, multiple DPI settings and physical pointer acceptance |
| Queue | Fixed transport and separately scrollable queue | Queue editing, saved lists and unified sharing |
| Ambient | Optional colors from the existing decoded-video tap | Full Adaptive controls and broader HDR acceptance |
| Nodes | Existing filename graph and background physics; final drag position, coprime 384-distinct-other sampling above 1,200 songs, graph cache v3, compact filters/groups/display/forces controls, zoom-aware labels, local-neighborhood focus, keyboard/pan access and separate WinUI preferences; Canvas-hosted world geometry and hub radius floor fix visible graph coverage/readability; final 16-check exploration and capture acceptance passed locally | Physical gestures, wider real libraries, Narrator/high-contrast acceptance and broader accessibility |
| Stack | Existing C++ mixer remains in the native implementation | Full WinUI controls, persistence, analysis and shared-clock acceptance |
| Other mobile functions | Retained in the previous desktop implementation where already supported | History, lyrics, A–B, sleep, lock and developer tools in the new shell |
| Accessibility | Native controls, named transport actions and locally checked Nodes keyboard/pan paths | Full keyboard, Narrator and high-contrast acceptance |
| Languages | Initial English interface with Unicode filenames | Migrate and review the existing translations |
| Deployment | Self-contained runnable publish folder launches with explicitly published compiled XBF/PRI resources; exact tested hashes and final captures recorded | Frozen ZIP/installer, signing and offline clean-machine acceptance |

This remains a preview, not full parity or a production release. Android source, package identity, signing material and APKs are not part of this desktop migration. Preserve concurrent Android work from the other chat.
