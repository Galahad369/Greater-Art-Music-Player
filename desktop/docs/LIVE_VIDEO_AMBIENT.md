# Windows live-video Ambient — acceptance

Desktop 0.1.9/code10 implementation candidate. Android 1.21.39 and frozen artifacts remain unchanged. This is a Windows parity stage, not full parity or shared-sample Stack audio.

## Ownership and quality

VideoAmbient.cpp implements an original optional synchronous Media Foundation transform inside the existing primary Media Engine. Output is the exact input IMFSample: payload, identity, timestamps and duration are retained. No second decoder/player, screen capture, source resizing, FPS/bitrate limit or software-decoder fallback is introduced for colors.

The D3D-aware transform copies NV12/P010/BGRA input to one reused staging texture about every 900ms. Readback uses D3D11_MAP_FLAG_DO_NOT_WAIT; busy GPU data is deferred while the original frame passes through. Only 36 pixels are read for two regions and approximate brightness/chroma. P010 precision reduction affects palette estimation only, never the original video. Tagged PQ/HLG inputs cannot negotiate an 8-bit analysis format. Analysis memory is bounded independently of playback. Existing GPU multithread protection is required, not changed. CPU buffers have length/stride checks. Unsupported/protected sampling retains artwork fallback; real HDR display/color fidelity is not yet accepted.

References: [optional effects](https://learn.microsoft.com/en-us/windows/win32/api/mfmediaengine/nf-mfmediaengine-imfmediaengineex-insertvideoeffect), [D3D-aware transforms](https://learn.microsoft.com/en-us/windows/win32/medfound/direct3d-aware-mfts).

## Presentation

Solid, Classic Ambient, Adaptive Ambient and Custom Image remain distinct. Preferences persist; new labels cover six languages. Classic uses live video with artwork fallback. Adaptive adds restrained brightness/chroma-weighted broad lighting. Palette fades take 1.6s; unchanged samples do not restart them. Reduced-motion preference stops decorative movement. Media changes clear old colors; publication is generation-checked. Hidden/high-contrast/solid/custom-image states disable sampling; pause stops input frames. Crop/Fit and surface ownership remain unchanged.

The full Android intensity/react/motion controls, all Aurora/Bloom/Flow behavior and tiled-video backgrounds remain unfinished.

## Verification and mistakes

- Synthetic regressions check exact original sample, bytes, time/duration, bounded admission, flush, stale generation and disabled publication.
- --verify-ambient requires real decoded/GPU red→blue→green colors from the owned 1280×720 fixture, unchanged player identity, pause hold and disabled-analysis hold. Artwork-only colors cannot pass.
- The independent six-voice fullscreen/Mini/dock probe is preserved. Frames/player positions are not acoustic synchronization proof.
- Compilation caught SDK flag-name errors. The initial real probe failed before rendering; format enumeration and fixture preference/visibility setup were corrected. Synthetic tests alone missed that integration defect.
- A once-per-resource recovery removes the optional effect if its native pipeline fails. The explicit owned-fixture fault probe must pass before claiming recovery verified; ordinary navigation never reloads playback.

Evidence is in ignored native/build/ambient-019-* directories. Record final exact-head CI/installed checks as observed; never overwrite older releases. Next: one bounded PCM mixer/one WASAPI clock, then remaining queue/settings/format/accessibility parity.

October 15 local acceptance: 57/57 native core checks, including lifecycle gating, retained-buffer ownership, output-first negotiation and synthetic HDR precision/payload regressions. The final real-video probe reported five GPU palette publications, 158 tap input frames, source 1280×720, unchanged primary and pause/disabled holds. Explicit injected-effect failure recovered native video with the same identity/position/source; the six-voice transfer probe reported 224 rendered frames and passed fullscreen bounds, Mini/dock transfers and MSAA checks. Evidence: native/build/reviewed-019-{core,ambient,ambient-fault,transfer}. These are owned SDR-fixture results, not universal compatibility or acoustic proof. Installer and exact-head hosted CI remain separate acceptance gates.
