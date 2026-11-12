# Stack output measurement — process isolation and A55 negative controls

Status: private QA experiment, **not output-sync acceptance**. Greater Art remains
1.22.24/code345, with unchanged production code and the exact previously tested
APK. No microphone or whole-desktop audio was recorded. No permission was granted,
signer changed, new Greater Art APK assembled, or artifact published.

## Why a different capture method was tested

The earlier Android PlaybackCapture controls retained individual AudioTrack tones
at full level after their gains were set to zero. They cannot prove what the user
hears. The owner approved an isolated Windows capture of the owned A55 emulator,
with synthetic inclusion/exclusion controls first.

[Microsoft documents process-tree loopback](https://learn.microsoft.com/en-us/samples/microsoft/windows-classic-samples/applicationloopbackaudio-sample/)
as distinct from system-endpoint capture. The reviewed sample was pinned at
`0b4e48a88ba446f3d87c5f7df12b33a74899a1e1`. A private helper uses the same native
activation API with **include-target-tree only**. There is no exclude-tree,
microphone or endpoint fallback. It checks the target process image, bounds each
capture to30s, creates output files exclusively, and handles silent buffers.
Existing Windows SDK/MSVC tools were used; WIL or another framework was not
installed. The 48kHz float32 QA recording does not change app playback quality.

## Executed controls

| Check | Observed result | What it establishes |
| --- | --- | --- |
| Owned440Hz and997Hz Windows tone processes, captured separately | Both present in their respective captures; the other process's component is about104dB below the target | Process isolation works for these controlled sources |
| Owned silent process while the other tones run | RMS8.65e-12 | No whole-desktop fallback in this test |
| Wrong expected target image | Nonzero exit; no WAV created | Target-identity guard fails closed |
| A55 five gain phases: both muted, A only, B only, both, both muted | Strict gain check **fails**; measured mixed/solo amplitudes are0.793 and0.948 | Do not qualify the capture as a precise gain or timing instrument |
| A55 single-track absence controls | The estimator reports absent/present ratios0.00639 and0.00585 even though the other AudioTrack was **never created** | Its0.001 leakage assertion can fail on a nonexistent track; the original failure does not establish an app gain bug |

The A55 recordings spread energy around the nominal441Hz/733Hz tones. The
441Hz-only absence control has a strong439.35Hz bin and several neighbouring
peaks, rather than a single stable line. This is observed spectral spread;
its cause in the emulator/host render chain is **not isolated**. The helper
reported zero Android AudioTrack underruns at all five phase boundaries, and
native capture reported no packet discontinuities. Those counters do not remove
the observed distortion or prove sample-accurate timing.

The failed gain report and assertions remain unchanged. The absence controls
diagnose the test; they do not retroactively turn it green. Actual Stack output
was **not** recorded after this qualification failed. Further work must validate
the estimator against known time-varying signals and physical-absence controls
before attributing millisecond errors to the app. Even a passing emulator check
would not establish physical-phone speaker timing.

## Preserved app and device state

The installed Greater Art APK hash matched
`cd5f4d32a8f7252f9753566f0784bdbe68355b9dc026300eb9dbf808ebd0e026`
before and after both A55 checks. `MANAGE_EXTERNAL_STORAGE` and
`SYSTEM_ALERT_WINDOW` stayed `default`; test `RECORD_AUDIO` stayed denied.
The private synthetic helper declares no permissions and is force-stopped.
The A55 profile hash stayed
`0387c814ba4ce319ceb37d88b5f48ee3fecaa3f027d03ccbb00381118edd1ae9`.
Only the A55 instance started for this test was stopped afterward to release
memory. Its installed apps/data and renderer configuration remain intact.

## Evidence

Private files are under `App/output/stack-alignment-audit-20261111/`; do not bundle
recordings, user media, model weights or workstation-specific scripts into the
application or public release.

| Receipt | SHA-256 |
| --- | --- |
| `process-capture-controls-01/report.json` | `e797202c5e6d84a06645bb2ac38d522a03303abc3f452e9eee605749119679d2` |
| `process-a55-gain-01/report.json` — retained failure | `4b116904740ef61203163b72aef8a6e65ab0f6f3d7f3d2e78a1dda126d26e912` |
| `process-a55-absent-track-01/report.json` | `ed3b77f61e649516490ff5a6e11f941c33b5938552bcf8d8e410d9d3320bd422` |

The private probe executable is
`66267834d4541508cc97ec2f0dc2691c2bac949bb789729c50861d4ce4391b12`.
Its source hash is
`d9c7bf24feb75420bb45dfc5aaa8dfe7ec82a0c6655f76e879116efc6ad5acdc`.
The initial Android helper source and build are frozen in
`output-gain-build-01/source/`; the separate version2 helper adds only the
physical-absence controls. These are private test helpers, not Greater Art
versions or alternative production players.

## Known coded timing — additional qualification

Four immutable12s PCM fixtures encode independent997Hz/2333Hz carriers with
known amplitude patterns: zero offset,+1632 frames (37.006803ms),-3220 frames
(-73.015873ms), and unrelated patterns. All use44.1kHz stereo PCM16. This is
synthetic measurement qualification, not music matching or a playback-quality
change. Before device use, the fixture manifest froze a2ms timing tolerance,
positive correlation at least0.90 and negative correlation below0.45.

Using existing SciPy filtering/correlation functions, the private complex
demodulation check recovers all host source controls and synthetic shared-clock-warp controls. Checks also reject
a sabotaged constant-zero timing result and forced acceptance of unrelated codes.
Those host results do not establish the Android output chain.

Repeated native Windows capture activation was checked separately: three5s
captures of one continuously playing owned process retained RMS
0.0151526..0.0151532. This rules out a general repeated-activation loss in that
control, not every emulator-specific audio issue.

The first A55 coded attempt played directly from the private Activity's creation
without a visible play gesture or managed audio focus. Only the initial zero case
had a useful signal; later captures were near silent and failed. Audio-hardening
events were logged, but this API36 device says playback **would** be muted: that
log alone does not prove enforcement or the silence's cause. Do not disable
Android policy or attribute this result to Greater Art.

The private version4 helper starts from its actual visible button after granted
audio focus and finishes each signal naturally before the next case. The first
driver attempt failed before audio testing because Android capitalized that
button label. A package-scoped case-insensitive match repaired that harness
lookup; the same helper APK and all timing assertions remained unchanged.

The completed visible-entry run has these results:

| Case | Truth | Measured | Correlation | Frozen check |
| --- | --- | --- | --- | --- |
| Zero |0ms|0.019ms|0.999765|Pass|
| Late |37.006803ms|36.906ms|0.995127|Pass|
| Early |-73.015873ms|-75.766ms|0.636473|**Fail**|
| Unrelated |Reject|No valid timing|0.153621|Pass|

The capture therefore remains **unqualified**. Do not widen2ms, lower0.90,
discard the failed early case, or call3/4 complete. A separate comparison against
the immutable source finds only0.609..0.699 correlations in that failed recording;
the successful positive recordings generally exceed0.993. The weak matches do
not uniquely identify whether rendering, capture or feature extraction caused
the error. They also cannot establish a Stack-player timing defect. The original
gain qualification is still red; the coded test does not override it.

The exact Greater Art APK, profile and permission state matched before/after.
The owned helper was force-stopped, then only the A55 instance started for these
checks was stopped. No actual Stack audio was recorded after qualification
failed. Retain the failed setup and signal evidence.

| Private receipt | SHA-256 |
| --- | --- |
| `coded-timing-fixtures-01/fixtures.json` | `d1fb567ada99cdd16ab807cdbf46213a4178cd2ca2cf4c36879c028021c031c9` |
| `process-sequential-controls-01/report.json` | `fe1cc90f223a940a660670fa7bca3e019415a1ab03b6317ffb75d87f064c3604` |
| `coded-timing-a55-01/report.json` — near-silent failure | `b907f28becef8eaf93a8ebb484e63d767d20377cf50ab8b684c01172499b3b15` |
| `coded-timing-a55-02/report.json` — button lookup failure | `c18bc4c2c92173d378c500cb675ee35f753a92e2c4b3c39e1719cff7a252a8de` |
| `coded-timing-a55-03/report.json` — completed3/4 run | `f3ba8b0fa1bd132307d03d27a4be56f00b469848830e8ee767ca30d636d7202a` |
| `coded-source-clock-01/report.json` | `4947156a719dd30fa4a269c904f004e7c3701080f2a4beba32ae613a450497a6` |

References: [SciPy correlation](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.correlate.html),
[forward-backward SOS filtering](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.sosfiltfilt.html),
[Android audio focus](https://developer.android.com/media/optimize/audio-focus).
No new third-party package was installed. The private helper's version4 identity
is not an Android Greater Art version or a new release.

## Free-model review limits

Three zero-price OpenRouter requests used manually sanitized numerical findings,
not audio, personal files, credentials or chat archives. Two returned text; all
three report cost0. Hermes itself could not start because its dependency
environment was missing; the direct consultation workflow was used instead.
The answers are advisory, not independent execution. One proposed an arbitrary
reference-correlation gate; the other misstated measured results and asserted
unsupported harmonic/percussive and capture rules. Neither was adopted.
The user-authorized free quota is a ceiling, not a reason to spend unused calls.

Review receipt: `free-review-01/reviews.json`, SHA-256
`cd93706642d62a3626b4a63350683ea53683717021fd5d0f0c03d0312dc119bb`.
