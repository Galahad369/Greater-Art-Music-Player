# Issue #147 — Physical-device regression matrix

This is the test plan and evidence index for issue #147. Merging documentation **does not** certify physical-device behavior or close issue #147.

## Required matrix

### Player lock
- audio portrait
- video portrait
- video landscape
- immersive/fullscreen
- controls hidden then rotate
- viewport/split-screen resize
- Expanded Mini
- Unlock stays tappable above blocker

### Expanded Player pull
- 24dp activation gate
- accidental small pull snaps back
- deliberate pull dismisses
- fast fling works intentionally
- short landscape viewport

### Nodes
- shaky tap remains tap/play
- deliberate >=20dp net movement drags
- empty-space pager swipe unchanged
- two-finger graph zoom unchanged

### Pure wallpaper
- full 100% reveal
- bottom region accepts upward recovery
- horizontal pan
- Crop + Fit
- custom image/video
- current-video primary/mirror
- portrait + landscape

### Library thumbnails
- repeated fast flings
- warm disk-cache path
- memory pressure where feasible
- SMALL/MEDIUM/LARGE all remain 103x56dp

### Stack
- same master / different singer
- different mastering
- side-vs-mono selection cases
- repeated chorus / looped arrangement
- long-track drift

## Output standard

For each device/OS case record PASS/FAIL, reproduction steps and evidence. Failures should become focused issues rather than being buried in this matrix.

No automated agent should mark this complete without actual physical-device evidence.

## October 9 Stack evidence — virtual only

Source stages: PR #152, 1.21.22/code245 (`303d82a`, retaining initial `49a3cd6`),
and dependent PR #153, 1.21.23/code246 (default-off debug prototype).
Main concurrently consumed 1.21.19–21; no physical phone was available.
All emulator checks use GreaterArt_A55_API36, not a physical Samsung A55.

| Check | Result | Boundary |
| --- | --- | --- |
| Alignment synthetic offsets | 9/9 accepted, maximum 0 ms error | Synthesized evidence only |
| Unrelated fixtures | 0/20 false matches; 20/20 abstained | Small synthetic corpus |
| Linear timing drift | scale error 7.93e-6; intercept error 3.04 ms; residual 4.76 ms | 180-second synthetic fixture |
| Real pair/cache corruption | PASS, five anchors; -27.553833 ms, scale 0.999970798, residual 1.6383835 ms | No acoustic ground truth |
| Legacy independent-player seek convergence | FAIL, mean settled max drift 96.5 ms; target <75 ms | Unchanged production runtime; assertion not weakened |
| PCM native mono/stereo clips, cancellation | PASS | Own temporary fixtures; bounded queue stall |
| Default-off prototype gate | PASS | No host acquisition/output change |
| Eight-voice prototype transport | PASS | Pause/seek/solo/primary loop/interruption handler |
| Eight-voice cold output budget | NOT ACCEPTED | 3 underruns, peak 40.27 ms per 5.33 ms block |
| Six compressed takes on one output | PASS, short segment | 0 underruns/waits, peak 3.532 ms per 5.805 ms block; Java heap snapshot 15.13 MB excludes native |
| Actual noisy broadcast/headphones | NOT TESTED | Direct shared pause handler only |
| Full-song listening, thermal/long-run resource pressure | NOT TESTED | Physical-device acceptance still required |
| Production PCM host/video integration | NOT IMPLEMENTED | Prototype does not replace ordinary Stack |

Do not extrapolate a short six-source transport pass into perfectly aligned
singers, sustained eight-voice performance or physical/acoustic synchronization.
The prototype refuses tempo maps requiring an unimplemented pitch-preserving
stretcher. Existing independent-player convergence remains an explicit open issue.
No quality cap, permission, production cache-scheduler or surface-route changes
are introduced by this evidence report. Main has now finalized owner-confirmed
1.21.18 provenance; the unchanged version guard passes tracked SHA/manifest
checks, without retroactively certifying older binaries. New sources are SOURCE_ONLY.

Exact-source `881f39c` API 36 repeat: default-off native/gate coverage passed;
enabled transport/native coverage passed in 17.196 s. Eight-voice peak mix was
461.121 ms with 0 reported underruns; six compressed voices peaked at 8.005 ms
with 0 underruns, 1 decoder wait and a 14.79 MB Java heap snapshot. These variable
cold timings reinforce the NOT ACCEPTED performance gate above. Each variant
intentionally skips the opposite feature-gate method, not three exercised tests.
PR #152 merged as `af626b5` after all exact-head CI checks passed.

## Physical execution worksheet — unexecuted (10 October 2026)

This worksheet is a **test specification**, not a verification report. Every physical case starts as **NOT RUN**; results in the virtual/emulator evidence above must never populate physical PASS cells. Copy the table per physical device and record actual observations.

**Device session header (fill before running):** device make/model: ____; Android/API and build: ____; Greater Art installed APK source/versionCode: ____; APK SHA-256: ____; display density/orientation: ____; audio output route: ____; test media fixture identities/durations: ____; tester/date: ____; evidence folder/links: ____.

**Result vocabulary:** `PASS` (physically observed with evidence); `FAIL` (reproducible defect with issue); `BLOCKED` (environment/feature prevents execution); `NOT RUN` (no physical observation). A skipped fixture is **not** a pass. Capture a short recording or screenshots for UI behavior; capture logs, timing measurements and fixture identifiers for Stack/audio behavior. Use fresh installs and returning-user state where relevant.

| Case | Scenario | Reproduction steps | Expected / measurement | Physical result | Evidence / issue |
| --- | --- | --- | --- | --- | --- |
| LOCK-01 | Portrait audio + optional landscape | Open expanded audio player; lock, tap seek/play area, unlock | Locked gestures are blocked; unlock remains reachable | NOT RUN | — |
| LOCK-02 | Portrait video | Lock, hide controls, tap video, unlock | No accidental transport or seek while locked | NOT RUN | — |
| LOCK-03 | Fullscreen/landscape video | Allow chrome to auto-hide; tap once while locked | Lock icon hides with chrome and reappears on first tap | NOT RUN | — |
| LOCK-04 | Fullscreen orientation change | Hide controls, rotate twice, enter/exit fullscreen | No stuck invisible lock, inaccessible unlock or stale chrome | NOT RUN | — |
| LOCK-05 | Split-screen/viewport resize | Resize during locked/expanded playback if OS supports | Tap targets and overlay anchor remain visible | NOT RUN | — |
| LOCK-06 | Expanded Mini / window transition | Move between mini, expanded and fullscreen while locked | Lock remains reachable; mini window screen-awake exception holds | NOT RUN | — |
| PULL-01 | Expanded player 24dp gate | Perform sub-threshold short pulls and then deliberate pull | Small pulls snap back; intentional pull dismisses | NOT RUN | — |
| PULL-02 | Fling and landscape | Fling expanded player; repeat in narrow landscape viewport | No accidental dismissals or trapped panel | NOT RUN | — |
| NODES-01 | Shaky tap versus drag | Tap a node with minor tremor, then drag beyond ~20dp | First starts playback; second moves node | NOT RUN | — |
| NODES-02 | Pager and pinch | Swipe empty graph space and pinch with two fingers | Page switching and graph zoom do not interfere | NOT RUN | — |
| WALL-01 | 100% reveal/recovery | Fully reveal wallpaper; swipe up from bottom dock region | Complete reveal and recoverable foreground | NOT RUN | — |
| WALL-02 | Pan and aspect modes | Pan left/right; toggle Crop/Fit; repeat portrait/landscape | Pan works; visual fit matches selected mode | NOT RUN | — |
| WALL-03 | Wallpaper sources | Try custom image, custom video, current-video primary/mirror | No blank surface, unintended decoder takeover or navigation loss | NOT RUN | — |
| THUMB-01 | Rapid Library fling | Repeated fast fling cold and warm; include audio/video mixed list | Responsive scrolling with prompt RAM hits and eventual thumbnails | NOT RUN | — |
| THUMB-02 | Cache/low-memory | Warm disk cache, background/return; memory pressure if reproducible | No crash, corrupted artwork or permanently blank rows | NOT RUN | — |
| THUMB-03 | List and Mini sizes | Compare each Library row size and each Mini size | Small follows Mini; larger rows enlarge artwork; aspect and touch targets stay valid, no invisible gutter | NOT RUN | — |
| STACK-01 | Same master/different singer | Capture acoustic reference; run Align; manual A/B playback | Applied offset matches ground truth; no false confident alignment | NOT RUN | — |
| STACK-02 | Alternate mastering and gain | Align level/processing variations with known common timeline | Confidence and timing documented; no hidden quality reduction | NOT RUN | — |
| STACK-03 | Stereo-side and mono | Compare stereo-side/mono and mono/mono variants | No cross-domain spurious confident match | NOT RUN | — |
| STACK-04 | Repeated chorus/loop | Align intentionally ambiguous repeated sections | Abstains instead of wrong repeated-section lock | NOT RUN | — |
| STACK-05 | Long-track drift | Align/seek near start, middle, and end; log offsets/timestamps | Errors and any drift recorded at all three points | NOT RUN | — |
| STACK-06 | Production seek convergence | Run at least 6 voice layers and repeat seek/pause/loop | Measure max settled inter-voice drift; compare <75ms target | NOT RUN | — |
| STACK-07 | Debug PCM prototype (if installed) | Enable explicitly; test eight-voice cold start and compressed takes | Record underruns, worst mix block vs output deadline; no production claim | NOT RUN | — |
| AUDIO-01 | Headset/interruptions | On real device use wired/Bluetooth; receive call/alarm/noisy intent | No concurrent legacy+PCM audio; recovery position and state correct | NOT RUN | — |
| AUDIO-02 | Sustained/thermal run | Full track / extended Stack session with repeated transports | Thermal, CPU, dropouts, memory and crashes logged | NOT RUN | — |

### Executing and closing the matrix

1. Record the **exact installed APK** and physical device/OS details above. A source-only commit, emulator success, or release checksum without a matching installed build is insufficient evidence.
2. For each case record actual PASS/FAIL/BLOCKED/NOT RUN, evidence link, measured values where applicable, and failed reproduction sequence. On failures file a focused GitHub issue, link it in the row and leave the row **FAIL** until rerun on a fixed build.
3. Include the fullscreen auto-hide/lock test both immediately and after the auto-hide delay; on Stack report objective drift, confidence/abstentions, audio route and thermal duration rather than subjective 'seems synced'.
4. Compare against the same app build on at least one real phone. A second device / OS and headset route adds coverage but cannot be inferred from emulator results.
5. **Do not close #147** until required device/OS cases actually pass or each exception is explicitly documented and accepted. Completing or merging PR #154 only makes this checklist available on `main`.

**Existing evidence classifications:** all October 9 metrics above are synthetic/emulator-only; `PR #153` is a default-off PCM prototype and not the production Stack route. A failed convergence target or cold eight-voice deadline is an outstanding blocker, not a physical PASS.

**Suggested run order:** lock/player and Library/Nodes/wallpaper first, then Stack analysis and measured audio-route/thermal cases. Save device recordings with case IDs (for example `LOCK-03.mp4`) and annotate discrepancies in the corresponding GitHub issue. This order is guidance only; it does not imply any case has run.
