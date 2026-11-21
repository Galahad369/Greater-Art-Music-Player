# Issue #147 — API36 component runner repair (1.22.52/code373)

## Observed failure

[Run 44315810518](https://github.com/Galahad369/Greater-Art-Music-Player/actions/runs/44315810518)
at PR head `1fb16300c2e2758686a867910f625c21e58073fe` successfully booted
the disposable API36 emulator. It then failed before Gradle or any Android
test executed: `/usr/bin/sh: 1: set: Illegal option -o pipefail`.

The pinned action [splits script input at newlines](https://github.com/ReactiveCircus/android-emulator-runner/blob/a421e43855164a8197daf9d8d40fe71c6996bb0d/src/script-parser.ts)
and [executes each line in a separate sh process](https://github.com/ReactiveCircus/android-emulator-runner/blob/a421e43855164a8197daf9d8d40fe71c6996bb0d/src/main.ts).
Therefore the original directory change and backslash continuation would also
not apply to subsequent lines. This was a workflow defect, not an app test
failure. Retain the failed run and 1.22.51 history.

## Correction and required evidence

The workflow invokes one Python 3 standard-library entry point,
`scripts/run-api36-fullscreen-smoke.py`. It resolves the Android project
relative to the script and passes Gradle arguments without shell expansion.

Run the classes independently because the repository's October 23 handoff
records an earlier combined class filter running only the first class.
After each invocation, inspect fresh JUnit XML before the next class runs.
Retain existing and per-class report directories under a new generated
evidence folder; print each method's result and the total to CI logs.

| Class | Required methods | Scope |
| --- | ---: | --- |
| NowPlayingToolbarChromeTest | 4 | Component pixels, button-center touch, pull lane |
| FullscreenTransportIconColorTest | 4 | Rendered control contrast and ordinary slider |
| FullscreenAmbientBoundsTest | 2 | Fit rectangle bounds and invalid-metadata fallback |
| FullscreenToolbarTest | 4 | Speed menu and narrow component layouts |
| Total | 14 | Fixture-free virtual component evidence only |

Require successful Gradle execution, exactly the expected class and number
of unique methods, and zero failures, errors or skipped tests. Missing,
partial or stale results must not turn a successful command into test PASS.
The separate media-dependent `FullscreenRotationTest` remains excluded.

## Validation status

[Run 44317991598](https://github.com/Galahad369/Greater-Art-Music-Player/actions/runs/44317991598)
at source `5dc6dc9c0c4ae406fc91b5691dee78ba6dcaf911` executed **all 14 tests:
12 passed, two failed, zero errors/skips**. Chrome 4/4, bounds 2/2 and toolbar 4/4
passed; the color class had two failures among four executed tests.
Its aggregate console report did not retain which methods failed or their
assertion stacks, and the job had no uploaded reports. Preserve that limitation.
Android build/unit/lint/release assembly, version and public-security CI passed;
Windows CI was still running when this follow-up was prepared.

No app behavior or instrumented test assertion is changed. The source identity
advances under the repository's workflow/tooling version rule. Android CI must
still run unit tests, lint and optimized release assembly for the new identity.

## Acceptance boundary

Keep [the original scope and A55 checklist](ISSUE_147_HOSTED_EMULATOR_1.22.51.md).
These are disposable hosted debug-component tests, not the exact private
optimized 1.22.50 APK, real-media fullscreen SDR/HDR comparisons, Battery Saver,
Mini return, a physical A55, acoustic/thermal acceptance or original-public
signing. No APK is uploaded or promoted and #147 stays open.

## Diagnostic and fixture follow-up (1.22.53/code374)

Source inspection found two concrete fixture defects:

1. The fullscreen slider test queries `PLAYBACK_TIMELINE`, but production
   `CompactSlider` only registers that inspector label; `inspectElement`
   never adds a Compose test tag. Pass an explicit test-only tag through
   the existing slider modifier.
2. The ordinary-slider test captures its forced 384x100dp parent and samples
   the parent's midpoint, outside the top-aligned 48dp slider. Move that
   test-only tag onto the slider and capture its own bounds. The white
   parent backdrop and all original RGB/alpha thresholds remain.

The upstream [fixed-size measurement](https://github.com/androidx/androidx/blob/a168b01692abf23dec8e11fd419ac96fa4437fba/compose/ui/ui-test/src/commonMain/kotlin/androidx/compose/ui/test/DensityForcedSize.kt),
[Box constraints](https://github.com/androidx/androidx/blob/a168b01692abf23dec8e11fd419ac96fa4437fba/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Box.kt)
and [window-region capture](https://github.com/androidx/androidx/blob/a168b01692abf23dec8e11fd419ac96fa4437fba/compose/ui/ui-test/src/androidMain/kotlin/androidx/compose/ui/test/AndroidImageHelpers.android.kt)
support this fixture diagnosis. These source findings do not retrospectively
identify the two failure methods absent from the earlier log.

The runner now logs each method's status and complete XML assertion message
and stack. Pixel assertion messages also include actual color and capture size.
No production code, thresholds or required checks are relaxed.

**Actual 1.22.53 rerun results: pending.** Record its run and method results
in PR #247 and issue #147 when available; keep the earlier failed run.
