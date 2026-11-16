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
(an invalid test import, removed without changing assertions). Optimized assembly
is next. `self-review38.json` is a source-bound self-review, not independent review;
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
