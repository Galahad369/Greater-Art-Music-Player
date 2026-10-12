# Greater Art Desktop

**0.1.0 / code 1 — local development milestone, not an Android-parity release.**

Windows desktop work is isolated from Android on `feat/greater-art-windows-desktop`.
There is no published installer, no approved desktop license change, and no claim
that the existing Android feature set has been ported completely.

## Implemented

- Compose desktop with simultaneous Stack, Library and Nodes panes.
- Explicitly selected local folders; recursive discovery without following links
  outside the selected roots; Unicode filenames and distinct file identities.
- One native libVLC primary player shared by Library preview and expanded player.
- Real local video/audio, seek, pause/resume, volume, playback speed and loop.
- Up to seven additional audio companions, with gain, mute, solo and removal.
- Persistent selected roots, favorites and pane widths; draggable dividers.
- Filename graph computed using the unchanged pure Android graph algorithms.

Companion sources currently provide audio only. They do **not** have Android's
alignment engine, saved Stacks, readiness barriers or drift correction. Different
takes are not promised to be synchronized. Expanded playback is a separate desktop
window; maximizing it is not yet exclusive/borderless fullscreen.

## Local build

Requires JDK 21 and a reviewed 64-bit VLC runtime. No runtime is downloaded by the
application. For this development milestone, point `GREATER_ART_VLC` to an existing
VLC installation containing `libvlc.dll`, `libvlccore.dll` and `plugins/`.

From `desktop/` in PowerShell:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/openjdk/jdk-21.0.8'
$env:GREATER_ART_VLC = 'C:/Program Files/VideoLAN/VLC'
& "$env:JAVA_HOME/bin/java.exe" -classpath ../greater-art/gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:test :app:run --no-daemon --max-workers=2
```

If Java rejects a certificate that Windows trusts, diagnose the TLS chain first.
The tested local workaround uses the existing Windows root store for that process:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

Do not disable certificate verification, modify global trust stores blindly, or
put credentials into Gradle properties.

## Verification and remaining gates

On October 13, 2026, three JVM tests passed and the real native backend probe
passed using an owned 24-second 1280×720 video fixture with audio and VLC 3.0.24:
play, pause, seek/resume, speed, volume, simultaneous companion playback, continued
playback while minimized, and native cleanup. PNG snapshots before and after seek
contained non-black decoded frames at the fixture's original 1280×720 dimensions.

Those results are **not** acoustic synchronization measurements, continuous frame
pacing measurements, or complete Compose-window/device acceptance. D3D11 decoder
warnings occurred during the probe and remain under investigation. No native
resolution, bitrate or frame-rate limit was added.

The local self-contained app image also built successfully. Its launcher backend
probe exited 0 with development runtime/JDK environment overrides cleared, using
the bundled resources. This is not a clean-machine or full-workspace acceptance
test, and the app image is not a signed installer or a full-parity release.

Before public distribution: resolve license obligations for the complete chosen
runtime/plugins, bundle the runtime and JRE, verify offline packaged startup,
review installer signing, and complete the feature-parity and regression gates.
Android is not being relicensed. Public distribution remains on hold until the
owner has enough information to approve a desktop licensing route.

See [HANDOFF.md](HANDOFF.md) for the exact continuation state.
The initial [parity tracker](PARITY.md) distinguishes implemented behavior from
missing functionality and unverified acceptance.

## Local packaging (not publishing)

From `desktop/`, explicitly run `./packaging/prepare-runtime.ps1`. It downloads the
pinned official VLC 3.0.24 archive, supports an interrupted-download resume, checks
SHA-256 and archive boundaries, and refuses to overwrite an existing runtime.
Then run the Gradle wrapper command above with `:app:createDistributable` instead
of `:app:test :app:run`. The app image includes the JRE and native resources.
Running `:app:packageExe` is a separate installer step, not a public release.
Do not copy a lone launcher EXE without its adjacent `app/` and `runtime/` trees.
Packaged backend verification uses `GreaterArtDesktop.exe --backend-probe <owned-fixture>`;
it is an opt-in actual playback test, not full workspace or acoustic acceptance.
