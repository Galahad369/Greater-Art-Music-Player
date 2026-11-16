# Android 1.22.36/code357 — player transitions and icon consistency

State: **SOURCE_ONLY**. This is not a verified release or a claim that every
black-frame/crash case is fixed. Keep app quality, existing functions, previous
artifacts and the separate unfinished backing-music goal unchanged. No push or merge.

## Changes

- Advance the floating-mode preference inside one DataStore transaction. The
  toolbar hosts issue a cycle command; Settings keeps its explicit setter.
  Consecutive taps no longer depend on the last preference value rendered by Compose.
  The legacy Compact/default migration is retained in the shared reader.
- Request the real fullscreen Activity directly from the video overlay. Do not
  first turn the overlay itself into an immersive layout and resize the video.
- Keep the expanded host while fullscreen hides its window. Do not collapse it
  to Detached, reparent its video and dispose its composition during entry.
  Home/Close still select their existing return destinations. The hidden overlay
  must not also handle the system Home broadcast while the real Activity owns it.
- Mini/docked and home-widget transport glyphs use the same Material Rounded
  shapes and compact22dp size as normal Now Playing. Touch areas, callbacks,
  selected/disabled states and contrasting fullscreen/hero controls stay intact.
  The source assets are Google's Apache-2.0 Material Icons; the license is bundled.
- Add real preference-transaction, overlay-geometry, retained-host and native-icon
  regressions, plus1,000-cycle connection and late-surface-release JVM checks.

No player preparation, decoder replacement, bitrate/resolution/rate reduction,
buffer/cache/sampler change, SurfaceView-to-TextureView migration or Stack change.

## Original failure evidence

Private evidence root: `App/output/icon-theme-20261115/`.
Saved tree baseline: `transition-baseline.json`, main at
`530acbcb9adf0b750809e72b3d67c099f4d7a533`.

The retained34debug APK, SHA-256
`003cb5ae9e1c78425433ef658e2b77ef4f07e439efeb2fd1905d7c8a840d7c72`,
is used only for baseline reproduction, not as evidence for the new source.

- `transition-red34-02` and `-03`: two real mode taps from Mini finish on Android
  PiP, not No Floating. This is a dropped request, not just an icon animation delay.
- `transition-red34-03/transition-red-test.txt`: the unchanged overlay-geometry
  assertion fails because CURRENT_MEDIA_TITLE is no longer displayed after the
  Fullscreen tap. The first prototype used a callback timeout instead; keep that
  failed harness and use the final geometry assertion, not the timeout, as proof.
- The same03normal-button journey completes three fullscreen round trips. Its
  window dump shows the outgoing overlay collapsed to279×157 and its screenshot
  exposes Library while the new Activity initializes. Logs retain the same player
  identity; this does not prove the transition is visually good.
- `transport-vector-red.json` rejects the retained24dp play resource. The same
  source-resource check passes the four new22dp vectors. Native rendering remains
  a separate check.

## Verification limits and failed attempts

The03A55 guest exits abnormally during shutdown; retain that failed receipt.
The04instrumentation process never reaches any test. Android exit-info records a
startup ANR, while the Java crash buffer is empty. Do not call this a successful
baseline, blame it on a specific app method without a trace, or call it crash-free.
The final04cleanup restores the exact optimized34APK, raw preferences, appId/data
inode, permission modes and volume. It exits normally with the profile hashes intact.

One exploratory ActivityScenario STARTED test invoked the intentional Home/minimize
path and destroyed its Activity. That does not prove a transient-pause surface bug.
Its lifecycle-isolated replacement never ran before the startup ANR. Preserve the
replacement in `FullscreenSurfaceLifecycleProbe.kt` outside the app source, and
qualify it separately before changing the existing pause policy. No lifecycle
assertion was weakened to make this pass.

`transition36-all-green01.log` ends without a build-success result after an idle
AAPT2 process exits. Old604-test XML files are not results for the current changes.
The second attempt, `transition36-all-green02.log`, succeeds in7m1s using one worker
at lower process priority: **606JVM tests pass**, including both1,000-cycle tests;
lint reports **zero errors/54warnings**; debug/test APK assembly passes. These are
host results, not native or optimized-device acceptance. Optimized assembly and
exact-file checks remain separate.

The source35optimized assembly did pass in `optimized35-gate-03`; its original-public
certificate check failed, and it has no new exact-file device acceptance. The last
focused optimized smoke is still34, SHA-256
`ce3c239a146417919b7bd13506954b0f364a4818382ce837699dbb2c1b57e350`.
Its path is `App/output/icon-theme-20261115/optimized34-maintenance/GreaterArt-1.22.34.apk`.
It does not contain these repairs. The unchanged maintenance signer is
`0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`;
the original-public key remains unavailable.

## Resume without repeating the failed setup

1. Reuse the final four product regressions: PlayerPresentationTransitionTest,
   FullscreenServiceHandoffTest, CompactPlayerIconStyleTest and FloatingModeTransactionTest.
   Keep the private unqualified lifecycle probe separate. Preserve all original checks.
2. Read current resources before a bounded A55 run. Do not repeatedly cold-boot into
   the same startup ANR or interrupt the owner's other work. Temporary3GiB A55 RAM
   remains permitted; keep its GPU, stored profile and data unchanged.
3. Test the exact optimized candidate through Library → Now Playing → mode taps,
   Mini/PiP policy, portrait fullscreen, Rotate, Fit/Crop, Home/Close and return.
   Check actual frames, original dimensions and player identity, not only state flags.
4. Restore settings and verify the installed candidate/hash without a downgrade,
   wipe or signer change. Device smoke and original-public signing must remain
   separate from host tests/assembly. Do not publish or claim full acceptance yet.

Primary implementation references:
[DataStore atomic edit](https://developer.android.com/reference/kotlin/androidx/datastore/preferences/core/package-summary),
[Media3 view handoff](https://developer.android.com/reference/androidx/media3/ui/PlayerView),
[Material Rounded icon source](https://github.com/google/material-design-icons/tree/master/src/av).
