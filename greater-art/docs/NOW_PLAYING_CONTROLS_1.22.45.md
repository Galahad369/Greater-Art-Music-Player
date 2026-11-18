# Now Playing controls — 1.22.45 / code366

**Not accepted:** the exact optimized APK passed the three original changes but
failed DEV → Pick because the inline service panel inherited the small badge
slot's constraints. Preserve this candidate and its failed evidence. The repair
is separately versioned as [1.22.46/code367](DEV_PANEL_1.22.46.md).

## Approved scope and behavior

The owner approved the specific plan and then requested half-transparent notices.
Change only the mobile player/inspector and shared in-app notification styling.

- Put DEV below the expanded handle, within its existing reserved center gap.
  Its parent must still recognize a pull that starts over the DEV button.
- Pull-down reveals the page or app behind the overlay. Do not launch MainActivity,
  move the underlying task or change its selected page. Home remains independent.
- Lock permits ordinary and Stack queue scrolling, but not track/primary selection,
  queue actions, seeking or transport. Unlock restores those actions. Preserve
  accessible scroll semantics instead of hiding the whole queue.
- Make “Tap an element” use the small label style; retain the48dp Cancel target.
- Use one theme-based50%-opaque plate for in-app notices. Keep text/icons opaque,
  dismissal, undo and announcements intact. Android system notifications/toasts
  and dialogs are not made translucent or replaced.
- Mini Window selection uses the standalone-window glyph, not the launch/PiP icon.
  Other controls retain their dimensions, spacing and actions.

## Cause and implementation

The old pull completion calls `returnToLibrary()`. The fix changes the destination,
not the player or drag thresholds. It chooses docked/detached presentation from
the already visible page without launching an Activity or backgrounding its task.

The old full-screen sibling input blocker wins hit testing over the queue, and
root `clearAndSetSemantics` also removes scrolling for accessibility. The new
initial-pass parent gate leaves only the measured queue viewport open to gestures.
Queue callbacks are disabled separately. Non-queue actions keep their semantics
hidden while locked; the existing transparent Unlock target stays above the gate.
Remembered callbacks prevent position ticks from restarting an active gesture.

DEV is composed in the toolbar slot, not floated over its parent drag recognizer.
This avoids replacing one positioning bug with a dead pull handle.

## Verification and preservation

Evidence stays in `App/output/now-playing-controls-20261117/`. Preserve an original
APK challenge, focused Compose tests, host tests/lint, optimized assembly and the
exact maintenance-signed A55 smoke separately. Inspect actual screenshots. Source
and tests are reviewed against the saved working-tree baseline, not just HEAD.
Never weaken scroll/callback assertions to turn a failed check green.

Original source baseline: `cbf78a6`; exact tested43/44 APKs, signing identity,
unrelated Stack reports, CodeGraph config and Windows Trust stash remain intact.
Video, HDR/SDR routing, sampling, bitrate, resolution, frame rate, surface ownership,
Stack matching and the Gradle update are out of scope and must not be changed.
No public-signing, physical-phone or acoustic acceptance is claimed.

## Existing guidance

[Compose scrolling](https://developer.android.com/develop/ui/compose/touch-input/scroll/nested-scroll-modifiers)
already provides the list's scroll behavior. Keep it; gate selection rather than
replacing it with custom scrolling. [Material3 theming](https://developer.android.com/develop/ui/compose/designsystems/material3)
provides the semantic colors. Reuse those roles rather than install another UI library.
