# Quality follow-up — October 19, 2026

## Reviewed baseline and branch decisions

Baseline: main `9e34f40dfc43069d652deac67cdb3f6b34c6472b`, Android
1.21.55/code278, SOURCE_ONLY. Existing verified Android/desktop artifacts are
unchanged. The primary checkout contains unique local commits and uncommitted
release/handoff work; this follow-up uses a separate, clean checkout of main.

| Work | Review result | Integration decision |
| --- | --- | --- |
| #189 | Merged; reported Android, desktop, version and security checks passed | Preserve; test actual fullscreen behavior |
| #187 | Zero changed files, initial-plan commit only; targets old Grok base, not main | No implementation to merge; proposed 1.21.55 is consumed |
| #188 | Zero changed files, initial-plan commit only; targets old Grok base, not main | Implement diagnostics in a fresh version after the test-harness repair |
| `fix/fullscreen-control-visibility` | Superseded by merged changes | Do not re-import older gesture code |
| #174 | Draft; historical-privacy check fails | Keep separate and blocked; no history/ref/signature changes |

## Fullscreen harness repair — 1.21.56/code279

The baseline fails `compileDebugAndroidTestKotlin`: `createEmptyComposeRule`
cannot host `setContent`. A passing ordinary app build does not catch this.
Component toolbar tests now have a hosting rule; the real Activity/surface test
keeps its empty rule. Android CI also compiles `assembleDebugAndroidTest`.

Screenshot timing is another distinct problem: Android reports a landscape
configuration before the system compositor finishes its rotation animation.
Wait for root relayout and stable paused-fixture pixels; do not label a tilted
transition capture a final-layout defect or final-layout pass.

Local Java21 gate: 395 JVM tests passed with no failures/errors/skips; lint
reports zero errors, 45 warnings and three hints. App and instrumentation build.
API36 run: nine fullscreen/audio/toolbar tests pass in the 56.499-second suite;
one fixture-gated PiP test initially skips. Its explicit fixture-enabled rerun
passes in 10.015 seconds. Always inspect assumption/skip status codes as well
as JUnit's `OK`, which can include skipped tests.

The final stable-compositor screenshot helper passes its real-video rerun in
18.253s. Portrait/landscape screenshots are visually inspected and the rotation
recording is collected under `app/build/verification/quality-followup`. The video
keeps its native 1280x720 dimensions and FIT geometry. This is bounded API36
evidence, not proof of no black frames during every possible handoff.

Android14/15 images are not installed. SDK Manager was attempted with network
access but failed fetching package manifests. Cross-API, physical Samsung,
complete gesture-interruption and continuous flicker checks are not claimed.

## #187 scope review

- SAF selection, persistable access and URI-backed production Library scanning
  already exist (`scopedMediaTreeUri`, `ScopedMediaDiscovery`, `rescan`). Do not
  install a competing file-only tree walker or silently fall back to broad
  storage after revocation. Broad-access removal remains a separate parity gate.
- Nodes already has strength-weighted attraction, user-configurable distance,
  repulsion and damping, a bounded background solver and weighted hub origin.
  Preserve drag activation and pager gesture ownership when refining focus.
- Artwork-adaptive Material accents and release minification still need reviewed
  implementation and verification. Do not infer them from an empty draft's title.

## #174 security failure and safe options

Read-only review of run 39048218582 reports 21 findings. They include workstation
home-path strings in reachable historical build/lint logs and source/test files,
plus current-file findings in HANDOFF and a pattern test. A successful rewrite
rehearsal does not remove those strings from published reachable history.

1. Fix current-file literals using synthetic fixtures and repository-relative
   instructions, then rerun the strict history-aware check on that branch.
2. Inventory affected blobs/refs and distinguish real exposure from synthetic
   pattern cases. Do not publish the actual personal paths in new reports.
3. If reachable historical exposure remains, prepare a backed-up rewrite plan
   covering all affected refs, release/tag impact and collaborator resynchronization.
4. Obtain explicit owner approval before any published-history rewrite, protected
   ref change or retag. Rehearse offline first and retain verification evidence.

Do not weaken detection, ignore the failing check, force-merge #174 or represent
ordinary main's passing audit as resolution of the separate privacy incident.
