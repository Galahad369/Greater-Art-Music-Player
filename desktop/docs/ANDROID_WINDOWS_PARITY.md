# Android → Windows control inventory

Desktop **0.1.11/code12 acceptance candidate**; latest frozen installer remains 0.1.4 until acceptance. Android **1.21.39/code262** is an emulator-verified debug sideload. This is a source/control inventory, **not a completed exhaustive parity audit**. No feature is accepted merely because a button exists. Windows uses original C++/Win32, not the superseded Compose/VLC prototype.

Android paths below are relative to `greater-art/app/src/main/java/com/local/listentomusic/`; Windows paths are relative to `desktop/native/`. “Implemented” means the described narrow behavior exists; the evidence column limits what was actually tested. “Missing” is an explicit unfinished requirement, not permission to drop it. Final artifact evidence is in ../RELEASE_0.1.4.md.

| Feature/control | Android source | Windows implementation | Parity status | Actual evidence / missing acceptance |
| --- | --- | --- | --- | --- |
| Shared Library/Stack/Nodes session | MainViewModel.kt | App.cpp model + Media.cpp actor | Implemented | Real six-voice renderer transfer probe; one primary renderer |
| Three primary views | ui/LibraryScreen.kt; ui/StackScreen.kt; ui/NodesScreen.kt | App.cpp resizable three-pane workspace | Implemented | Owned-window workspace image; narrow/mixed-DPI matrix pending |
| Chosen-folder recursive scan | data/MediaScanner.kt | Core.h scanLibrary | Implemented | Scope, cancellation, duplicate filename and Unicode tests |
| Refresh and filesystem observation | data/LibraryObserver.kt | App.cpp scan action | Partial | Manual refresh exists; filesystem event observation missing |
| Favorites | data/AppPreferences.kt; ui/LibraryScreen.kt | Core.h settings + App.cpp favorite | Implemented | Unicode favorite persistence test |
| Name/date sorting; filter/reset | ui/LibraryScreen.kt; MainViewModel.kt | App.cpp filter/actions | Implemented | Source behavior and own-window rendering; full interaction acceptance pending |
| Custom order; multiselect/batch | ui/LibraryScreen.kt; data/AppPreferences.kt | None | Missing | Drag reorder, selection and batch-to-Stack not implemented |
| Local playlists and M3U8 | data/AppPreferences.kt; MainViewModel.kt | Features.h + Tools.inl | Partial | UTF-8 roundtrip, no overwrite/partial export tests; rename/editor/rules missing |
| Extended search/local overrides | data/AppPreferences.kt | None | Missing | Artist/lyrics search and display overrides not ported |
| CUE/virtual clips | model/CueSheet.kt | None | Missing | Segment identities/clip-boundary playback not ported |
| Metadata/artwork | data/MediaScanner.kt; data/ThumbnailRepository.kt | Assets.inl shell metadata/artwork | Partial | Selected artwork/artist only; all-row, embedded/folder cover matrix pending |
| Row sizes/current marker/tap | ui/LibraryScreen.kt | App.cpp row + compositor-paced Window.inl | Partial | Rectangular rows/current-only marker, 155 DPI icon render checks; thumbnail/detail toggles missing |
| Duplicate/excluded folders/moved files | data/AppPreferences.kt; MainViewModel.kt | Core.h path dedup + load failures | Partial | Same-title files preserved; content duplicate tools/exclusions/recovery UI missing |
| Exact-file deletion | MainViewModel.kt; ui/LibraryScreen.kt | App.cpp sourceMenu/Recycle Bin | Implemented | Three confirmations in source; native Recycle Bin/undo scenario not accepted |
| Real native video | playback/PlaybackService.kt | Media.cpp Media Foundation | Partial | Generated 1280×720 video renders; broad format/device matrix pending |
| Bundled codec audio | playback/PlaybackService.kt | CodecReader.cpp + AudioFallback.cpp | Implemented | Real WAV/Opus WASAPI tests; AIFF production fallback repeated eight times |
| Play/pause/seek/speed/volume | ui/NowPlayingScreen.kt | App.cpp + Media.cpp | Implemented | Endpoint pause/seek/½×/2× and invalid-command checks; audible pitch acceptance pending |
| Repeat/random/off | data/AppPreferences.kt; playback/PlaybackService.kt | App.cpp + Window.inl | Partial | Primary/Stack loop, shuffle and repeat-off logic; all end/queue scenarios pending |
| Resume | data/AppPreferences.kt | Features.h + App.cpp | Implemented | Separate atomic feature persistence; long-running restore scenario pending |
| Editable/persistent queue | MainViewModel.kt; ui/NowPlayingScreen.kt | Visible Library/playlist queue | Partial | Shared order exists; persistent editing/reordering/removal missing |
| Queue Search/Locate/Clear | ui/NowPlayingScreen.kt | None in expanded player | Missing | Library filter is not a substitute for player queue controls |
| A–B practice | playback/PracticeLoop.kt | Features.h Practice + Tools.inl | Implemented | Mark A/B, end-loop and clear math tests; live gesture sequence pending |
| Sleep timer | ui/NowPlayingScreen.kt; MainViewModel.kt | Tools.inl deadline + Window.inl | Partial | Minute-based pause implemented; end-of-track mode missing |
| Seek interval/temporary 2× | playback/TemporaryPlaybackSpeed.kt; ui/PlaybackMotion.kt | Fixed keyboard seek | Partial | Configurable interval, hold gesture and YouTube feedback missing |
| ReplayGain/peak protection | playback/ReplayGain.kt | None | Missing | Endpoint integer clipping is not ReplayGain parity |
| History opt-in/burn | data/AppPreferences.kt | Features.h + Tools.inl | Implemented | Off by default, bounded 2000, Unicode persistence/no off-state write tests |
| Windows media keys | playback/PlaybackWidget.kt | WindowsMedia.inl | Partial | Native SMTC implementation; all hardware keys and target states pending |
| Device switching/reconnection | playback/PlaybackService.kt | WASAPI default device at open | Missing | Active endpoint changes not handled |
| Title/artist/file info | ui/NowPlayingScreen.kt | App.cpp + Assets.inl | Partial | Unicode title, selected artist/album; complete metadata/unknown duration parity pending |
| Unified share options | ui/NowPlayingScreen.kt; MainViewModel.kt | WindowsShare.inl | Partial | Current original, queue M3U8/files; Windows target-app acceptance pending |
| Synced lyrics | model/TimedLyrics.kt; model/LocalLyrics.kt | Features.h LRC + Tools.inl | Partial | Unicode/multistamp/offset/follow tests; richer highlighting/editor missing |
| Black Disc | ui/NowPlayingScreen.kt | App.cpp/Window.inl disc | Partial | Optional rotating audio artwork exists; live artwork/style acceptance pending |
| Fixed controls/first-class speed | ui/NowPlayingScreen.kt | App.cpp fixed footer | Implemented | Owned-window Now Playing screenshot; flexible list never owns transport height |
| Marquee | ui/NowPlayingScreen.kt | App.cpp + FrameClock.h | Partial | Fractional physical positioning, DWM-paced dirty-region redraw, reduced-motion static fallback; physical 180 Hz/mixed-monitor acceptance pending |
| Expanded/fullscreen/lock | playback/FullscreenVideoActivity.kt; ui/NowPlayingScreen.kt | Window.inl + App.cpp | Partial | Deliberately minimized fullscreen regression passes; fullscreen unlock overlay missing |
| Mini same player/no reload | playback/MiniWindowOverlayService.kt | Window.inl stable renderer transfer | Implemented | Six-voice dock/expanded/Mini probe |
| Mini S/M/L, borders, bounds | playback/FloatingOverlaySize.kt | App.cpp compactMetrics + Window.inl | Partial | Borderless size adaptation; full multi-monitor/DPI/resize matrix pending |
| PiP-equivalent/No Floating | playback/PlayerWindowMode.kt | Mini only | Missing | Three persisted presentation choices and tray restore missing |
| Stack up to eight/mute/solo/gain | playback/StackPlaybackCoordinator.kt; ui/StackScreen.kt | StackPcm.cpp + AudioFallback.cpp | Partial | Eight-source/headroom and mute/solo/gain tests; six real decoded sources and own-process WASAPI output; heterogeneous acoustic acceptance pending |
| Stack primary/enable/stop | playback/StackPlayback.kt; ui/StackScreen.kt | First slot primary | Partial | Primary switching and full track enable/stop UI missing |
| Saved Stacks/offsets | data/AppPreferences.kt; ui/StackScreen.kt | Core.h schema02 + App.cpp | Partial | Unicode/gain/mute/offset roundtrip; rename/full editing and all legacy fixtures pending |
| Manual offset controls/shared seek | ui/StackScreen.kt | App.cpp + Alignment.inl + StackPcm.cpp | Partial | Fine offset menu ±1ms/0.1ms/one actual-endpoint sample, fractional-sample persistence/interpolation, epoch-safe shared seek; full interaction acceptance pending |
| Structural sound align | playback/StackAudioAlign.kt | AudioAnalysis.cpp + Alignment.inl | Partial | Actual decoded 500ms delay and ±lag tests; simpler than Android instrument engine |
| Instrument/chroma/fine timing maps | playback/StackInstrumentAlign.kt; playback/StackFineTiming.kt; playback/StackAlignmentMath.kt | None | Missing | No instrumental separation, chroma or linear/nonlinear time-warp parity |
| Start barrier/drift trim | playback/StackSyncControl.kt | Alignment.inl + one prebuffered endpoint | Implemented differently | One audible PCM clock replaces independent rate trims/seeks; only silent primary video follows; rapid-add position/prebuffer checks |
| Shared-clock prototype | playback/SharedClockPcmPrototype.kt; playback/StackPcmStream.kt | StackPcm.cpp + one WASAPI client | Windows production implementation | Calibrated own-process digital capture catches one-sample sabotage; physical acoustic acceptance and automatic cover matching remain separate |
| Alignment progress/cancel/confidence | playback/StackAlignmentController.kt | Alignment.inl generation cancel | Partial | Uncertain offsets preserved; no detailed progress/confidence/cancel UI |
| Stack recommendations | playback/StackRecommend.kt | None | Missing | Not replaced by filename graph |
| Filename graph | graph/FilenameSimilarity.kt; graph/GraphBuilder.kt | Core.h filenameFeatures/buildGraph | Partial | Latin/CJK/shared-letter/sparse tests; full Unicode normalization/candidate-index parity missing |
| Force graph/tap/drag/zoom/wand | graph/GraphLayout.kt; ui/NodesScreen.kt | Core.h + App.cpp | Partial | Real nodes/weighted rendering; complete gesture/large-library acceptance pending |
| Graph options/cache | graph/GraphOptions.kt; data/GraphRepository.kt | Core.h graph-v2 + Tools.inl | Partial | Invalidation, neighbor/threshold settings; labels/orphans/edge/group options missing |
| Classic video/artwork Ambient | ui/components/VideoAmbientColors.kt | VideoAmbient.cpp read-only MFT + Ambient.inl + Assets.inl | Partial | Actual GPU red/blue/green, unchanged input sample/player/source, pause/disabled hold and smooth palette; broad format/HDR acceptance pending |
| Aurora/Bloom/Flow | ui/components/AdaptiveAmbientEngine.kt; ui/components/AdaptiveAmbientBackdrop.kt | Ambient.inl + App.cpp lighting | Partial | Restrained brightness/chroma-reactive broad lighting; complete four-layer intensity/react/motion controls remain unfinished |
| Custom image Crop/Fit | data/AppPreferences.kt; ui/SettingsScreen.kt | Assets.inl + App.cpp | Partial | Aspect-preserving custom image; dim/intensity controls and background decode scheduling pending |
| Custom/current video + Stack tiles | ui/SettingsScreen.kt; playback/ParallelPlayback.kt | None | Missing | No extra video decoder introduced as a fake color/background workaround |
| Themes/fonts/high contrast | ui/theme/Theme.kt; data/AppPreferences.kt | App.cpp + Features.h | Partial | Three palettes, light/dark/high contrast, four fonts; system theme/all eight accents/Void parity missing |
| Six languages | data/AppPreferences.kt; ui/SettingsScreen.kt | Language.h | Partial | Five non-English Library label tests; most dialogs still need translation/native review |
| Preferences/backups/reset | data/AppPreferences.kt; ui/SettingsScreen.kt | Core.h + Features.h atomic schemas | Partial | Malformed input/Unicode tests; full import/export/reset UI missing |
| Wave/cache/performance | data/WaveformRepository.kt; data/ThumbnailRepository.kt | Assets.inl + WorkQueue.h | Partial | Source-keyed wave cache, bounded workers/disk; user cache controls and row artwork parity missing |
| Developer/inspection/local crashes | ui/DeveloperDiagnostics.kt; playback/VideoDiagnostics.kt | Accessibility.inl + Tools.inl + Crash.h | Partial | Ctrl+click element report/local exception file; quick disable/export/cross-device diagnostics pending |
| Keyboard/accessibility | ui/theme/DesignSystem.kt; all screens | Accessibility.inl + Window.inl | Partial | Named MSAA hit tests pass; Narrator, graph semantics and complete native focus flow pending |
| Tray/drop/filesystem/native shortcuts | Windows adaptation requirement | Native dialogs/context/SMTC | Partial | Folder dialog/context/shortcuts exist; tray, drag-drop, remapping and device picker missing |
| Offline installer/portable | Windows adaptation requirement | Setup.cpp; Product.wxs; package.ps1 | Partial | Per-user install/upgrade + hashes/shortcuts tested locally; clean-machine/signing remain open |

## Next implementation stages

1. The original decoder now supplies live colors through an optional pass-through effect, with no secondary decoder or renderer replacement. Complete its hardware/format/recovery acceptance and the full Adaptive controls; bundled **video** output/format parity remains separate work.
2. Add queue Search/Locate/editing, all-row artwork, filesystem observation and full translated settings with backup/cache controls.
3. The Windows shared-clock output now exists and has calibrated digital fixture proof. Port multi-anchor instrumental timing maps only with decoded-media tests and acoustic acceptance; then tiled Stack video and missing presentation choices.
4. Complete native accessibility, clean-machine/mixed-DPI/endpoint tests and owner-approved signing. Audit every remaining Android control before claiming 1.0.0 parity.
