# Android maintenance sideload — 1.21.93 / code316

Date: **30 October 2026**. This is a locally verified, optimized maintenance
sideload, not a Play Store release, security certification or full Stack
shared-output acceptance.

## Exact artifact

| Field | Value |
| --- | --- |
| APK | [`releases/GreaterArt-1.21.93.apk`](../releases/GreaterArt-1.21.93.apk) |
| Application ID | `com.local.listentomusic` |
| Version | `1.21.93`, code `316` |
| Runtime source commit | `58048b54e4119c3aad50ca99d4595afe479653c3` |
| Size | `5,478,717 bytes` |
| SHA-256 | `cee3f50632cdf186a1b58304c195378c1917284f20ce04d354c3549ab180f6cc` |
| Certificate SHA-256 | `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf` |
| Variant | R8-minified `release`, non-debuggable |
| Shared-output prototype | `STACK_PCM_PROTOTYPE=false` |

The source commit is recorded externally here. The APK's generated VCS entry
reports `NO_SUPPORTED_VCS_FOUND`; it does **not** embed a usable commit SHA.
Only documentation and this immutable APK are added after that runtime commit.
No runtime, resource, dependency, manifest, build or test source changes are
part of release finalization.

## Included work

- Library: compact ordinary artwork, an8dp indentation only on the current
  artwork, and a separate6dp dot. No PlayArrow badge. Retain aligned titles,
  row actions, accessible state and the keyboard-safe dock.
- Now Playing:16sp title,22dp action glyphs,48dp Play/Pause and at least48dp
  touch targets. Retain every control, video geometry and the flexible queue.
- Ambient: stronger colour-driven glow, reflected-light ribbons and edges
  across Library, Nodes, Stack and Now Playing. Reuse existing sampled colours
  and cached artwork. No extra video decoder or source-quality limit.
- The current-media title holder is transparent in video and audio layouts.
  Only its fill, outline and clipping were removed; the transport panel remains.
- Preserve the reviewed desktop history, without new desktop runtime edits.
  Reject the uncommitted Android graph anchor/test deletion that did not match
  desktop physics. Its original diff remains in the private integration backup.

## Build and artifact checks

Java21 and the repository's hash-pinned Gradle9.8.2 wrapper completed:

```text
testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
--offline --no-daemon --max-workers=1 --no-parallel
```

Invoke the wrapper with
`java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain`.
The combined build passed in7m03s. There were473 JVM tests with zero failures,
errors or skips. Lint reported0 errors,47 warnings and4 hints; warnings were
not hidden or represented as a clean lint report.

Use explicit process-scoped `ANDROID_RELEASE_*` signing configuration pointing
to the existing owner-held sideload key. No key was generated, replaced,
exported or committed. `apksigner verify --print-certs`, `aapt dump badging`,
ZIP CRC verification and `zipalign -c -P 16 4` passed. The package/version and
pinned certificate match. Neither the manifest nor installed package is
debuggable. The manifest contains no `INTERNET` permission. The much smaller
APK is an optimized variant, not lower-quality media or a renamed debug APK.

## Emulator evidence

Device: owned `GreaterArt_Scroll_QA36`, Android16/API36,1080x2340 at450dpi.
This is emulator evidence, not physical-phone acceptance.

**Debug instrumentation:23/23 passed in520.008s.**

- ReflectedAmbientUiTest:6
- AdaptiveAmbientUiTest:3
- CompactNowPlayingTest:6
- LibraryImeDockTest:2
- AmbientSurfaceJourneyTest:1
- AdaptiveAmbientDeviceFlowTest:1
- FullscreenToolbarTest:4

These exercise narrow audio/video layouts, light/dark title transparency,
48dp hit targets, compact callbacks, fixed transport during queue scrolling,
actual audio/video IME dock continuity, existing video colour sampling,
Library/Nodes/Stack/expanded navigation, two Stack voices and fullscreen
toolbar controls. All six new compact screenshots were opened and visually
inspected. Pixel checks compare the title holder's blank interior to the same
backdrop outside it. Component captures are not proof of system-overlay insets.

**Separate optimized-APK native smoke:** install with `adb install -r` over the
same-version debug test installation succeeded without clearing data. The
installed package reports1.21.93/code316 with no DEBUGGABLE flag. Library
discovers413 local test files. A real Baka cover MP4 reaches PLAYING with no
media-session error. The dock and actual expanded system overlay were captured
and inspected: the video preserves its aspect, the title has no card, the
toolbar respects the status area, and seek/transport/speed stay above navigation.
Favourite changes to its selected state. Share presents distinct current-file
and queue-as-M3U8 options from one button. No share was sent.
The directly accessible speed menu opens and1.25x is reflected by the player.
Fullscreen opens in portrait with the full square video frame and letterboxing;
the actual capture was inspected. Android Back returns to the expanded player.
This bounded smoke is not the complete rotation/gesture/PiP regression matrix.

Local evidence is retained under the sibling workspace
`output/maintenance-release-20261030/`: build/test logs, six compact captures,
native release XML/screenshots and the independently checked candidate bytes.
The earlier Ambient/Library test runs, including rejected captures and failures,
remain in their original dated evidence folders.

On a later emulator restart, Android System UI displayed an ANR dialog. This
is retained as rejected startup evidence; app exit records showed no new app
crash or ANR for that restart. Older force-stop, low-memory and historical ANR
records are not erased. Do not call this normal-device startup or performance
acceptance, and do not infer universal crash freedom from an empty crash buffer.

## Organization and prevention

- One canonical source checkout and one canonical latest APK path. Archive21
  ignored loose screenshots/logs into the existing Android screenshots/logs
  folders after exact-path and SHA-256 checks. Repair the old inspection links.
  No original source, historical release, key, fixture or unique backup is deleted.
- Preserve56 changed/untracked files in the existing private integration backup
  before consolidation. All31 previously dirty desktop files match merged PR212.
  Preserve the exact temporary desktop stash under a private archive ref before
  dropping its active stash entry. Do not publish private recovery refs.
- Keep sequential versioned source commits, exact-source build provenance and
  bounded test results. Do not relabel an old APK, weaken assertions or accept
  screenshot filenames without opening the images.
- Set Java21 explicitly for Android signature tooling; the workstation's default
  Oracle Java8 launcher stalled during a repeat check. Stop that check and rerun
  with the existing Java21 executable, not a new key or toolchain. Set `AAPT`
  to the installed build-tools executable for the Windows release guard, whose
  SDK-directory fallback does not discover `aapt.exe` by itself.
- The user approved this maintenance release with shared output disabled.
  Preserve the [separate parity gate and192kHz failure](STACK_SHARED_OUTPUT_REFINEMENT_2026-10-28.md).
- Public CI and local secret/privacy checks are evidence, not certification or
  a claim that historical exposure has been repaired.

## Remaining acceptance gates

Physical Samsung playback/rotation/cutouts, complete fullscreen/PiP/surface
regressions, receiver-specific sharing, measured GPU/frame-time/battery results,
physical acoustic Stack alignment, full shared-output quality/function parity,
modern production signing, storage-permission cutover and historical privacy
remediation remain separate work. No resolution/FPS/bitrate cap or decoder
fallback was added to conceal lag.

Do not trigger the tag-release workflow to rebuild this artifact: its separate
GitHub signing identity has not been proven update-compatible. This delivery
uses the verified local bytes and existing sideload certificate only. Preserve
the earlier1.21.80 APK unchanged; its SHA-256 remains
`76791737701736a45b9b441851212f2c8cacf83542991b5c0e58ffbbfc3d29b4`.
