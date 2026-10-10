# Branch and issue review — 10 October 2026

Source **1.21.26 / code 249 — SOURCE_ONLY**. Download stays owner-confirmed
**1.21.18 / code 241**. No binary was replaced or retroactively certified.

## Compatibility decisions

- PR #153, fceb8e9: retain unique default-off shared-clock PCM harness and stereo
  kernel; NOT a production audio replacement. Cold eight-voice deadline misses
  prevent adoption. Exclusive audio lease, native format admission and tempo
  abstention stay intact. Issue #146 remains open.
- PR #155, 6645e91: retain read-only scoped-folder preview. It does not redirect
  Library/playback or replace All Files Access. Repair superseded-preview
  cancellation, propagate cancellation and check between folders/documents.
  Blocking provider queries remain provider-dependent. Issue #148 remains open.
- Retain Claude adaptive independent-player sync and its measured limitations.
- Retain 501336c's unique backlog bootstrap document as historical context; its
  baseline is obsolete. Three Copilot heads are already ancestors of main.
- Preserve both parallel 1.21.24/code247 histories and SAF 1.21.25/code248.
  Fresh integration identity avoids relabelling old code or binaries.

## Repairs

Partial wallpaper reveal accepts horizontal drag. Pager pan sync freezes during
reveal. Fit/Crop stay aspect-preserving; only foreground translates, never the
wallpaper viewport. No Stretch option or source quality cap. Actual Fit distortion
reproduction still needs device evidence.

Independent Mini S/M/L uses 1/1.25/1.5 scaling of its old borderless 103×56 dp maximum
footprint. Dock preview and content insets match. Library Small follows Mini;
Medium/Large scale artwork by 1.2/1.4. Previously thumbnail size ignored row choice.

All Settings sections start collapsed, including experimental SAF. Hidden controls
are not composed. Palette labels identify colours and chips show actual accents.
Ambient background uses existing compositor samples and soft gradient, without
an extra decoder/audio stream. No usable video sample means a neutral fallback.

## Issue disposition

| Issue | Remaining work |
| --- | --- |
| #23 | Production signing/release proof and admin protections; no secrets read or protection bypass. |
| #24 | Coordinated protected-history rewrite, explicit authority and backup; current audit is not historical path clearance. |
| #146 | Sustained eight-voice benchmark, production host, physical acoustics. Prototype only. |
| #147 | Physical-phone matrix; emulator cannot close it. |
| #148 | Full URI playback/cache/share/delete migration. Preview only. |
| #149 | Independent High Contrast, adaptive accent and launch transition. Void and clearer Settings retained. |
| #150 | Tracker stays open while linked work remains. |
| #158 | Ten artifacts inventoried below; preserve immutable history, no retroactive verification. |

## APK inventory (#158)

Read-only SHA-256, aapt manifest, apksigner and 16 KiB zipalign audit. All ten have
package com.local.listentomusic, valid signatures/alignment and certificate
9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf.
This proves binary identity/integrity, NOT source provenance or runtime quality.
**1.21.4 and 1.21.10 are mislabelled; do not distribute under those names.**

| File | Manifest version / code | Bytes | SHA-256 | Disposition |
| --- | --- | ---: | --- | --- |
| GreaterArt-1.21.2.apk | 1.21.2 / 225 | 26582677 | `c91c0e03bb9d2c6db7831e14902e9e88bb9748b43ff404b628e8097b0814162d` | Historical provenance unverified |
| GreaterArt-1.21.4.apk | 1.21.3 / 226 | 26582677 | `2f1288a7c4059c0c9574531a501afc159b92e909286cda3b3fa6fa9113305765` | MISLABELLED — retain, do not distribute |
| GreaterArt-1.21.5.apk | 1.21.5 / 228 | 26582677 | `fd125e071d4dc80516097cff90e3cdd3536d5585afa138d04e77c95ab9c05eb0` | Historical provenance unverified |
| GreaterArt-1.21.7.apk | 1.21.7 / 230 | 26582677 | `317823af0160505481ee8ddcade2ef26dc625a1d7cfb69dae638d1cc175f33e7` | Historical provenance unverified |
| GreaterArt-1.21.8.apk | 1.21.8 / 231 | 26599061 | `265b0ea32f52ab915f67431fbbd133e99d049c92a6c0acc600fb5fe8616f2a8f` | Historical provenance unverified |
| GreaterArt-1.21.10.apk | 1.21.9 / 232 | 26599061 | `6aa37408f47c9dca493951006a38c4a172d4506b87c7251fb60ae3c3158ff98a` | MISLABELLED — retain, do not distribute |
| GreaterArt-1.21.12.apk | 1.21.12 / 235 | 26599061 | `7e42e33b8aed3c244b8d150ef40ae551d60676b131599f24f352f53e7f37d3fa` | Historical provenance unverified |
| GreaterArt-1.21.15.apk | 1.21.15 / 238 | 26615449 | `ccdfe90b0dce25d3e08824c7d13d2cba074fc5bac7b3e48e0865120248cf5f81` | Historical provenance unverified |
| GreaterArt-1.21.16.apk | 1.21.16 / 239 | 26615445 | `d5ad25110fab1011e959e40025b451cdde7308535683618ab5ff58e8ecf8d07b` | Historical provenance unverified |
| GreaterArt-1.21.18.apk | 1.21.18 / 241 | 27746006 | `bba973d127a9dec1f03b29fca1a146707c9feee82497e9a65ab196870b696b3d` | Owner-confirmed current artifact |

## Verification

Final localized 1.21.26 source: 300 JVM tests, zero failures; lint zero errors,
30 warnings / 2 hints; app and instrumentation APK assembly pass. API36 default-off
PCM admission/native decoding plus cache run reports OK (6 tests; the enabled-only
prototype case is intentionally skipped). Final-source six-real-take production
seek/resume/whole-group loop regression passes in 13.638 seconds with its original
assertions. No threshold was loosened.

A55-sized 1080×2340 API36 UI: all seven Settings sections initially collapsed;
Background expands to Ambient and Fit/Crop controls. Real-video Fit full reveal
kept a 16:9 stage, undimmed exposed wallpaper and upward recovery. Exposed horizontal
gestures do not resize the stage. Ambient visibly changes to a muted gradient from
the playing video. This one clip does not establish every aspect/source combination.
Crash buffer showed no app fatal exception in this bounded session. A separate
Pixel Launcher ANR was dismissed; it is not an app crash. Failed/null accessibility
dumps were rejected, not reused as evidence.

Landing page and both illustrative films match 1.21.26 SOURCE_ONLY; script syntax
checks pass. No new release download was advertised. Security/version range gates
and remote exact-head CI are recorded on the integration PR. No universal bug/crash-free,
physical acoustic or sample-lock claim.
