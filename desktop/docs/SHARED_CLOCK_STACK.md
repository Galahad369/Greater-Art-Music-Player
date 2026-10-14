# Windows Stack — one audible clock

Desktop 0.1.10/code11 implementation candidate, October 15, 2026. Android source/APKs are unchanged. This stage replaces independent audible Windows players; it does not certify arbitrary cover-song matching or full Windows parity.

## Production route

Local files → existing reviewed in-process CodecReader → bounded StackPcm producer → fractional offsets and headroom-safe float mix → one global pitch-preserving tempo processor → one event-driven WASAPI client.

Two to eight voices use the same integer master frame. Positive offsets read later source samples; negative offsets contribute silence until their entrance. Fractional offsets use a normalized 32-tap windowed-sinc interpolator, not rounded millisecond seeks. Sources convert only to the actual endpoint rate/channel layout; there is no artificial quality cap. The existing full-resolution native primary video is silent and follows this common audio clock. Companions no longer create their own Media Engines or output clients. Single-track playback retains its existing route.

The producer waits for all required source data before publishing a block, with six 1024-frame blocks maximum queued. Seek epochs discard stale blocks. Mute/solo/gain changes do not reopen sources or reset the clock. Offset changes use one coordinated seek; ordinary playback never independently rate-trims or seeks audible voices. Whole-Stack loop appends the primary opening to the same PCM stream without an audio endpoint restart. Longer companions cannot extend the primary boundary. Short/exhausted companions contribute silence.

Add/remove and saved-Stack load prepare a new common source set. Saved loads batch preparation once, after restoring gains/offsets. Playback fails closed if a required voice cannot decode; it does not silently return to loose independent players. Settings, Library, Mini and fullscreen share the same transport/presentation state.

## Output proof, not position counters

- `--self-test <directory>` includes real-codec six-voice integer offsets, half-sample interpolation, delayed entrances, endpoint-channel conversion, epoch-safe seek, primary EOF and three contiguous group loops.
- `--verify-stack-audio <fixture> <directory>` exercises real WASAPI play/pause/seek/0.5×/2× and reports endpoint format and underruns.
- `--verify-stack-output <directory>` generates six independent deterministic local signals plus a calibrated sum. Windows process-loopback captures only this test process, never the microphone or other applications. It compares the six-source output against calibration and must detect a deliberately introduced **one-sample error** in one voice. Capture latency may differ globally; relative voice errors may not.
- Existing real-video Ambient, effect-fault and fullscreen/Mini/dock probes must still pass with the same primary source dimensions/identity. Output captures are digital Windows-path evidence, not a physical speaker/microphone measurement.

Initial captured output passed: normalized comparison error 1.3411e-7; one-sample sabotage error 0.550466; zero output underruns during that capture. The separate six-MP4 speed/seek test then exposed 17 underruns. These failed results remain under native/build/pcm-011-controls; they are not discarded as noise. Final acceptance after the scheduling repair must be recorded separately.

## Mistake and prevention

Feeding only one 1024-frame block per `Sleep(3)` loop is not a 3ms scheduling guarantee. Windows timer granularity can stretch that wait, starving a 2×/4× stream even when decoder throughput is sufficient. The renderer now batches ready input ahead, prebuffers the endpoint before starting, and follows WASAPI events with MMCSS Audio scheduling. No resolution, bitrate, sample-rate, FPS or decoder-quality reduction is used to hide the problem. Buffer thresholds and final underrun results must be exercised on an actual endpoint, not inferred from a model.

References: [WASAPI buffer ownership](https://learn.microsoft.com/en-us/windows/win32/api/audioclient/nf-audioclient-iaudiorenderclient-getbuffer), [Microsoft process-specific loopback API/sample](https://learn.microsoft.com/en-us/samples/microsoft/windows-classic-samples/applicationloopbackaudio-sample/).

## Remaining boundaries

Shared-clock mixing guarantees a common output schedule for supplied offsets. It cannot infer identical musical timing from different performances, remove vocals, or make a weak RMS/onset match trustworthy. Existing sound analysis remains confidence-gated; multi-anchor instrumental/time-warp parity is still pending. Endpoint unplug/reconnect recovery, mixed-rate compressed-file acoustic acceptance and full Windows parity require further work. Do not relabel this as 1.0.0/full parity, universal sample-perfect song alignment, crash-free or production-signed.
