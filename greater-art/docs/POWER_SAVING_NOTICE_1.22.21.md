# Now Playing Power saving notice — 1.22.21/code342

Status: source-only; focused notice checks pass. Broader UI, signing-policy and alignment release gates remain open. This is not a fully verified release.

## Requested behavior

Show a small dismissible notice when Android Power saving is on in normal Now Playing:

> Power saving makes Ambient less vivid. Video quality is unchanged.

Use the existing low-power glow automatically. Do not change native playback, source resolution, frame rate, HDR routing, the Ambient effect or the user's Power saving setting.

The notice sits below the measured toolbar, so it does not cover its buttons or change the video/queue layout. It closes with its own accessible button or the standard Snackbar timeout. It is shown once per Power saving interval in the player visit, not on every song or ordinary recomposition. Hide it in fullscreen and PiP, or when Power saving ends. Bundle the message and dismiss label for every supported language.

## Executed checks

- Full JVM suite: 542 tests, zero failures/errors. All supported-language notice/dismiss text is present. Lint: 0 errors, 52 warnings and 4 hints. Development and test assembly passed.
- Four new A55 notice tests and eight existing Stack review/manual tests passed. The notice tests cover off/on, dismiss, recomposition, a new Power saving interval, hidden player and narrow German text.
- Eight existing compact-player/toolbar tests failed in the full run and in isolation. The preserved exact 1.22.20 development pair reproduces the same eight failures. No old assertion or timeout was changed. They include a 128 dp fixture wrapper checked as a 64 dp toolbar, missing Pause after a search/play sequence, and a light-header pixel mismatch. The latter two are not fully diagnosed; do not claim all UI checks passed.
- Mandatory `assembleRelease` passed in 3m 36s. The same-key maintenance candidate passes package/version, non-debuggable state, signature, ZIP and 16 KiB alignment checks.
- The exact installed optimized APK passed the actual A55 Stack → Now Playing notice flow. Enter with Power saving active, observe the message, resume video playback, dismiss the notice and retain the player actions. Playback advanced from 0:14 to 0:20 in the inspected captures. Dismiss leaves Power saving on. Original battery/power and app-ops were restored.
- A separate live Power saving switch visibly shows the notice on the same expanded player. The screenshot captures its fade-in. The ordinary UI dumper instead returned the underlying Stack window, so that accessibility-root result is not proof that the player disappeared. Live-switch appearance was visually inspected; dismissal during that separate switch was not repeated.

## Exact local artifact

`App/output/now-playing-power-saving-1.22.21-20261111/optimized-local/GreaterArt-1.22.21.apk`

Version 1.22.21 / code 342; 5,579,536 bytes; SHA-256 `439c3b87ccf0cf2db6c1db9c5f41a1fa974aaf553fd5b7588ce62dcf2b777cb2`; maintenance certificate `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`. Installed bytes were checked before and after the successful flow. Android source-input hash: `304bc1f2c1ff3db0b74441bb4c2a2e489a5a8a3e9d53f6c05949dca4550db255`; assembly used the pending source tree based on `fa7172e`.

Evidence: `App/output/now-playing-power-saving-1.22.21-20261111/`, including archived unit XML, development logs, old-source comparison, optimized assembly/identity, native XML/screenshots and both initial smoke failures and later successful checks. The first gate invocation used a missing evidence parent; it did not run a build. Create the task parent and use absolute evidence paths. The native helper initially reused its `playback-before.txt` name; later runs must use run-specific evidence filenames.

The original-signature gate still rejects the different global debug key. That failure was retained, not repinned or bypassed; the candidate above was signed separately with the established maintenance identity. It cannot update an original-signer installation. No previous APK was replaced, no app was uninstalled, and no data was cleared. No push, merge or publication occurred.

## Remaining acceptance

Keep the eight pre-existing UI failures visible. Do not turn a focused notice smoke into full-app, physical-phone, HDR or acoustic acceptance. The exact-version release gate remains unqualified while those broader checks and original-signer policy are open.

Keep the exact 1.22.19 and 1.22.20 optimized artifacts unchanged. The 1.22.20 alignment repair is local commit `fa7172e3fe7339a909d729e6d4dd3f7984ffd020`; its Android source-input hash remains `292c70df06283e95e376e6c704d3915ddcf4d51c422ca0c68cb4e2d0517ee178`. The small notice does not resolve the remaining six-take/output or Quick picture gates. Do not push, merge or publish this work without authority.
