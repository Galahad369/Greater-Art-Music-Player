# Issue #147 — modal focus and dock restoration diagnosis (1.22.56/code377)

## Exact run

[Hosted API36 run 44895889143](https://github.com/Galahad369/Greater-Art-Music-Player/actions/runs/44895889143) at prior commit `95a1f69608d7057190d2ce0ada0cdf41c5039770` actually executed all **15** Android instrumented tests.

- 14/14 pre-existing fullscreen UI, color and geometry tests **PASSED**.
- The new `LibraryDockMenuTest` ran and failed **after** a confirmed physical Compose tap on the final selection-menu item **Share selected media files**. The follow-up confirmation dialog displayed and the Cancel action was clicked.
- The test timed out after 30 seconds waiting for the original WindowManager dock to become visible and touchable following dialog dismissal. Its failure was `Timed out waiting for selection popup dismissed`, with zero other errors/skips.
- The test did **not** reach the separate long playlist menu's last **Delete list** action. Therefore **neither full-menu parity nor a genuine popup overlap is established**.

## Why a diagnostic commit

The observed symptom is dock restoration / foreground focus. `MainActivity.onWindowFocusChanged` feeds `PlayerWindowVisibility.windowFocus`; the Service's real dock then checks that flow and updates alpha and input flags. The first test only logged a generic timeout, so the report cannot distinguish these possibilities:
1. Activity did not regain focus after a Material3 DropdownMenu followed by AlertDialog.
2. Focus returned but the Library/IME/expanded visibility state remained incorrect.
3. Policy reported visible but WindowManager root stayed alpha-zero/non-touchable.
4. The test fixture has a permission or lifecycle side effect that does not occur with the owner's exact A55 APK.

## Precisely scoped change

Retain *every* existing popup touch, scroll, cancellation and dock-return assertion and the same 30-second timeout. In the timeout message, additionally report:
- actual Activity `hasWindowFocus()`;
- `libraryShowing`, `dockSurfaceReserved`, `keyboardShowing`, `expandedShowing`, `dockedVisible`;
- the actual dock WindowManager root identity, alpha and `FLAG_NOT_TOUCHABLE` state.

Do **not** force focus back, emit synthetic callbacks, replace pointer clicks with semantics actions, relax expected counts, or hide the failure. No production app changes. No user data or release artifacts altered.

## Gate

At source 1.22.56/code377, rerun Android compile/unit/lint/optimized unsigned build, version and security checks, Windows CI and the optional API36 **15-method** instrumentation runner. If the same failure occurs, classify it with the expanded evidence before proposing a separate production fix. The signed owner's A55 fullscreen/HDR/physical acceptance remains unverified; keep issue #147 open and PR #247 draft.
