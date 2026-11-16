# Stack recording calibration — 11 November 2026

Status: diagnostic evidence only. Production remains Android1.22.21/code342,
SOURCE_ONLY. No matcher, player, quality, version, signing pin or release changed.

## Important correction

In this A55 test, app-only `AudioPlaybackCapture` retained both native
**MODE_STATIC** test tracks at unchanged amplitude across the commanded gains.
The recording did not reproduce those tracks' speaker-mixer gain state.
Equivalent behavior for Media3 streaming playback has not been calibrated.
Earlier raw-capture lag and voice-presence results therefore remain insufficient
to establish what the phone speaker emits.

This does not disprove the user's synchronization problem. It identifies a
measurement limit that must be corrected before diagnosing audible errors from
these recordings. Do not loosen matching gates or invent file offsets to fit an
unqualified capture path.

## Reproducible native test

Private instrumentation ran inside Greater Art's UID10216 on
`GreaterArt_A55_API36`, API36. It created two ordinary native AudioTracks:

- Stereo PCM16,44.1kHz, one-second static loops.
- Frequencies441Hz and733Hz, each0.15 full-scale amplitude.
- Normal media usage and explicit capture-by-all policy.
- No real music, microphone, whole-device capture, hidden Android API or change
  to production code. Android single-app consent and the fixed target-UID filter
  limited the recording to Greater Art.

Five12-second phases set the track gains to `(0,0)`, `(0.1,0)`, `(0,0.1)`,
`(0.1,0.1)`, then `(0,0)`. The0.1 playback gain keeps the audible test quiet.
Analysis used settled six-second windows and a two-frequency least-squares fit.
Controls correctly distinguish gain-honoring capture, gain-bypassing capture and
no signal. The actual normalized amplitudes were:

| Commanded gains |441Hz amplitude |733Hz amplitude |
|---|---:|---:|
|0,0 |0.149993789 |0.149995438 |
|0.1,0 |0.149993789 |0.149995438 |
|0,0.1 |0.149993789 |0.149995438 |
|0.1,0.1 |0.149993789 |0.149995438 |
|0,0 |0.149993789 |0.149995438 |

All gain commands returned success. During the last muted phase, the native
AudioFlinger speaker mixer listed the two active Greater Art tracks with
`G db`, `L dB` and `R dB` all `-inf`. The recording nevertheless retained both
tones at full amplitude. Observed result: `TRACK_GAIN_BYPASSED_BY_CAPTURE`.
Do not generalize this measured routing behavior to every Android device.

The instrumentation runner completed `OK (1 test)` before permission restoration.
The initial harness failed before playback because it checked a static track for
`STATE_INITIALIZED` before its first write. Static tracks begin in
`STATE_NO_STATIC_DATA`; the corrected test checks initialization after loading.
That failed setup is retained and is not a playback defect or a successful
negative control.

## Earlier rate-only experiment

On the retained development1.22.20 APK, a private main-thread probe held only each
actual companion's `lastRateMs` beyond its test deadline. It preserved normal
entry, start gates, drift measurement, hard resync, Apply, selected timing,
pitch, media and native quality. Any escaped trim would fail the test instead of
being reset. The reference was explicitly Miku; normal sound review proposed the
same Riona map, +0.650085ms and scale1.0000031311324713.

The previous approximately70-second trace had340 companion parameter changes.
The held-rate run had one, for the selected scale, but four additional hard seeks
besides live Apply and three primary audio-underrun events. It is not a safe
production fix. One raw-capture group measured approximately-102.3ms with strong
local support, but the full diagnostic was below its distributed-support gate.
The subsequent capture calibration further limits that number: it is **not an
audible-output offset**. Emulator scheduling, capture timing and physical output
latency are not isolated here. Do not implement a guessed compensation.

A normal Solo-primary recording on the exact optimized1.22.21 APK also failed a
single-track reference model. Both-channel checks and the original fixture hash
were verified. The independent tone calibration shows a possible capture-path
explanation. It does not establish the actual Media3 companion gain during that
Solo recording, or prove that Solo fails at the speaker. All original failed
diagnostics are retained, not relabeled as passes.

## Evidence identities

Private evidence root: `App/output/stack-alignment-audit-20261111/native-rate-hold/`.

- Final tone test APK: SHA-256
  `bdd2c355a0dc5f40fcb68ef5104c3327b2580603b2e33c22d426040ed8dc8210`.
  Same existing maintenance signer as the local candidates; no new key.
- `volume-observer-02.log`, `volume-driver-02.log`,
  `capture-volume-events-01.log`, `volume-audioflinger-02.txt`,
  `capture-volume-analysis-01.log` record the actual runner, consent, phases,
  mixer state and controlled measurement.
- `../capture-volume-01.wav`: SHA-256
  `3592901f043529ec368f4bb759958c73e47f15e286f3b000b7bccdf710cd7f09`.
- Held-rate trace: SHA-256
  `3d2bed87c0ccce964a3fe5998fd9b2ac3f20059ef0643732cc9dd63278173edc`.
- `../riona-rate-held-20-12220.wav`: SHA-256
  `10f0d42bdcc67590cd929ec4f4f07925ab57844a95eedec7583bcb45a0dc9947`.
- `../miku-primary-calibration-21.wav`: SHA-256
  `babb9f5137640259078a9162edd56a376f92095ac60e5516ee546eb71614bf6a`.

Private harnesses and recordings remain outside the app repository. They are not
production features or public artifacts. Their builds do not create a new app
release identity. This report is a source/evidence self-review, not independent
release approval.

## Restoration and remaining work

The exact optimized1.22.21 APK was restored after the private tests: SHA-256
`439c3b87ccf0cf2db6c1db9c5f41a1fa974aaf553fd5b7588ce62dcf2b777cb2`.
The standard test package was restored too. Test recording permission is denied,
all-files/overlay app-ops are default, no projection remains, and Power saving
remains unchanged. Solo was explicitly turned off after the primary calibration.
Previous APKs, app data and the owner's untracked CodeGraph file are preserved.

The strict sound repair still abstains on three companions. Android Quick picture
still misses45seconds; Wide passes its existing corpus. Actual audible Stack
synchronization, full quality/function parity of shared output, physical-phone
tests and historical-original-signing acceptance remain open. No push, merge,
publication or production workaround is implied by these diagnostics.
