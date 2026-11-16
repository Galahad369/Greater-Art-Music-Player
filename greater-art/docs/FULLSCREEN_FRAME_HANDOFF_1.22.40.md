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

Optimized assembly is next. There is no attached A55; keep
the owner's other apps running. Native visual acceptance and exact-APK smoke remain
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
