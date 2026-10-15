# Native Windows icons and visual pacing

Desktop **0.1.11/code12**, October 16, 2026. Original vector paths replace every Segoe MDL2 private glyph and the old GDI heart/graph/wand special cases. No external icon pack, bitmap upscaling, web font or new runtime dependency.

## Drawing contract

31 semantic icons share a 24-unit coordinate system, 1.8-unit rounded outline, consistent optical centers and antialiased GDI+ rasterization at physical DPI. The primary Play/Pause glyph is 30 logical units inside the existing button; navigation glyphs are 24 and row placeholders 26. Hitboxes and accessible action names are unchanged. The original waveform app mark is antialiased, with seven ICO sizes from 16 to 256 pixels. WM_GETICON selects the requested monitor-DPI resource rather than stretching one 64px image.

`--self-test <owned-directory>` renders every icon at 96/144/192/288/384 DPI, checks non-empty ink, antialiased coverage and clipping, and writes contact-sheet PNGs plus vector-icons.txt. All **155** render cases passed locally. Contact sheets were visually inspected. These checks establish scalable geometry, not complete mixed-monitor or physical-display acceptance.

## Presentation and quality

The old 33ms title/16ms decorative timers are removed. A separate worker uses an unnamed high-resolution waitable timer at the current monitor rate, posts at most one pending presentation event and sleeps when inactive. No UI-thread waits or global timer-resolution changes. Marquee positions and the visual seek thumb interpolate in physical floating-point coordinates between playback polls. Retained per-window paint buffers are recreated only on resize; actual sparse update regions are preserved, rather than flattened to their bounding box. Hidden windows and OS reduced-motion preferences suppress decorative animation. Slow palette analysis remains separate from playback and control redraws.

This does **not** set source video resolution, bitrate or FPS, alter audio clock readings, or promise one GDI frame per physical 180 Hz refresh. DWM scheduling, system load, remote sessions and mixed-refresh monitors affect presentation. The real-window probe records monitor/compositor refresh and visual frame count; a physical 180 Hz run is still an explicit acceptance gate. No 180 Hz claim is inferred from a sharp screenshot.

## Failures retained and repaired

The initial hidden-window probe correctly produced no animation; visible acceptance must explicitly show its owned HWND. Enabling animation then exposed WM_TIMER starvation under continuous paints. The visual-event path now services due playback polling independently of that lower-priority timer. Actual visible measurement exposed only **8.2 visual events / 4.7 paints per second**, despite a 180 Hz display: repeated GDI+ radial backdrop rasterization dominated CPU. Native 16-bit interpolated gradient geometry and sparse clipping improved this to **60.0 events / 56.0 paints**; DwmFlush itself was not a reliable 180 Hz clock for this GDI window.

The final candidate uses monitor-derived high-resolution waits plus two bounded **native-destination-resolution** background layers. Soft light centers share cached coverage only within one-eighth of a physical pixel; no video frame or resolution is reduced. Cached layers are limited to 8,388,608 pixels each (64 MiB total at 32-bit); larger destinations use the same full-resolution vector gradient directly. No input media bitmap or proxy is involved. Final measured/installed results must be recorded separately, not extrapolated from the earlier 56 Hz run.

API contracts: [CreateWaitableTimerEx](https://learn.microsoft.com/en-us/windows/win32/api/synchapi/nf-synchapi-createwaitabletimerexw) supports process-local high-resolution timers on Windows 10 1803+; older sessions fall back gracefully. [DwmGetCompositionTimingInfo](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmgetcompositiontiminginfo) is used only for diagnostics, with a null HWND as required on modern Windows.
