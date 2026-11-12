# Queue Add to list crash — 1.22.22/code343

Status: local source fix; build and exact optimized A55 acceptance pending.
No push, merge, publication or replacement of earlier APKs.

## Reproduced failure

On the owned A55 API36 emulator, the exact optimized1.22.21 APK
`439c3b87ccf0cf2db6c1db9c5f41a1fa974aaf553fd5b7588ce62dcf2b777cb2`
was opened through Library → song → dock → Now Playing → row Actions →
Add to list. The action killed the app with:

```text
android.view.WindowManager$BadTokenException:
Unable to add window -- token null is not valid; is your activity running?
```

This reproduced with the existing Unicode-named Miku fixture. The separately
reported Studio Film file was not required to reach the same button failure.
Keep the user file unchanged; this is a host-window defect, not evidence of a
corrupt media file or playlist.

The playlist picker in `PlayerWindowExpandedContent.kt` used a plain
Material3 `AlertDialog`. Its default application window requires an Activity
token, but expanded Now Playing is a Service-hosted system overlay.

## Focused repair

Set this picker's `DialogProperties.windowType` to
`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`. The pinned Compose
UI1.12.1 already supplies this API; no dependency, permission or host change is
required. The [AndroidX implementation and documentation](https://android.googlesource.com/platform/frameworks/support/+/d463c995fe1c3128619cc6e8039fddef46ad890a/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidDialog.android.kt)
describe this non-Activity use. Keep the existing Material dialog, callbacks,
Favorites/manual lists, duplicate prevention, Done/Back/outside dismissal and
player instance. Do not add a catch that silently discards the user's tap.

Playback quality, source access, surface ownership, Ambient and both alignment
methods are unchanged. The pending music-first policy has not been approved or
implemented by this crash repair.

## Regression and preservation evidence

Private task folder: `App/output/queue-add-list-crash-20261112/`.

- `baseline-native-03.log` and `baseline-native-03/crash.txt` record the intended
  button-triggered crash, not a build or setup failure. The crash-log SHA-256 is
  `c8f125686b0528da466e1bd1982dee7a86772de684d11c32fd79e1c86879e273`.
- `Verify-A55QueueAddToList.ps1`, frozen SHA-256
  `21353c7364e6423fa1e1cd3bcaa27506f4524b648791bb9fdf95e0f3d665e7dd`,
  is the actual-entry-point regression. It must show the picker, add and retain
  Favorites, reopen, dismiss with Done and Android Back, retain paused playback,
  and match the exact installed APK before and after. It restores a changed
  Favorites state and the two temporary app-ops.
- The first adapter expected a nonexistent dock accessibility label; the next
  expected the closed underlay to appear in accessibility. Captured screenshots
  and XML disproved both assumptions. Those attempts are setup failures, not the
  crash reproduction. The final adapter uses the actual visible dock window
  bounds and opens the row before finding its revealed action.
- `baseline-files/` preserves the pre-fix source and dirty handoff. User-owned
  `codegraph.json` remains outside this change. All earlier APKs are immutable.
- `preflight/build.json` passes Java21, API37 and destination-space checks.
  The first preflight invocation lacked its output parent; it did not assemble.
  The first unit command was blocked by the sandbox's Gradle lock access, not by
  a source compilation error. Preserve those distinct limits.
- The actual pinned Gradle build then passed all542 JVM tests and `lintDebug`
  in3m58s. Retained XML is in `static-results/`; this is not native acceptance.

Self-review: the executable change is only two imports and the dialog property
with its host explanation. No callbacks, storage operations, tests, deadlines,
signing policy, player or matcher logic were removed or weakened. Documentation
also preserves the preceding backing-music diagnostic; that is not a matcher
change. The user-owned CodeGraph configuration is not staged.

Do not claim the repair verified until the same native check passes on the exact
new optimized APK. A successful assembly or a dialog-properties source check is
not enough. Original historical signing and wider alignment acceptance remain
separate open gates.
