# Android 1.22.37/code358 — hidden player-window policy

State: **SOURCE_ONLY; exact-current-APK A55 acceptance is not verified.**
Inherits the mode-cycle, direct fullscreen-entry and Rounded-icon changes in
[1.22.36](PLAYER_TRANSITIONS_1.22.36.md). Keep that consumed identity and its
artifacts; do not promote36as the corrected candidate.

## Logic-review finding and repair

Retaining the expanded host avoids an unnecessary collapse/reparent during
fullscreen entry. However,36only makes its window transparent and untouchable.
Its old expanded flags can still request focus and14% background dimming.
Visibility must govern those flags, not only the pixels of the overlay.

37applies one visibility policy at every native root-layout update. This includes
rotation, size changes, share restoration and ordinary visibility updates:

- Hidden or sharing: alpha0, no input focus, no touches, no dim flag/amount.
- Visible Expanded: retain its normal focus, touches and14% background dim.
- Visible Docked/Detached: touches work, but no keyboard-focus capture or dim.
- Preserve geometry/fullscreen/awake flags and the existing surface/player.

No decoder, quality, aspect-ratio, playback, Stack, pause-policy or signing change.
The expanded view is not removed/GONE merely to hide it; doing that would undo
the surface-lifetime benefit. All new policy helpers are pure and do not allocate
window objects during layout.

## Tests and actual limits

Private evidence root: `App/output/icon-theme-20261115/`.

`window37-red-red01.log` and `window-policy37-red.xml` retain the original policy
failure: expected hidden dim0, got0.14. The baseline helper expressions are a
behavior-preserving extraction of36's existing expressions, not a device capture.
The same hidden-window check also requires nonfocusable/untouchable flags and
removal of DIM_BEHIND. Additional tests cover normal Expanded restoration,
interactive nonfocusable Mini and1,000hide/show updates preserving other flags.
Native FullscreenServiceHandoffTest now also waits for real Activity window focus.

`window37-all-green01.log` succeeds in8m7s: **610JVM tests pass**, including all four
policy tests and the retained1,000-cycle connection/surface tests; lint has **zero
errors/54warnings**; debug and instrumentation APK assembly pass. Keep all original
assertions, the36failed process/ANR evidence and the private unqualified visible-pause
probe. These host results do not prove rendered transitions are perfect. Optimized
assembly, signing/exact-file identity and device smoke are separate gates.

The36optimized assembly passed at source
`bbee85914a37c66cd9a4c3fbda9e72cf3d989c86` in `optimized36-gate-01`;
original-public identity failed and there was no exact36device smoke. Its later
window-policy finding prevents treating that assembly as an accepted release.
The retained exact optimized34APK remains separate from current37source.

The owner is multitasking and Greater Art is lower priority. Use one lower-priority
worker for host checks; do not stop other apps, repeatedly cold-boot into the same
A55 startup failure, weaken the8GiB release preflight or delete artifacts. Attempt
optimized assembly when prerequisites permit. Device smoke and original-public
signing remain independent gates, and publishing/pushing/merging remain out of scope.

## Remaining device work

Run the four focused player/mode/icon tests and the existing fullscreen regression
on a healthy owned A55. Test real Library → Expanded → fullscreen/Rotate/Fit/Crop →
Home/Close/Mini, sharing/cancel, repeated mode taps, original frame dimensions and
advancement. Confirm the hidden overlay has no focus/dim, restore settings and
bind evidence to the exact optimized APK. Do not claim all unknown crashes fixed.

One additional source-review follow-up is not exercised: Native PiP's launch-error
cleanup finishes MINI_WINDOW although its launch begins NOW_PLAYING. Qualify that
negative launch path before claiming every floating-mode failure is repaired;
do not broaden an ordinary successful-flow pass into that claim.

Primary reference: Android's
[WindowState dim policy](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/services/core/java/com/android/server/wm/WindowState.java)
treats dim requests as window state. The app must clear them when hiding a retained
overlay; a transparent view alone is not the app's complete visibility contract.
