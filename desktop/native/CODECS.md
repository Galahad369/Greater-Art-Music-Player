# Bundled codec boundary

Greater Art uses its own Win32 UI and playback coordinator. No VLC, FFmpeg command-line program, Java runtime, external player or runtime download is required. Media Foundation remains the primary video renderer. Unsupported **audio** can use an original WASAPI output path and dynamically linked FFmpeg libraries. Failed video is never disguised as audio-only success.

## Reproducible source

- FFmpeg **9.0.2**, vcpkg port **9.0.2#1**.
- vcpkg registry baseline: `e456309491fd875487bf02b8a0dca76c1b1b00cc`.
- Only avcodec, avformat, avfilter, swresample and swscale features; defaults disabled. No GPL/nonfree features or external codec packages.
- Custom triplet uses dynamic codec libraries and static MSVC CRT. Build tools do not become app dependencies.
- `build-codecs.ps1` builds source locally. A temporary SUBST drive avoids FFmpeg's space-in-path limitation; it copies no checkout and removes only its own unchanged mapping.
- `prepare-codec-package.ps1` preserves the exact patched source tree, generated configuration, source archive hash, port patches, build recipe and license alongside the binaries. Packaging must fail if these are absent.

## License and replacement

This configuration reports **LGPL version 2.1 or later**, not GPL. The codec copyright/license text and corresponding source archive are installed in `Codecs`. Compatible modified DLLs may replace the bundled ones; the runtime has no checksum lock preventing that. Installer integrity checks are not a prohibition on replacing libraries after installation. These library terms do not change the Android edition's license.

Review the [FFmpeg legal checklist](https://ffmpeg.org/legal.html) before public distribution. Publisher signing, an independent license/security assessment and clean-machine acceptance remain separate gates. A local development package is not certification.

## Quality and security

Local file streams only. URLs, UNC roots and alternate data streams are rejected. A custom AVIO stream reads Unicode paths; container/protocol allowlists reject network and playlist demuxers. No executable subprocess decodes app media. No source resolution, bitrate or FPS cap, proxies or transcoding are used. Audio conversion to the actual Windows endpoint format is necessary device output conversion, not a lower-quality source copy. Speed processing uses in-process atempo.

## Current limits

The bundled video-decoder output is not yet connected to the native renderer. Native Windows video formats still vary by PC. WASAPI default-device changes and mixed-codec Stack acoustic acceptance are unverified. Do not describe this checkpoint as complete Android format parity or sample-perfect synchronization.
