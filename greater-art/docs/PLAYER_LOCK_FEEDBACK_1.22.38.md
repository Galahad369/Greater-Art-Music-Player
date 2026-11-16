# Android 1.22.38 / code 359 — clear Lock state

The owner approved this focused change on 16 November 2026. No other toolbar
button, action slot or pull-handle spacing may change.

## Behavior

- Unlocked: the existing 22dp open padlock, no selection background.
- Locked: a 24dp closed padlock on a 32dp filled selection with contrasting text
  color, inside the existing 48dp touch target. The selection appears only on Lock.
- Locking gives one brief haptic and a 1.6-second “Controls locked” notice.
- A blocked tap gives a brief Lock emphasis and “Tap lock to unlock” reminder.
  Repeated taps restart only that notice. There is no continuous pulse.
- Unlock and Back retain their existing meaning. The fullscreen chrome still
  shares one visibility timeline and preserves the existing center-tap reveal/hide.
- The notice is noninteractive, near the media's lower edge; it never adds a row
  or moves the video, queue, seek bar or transport. TalkBack gets a state description
  and a polite notice. Playback, surface ownership, source quality and Stack are unchanged.

## Checks and limits

Evidence: `App/output/lock-state-20261116/`, outside the repository. `baseline.json`
preserves the initial source and user changes plus the retained 34/37 APK hashes.
Preflight passes with the installed JDK 21 and API 37 SDK; over 30 GiB was free.
`host-checks-03.log` passes in 2m 37s: 610 JVM tests, no failures/errors/skips,
lint zero errors/54 warnings, debug APK and instrumentation APK assembly. Preserve
`host-checks-01.log` (restricted Gradle-cache access) and `host-checks-02.log`
(an invalid test import, removed without changing assertions). `self-review38.json`
is a source-bound self-review, not independent review;
658 unrelated files and the retained APK hashes match the saved baseline. The
toolbar layout and actions after the Lock slot are byte-for-byte unchanged.

New instrumented tests check that selected Lock paints a real highlight without
moving any action or the central gap, restores its plain state, blocks playback
input, gives the reminder and expires the notice. Existing tests are not weakened.
These tests require an Android device; compilation alone is not visual acceptance.

ADB starts outside the restricted session, but no device is attached. The resource
check found about 1 GiB free RAM. Leave the owner's other work alone; do not close
apps, wipe a device or blindly repeat the previous A55 startup failure.

The existing maintenance signing identity must remain unchanged. Original-public
identity and exact optimized A55 smoke are separate gates. No new public release,
push, merge, replacement signer or cleanup is authorized by this task.

Reference: [Material toggle icon buttons](https://github.com/material-components/material-components-android/blob/master/docs/components/IconButton.md)
supports visibly different selected and unselected states. Reuse the existing
Compose IconButton and theme contrast pair; do not add another UI dependency.

## Exact optimized candidate — device acceptance still pending

`optimized38-gate-01/build.json` records preflight and optimized assembly PASS
(5m 35s) from executable commit `96ef7c18818f96daedd3b0eea7940eb8b6e74b03`.
App-input SHA-256:
`ee2cb6c2321036e0eb3e2d907d34274ade669d56363ed6c2e159665654f2ed3b`.
The subsequent documentation commit does not rebuild or alter those inputs.

Private APK: `App/output/lock-state-20261116/optimized38-maintenance/GreaterArt-1.22.38.apk`.
Version 1.22.38/code 359; package `com.local.listentomusic`; 5,604,312 bytes;
non-debug/non-test. SHA-256:
`ed2f8c8986cb615eaa2a33d3250f644453903b89b75245571f92e040bb0f0f25`.
Pinned, unchanged maintenance certificate:
`0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.

`optimized38-maintenance/artifact-verification.json` passes identity/manifest checks;
16 KiB zip alignment and ZIP CRC pass. The bundled icon license is intact. The
optimized DEX contains the new selection and notice markers, absent from retained
37. Both unfinished PCM feature flags remain false. Full-repository user dirt is
preserved; it is not a claim of a clean checkout or a signed provenance attestation.

The original-public certificate check still FAILS; no key is replaced. The exact
38 APK has not been installed or smoke-tested on the A55. Do not call it a verified
release, claim measured visual acceptance or replace the verified public download.
Resume with a healthy owned A55: inspect selected/unselected Lock in normal and
fullscreen modes, verify blocked taps/drag/seek and unlock, run the two new tests
and existing toolbar/lock regressions, then perform a normal-entry smoke on these
exact optimized bytes. No push, merge, cleanup or publication occurred.
