# Issue #147 — long Library menus versus real player dock (1.22.54/code375)

## Question

Do the Library playlist selector and selection **Add to…** menu leave
their final actions visible and physically tappable when the docked Mini
Window is present? The original proposal to shift menus upward was a
hypothesis, not an observed fix.

Current source:
- 'ui/LibraryScreen.kt': two standard Material3 DropdownMenu instances
  containing a variable number of playlist rows and trailing actions.
- 'ui/GreaterArtApp.kt': an outer Scaffold reserves the scaled dock height
  for the Library foreground, but popups use a separate window.
- 'MainActivity.onWindowFocusChanged' to 'PlayerWindowVisibility.windowFocus'
  to 'MiniWindowOverlayService.updateRootLayout': losing Activity focus is
  intended to hide and disable interaction with the dock overlay.
- Material3 DropdownMenu is scrollable by default if it exceeds available
  space (https://developer.android.com/develop/ui/compose/components/menu).
  A missing custom dock offset does not, by itself, prove overlap.

## Test-first approach

LibraryDockMenuTest exercises a real MediaSession plus the real
TYPE_APPLICATION_OVERLAY dock. It does not stub popup focus or fake the
WindowManager layer.

1. The disposable emulator grants only app-declared
   SYSTEM_ALERT_WINDOW, MANAGE_EXTERNAL_STORAGE and
   POST_NOTIFICATIONS to the test-installed package. No keystore is used.
2. The test creates one UUID-owned 60-second WAV in Downloads and one
   UUID-owned playlist containing that file; playback reaches READY with the
   LARGE dock visibly touchable.
3. It enters selection through the short song-list dialog before creating
   11 more temporary playlists. This keeps the unrelated tall AlertDialog
   outside the test's scope.
4. With twelve owned playlists, it opens Add to…, scrolls to the last
   Share selected media files item, performs the Compose touch click and
   checks the resulting confirmation. It dismisses without sharing anything.
5. It opens the playlist selector, scrolls to Delete list, clicks it,
   observes the confirmation and cancels; no playlist is deleted through UI.
6. Each popup logs the real dock-window alpha/untouchable state and visibility
   policy. Once closed, the original dock must be visible/touchable; its
   WindowManager view identity, playing media ID and live playback are
   unchanged. All test-owned playlists and audio are removed in finally,
   and touched settings/playback records are restored.

**Failing evidence is not automatically proof of an occlusion defect.**
First distinguish audio fixture indexing and appops setup, popup focus,
a scroll-semantics failure, actual pointer occlusion, and genuine menu
positioning. The runner preserves per-test JUnit XML, method diagnostics
and earlier runs. Never weaken assertions to manufacture a green build.

## CI integration

Extend the optional (non-required) API36 workflow on draft PR #247:
- Retain all 14 previously passing fixture-free fullscreen classes.
- Add the one dedicated real-dock integration test as the **15th** method.
- Grant existing manifest-declared permissions in the disposable emulator
  after the first component tests install the debug app, immediately
  before the dock test.
- Re-run Android build/unit/lint/assembly, Version Consistency, security,
  Windows and the optional emulator job at the exact new PR head.
- The workflow is also triggered by future PR edits to affected Library and
  dock UI files, not just edits to the workflow or runner.

## Acceptance / ownership

This is **SOURCE_ONLY**. The new test has not passed until the actual hosted
run reports one correct LibraryDockMenuTest method alongside the original
14, all without error/skip. If reproducible occlusion is observed, prepare a
separate, narrowly scoped production menu fix with unchanged playback
window and verify it on the same device, plus landscape and all dock sizes.

Hosted Linux/Android virtual tests cannot substitute for the owner's exact
maintenance-signed 1.22.50 A55 package, real SDR/HDR video samples, rotated
popups, device-specific insets or physical touch/thermal/audio acceptance.
Issue #147 remains OPEN. Do not change verified 1.21.93 APK, keys,
stored user media, historical branches, Stack/Nodes, or published releases.

No production popup position, player runtime or storage permission behavior
changes in this test-first commit.
