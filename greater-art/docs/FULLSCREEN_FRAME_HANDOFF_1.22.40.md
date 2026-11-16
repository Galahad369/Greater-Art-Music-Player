# Android 1.22.40 / code 361 — frame-ready entry and correct return

Retains the [complete 39 repair](FULLSCREEN_FRAME_HANDOFF_1.22.39.md): keep the
outgoing expanded picture visible, but not interactive, until a new native
fullscreen generation renders. No successful-path delay, quality cap, extra
decoder, forced landscape, bitmap screenshot or player reprepare is added.

## Review finding and repair

39's readiness observer also updated window visibility on false. During Home/return,
clearing fullscreen readiness could therefore show Expanded before the service's
return action selected Detached. That would reintroduce a visible intermediate step.
40 observes true frame-ready commits only; the existing return action remains the
authority for the destination geometry and visibility. Repeated visits still commit
each new ready frame. Sharing, focus/touch/dim protection and accessibility remain.

The original alpha and readiness-reset failures are preserved in
`App/output/fullscreen-ready-20261116/policy-red-01.xml` and `policy-red-02.xml`.
The latter tests use a behavior-preserving stream extraction of the 39 observer:
false must emit no visual commit; false/true/false/true/false must emit true/true.
The unchanged assertions fail on the original behavior. This is host policy evidence,
not a screen recording or a claim that physical composition is verified.

39 source `26a7995` and its optimized assembly PASS remain in
`optimized39-gate-01/build.json`; original-public identity fails and A55 smoke was
not run. Do not promote 39 or rename its APK to 40. 40 uses the next identity.

`checks-all-03.log` passes in 3m 1s: 618 JVM tests, no failures/errors/skips, lint
zero errors/54 warnings, debug and instrumentation APK assembly. Both unchanged
regression groups now pass. `self-review40.json` is a source-bound self-review,
not independent review: 655 unrelated files and the retained APK hashes match
the baseline. Existing attach/reconcile, entry launch, return, orientation,
Now Playing and quality code remain unchanged outside the focused repair.

Native visual acceptance and exact-APK smoke remain
separate from tests/assembly/signing. First-frame readiness uses the existing
generation/timestamp attribution, not an independently measured compositor fence.
The eight-second failure return starts when the fullscreen Activity resumes; it
is not a successful-path delay or a general proof against launch denial/startup ANR.

Resume device QA with normal SDR Ambient, Power saving/native output, paused video,
HDR routing, repeated fullscreen/Back/Home/Mini visits, Rotate and Fit/Crop. Confirm
one visible cut, no blank interval, original dimensions, same player and no intermediate
Expanded on Home. Then run normal-entry smoke on the exact optimized APK bytes.
Preserve all existing assertions, keys, source, APKs and the user's separate Stack
work. No push, merge, public release or cleanup is part of this task.

## Exact optimized candidate and device attempt

`optimized40-gate-01/build.json`: preflight and optimized assembly PASS in 3m 47s
from `acb1c6245e5b113aec4a8c94113ea9b609b8df5b`; app-input SHA-256
`91d68327011c433e486a521489ac0809599fae3fbef71df01057b8647803028d`.
The later documentation commit does not rebuild or change these executable inputs.

APK: `App/output/fullscreen-ready-20261116/optimized40-maintenance/GreaterArt-1.22.40.apk`.
Version 1.22.40/code 361, package `com.local.listentomusic`, 5,604,312 bytes,
non-debug/non-test. SHA-256:
`cf47837986a3b2d0351c6112c042e9d719b2e72d1e36e82d45ba98f4761fb529`.
Unchanged maintenance certificate:
`0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
Exact identity/manifest, ZIP CRC and 16 KiB alignment pass; the icon license remains
bundled, and both unfinished PCM flags remain false. R8 mapping contains the
true-only fullscreen-frame commit implementation. Original-public identity still
FAILS; no key is replaced and no verified public download is changed.

`a55-40-01/boot.json`, `boot.log` and `ui-health.txt` retain the actual device attempt.
After compilation freed about 3 GiB RAM, one owned existing A55 was started with
the previously permitted temporary 3072 MiB option and unchanged GPU configuration.
Android booted in about 33 seconds, but UI Automator returned
“ERROR: null root node returned by UiTestAutomationBridge.” No APK install or app
test followed. The owned emulator received a graceful shutdown; no wipe or new AVD
was used and both original profile hashes match. This is an unavailable UI-test
root, not proof that Greater Art crashed or that the host could never recover.
Do not blindly repeat cold boots. Keep the owner's other apps running.

The candidate is **NOT A55-verified** and is not a verified release. Resume the
normal-entry/rapid-return visual and exact-file smoke checks above on a healthy A55.
All original source, user work, prior APKs, signing files and failed evidence remain.
