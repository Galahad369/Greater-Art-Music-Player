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
