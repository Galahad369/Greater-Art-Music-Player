# WinUI desktop rebuild

## Scope and decision

The owner approved WinUI 3 on October 28, 2026. Android is outside this work.
The mobile app provides the product reference. It does not provide desktop window code.

The first gate is real playback through Library, Now Playing, Mini and fullscreen.
The old desktop implementation remains available during this gate. Existing settings and releases must remain unchanged.

## Evidence and recommendation

This is an engineering investigation, not a systematic literature review.
AI agents inspected official documentation, public source code and the local implementation.
No private media files were sent to a third-party service.

| Question | Evidence | Decision and limit |
| --- | --- | --- |
| Which Windows framework? | [Microsoft WinUI quickstart](https://learn.microsoft.com/en-us/windows/apps/get-started/start-here) supports C# and C++. | Use C# for the WinUI shell. Keep the existing C++ playback implementation behind a small C interface. |
| Which SDK channel? | [Microsoft release channels](https://learn.microsoft.com/en-us/windows/apps/windows-app-sdk/release-channels) lists 2.6.4 as Stable, released October 27, 2026. NuGet restore resolved that exact version. | Pin 2.6.4 and commit the package lock. Do not copy experimental package versions from a demonstration app. |
| Can the app stay offline? | [Microsoft self-contained deployment](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/self-contained-deploy/deploy-self-contained-apps) describes local runtime deployment. | Publish both .NET and Windows App SDK as self-contained. The EXE needs its accompanying files. A clean-machine test remains necessary. |
| Can the decoder remain unchanged? | Local `Media.cpp`, `CodecReader.cpp`, `AudioFallback.cpp` and `StackPcm.cpp` provide the current playback services. | Reuse these services. A presentation change must not load the file again or create another player. |
| Can a native video window fit inside WinUI? | [Microsoft windowing guidance](https://learn.microsoft.com/en-us/windows/apps/develop/ui/windowing-overview) distinguishes effective pixels from physical pixels. [SetParent](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setparent) documents style and DPI restrictions. | Use a child HWND in a reserved video area. Convert layout coordinates with the current rasterization scale. Test resize and window transfers. |
| Can effects cover native video? | [SwapChainPanel documentation](https://learn.microsoft.com/en-us/windows/windows-app-sdk/api/winrt/microsoft.ui.xaml.controls.swapchainpanel) describes composition and backdrop limits. | Do not assume that XAML controls can cover a native video child. Keep controls outside the video area. |
| Which existing UI can guide the work? | [WinUI Gallery source](https://github.com/microsoft/WinUI-Gallery) demonstrates native controls. Its [MIT license](https://github.com/microsoft/WinUI-Gallery/blob/main/LICENSE) permits reuse with attribution. | Use the documented control patterns. The new application layout is original; no Gallery source was copied. |

The sources support the chosen framework and deployment method. They do not prove playback continuity.
Only tests of the actual application can provide that evidence.

## Product structure

- Library selection starts playback and keeps Library open.
- The dock opens Now Playing directly.
- Now Playing gives video, identity and queue separate areas.
- The queue uses the remaining height. The transport controls stay visible.
- Speed remains directly available.
- Mini and fullscreen use the same native player.
- Settings use normal native controls with clear labels.

The shell uses native text and icon rendering. No bitmap toolbar icons are required.
Decorative color analysis is optional. It must not replace the original video stream.

## Verification contract

`--verify-flow <local-video> <output-directory>` runs a muted test through the real shell.
The report records player identity, source dimensions, frame counters and playback position.
Separate BMP files record only windows owned by the test process.

Renderer counters do not prove that a visible image exists. A black capture must not be reported as visual acceptance.
Command success does not prove a working seek or rate change. Compare the resulting playback state.

Keep failed reports. Use a new output directory for each run.
Never overwrite a versioned release with another candidate.

## Local acceptance evidence

The native core passed 73 checks. The graph interface passed 43 checks.
The desktop version validator passed nine tests.
The real 1080p and 4K playback flows passed with the same player identity and unchanged source dimensions.
The tests checked seek, pause, resume, measured speed, repeated Mini/fullscreen changes and compact control bounds.
The real library produced 209 nodes and 895 connections. Hidden Nodes rendering stopped.

Windows Graphics Capture showed the actual WinUI layout and video together.
Older GDI captures sometimes omitted the XAML interface. Those failed capture methods remain in local evidence.
No Windows capture privacy setting was changed.

The checks use native control automation and mathematical gesture tests.
They do not prove physical pointer input, acoustic output or monitor refresh rate.

## Remaining acceptance gates

- Extend the locally tested playback flow to more files, displays and failure cases.
- Verify physical pointer input and multiple DPI settings.
- Verify offline launch on a clean Windows machine.
- Complete Stack, Nodes, saved-list and accessibility migration before claiming full desktop parity.
- Preserve the existing Stack and graph regression suites.
- Complete installer signing and physical listening checks separately.

The research review approved the bounded prototype, not an untested full replacement.
No claim of 180 Hz presentation, perfect acoustic alignment or complete security certification follows from this document.
