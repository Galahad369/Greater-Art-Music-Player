# Now Playing reflection correction — 30 October 2026

Local source identity: **1.21.94/code317, SOURCE_ONLY**. Latest immutable verified
release remains1.21.93. This focused correction is not yet committed or published.

## Cause

The1.21.93 player used the reflected renderer only when the browsing wallpaper
was explicitly Ambient or Adaptive Dynamic. Default Current video and other
wallpapers sent Now Playing through the older vertical-gradient-only renderer.
That route could not display reflections. Earlier screenshot tests covered the
two opt-in modes, not the default route used by the actual release capture.

The opt-in effect was also attenuated by the default dim veil and queue/control
surfaces. Whole-screen ribbons spent much of their area behind opaque media.
This was a routing and presentation problem, not missing video decoding.

## Approved correction

- Make production Now Playing's reflected background independent of the
  browsing wallpaper. Keep browsing pages unchanged.
- Reuse the existing cached artwork and sparse24x24 video-colour samples.
  No second video, decoder, surface, readback or dependency is introduced.
- Concentrate the reflected field in the lower two-thirds of the player.
  Increase reflected-light strength at normal intensity; keep neutral media
  silver/grey rather than inventing unrelated colours.
- Reduce panel alpha from.58 to.32 and ordinary queue rows from.42 to.22;
  selected rows retain their distinction at.50. Keep geometry, boundaries,
 48dp hit targets, compact glyphs and every playback function.
- Keep the title holder transparent. Preserve user dimming, zero intensity,
  Classic's steady geometry and Adaptive motion/accessibility/power/lifecycle/
  scrolling gates. Fullscreen remains unobscured.

This adapts the existing Compose Canvas renderer rather than importing another
music player's blur stack or adopting a new shader framework. Official
[drawing-modifier guidance](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
supports draw-phase effects; the existing sampled-colour architecture fits this
scope without another live media pipeline. This is not a performance benchmark.
The default path now draws richer effects than its former single gradient, so
its rendering cost must not be described as unchanged.

## Verification

- Java21/Gradle9.8.2 unit/lint/debug app/test assembly passes.473 JVM tests,
  zero failures/errors/skips; lint0 errors,47 warnings and4 hints.
- Initial API36 run:21/22 passed in97.653s. The new light-video zero-intensity
  capture contains Android's System UI ANR dialog and shadow, invalidating its
  uniform-pixel assertion. Preserve the failed log and screenshot as rejected
  evidence. Do not remove the assertion or weaken its sample coordinates.
- Added maximum-intensity/minimum-persisted-dim cases and a separately gated
  actual Current video/default-dim expanded-player journey. Final rerun and
  visual acceptance are pending. Their test APK compiles successfully in34s.
- Inspected the failed screenshot and two accepted dark-audio captures: saturated
  artwork produces strong coloured ribbons; neutral artwork retains visible
  silver light. This is not acceptance of the full screenshot matrix.

### Current blocker and internal artifact

Windows reported87MiB available during the failed run. The final check still
reports98MiB after the user freed memory and both emulators stopped. Do not run
another emulator or R8 build at this level. Stop only the owned QA device;
the user separately authorized stopping CampusPlayMatch_API36/emulator-5572.
Its installed apps and saved data remain intact. Neither emulator is running.
Do not close other user apps without permission.

Internal **debug** test APK, not a promoted or optimized release:

- `output/now-playing-reflections-20261030/GreaterArt-1.21.94-internal-test.apk`
  relative to the parent App workspace.
- Bytes:27,072,245.
- SHA-256:`9538f7fd0504d74076a3315ad3038b3e2573ed72df627d19e639cd734021276a`.
- Manifest: `com.local.listentomusic`,1.21.94/code317, debuggable.
- Exact copy hash and16KiB alignment pass. The app upgraded the owned emulator's
  existing installation without clearing data. No signing configuration changed.
- The1.21.93 release hash remains
  `cee3f50632cdf186a1b58304c195378c1917284f20ce04d354c3549ab180f6cc`.

This APK was built before another task's later Trust/privacy, workflow and
desktop changes appeared in the same checkout. It verifies neither those new
features nor the current combined tree. Preserve their ownership; do not stage,
revert, switch branches or publish a consolidated release without coordination.
This focused correction remains uncommitted/unpublished. Allocate any next
consumed identity against the live ledger after coordinating writers.

### Resume without restarting the investigation

1. Coordinate the other checkout writer and confirm roughly3GiB of available
   host RAM. Do not start CampusPlayMatch and Greater Art QA together.
2. Start only GreaterArt_Scroll_QA36/API36. Wait for completed boot, a fresh
   accessible Home screen and no System UI dialog. Reject stale/null UI dumps.
3. Run the same assertions: NowPlayingReflectionsTest8, CompactNowPlayingTest6,
   ReflectedAmbientUiTest6, FullscreenToolbarTest4, plus
   `AmbientSurfaceJourneyTest#currentVideoDefaultDimKeepsRealExpandedReflections`1.
   This is25 cases, not a completed25-case result.
4. Enable `nowPlayingReflectionScreenshots=true`, `compactPlayerScreenshots=true`,
   `reflectedAmbientScreenshots=true` and `playerDefaultAmbientJourney=true`.
   The existing owned fixture is the local Bitter Choco Decoration cover video.
   Keep source dimensions/controller/URI/playing/first-frame assertions intact.
5. Open the accepted captures, including maximum intensity1/dim.25, neutral
   artwork and the actual overlay. Retain the System UI failure and original
   logs. Do not change the uniform-pixel assertion to make an overlay pass.
6. Only after source coordination and visual acceptance, build and separately
   smoke-test an exact-source optimized candidate. Keep release1.21.93 immutable
   until the new artifact meets the repository's release-finalization rules.

Local evidence remains in the sibling workspace
`output/now-playing-reflections-20261030/`. Resolve output paths from the
repository root before launching a build: the first logging command used an
incorrect relative directory and was retried with an explicit absolute QA
folder. Only the accidentally created empty folder was removed.

## Prevention and boundaries

Test the actual Now Playing component and real WindowManager overlay with
Current video, default intensity.5/dim.35, audio/video, light/dark, saturated and
neutral artwork. Also inspect maximum intensity1/minimum persisted dim.25.
Keep zero-intensity and transparent-title checks. A passing isolated AppBackground
test does not prove that Now Playing reaches the same renderer.

No Android playback/surface ownership, Nodes physics, Stack engine, desktop,
permission, signing key or historical APK change is included. Shared output
remains disabled. Physical-phone GPU/frame pacing/battery, acoustic Stack sync,
complete rotation/PiP and universal crash freedom remain unverified.
