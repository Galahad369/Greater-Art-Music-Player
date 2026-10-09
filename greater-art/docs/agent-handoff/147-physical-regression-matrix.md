# Issue #147 — Physical-device regression matrix

This branch is the workspace for recording physical verification and any test harness/docs needed to support issue #147.

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
