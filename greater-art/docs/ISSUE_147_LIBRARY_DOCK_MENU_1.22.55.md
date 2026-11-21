# Issue #147 — Library popup test harness repair (1.22.55/code376)

## Failed evidence: first 1.22.54 run

Do not claim that the popup is blocked, or that it is fixed, from
[hosted emulator run 44895275877](https://github.com/Galahad369/Greater-Art-Music-Player/actions/runs/44895275877):
**0 of 15 intended instrumentation methods executed**.

- Android launched the virtual API36 device, but Gradle could not resolve the
  existing Kotlin Compose plugin 2.4.21 after the workflow's display name
  changed and its cached Gradle User Home namespace changed. The previous
  workflow name ran fourteen component tests successfully on 1.22.53.
  The plugin remains on its project-pinned version; the upstream Gradle
  Plugin Portal publishes 2.4.21. No dependency downgrade or bypass.
- The later dock-specific permission preflight understandably found
  "No UID for com.local.listentomusic": no prior Gradle step had installed the
  test app.
- [Android CI run 44895275891](https://github.com/Galahad369/Greater-Art-Music-Player/actions/runs/44895275891)
  compiled production Kotlin but failed at the new test class because
  Kotlin2.4.21 could not infer the ActivityScenario generic in the test.
  The compiler pointed to two consequent lines in that fixture. This
  was a test type-annotation issue, not an observed player bug.

## Minimal correction

- Restore the workflow display name
  "Android API36 Fullscreen Component Smoke" while retaining the broader
  optional job label and the Library/dock source-path triggers. This restores
  the previously working GitHub Actions Gradle cache identity.
- Before the dock integration class, confirm the isolated emulator's
  ANDROID_SERIAL begins with "emulator-". Install its locally compiled
  debug APK if not already installed, then apply the already-declared
  SYSTEM_ALERT_WINDOW, MANAGE_EXTERNAL_STORAGE, and POST_NOTIFICATIONS
  test permissions. Never install to a non-emulator.
- Explicitly type the ActivityScenario<MainActivity> local in the new
  LibraryDockMenuTest. Do not change its scroll, touch, playback, window
  identity, cleanup, or file-safety assertions.
- Advance the branch's Android source ledger to **1.22.55 / code376**
  in the exact same executable commit. Preserve published 54 history.

## Acceptance required

Run the exact new PR head:

1. Version guard, Android unit/lint/instrumentation compilation and release
   assembly; security dependency/privacy/CodeQL; Windows desktop checks.
2. Hosted API36 runner with five classes and 4 + 4 + 2 + 4 + 1 = **15**
   actual testcase records, all correctly named, no errors/skips/failures.
3. For the new real-dock class: verify actual pointer clicks on the final
   "Share selected media files" and "Delete list" menu actions after scrolling,
   with existing media/session/dock identity retained. Log the dock alpha,
   touchable state and popup behavior while menus are open.

If the integration test fails, classify fixture/permission failure, scroll
semantics, focus/window overlay or genuine occluded pointer separately before
altering any production menu. The app's custom menu-offset idea remains
unimplemented and unapproved until a reproducible failure calls for it.

This hosted disposable debug APK is not the owner's maintenance-signed
optimized A55 APK or a physical device. Issue #147 remains open. Preserve
verified 1.21.93/code316, signing identities, user data and past CI evidence.
