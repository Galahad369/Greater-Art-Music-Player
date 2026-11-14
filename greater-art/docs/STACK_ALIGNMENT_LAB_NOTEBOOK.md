# Stack alignment experiment notebook

This is the single experiment index for the current mobile Stack investigation.
Use it before repeating an experiment. Keep failed evidence. Append a dated entry
when a result changes a decision; correct a mistaken claim explicitly instead of
silently replacing it. Detailed logs and raw media remain in their existing local
evidence folders, not in Git.

## Target and rules

- Align the shared **backing music**, not the singers' phrasing, for all six
  original 孤独毒毒 takes. Keep picture alignment a separate method.
- Preserve primary playback quality, manual/saved timing and existing functions.
- Player positions, a plausible offset or a successful build are not proof of
  the emitted mix. Keep the existing 2 ms output and false-match gates unchanged.
- No push, merge, production engine replacement or release until the relevant
  acceptance checks pass. The shared-output prototype remains disabled.
- Record the source/artifact, hypothesis, unchanged check, observation, decision
  and next test. Distinguish a harness error from an app failure.

## Current decision board — 14 November 2026

| Question | Evidence-backed status | Decision |
| --- | --- | --- |
| Does sound alignment work for all six takes? | **No.** Exact1.22.27 and1.22.30 reviews propose Riona+0.650ms and unc+83.216ms; Forte, Yoshino and Morisray remain unchanged. | Do not describe the two accepted estimates as a six-take success. |
| Is picture alignment usable on this corpus? | The 1.22.26 A55 Quick/Wide runs preserve the frozen dispositions; Riona still rejects. | Keep this separate from backing/audio accuracy. |
| Is the capture instrument qualified? | **Yes, for the raw A55 device-output route in E21.** Original timing, gain/isolation and physical-absence suites pass twice. Old speaker/gRPC failures remain failed. | Use the qualified route and fixed prospective interval; do not claim physical-speaker or high-bit-depth fidelity. |
| Do six independent players meet emitted timing? | **No.** Exact1.22.27 quiet output reaches17.501ms; premix passes0.0906ms. E23 unity restores code shape but leaves177.414ms offsets; static scheduling still fails2ms and startup correlation. | Keep default backend/correction unchanged; do not infer missing singers from ambiguous codes or tune gates. |
| Are focused transport defects repaired? | Source1.22.28 has real red/green cancellation, composed endpoint, short-primary seek and decoder-wait regressions;560 JVM and eight focused A55 checks pass. | Build and smoke the exact optimized file separately; do not call these full backing/output acceptance. |
| Is shared output ready? | No. Full transport/quality parity and the retained high-resolution underrun gate remain open. | Keep it disabled; do not lower quality to pass. |
| Is the raw mixer loop the proven bottleneck? | No. Warmed actual-source math fits the synthetic A55 block budget; the full engine still has an unexplained large peak. | Profile preparation, startup and full transport before a mixer rewrite. |
| Can an existing tool replace the matcher now? | **Not with the tested configurations.** All 74 cases ran with correlation, fingerprint and chroma/onset DTW; a separate key-change variant also ran. Raw matching still follows the singer in the controlled late-vocal case. | Keep the matcher unchanged. Reuse these tools as independent coarse references, not automatic production decisions. |
| Does an independent instrumental reference help? | **Coarse identity: promising, not adopted.** E31 passes controlled backing/vocal discrimination and36/36host/A55 coarse key/offset comparisons; that is parity, not independent truth for the originals. Native primary/key-shift timing remains unqualified. E32 fine variants fail. | Keep the clean reference outside audible Stack slots and private media outside Git. Do not turn a coarse pass into an automatic2ms map. |
| Is the new full-song actual mix accepted? | **No.** E33 retains210s of real output but disproves single-primary global clock registration even on a perfect six-mix control. The joint clock passes that control but fails on real output. | Keep actual gain/presence/drift and whole-song output conclusions open; never infer absent singers from an unqualified clock. |

## Evidence locations and artifact

Local roots below are relative to the user's `App` folder:

- `output/stack-alignment-audit-20261111/`: original-media ownership manifest,
  existing backing/vocal controls, separation trials and coded capture fixtures.
- `output/stack-picture-sampling-20261113/`: preserved picture candidate and tests.
- `output/stack-backing-repair-20261113/`: current sound, capture and CPU probes.
- `output/stack-backing-repair-20261113/external-benchmark-01/`: approved external
  benchmark, isolated environment, dependencies, inputs and results.
- `output/overnight-alignment-20261114-0049/`: E21 qualified raw capture,
  E22 independent-player output failures and preserved source-fix challenges.

Preserved production baseline is `7749357c803a54b39ac54e08139990721b356fab`,
Android **1.22.27 / code348**. Focused source is now1.22.30/code351;28and29
repairs/checks are retained below. Optimized current-file acceptance is recorded
separately from backing/output and public-signing acceptance.
Exact retained prior candidate:
`output/stack-backing-repair-20261113/optimized-local-27/GreaterArt-1.22.27.apk`.
SHA-256: `d29be1d8eb9dfab75f5d1889724e6a386962bd6680ee7a5ddda270a61b88e733`.
It has focused A55 checks, not completed backing/output acceptance. Preserve it
and all previous artifacts. The original-signer release gate remains separate.
Current private optimized30 is
`output/overnight-alignment-20261114-0049/optimized-local-30/GreaterArt-1.22.30.apk`,
SHA-256`2929ef6f567db1af5c614986aa60b8f91947b3da8959a31f1f7f08f8a1d80c05`.
It uses the existing maintenance certificate, not the unavailable original signer.

## Experiment ledger

### E01 — Full-mix and separation controls, 12–13 November

**Question:** Will the existing matcher follow shared music when the voice changes?

**Observed:** Full-mix matching can reject identical backing with changed vocals
and accept different backing with a shared loud voice. Analysis-only separation
finds the correct 700 ms music peak in a controlled late-singer case, but the
unchanged regional gate accepts only 4/6 anchors and correctly remains red.
Direct-drum estimates do not cure all cases; a designated vocal-stem-only control
also passes despite no added backing. Stem bleed has not been ruled out.

**Decision:** A plausible separated peak is insufficient. Do not loosen support,
add a positive rescue, or integrate either separator as-is. The owner selected
the rejection-only repair. Mobile model cost is also material: the measured A55
model core took 31.754 s cold / 8.031 s warm, about 1.20 GiB peak RSS.

**Evidence:** [Backing controls and limits](STACK_BACKING_MUSIC_EVIDENCE_2026-11-12.md).

### E02 — Picture sampling and mixed cadence, 13 November

**Observed:** Bounded nearest-frame sampling and unbiased tied-peak centering
repair the original translation error. Isolated A55 Quick runs take
33.813 / 33.895 s; Wide takes 256.345 s. Exact optimized 1.22.26 normal-UI reviews
take 40.325 / 285.770 s. Frozen corpus dispositions remain intact; Riona rejects.

**Decision:** Keep the fix and the rejection. This does not establish sample-level
audio timing, and these times must not be mislabeled as fully cold runs.

**Evidence:** [Picture candidate and normal-entry checks](PICTURE_TIMING_1.22.26.md).

### E03 — Missing native-region support, 13 November

**Observed:** The baseline can accept a timing map after missing native
measurements shrink its support denominator. The frozen challenge catches the
defect. Version 1.22.27 retains all planned regions in that denominator and
invalidates old pair decisions. The same focused checks and all 555 JVM tests
pass after the repair; lint, optimized assembly and exact-file checks pass.
Normal A55 review, coordinated live Apply and manual Earlier/Later/Apply/reopen/
restore pass. The 8.875 s sound review uses retained features, not a cold cache.

**Decision:** Keep the stricter rejection fix. It does not recover the three
uncertain takes or prove backing identity. No new positive rescue was approved.

**Evidence:** [Exact source, challenge and A55 checks](STACK_NATIVE_SUPPORT_1.22.27.md).

### E04 — Capture qualification, 13 November

**Observed:** A fresh unchanged coded-timing suite passes zero, +37 ms, -73 ms and
unrelated-code rejection (4/4). Keep the earlier failed -73 ms run as well.
The original gain test still fails, as does a physically absent-track control:
the classifier reports a false absent-band ratio even with no second source.
Android UID capture previously retained muted static-track tones and therefore
does not establish post-gain speaker output.

**Decision:** Do not infer a production gain defect from this unqualified
instrument. Do not replace emitted-audio acceptance with player clocks, the
passing timing subset or a new estimator chosen after seeing the failures.

**Evidence:** [Capture recheck and exact gates](STACK_CAPTURE_CALIBRATION_2026-11-11.md#november13--exact12227-capture-recheck).
Private runs: `capture-logger-01/a55-full-{timing,gain,absence}-native27-01/`.

### E05 — Volume/headroom hypothesis, 13 November

**Hypothesis:** Low guest media volume causes the failed gain qualification.

**Observed:** The first attempt stops before playback: the shell volume setter
returns success without changing the index; a preservation helper also receives
the wrong Path type. Preserve this harness error, not as an app failure.
The retained v2 trial changes the real Settings Media volume from 5/15 to 15/15,
verifies it, reruns the original gain test and restores 5/15. The gate still
fails (absent-band ratios 0.002277 / 0.004464; combined/solo 0.9187 / 0.8180).

**Decision:** Low level alone is not a sufficient explanation. Do not repeat
volume-only fixes or disable Android safety/audio policy.

**Evidence:** `volume-headroom-native27-01/` and `volume-headroom-native27-02/`
under the current sound root; same [capture report](STACK_CAPTURE_CALIBRATION_2026-11-11.md).

### E06 — Common-clock phase hypothesis, 13 November

**Observed:** A diagnostic with known shared-warp and single-source-warp controls
passes those synthetic checks and reproduces the original scorer results.
The old failed -73 ms capture has about 14.6% out-of-band energy versus 0.94% in
the fresh passing capture. Differential phase RMS is almost unchanged at about
0.102 ms. The precise source of the distortion is not established.

**Decision:** Pure common-clock warp alone is not supported as the explanation.
No PCM repair or replacement acceptance gate was adopted.

**Evidence:** `capture-phase-diagnostic-01/report.json` and
`capture-phase-native27-01/report.json` under the current sound root.

### E07 — Callback boxing and actual mixer CPU, 13 November

**Hypothesis:** Per-sample lambda boxing or the 32-tap interpolation loop explains
the high-resolution shared-output underrun.

**Observed:** Production `mixPcmBlockInto` is inline. The actual compiled render
coroutine has no `Function3.invoke`, `Float.valueOf` or `Long.valueOf` sample
calls. A standalone A55 DEX uses the unchanged production math and full 32 taps;
six fresh processes compare cold and warmed synthetic blocks. At 192 kHz,
eight fractional-offset voices have warm median 610–612 microseconds and p95
737–801 microseconds per 1024-frame block, below the 4267 microsecond block
budget at 1.25×. Cold blocks take about 10.2–10.5 ms. Six mixed-rate voices have
warm median 1189–1198 microseconds. The test excludes decoder, Sonic,
AudioTrack and production scheduling; it is not output acceptance.

**Decision:** Do not rewrite a nonexistent boxed callback or reduce filter
quality. The original full-engine peaks (mix 178302 / prepare 300224 / render
456367 microseconds) remain unexplained and are not timestamp-bound to the
underrun. Investigate full-path startup/preparation before assigning causality.

**Evidence:** `pcm-hot-loop-device-01/report.json`; production math SHA-256
`5dd32cd8363396245e0c5dbcbc89b565220deaf4ce49079a1c2856329b8515d2`.

### E08 — Audalign + Sync Toolbox setup, 13 November

**Authority:** The owner approved an isolated local benchmark of Audalign 1.3.2
and Sync Toolbox 1.4.2 on all six originals plus difficult false-match controls.
No media upload, app adoption or engine replacement is authorized by this trial.

**Observed:** A separate Python 3.11 environment was created. It protects the
existing NumPy 1.26 calibration environment from Sync Toolbox's NumPy 2
requirement. The first package request fails TLS certificate verification before
download; this is not evidence that the packages are unavailable. The documented
pip 24.0 `--use-feature=truststore` option successfully reaches PyPI using
Windows' existing trusted certificates. All 65 wheels (168,878,594 bytes) were
then hash-pinned and installed offline with `--require-hashes`; `pip check`
passes. Both top-level wheel hashes match official PyPI metadata; both tools
have MIT licenses. HTTPS checking stayed enabled; no system certificates or
global Python packages were changed.

**Harness errors retained:** Metadata inspection first encountered an email
Header serialization error. The first Sync feature call omitted the required
normalization threshold. Its first path scorer then used integral-valued float
coordinates as NumPy indices. The retained v2/v3 scripts supply the documented
0.001 normalization value and validate zero fractional remainder before an exact
integer cast. Original failed receipts/logs remain intact. These are harness
errors, not matching failures or library compatibility failures.

**Evidence:** `external-benchmark-01/README.md` and its retained setup/results.
Primary setup reference: [pip system certificate documentation](https://pip.pypa.io/en/stable/topics/https-certificates/).

### E09 — External baseline: all 74 cases, 13 November

**Method:** Five full-original companion comparisons against Miku; five retained
bounded instrumental-estimate comparisons; two sign controls; unrelated and
repeated-section controls; 60 raw/separated backing/vocal controls across six
regions. Original source hashes match before and after. No model reran, no media
was uploaded, and no app source, APK, playback or saved offset changed.

**Observed:** All three modes complete all 74 cases. Correlation gets the known
+700/-700 ms sign controls right. In every raw late-vocal region, correlation
and basic chroma/onset DTW follow **980 ms**, not the backing's **700 ms**.
Fingerprint follows about **975 ms** in those raw regions. Separated features
help some cases, but do not make the complete backing-presence problem pass.

| Mode | Positive cases returning a result /26 | Negative cases returning a result /38 | Known estimates within the existing 2 ms numerical limit /26 |
| --- | ---: | ---: | ---: |
| Audalign correlation | 26 | 38 | 14 |
| Audalign default fingerprint | 24 | 34 | 0 |
| Sync Toolbox basic chroma/onset DTW | 26 | 38 | 18 |

These are raw returned matches/paths, **not calibrated acceptance decisions**.
DTW always returns a path; it even produces a perfectly straight path for the
unrelated control. Path smoothness alone is not evidence of shared backing.
The last column is only constructed-offset arithmetic: it is not a complete
regional, native-output or six-original acceptance pass. The fingerprint and DTW
grids are about 46.44 ms and 20 ms, respectively.

Full-song correlation estimates are Riona +1.000 ms, unc +82.375 ms,
Forte -7624.875 ms, Yoshino +13.125 ms and Morisray +29.500 ms. Default fingerprint
also picks implausibly different sections for Forte/Yoshino. No independent
sample-accurate whole-song labels exist here; do not promote these numbers to
truth or apply them to saved Stacks.

**Cost on this Windows host, not A55:** Correlation wall time 14.345 s, fingerprint
67.031 s, Sync feature phase 77.751 s and basic DTW phase 5.747 s. Separate summed
pair work is 4.030 / 63.741 / 1.016 s; do not omit feature/import/preflight cost.
Recorded process peak working sets are about 303 / 789 / 464 / 267 MiB,
respectively, excluding child FFmpeg memory.

**Decision:** Neither tested baseline is a safe drop-in backing-only replacement.
Retain useful coarse/reference observations; do not turn a high rank, low path
residual or easy same-file success into proof of backing identity.

**Evidence:** `external-benchmark-01/REPORT.md`, `summary.json`, `trial-01/`
and `trial-sync-02/` / `trial-sync-03/`. Exact dependency hashes and source-bound
phase receipts remain in that one private folder. Tools:
[Audalign](https://github.com/benfmiller/audalign),
[Sync Toolbox](https://github.com/groupmm/synctoolbox).

### E10 — Documented key-change variant, 13 November

**Question:** Is Forte's poor coarse match explained by comparing different keys?

**Method:** Separate sensitivity trial using the author's documented chroma
quantization, automatic semitone shift and step weights on the same retained
zero-tuning features. This is not the complete author pipeline, which also
estimates tuning. Analysis feature transposition never changes playback pitch.
All 74 cases rerun; original baseline results remain intact.

**Observed:** The trial selects a +3-semitone feature shift for Forte and moves
its coarse full-song median from -1000 ms to +60 ms. Yoshino selects +9 (equivalent
to -3) and a +20 ms median. The other medians are Riona 0, unc +80 and Morisray
+40 ms. Full-song linear-map residual p95 remains about 37–69 ms. All six raw
late-vocal controls still follow 980 ms, and all negatives still receive paths.
Completion takes 8.444 s in the warmed host environment, plus the previously
measured feature cost; it is not a cold phone measurement.

**Decision:** Key-aware chroma is useful coarse evidence, not a backing-only or
2 ms solution. Do not replace the engine or add a positive rescue based on this
result. A low residual also occurs for wrong backing sharing a voice.

**Evidence:** `external-benchmark-01/trial-sync-recipe-01/results.json` and
`sync-recipe-01.log`;
[author's full audio-synchronization example](https://github.com/groupmm/synctoolbox/blob/master/sync_audio_audio_full.ipynb).

### E11 — Regional support and score meaning, 13 November

**Question:** Can higher confidence or a smooth path reliably select the backing?

**Method:** Read the retained 74-case results and inspect the exact Audalign wheel
source. No matcher rerun, fitted threshold, changed fixture or app change.

**Observed:** Correlation selects the correct backing in 0/6 delayed-vocal
regions, for both raw and separated inputs. The separated
correlation results do contain the correct 700 ms candidate somewhere in their
returned peaks in all six regions, but always rank the 980 ms voice first.
Basic separated DTW has 4/6 correct medians; the key-aware variant has 3/6 within
the unchanged 2 ms numerical limit. Both miss the necessary 5/6 count. Pure
backing-only correlation and both DTW variants get all six 700 ms medians right.
Fingerprint's 696.6 ms bin is 3.4 ms off even on the pure-backing controls; do not
relax the tolerance to call that sample-accurate.

Audalign correlation divides peak values by their own maximum absolute peak.
Its top score is therefore not normalized cross-correlation or a probability;
the observed wrong-backing controls can also score 1.0. Fingerprint confidence
is a count. The highest separated negative count is 239; the only six positive
scores above it are the easy backing-without-voice cases, not delayed-vocal
cases. Wrong-backing/shared-voice DTW paths can be just as straight as true ones.

**Decision:** A score-only cutoff, path smoothness or selecting the largest peak
does not solve this dataset. The five-of-six count is necessary, not proof of
the full native gate. Multi-peak presence is useful diagnostic evidence, not
permission to recover a rejected production map.

**Evidence:** `external-benchmark-01/regional-evidence-01/REPORT.md` and
`report.json`; [Audalign peak normalization source](https://github.com/benfmiller/audalign/blob/main/audalign/recognizers/correcognize/correcognize.py).

### E12 — Quantify what the retained separator removes, 13 November

**Question:** Is the remaining failure consistent with voice leakage, or has the
separator already removed the known vocal component completely?

**Method:** An oracle-only diagnostic uses the separately retained backing and
voice inputs that constructed the six mixtures. First verify that their sum
reconstructs the actual primary and delayed-vocal companion. Then fit coherent
backing/voice components to each retained estimated instrumental. Test the fit
on a known synthetic mixture and a no-voice control. No new inference, download,
audio capture, production matcher or timing map is involved.

**Observed:** All 12 raw-mixture reconstructions match within about 5.4e-8
relative RMS. Both synthetic checks pass. Relative to the backing component,
the separator suppresses coherent voice by about 16.7–19.7 dB. Nevertheless,
the remaining coherent voice is only about 4.2–7.6 dB below the coherent backing.
Unexplained RMS is about 42–57% of the estimated output, so the linear model
does not account for all transformed/removed content.

**Decision:** The separator helps materially but is not a clean backing oracle.
Do not describe its output as vocal-free or respond by weakening match guards.
These known component inputs do not exist for arbitrary user files; this
diagnostic is not a deployable backing-presence detector. Recording bleed and
nonlinear residual remain unclassified, not declared pure voice or music.

**Evidence:** `external-benchmark-01/separation-components-01/report.json` and
`Measure-RetainedSeparation.py`. Original inputs and the exact 1.22.27 APK remain
unchanged. Alignment/output acceptance is still incomplete.

## Next queue

### E13 — Fixed frequency-band ablation, 13 November

**Method:** Freeze seven bands before execution: unfiltered, 40–180, 180–500,
500–1500, 1500–4000, 4000–8000 and 8000–14000 Hz. Run all 60 retained raw/stem
control pairs (420 observations) with energy-normalized waveform correlation.
Use the same fixed four-second interior and all fully overlapping placements.
The synthetic +700 ms sign/normalization control passes. This diagnostic is not
the production search, which must still consider the full competing-offset range.

**Observed:** The separated 40–180 Hz band selects the correct 700 ms backing
timing in all six delayed-vocal regions. Its weakest positive NCC is 0.983040;
the largest negative is 0.980189. The raw bass band also selects the correct
timing in 6/6, but its vocal-only negatives can score essentially 1.0. No other
tested individual band has all six difficult positives above every negative.
The separated unfiltered four-second diagnostic gets 6/6 timings right, unlike
the external full-span high-pass correlation; the window/filter/search difference
prevents treating those as interchangeable results. Completion takes 24.901 s
on the host. Original sources are unchanged.

**Decision:** A promising low-band timing signal exists, but its approximately
0.003 score gap is a selection-set observation, not a reliable threshold.
Do not apply it to real Stacks or change the native support rule.

**Evidence:** `external-benchmark-01/instrument-bands-01/protocol.json`,
`report.json`, `instrument-bands-01.log` and `Probe-InstrumentBands.py`.

### E14 — Different retained singer, frozen bass boundary, 13 November

**Method:** Before execution, freeze E13's band and largest negative score
(0.9801886008). Use Gospel vocals instead of the Lushlife vocals used for band
selection, and Lushlife backing for the wrong-music controls. Keep the same
pinned separator and 700 ms backing / 980 ms delayed-vocal construction. Run
six regions: delayed voice with shared backing, wrong backing with a shared
voice, and a no-added-backing voice pair. These vocal excerpts overlap and are
not six independent singers. This is new relative to band selection, not a
claim of a new artist unseen by the model or all prior project work.

**Observed:** All six positives select exactly 700 ms and exceed the frozen
boundary. All six wrong-backing controls fall below it. However, all six
no-added-backing voice pairs also exceed it, with NCC about 0.980196–0.989517.
The frozen candidate therefore fails its negative controls. Completion takes
83.683 s on the host with about 1.25 GB peak working set. The 30 new inferences
use existing local weights and private noncommercial fixtures; no new download,
media upload, playback change or source-media modification occurs.

**Decision:** Do not raise the boundary after seeing this failure or present
correct positive timing as a backing-presence pass. Recording bleed in the
vocal inputs remains possible, so “no added backing” is not a claim of acoustic
purity. Low-band correlation finds timing but does not independently establish
that the intended instrumental is present. A future proposal needs explicit
backing evidence and separate negative validation before any production change.

**Evidence:** `external-benchmark-01/different-singer-bass-01/protocol.json`,
`report.json`, `different-singer-bass-01.log` and `Test-DifferentSingerBass.py`.

### E15 — Emulator-native speaker stream, frozen controls, 13 November

**Hypothesis:** Windows process-loopback or host resampling may cause the
remaining capture failures. Test the emulator's own speaker stream without
changing the app, fixtures, gain assertions or timing thresholds. The owner
limits this repair window to90minutes and two new candidate experiments.

**Method:** One new candidate uses the owned A55's existing authenticated local
gRPC streamAudio endpoint. Existing Node HTTP/2 and the installed SDK schema
suffice; no package, model or tool is installed. Auth stays enabled, tokens
remain in memory, and an unauthenticated request must fail. Record native
48kHz stereo S16, then convert its representation exactly to float32 for the
frozen scorer: no resampling, time-warp, dropped samples, gap fill or PCM repair.
Keep raw audio, packet timestamps and fixtures. S16 capture cannot establish
24-bit source fidelity. No microphone, desktop endpoint or other app is captured.

**Observed:** Anonymous access returns gRPC16 and zero audio. Four coded timing
cases pass on the first A55 boot and again after one unchanged-profile restart.
The second run measures+0.050915ms,+37.102777ms,-73.203014ms and rejects the
unrelated code at correlation0.145792 under the unchanged2ms/.90/.45 gates.
However, the original gain check fails: missing-tone ratios0.002234/0.008699
exceed0.001, and combined/solo0.914700 misses the5% gain criterion. Physically
absent-track ratios0.005686/0.006238 also fail the original estimator criterion.
The absence diagnostic completes with exit0 but explicitly does NOT qualify
capture. The owned Windows997Hz tone remains excluded. The exact1.22.27 APK,
helper, A55 profile and original permission states are unchanged.

**Decision:** A Windows-only fault is not a sufficient explanation. Timing pass
does not qualify gain or actual Stack output. Do not call the absent-tone scores
a proven app leak or assign them to quantization without a distinguishing test.
No real six-song recording is accepted, no production engine/matcher changes,
and no thresholds are relaxed. The new capture candidate fails its frozen gate.

**Normal-entry baseline:** The exact installed1.22.27 APK again shows Riona
+0.650ms and unc+83.216ms; Forte, Yoshino and Morisray remain unchanged. The
2.135s review uses retained feature caches, not a cold scan. Live Apply regroups
6/6, and manual Earlier/Later/Apply/reopen/restore passes. This checks application
and UI behavior, not the correctness of the backing estimate or emitted mix.

**Evidence:** Private App/output/stack-backing-repair-20261113/
repair-window-20261113-1718/: WINDOW.md, EXPERIMENT_1.md,
normal-six-baseline-01/report.json, speaker-timing-01/report.json,
speaker-timing-02/report.json, speaker-gain-02/report.json and
speaker-absence-02/report.json. Preserve the first boot's exit and the silence
capture deadline as diagnostic limitations, not manufactured app failures.

**Picture regression and visual correction:** Rebind only the existing private
1.22.26 UI adapter's APK identity to the exact1.22.27 bytes; leave numerical
assertions, deadlines, source helpers and rejection tests unchanged. Quick
reaches review in39.263s and Wider in248.245s, normal-UI intervals rather than
cold backend benchmarks. Both preserve four proposals: Forte/unc/Morisray0ms;
Yoshino+12.5ms Quick and+20.5ms Wider, inside the frozen+12..28ms range. Riona
stays unchanged. Picture Apply and6/6 resume/pause pass. The adapter then fails
to locate Yoshino because it confuses the horizontal pager and nested vertical
list; preserve that complete-run failure. A separate focused manual check uses
the already tested leaf-list locator, not weaker offset assertions. That check
passes Earlier/Later1ms, Apply/reopen and restoration to its original0ms. Its
log and actual screenshot are in yoshino-manual27-01; this does not turn the
earlier full sequence into a pass or establish saved-Stack reload behavior.

Actual Quick review screenshots were inspected: method, reference, current →
proposed values, manual actions and fixed footer are clear, but the rows are
tall. This is not final layout acceptance. Independent hash-pinned source-frame
inspection at45/90/135s shows corresponding scenes and lyric phrases in Miku
and Riona despite different character illustrations, framing and lighting.
The old helper's phrase "different Riona artwork" must NOT be read as proof
that visual correspondence is absent or that alignment is impossible. Existing
luma/fingerprint rejection alone does not settle that question. No precise
picture map or full-song visual acceptance follows from three frame pairs.
One FFmpeg Riona90s reference warning is retained in picture-pair-visual-01.log;
both original media hashes remain unchanged. No picture result is audio proof.

### E16 — Exact PCM and existing helper paths, 13 November

**Owner scope:** A new bounded synthetic-PCM diagnosis; no matcher change,
weaker check, quality reduction, push or merge. Two experiments only. See
[the full diagnosis](STACK_PCM_CAPTURE_DIAGNOSIS_2026-11-13.md).

**Host result:** Exact equal-gain tones with a0.125Hz combined-only frequency
shift produce0.848828 peak gain ratios under the unchanged Hann/FFT scorer.
Clean S16 and standard-resampling controls still pass the0.001 absence floor
(largest ratios0.000131/0.000073). Quantization or that resampler alone does
not explain the actual failures. Retained failure remains caught unchanged.

**A55 result:** Fixed PCM-file/static441/733Hz cases fail absent-source ratios
at0.002669/0.006163, just as streaming441 fails at0.003171. Later streaming733
passes that single floor at0.000100. Thus the streaming producer is not the
sole cause; the output problem varies during the run. Raw S16/float conversion
and source/device hashes are exact. Static peak gain ratios0.960200/1.458998
still fail. Integrated-band1.031380/1.022544 and combined-RMS1.004180 ratios
explain why a changing peak is not reliable gain evidence; they do not replace
the gate or qualify the recorder. Android mixing versus emulator output remains
unresolved. Low host RAM and a1123ms blocked AudioFlinger write are confounds,
not proven causes. No app code or test threshold changes.

**Workflow corrections:** Preserve the pre-boot wrong-registration failure,
missing-gRPC preflight, and actual-uppercase-button locator failure. Same-boot
adapter recipes retain the completed first stream and all original assertions.
The planned device cutoff was missed by about33s; use an in-worker absolute
deadline next time. Do not claim it was enforced by manual polling. Original
permissions/volume5 are restored/unchanged and only the owned A55 is stopped.

**Decision:** No third experiment or production adoption in this allocation.
Recommend one separately approved quiet-host cold/warm run of the unchanged
controls before any matcher work. A later guest-side output probe may need
additional setup; the standard image's tee feature is not proved available.
No root/property change or additional installation is performed.

**Evidence:** Private App/output/stack-backing-repair-20261113/
pcm-diagnosis-20261113-1853/: protocol, host/device reports, source/audio/packet
hashes, exact adapter recipes and analysis.json. Real six-song output remains
unqualified. The existing1.22.27 APK is not rebuilt or promoted.

### E17 — Quiet-host repeat stopped at preflight, 13 November

The owner approved a new30-minute quiet-host PCM recheck with original checks,
matcher and quality unchanged, no push/merge. Native Windows memory still shows
2401MiB available at20:14Taipei, below the agreed5GiB before boot; no emulator
is running. The owner explicitly chooses "Stop at preflight if still short."
Honor that condition: no A55 launch, qualification suite, new recording, app
change, APK build or artifact reverification. This is not another experiment
or a passing quiet-host result. E16's failures and unresolved boundary remain.

Receipt: private App/output/stack-backing-repair-20261113/
repair-window-20261113-1718/quiet-host-20261113-2004/preflight.json.

### E18 — Owner-directed3GiB attempt completes all original suites

The owner directs an actual attempt, without the prior5GiB host-free gate.
Use only a temporary `-memory3072` CLI override; preserve the stored4GiB A55
profile, original1.22.27 APK, matcher, media quality and every original check.
The A55 boots (guest MemTotal3047468kB) and all six initial/warm suites complete.
Timing passes4/4 twice. Gain and physically absent-source checks fail in both
rounds. Initial/warm maximum off/solo is0.003003/0.007017 against0.001; absence
ratios0.004223/0.005480 and0.002914/0.006189 all exceed the original floor.
Original scorers reproduce every stored result on the exact raw samples.

The3GiB configuration can run the workload; conservative host headroom is not
an absolute SDK requirement. This is not quiet-host or real-song acceptance:
host free RAM is665MiB–about1.9GiB and the recorder remains unqualified. All
runtime tests/shutdown request finish around20:47Taipei, before the21:06:31
cutoff. Read-only verification/closeout is late and is not claimed to meet the
whole-session30-minute bound. No third round or PCM repair follows.

The owned emulator exits, volume5/default app-ops and profile are preserved,
and no app code, APK, signer, matcher, quality, branch or remote changes occur.
See the [updated diagnosis](STACK_PCM_CAPTURE_DIAGNOSIS_2026-11-13.md) and private
App/output/stack-backing-repair-20261113/repair-window-20261113-1718/
attempt-3g-20261113-2036/. Keep every failed recording and exact prior APK.

**Read-only follow-up, not E19:** Public AOSP capture glue registers a QEMU
audio-capture callback, rather than directly exposing the app's submitted PCM.
The available source commit is35.6.3 Canary, not installed37.2.13; it cannot
prove an installed-version defect. Official release notes do not establish a
Windows fix for these failures. No additional recording, SDK change, root,
tool/model installation, matcher edit, build, push or merge follows this check.
See the diagnosis's bounded upstream follow-up for exact source links and limits.

**Offline client audit, not another audio trial:** The frozen client agrees
with the installed SDK's48 kHz/stereo/S16 protobuf contract. Delivery mode is
input-only, and timestamps are wall-clock values, not sample positions. The
actual parser/data callback preserves saved PCM through four reconstructed
wire-fragmentation layouts; a deliberate dropped stereo frame is caught.
This uses canonical generated messages, not captured server wire data, so it
does not qualify the recorder or isolate Android from the emulator. No A55,
audio, matcher, original-client or app change follows. Keep gain/absence red.
See the diagnosis and private `client-wire-audit.json` in E18's folder.

### E19 — Owner-directed repeat of the original3GiB controls

The owner explicitly directs the new run. Do it without another RAM approval
gate. One A55 launch uses temporary3072MiB and the exact1.22.27/code348 APK.
All six unchanged initial/warm suites finish23:28–23:34Taipei, before the
23:54:47 hard cutoff. Timing passes4/4 twice. Gain and physical absence fail
both times, reproduced by the original scorers on the saved audio bytes.

Off-to-solo maximum is0.005889/0.006225; absence is0.003438/0.003468 and
0.003572/0.006270, all above the frozen0.001 floor. No new matcher, separator,
numeric tolerance, player, source-quality limit or production experiment is
introduced. This repeat does not repair or qualify the recorder.

The run preserves all629 source entries, HEAD, input hashes, APK, registration,
stored4GiB profile, volume5 and default app-ops. The owned A55 is confirmed
exited. No new APK, push or merge. Private evidence:
`repair-window-20261113-1718/retry-3g-20261113-2324/` in the existing output.
See the diagnosis for full numbers, verifier recipe and runtime bounds.

**E19 saved-data follow-up:** The failed raw captures contain short sample
disturbances absent from the retained clean733 Hz stream. Local sine fits show
roughly-34-sample-equivalent phase slips;733 Hz wraps to+31.48 samples. Known
clean/quarter-cycle controls check that diagnostic. This is modulo phase, not
an absolute clock or a proved faulty layer. Earlier static-file output has the
same signature, so an app streaming producer alone cannot explain it. Original
gain/absence remain failed; no samples, matcher or checks change. Proposed
private48 kHz source comparison requires owner approval and must not replace
the original44.1 kHz acceptance. See the diagnosis and E19's three saved-data
reports. No new audio or helper installation occurs in this follow-up.

**E19 synthetic check:** With the14 initial441 Hz disturbance positions fixed,
34-sample insertion models fail the original absence floor at0.004232 for441
and0.003327 for733 Hz; clean controls pass at0.000131/0.000052. No other source
exists, and the733 Hz test reuses441 Hz positions unchanged. This establishes
sufficiency of a modeled disturbance, not an actual Android/QEMU cause or a
qualified recorder. Real samples and original checks are untouched. See E19's
`synthetic-slip-model-01/report.json`; no new helper/audio/install occurs.

### E20 — Approved private48 kHz diagnostic,13–14November

Build one separate permission-free helper; preserve original helper4 and the
exact Greater Art1.22.27 APK. A cold AudioTrack-creation failure and then an
incomplete48000 stream interrupt the planned eight-case matrix. One terminal
48000 reproduction completes five seconds. Completed441Hz cases fail the old
absence floor at both44100 (0.006420) and48000 (0.003094), with the approximate
34-sample phase-slip signature. Track underrun counters are zero. A source-rate
switch is not a demonstrated fix; downstream conversion remains unisolated.

The full two-frequency/reverse-order matrix is NOT complete. The stalled
stream averages34 frames per packet, not every packet; the post-verifier catches
that mistaken inference, and its failed source plus corrected histogram are
retained. Partial raw PCM from that failure was not saved by the old client.
The diagnostic retention repair's error branch is not exercised in the final
successful collection. Do not turn these limitations into passing acceptance.

Three launches exceed the initial one-launch/recovery limits; record that
procedural deviation. The original30-minute cutoff is not reset. No fourth
trial follows. Completed bytes remeasure exactly, original checks stay red,
the new package is removed, owned emulators exit, and existing app/helper,
source/profile/registration/volume/app-ops and prior APKs are preserved.
No Greater Art build, matcher/rate policy, quality reduction, push or merge.

Evidence: private `rate-diagnostic-20261113-2355/` in the backing-repair output.
See the diagnosis for build identity, signer limits, exact outcomes and the
index of failed/complete runs. This is not a completed six-song or release gate.

### E21 — Official raw device output qualifies,14November

**Hypothesis:** Direct guest-side output can avoid the previously observed
speaker/gRPC disturbance without changing original fixtures or numerical gates.
Use the official scrcpy5.0.1 Windows portable distribution, raw48kHz stereoS16
`output`/REMOTE_SUBMIX, no video, host playback, microphone or duplication mode.
The11,264,634-byte archive matches the official release/checksum SHA-256
`b12a2c4ee8be317422451fc7dcf8ee20a71b5ea7ef9ad73ddd825a25316227a5`.
Publisher GPG signature is not verified; do not conflate checksum with that check.

**Initial failure and one repair:** Wall-clock5s capture produces variable
233472/234496-frame WAVs. Both gain rounds fail the frozen5% combined/solo gate.
The exact unchanged scorer also reports0.885627 on newly generated clean,
equal-amplitude tones at those counts, close to the real0.883814 result.
This is nearest-bin FFT scalloping, not proof of real gain loss. Preserve all
initial failures; do not substitute band power or change the scorer.

Before new acquisition, fix the measured interval to the FIRST240000 contiguous
native frames, the originally requested five seconds. Retain every upstream
byte and the complete native WAV, including explicitly unscored overrun. No
start/count search, silence skip, resampling, phase repair, gap fill or discarded
failure is allowed. Lossless S16→float32 conversion is checked exactly. This is
one prospective transport-harness repair, not retrospective trimming to pass.

**Observed:** All original suites pass initially and again: timing4/4 each,
maximum positive error0.023978ms, minimum positive correlation0.999914 and
unrelated correlation0.136147. Combined/solo ratios are0.99780–0.99806; maximum
zero-gain ratio0.00007704. Physical-absence ratios are0.00003103–0.00003797,
below the original0.001 floor. The owned997Hz desktop tone remains excluded.
Before/after app/helper/profile/app-ops/test permission match. Stored GPU and
4GiB profile remain unchanged; this launch uses temporary3072MiB RAM.

Diagnosis closes01:19:50Taipei,30m43s after the fixed00:49:07 start, inside the
45-minute cap. No third route or second harness repair follows. Upstream scrcpy
can synthesize/clamp packet PTS; PTS is NOT used as sample-clock proof. WAV
readback counts are not native packet telemetry. This qualifies these synthetic
output measurements, not a physical speaker, high-resolution output or six-song
backing acceptance. No app quality or source rate changes are involved.

**Evidence:** E21's private `fixed-capture-qualification-matrix.json`,
`capture-qualified-summary.json`, `FIXED_WINDOW_PROTOCOL.md`, original failed
matrix and all raw/native/float WAVs under the overnight root. Original driver,
scorer, fixture and transport hashes are retained. Official primary references:
[scrcpy5.0.1 release](https://github.com/Genymobile/scrcpy/releases/tag/v5.0.1),
[raw reader](https://github.com/Genymobile/scrcpy/blob/v5.0.1/server/src/main/java/com/genymobile/scrcpy/audio/AudioRawRecorder.java).

### E22 — Qualified capture exposes independent-player output failure

**Frozen test:** Exact optimized1.22.27, original six independent carrier/code
files, inspected100%/unmuted/non-solo/zero-offset rows, normal start→pause→seek0
→resume, then20s of contiguous raw output. Keep the original scorer SHA-256
`0bd569ba5490669370260e98f1f61f30767b7fb1cfdd8d2cadc12ebcb92d1f9d`,
all2ms/.90/.45 limits and original source templates. The output measurement
uses a prospectively fixed first960000 frames; retain all native overrun.

**Observed:** The first independent-player run fails (a unit compilation
overlaps it). A second run with no compilation also fails: valid pairs reach
17.501ms and codes3/6 remain ambiguous in every window. Ambiguous codes are
not measured missing voices, nor valid timing values. The matching premixed
control, one file containing all six codes plus five silent Stack sources,
passes all24 source/window checks and60 pairs; maximum error0.0906ms.
Concurrent compilation is therefore not necessary for the failure, and the
instrument can recover all codes when they share one audible lane.

This supports a multi-lane playback-path problem, but does not identify rate
correction, AudioTrack clocks/latency or buffering as its cause. Do not change
deadbands or replace the engine before tracing decisions against output.
Position telemetry and a shared-clock prototype remain separate evidence.

**Normal original-song entry:** Six original media hashes match. Warm sound
review takes2.129s and retains Riona+0.650ms/unc+83.216ms, with the other three
unchanged. Normal live Apply→pause→seek0→resume produces a retained20s real-song
capture through the qualified route. It is not a six-source presence or
backing-accuracy pass: common backing/vocals make the old whole-mix correlation
diagnostic incapable of proving that contract. Full-song acceptance remains open.

**Harness failures retained:** The first coded UI adapter sends Escape and
dismisses the picker. The next taps an accessibility-exposed confirm button
behind the visible keyboard. A dedicated screenshot/IME inspection proves
that obstruction. Use normal Android Back only after confirming IME is shown,
then verify the picker remains open. These are not production audio failures.
Both failed receipts/adapters and the actual screenshot remain private.

**Evidence:** `coded-stack-original27-03/`,
`coded-stack-original27-quiet-01/`, `coded-premix-original27-01/`,
`actual-six-original27-01/` and earlier failed UI runs in the overnight root.

### E23 — Real transport repairs and rate-actuation split

**Source1.22.28/code349, not a verified release:** A cancelled-but-incomplete
analysis must retain state ownership until decoder cleanup completes. A real
coroutine regression fails the old isActive guard and passes isCompleted.
Stereo mono/side timing decisions must also agree at the composed song-end
offset; separate30ms offset/rate differences can otherwise add to60ms. Both
signs are tested without relaxing the original40ms individual guards. Map
revision11 invalidates the older decisions.

**Real A55 transport failure and fix:** With a10-second primary and20-second
companion, seeking to15seconds while playing or paused first clamps the old
primary to9.999seconds. Promote a healthy take covering the requested time
before that clamp, retaining the requested master position during rebasing.
Both actual Media3 cases fail before the fix and pass afterward. The test
checks the first ready survivor, not whether wrong10s playback eventually
reaches15s. The five existing review UI cases pass after updating two stale
copy assertions to the already-shipped caution text. These are transport/UI
checks, not backing or emitted-audio acceptance.

Review finds a second concrete boundary: offsets0/-20s/+20s can make the
entered companion's rebase unsupported. The first fix returns even when that
promotion rejects, leaving a paused seek at0. A real three-take regression
fails before correction and passes after returning only on successful
promotion. All three transport and five review cases pass. Unsupported
rebases retain the old bounded fallback without clamping saved maps; this
does not claim that every15s target is reachable under mutually unsupported
rebased offsets. `transport-device-red-02/`, `transport-device-green-02/`
and `TAIL_REBASE_REVIEW_PROTOCOL.md` retain the exact evidence.

**Unchanged default rate trace:** A production-configured primary and five
production-factory companions use the frozen six codes and zero/unit maps.
The trace completes but output fails: valid pairs reach12.697ms. There are
515 companion speed changes,0.95–1.05x, with no post-readiness seek, observed
buffering or reported underrun. All270 sampled gates are open. Position
readings plateau and jump; they are not evidence that emitted audio stalls.

**One isolated native-rate comparison:** Media3's pinned1.11.1 factory offers
native AudioTrack rate parameters. Official documentation warns the path is
device-dependent despite lower rate-command latency. Change only test player
factories, not production. The same raw-output/scorer gates fail more severely
(companion correlations roughly0.21–0.67 in the early windows). Therefore do
not adopt that backend or tune sync thresholds on this result.

**Unity-actuation split:** The private zero-map run has zero actual rate
callbacks and137 wanted-rate records. All24 source/window correlations exceed
.999, but stable offsets reach177.414ms. It fails the original2ms timing gate.
Three underrun callbacks occur, at ready+1.400s/+1.405s/+23.092s. Internal
voiceDiagnostics.trim is WANTED here; actual callbacks and the explicit
applied1.0 record prove actuation. This is not a production correction policy.

**Clock freshness split:** Pinned Media3 enables dynamic scheduling by default;
ExoPlayerImpl returns the snapshot position except for sleeping offload. A
test-only static-scheduling run removes observed100ms-sampled main plateaus
(legacy110runs, unity111, static0) and reduces rate events515→160. First two
capture windows still fail correlation; the later windows pass correlation
but pairs reach2.4029/2.3920ms, still FAIL2ms. Three late underrun callbacks
cannot explain the earlier failures by count alone. No backend, scheduling
default, rate constants or acceptance thresholds change in production.

**Measurement feasibility only:** A separate fixed4s/60s-section oracle uses
known-time source PCM and nuisance projection. Constructed all-six and each
omission pass unchanged .90/.45/5%/.001 controls; duplicated-source columns
are rejected. This is one constructed section with oracle timing, NOT a
blind matcher, backing detector, actual capture or whole-song acceptance.

**Evidence:** `transport-device-red-01/`, `transport-device-green-01/`,
`lifecycle-red.xml`, `stereo-endpoint-red.xml`, `focused-green.log`,
`legacy-clock-trace-01/`, `native-rate-trace-01/`, `NATIVE_RATE_ABLATION.md`
`unity-rate-trace-01/`, `static-scheduling-trace-01/`,
`source-identifiability-01/`, `UNITY_RATE_PROTOCOL.md` and
`STATIC_SCHEDULING_PROTOCOL.md` under E21's private overnight root. Existing
signing identity, native quality, matcher/scorer gates and disabled shared
output remain unchanged. No push, merge or release promotion.

### E24 — Disabled shared-output wait boundary

The actual PcmLane receive policy waits200ms before calling the output-pause
callback, while the sink holds100ms. A150ms producer delay can therefore
underrun without appearing in decoderWaits. This is a source-proven ordering
exposure, not proof of the old192k stress failure's cause.

Extract that exact boundary without changing the first run's policy. An
undispatched coroutine with an empty controlled channel suspends with no
protection callback; the new test fails before the fix. Replace the timeout
probe with tryReceive; protect before a real missing-block suspension, retaining
the2s timeout, cancellation, failure and exact sample/block identity. Buffered
and closed channels do not request a pause. All560 JVM tests pass afterward.

No source-rate, float precision, resampler, buffer quality or enablement change.
This does not prove uninterrupted output or full prototype parity; pausing
before starvation is not permission to conceal stalls or weaken the original
zero-underrun/performance checks. Evidence: `PCM_WAIT_PROTOCOL.md`,
`pcm-wait-red.xml`, `pcm-wait-red.log`, `pcm-wait-green-full-jvm.log`.

### E25 — Exact28 normal-entry and saved timing

Local commit975a0f2 preserves the E23/E24 repairs as1.22.28/code349.
Its optimized maintenance-signed APK is5,579,536bytes, SHA-256
`834626430c0a89816e6be94d9440090b48d19620694e771f4d96f5f9c20df78c`.
Exact installed bytes pass Library→Now Playing, play/pause/seek/resume/Home
and inspected video/control geometry. Earlier/Later preserves fractional
milliseconds; saving/reloading retains Riona1.625ms and Miku primary, then
Reset restores0. Only the newly owned test preset is removed.

Retain the failed driver receipts. One assumed an Activity survived restoring
MANAGE_EXTERNAL_STORAGE, but Android's exit record identifies KILL_UID on that
permission change. Another searched the wrong visible list/scroll position.
A bounded180px scroll exposes the clipped primary and the complete normal
sequence passes. Long downward overscroll can activate the intentional
wallpaper-reveal gesture; do not call that state a proven app crash or remove
the gesture to make a driver pass. The earlier background-only observation
is retained, not retrospectively declared a confirmed cause.

Evidence: `presentation-optimized28-01/`, `manual-saved-optimized28-01/`,
`manual-saved-optimized28-finish-01..04/`, release28 receipts and exact APK
under E21's private root. Original public signer is unavailable; this is not
an update-compatible public release or full backing acceptance.

### E26 — Native precision, full-path CPU and bounded copy trials

The unchanged enabled-private28 suite passes ordinary eight-lane48/192k
continuity, slow prefill/EOF and pause/seek/loop/solo, but fractional192k at
1.25×/pitch1.5 fails with14underruns and no decoder waits. Thus E24's missing-
data ordering protection does not explain this particular failure.

The native24 test's initial fail-on-wait callback fires before any sample
comparison and without an AudioTrack. It conflates asynchronous cold acquisition
with active playback starvation. In29, count initial waits and retain the2s
bound and every exact0f native24/channel/clip/seek assertion. The actual
zero-underrun/cadence tests are not changed. Cold acquisition and float/native24
precision checks pass; this is not a performance pass.

An opt-in observer retains bounded per-block stage CPU/wall/headroom/counter
data in memory and writes after pause, not during output. Initial active mean
render5454.86us/CPU5264.75us exceeds the4266.67us block budget. Prepare is
1661.21us, mix1445.20us and tempo2348.45us; mean off-CPU190.11us. At frame45056,
sampled queued time5.021ms becomes a12.821ms render interval, an empty queue
and underrun0→1. TSV213 within-row increments are not the full ending217counter.

Exact contiguous windows, cached-overlap ownership, mono expansion and float
bulk IO preserve sample bits. The independent old-adapter comparison preserves
every Sonic output byte across18 native-rate/speed/pitch cases. A review catches
the first copier's overlap-history discrepancy before commit; a repeated-halo
regression now compares actual scalar current-block history. A separate
non-suspending boundary retains the identical window kernel, gains and samples.

These bounded trials do NOT pass the original stress gate:18,10and19underruns
in the recorded successive candidates. Traced render means5054.30,5269.67and
5015.07us still exceed the same4266.67us deadline. Independent peaks/counts
must not be sold as an improvement or regression. No compiler/GC/Sonic cause
is established by those totals; do not cycle more copy/backend/buffer changes
under the same assumptions. Production route, source precision, filter length,
rate/pitch, buffers, numerical gates and default-off status stay unchanged.

Evidence: `prototype28-device-01/`, `prototype29-device-02/04/08/`,
`prototype29-trace-01/02/04/08/`, their frozen protocols and analysis reports.
See [29 pipeline scope and limits](STACK_PCM_PIPELINE_1.22.29.md).

### E27 — Shared output, original waveforms and picture regression

The private default-off common output passes the SAME frozen six-coded-source
scorer that independent players failed:24source/windows,60pairs, maximum
0.110726ms and minimum NCC0.999412; zero reported underruns. A repeat on the
final isolated-kernel revision also passes all24windows/60pairs at maximum
0.093078ms/minimum NCC0.999372. Keep both exact APK/source bindings; do not
select the better run or imply they are the optimized default-off artifact. This is actual
recorded output, not only positions, but it is synthetic unit/zero timing and
not production-host/backing/full-parity acceptance.

An additional private capture uses all six original files at60s and only the
already observed sound maps0,+650,+83216,0,0,0us. No new content estimate or
picture map. Source-waveform projection has predeclared synthetic omission,
wrong-sign, gain-error and duplicate-source controls; they pass unchanged
.90/.45/5%/.001 gates. All six original waveforms correlate0.980893–0.991268
in fixed held-out2..6s and10..14s windows. However, nominal absolute gains
fail by a common factor0.0232–0.0236. Existing media volume is5/15; its contribution
is not independently calibrated here. No after-the-fact gain normalization or
recorder repair is performed. This is not a proven app gain bug, full-gain pass,
backing detector, whole-song result or permission to enable the prototype.

The unchanged picture corpus initially fails its file-access precondition:
the QA child directory belongs to shell UID2000 while the app is UID10216.
run-as cannot provision that external-storage context. Preserve the original
six files, then explicitly copy the same hash-verified public QA originals from
inside instrumentation's actual app UID. No root/chmod/assertion relaxation.
Quick then passes35.439s within45s; Wider passes247.988s. Expected offsets in
companion order are Quick[null,0,0,12500,0]us and Wider[null,0,0,20500,0]us.
All old ranges/rejections remain. The shell-owned copy is retained on the A55.

Viewed fixed30/90/150s source scenes: Riona shares MV structure/lyric ideas but
has different character art, framing and sometimes scene content. This explains
why similar-looking material is not guaranteed a safe luma-patch match; it does
not prove every scene differs. Riona remains unchanged, not silently aligned
using sound. An optional free-model consultation stops before any model call
because no usable existing credential is available; no paid fallback/config edit.

Evidence: `shared-coded-output29-01/`, `shared-coded-output29-final/`, `shared-original-six29-01/`,
`original-source-output-projection-01/`, `picture-corpus29-01..03/`,
`riona-picture-scenes-01/` and `free-backing-consult-01/`. All media/captures
stay local. Original scorer files, source files and prior APKs remain intact.

### E28 — Exact29 artifact and a compilation-state falsification

Commit3c8615f retains29 as SOURCE_ONLY/default-off. The default-flag suite
passes576JVM tests with0failures/errors/skips; lint reports0errors,54warnings
and6hints. Preflight and optimized assembly pass from committed app inputs
`28c0e9807130af5b93caa45c59fde60714fd1acaf3da8c0e488e261b1a6148db`.
The private maintenance-signed APK is5,579,536bytes, SHA-256
`1e48a37028302e2d4b91a1892df2c12e2aea65b5af7fa5892bf9eec67143be12`.
Original public signing remains BLOCKED, not repaired by using another key.

The exact installed29 file passes normal Library→Now Playing play/pause,
seek/resume and Home return. Actual video and fixed controls are visible in
the inspected playing/paused screenshots. Temporary access settings restore.
No acoustic/backing/full-app/physical-HDR claim follows that smoke test.

The exact29 normal manual/saved sequence retains fractional1.625ms after
Apply/reopen and saved reload, preserves Miku as primary and removes only the
new fixture after restoring its zero/unit map. Actual screenshots inspected.
Two failed primary locators are retained: the same native XML yields0Miku
matches under a fresh worker'sIBM437 decode, and1after explicit UTF-8. This
disproves the assumption that the saved primary was lost. Fix private worker
encoding, not app selection or assertions; the unchanged card/value checks
then pass. Do not declare a UIAutomator freeze from a long driver loop alone.

Before more PCM edits, directly test the compilation-cost assumption with
the SAME private debug29/test bytes: one installed-default condition and one
forced Android `speed` condition. The original fractional192k assertion fails
at13and12underruns respectively. Corresponding active render means are4849.22
and4818.96us; CPU4509.78and4524.09us, both above4266.67us/block. Tempo is
bursty: its p95 is9838/9729us. Diagnostic trace completion is not acceptance.
One pair of conditions cannot prove a performance difference or that ART is
irrelevant, but full compilation alone does not fix this observed gate. Do
not repeat warm-up/compiler variations or waive the cold/install-state gate.

Preferences restore byte-for-byte, app-ops default and optimized29 file is
restored with a successful normal compilation reset. Source, filters, source
rate/precision, GPU, buffers, matcher and numerical gates are unchanged.
Evidence: `optimized-local-29/identity.json`, `release29-build/build.json`,
`optimized29-smoke-01/`, `ART_COMPILE_COMPARISON_PROTOCOL.md` and
`art-compile-comparison29-01/` under the same private overnight evidence root.
Manual closeout: `manual-saved-optimized29-finish-03/`; retain the first partial
sequence, both failed locators and `ui-unicode-proof.json` as well.
All original failures and prior APKs remain. No push, merge or publication.

### E29 — Bounded parallel preparation, failed cadence and safe disposition

Hypothesis: parallel prepare/mix and Sonic/output could avoid the measured
serial stage sum without lowering source quality. One producer, one ready
block and three reusable arrays retain exact FIFO/sample ownership. Lower-rate
or slow runs stay serial; extra premixing is capped at25ms. Pure tests cover
raw bits/final partial blocks, ownership/reuse/backpressure, level snapshot
boundaries, cancellation/join, failed closure and protected waits.

The source review finds an existing deadline defect: withTimeout2000 throws
TimeoutCancellationException into the run's owner-cancel path, bypassing
failure/legacy restoration. The new check fails against the old helper.
withTimeoutOrNull2000 converts only its own data deadline into IOException;
explicit owner cancel and an outer timeout remain cancellation. Buffered and
closed channels still take the nonblocking path. All old sample/timeout gates
remain; no production engine is enabled.

The first actual candidate reports0underruns over six fractional half-second
samples but advances only0.8004185requested media time and adds195ready pauses.
Its later2× check fails42underruns. This directly falsifies treating every
short empty-ready wait as an output starvation event or calling zero underruns
alone continuity. Keep the failure, not a cherry-picked initial green interval.

The revised candidate atomically waits only within observed queued headroom,
leaving half the unchanged output buffer as guard, then pauses before the
same2s wait. It still fails the original fractional gate at3underruns. The
new cadence/no-extra-pauses check reports0.8544304requested progress,
17underruns and461extra output pauses in6.003s. The other six original tests
pass, not full parity. Stop pipeline performance variations here.

One trace retains1127rows and ending28underruns; its stats snapshot reports27
because publication is periodic. Producer mean wall3169.60us/CPU2582.81us,
tempo wall2683.20us/CPU2598.87us, tempo p95wall11811us. Stage work overlaps;
the sum is not serial latency or a causal/throughput proof. A new hypothesis
would need direct producer-block/burst evidence, not another guessed buffer,
backend, compiler or quality variation.

The added high-rate EOF check initially reads inactive before asynchronous
rendering starts, failing at432000seek frames. Require active advancing output
before awaiting completion; exact576000frames/3000ms restore and0underruns
then pass. This is a corrected test precondition, not a stress-gate repair.
The original assertions remain intact.

Disposition: preserve the experimental source behind separate default-off
`stackPcmParallel=true`, also still requiring the debug shared-output opt-in.
Ordinary prototype/release behavior does not adopt the failed pipeline. Both
flags must be absent/false for normal delivery. Revised private-source JVM:
591tests,0failures/errors/skips; lint0errors/54warnings/6hints. Final default
source/build/identity/smoke stages are separate in the30 receipt.

Evidence: `BOUNDED_PCM_PIPELINE_PROTOCOL.md`, `pipeline30-timeout-red.log/xml`,
`prototype30-device-01/02/`, their exact identities and cadence observations,
`prototype30-trace-02/` and `prototype30-eof-03/`. Preferences restore exactly
per runner; default app-ops, microphone denial and exact optimized29 restore
are checked. No push, merge, production enablement or source-quality reduction.

Final30 default-source verification passes591JVM tests,0failures/errors/skips;
lint0errors/54warnings/6hints and debug/test assembly pass. Committed source
75ae3c6 also passes optimized assembly and private maintenance exact identity.
The exact1.22.30/code351 APK is5,579,536bytes, SHA-256
2929ef6f567db1af5c614986aa60b8f91947b3da8959a31f1f7f08f8a1d80c05.
Actual A55 normal play/pause/seek67s/resume/Home passes with inspected screenshots,
not a six-song/high-rate acceptance. Both experimental flags remainfalse.
Original test package, access defaults, microphone denial, volume5 and stored
profile hashes are restored; owned A55 exits06:51:16Taipei. Original public
signer remains unavailable. See the30 receipt; do not rebuild/replace these
bytes for documentation or call maintenance signing a public update.

### E30 — Retained energy helps one control, but fails the actual-six target

Distinct hypothesis: normalized correlation can hide weak coherent voice
leakage. The existing separator returns input minus estimated vocals without
output gain normalization; absolute full-mono instrumental/input RMS might
add backing-presence evidence. Freeze0.20 BEFORE reading retention values,
retain the old E13 bass-NCC boundary0.9801886008056108,40–180Hz filter and2ms
synthetic timing check. This is a proposed amplitude heuristic, not semantic
instrument identification, a calibrated confidence bound or an app change.

The retained-data selection pass reports12/12 positives and18/18 negatives
correct in0.649s. Importantly, all18 negatives
already fail the old strict NCC boundary; this selection result alone proves
no added discrimination. A fresh construction with the previously examined
Gospel vocal stem provides the useful incremental check: all6 voice-only pairs
have NCC0.980196–0.989517, above that old boundary, but RMS retention about
0.067–0.073 rejects them. All6 delayed-vocal/shared-backing positives retain
about0.226–0.249 and recover the known700ms. All6 wrong-backing negatives also
reject.30 new separator calls complete18cases in113.781s; no model download.

This is not a blind six-singer validation. The vocal excerpts overlap, use one
backing and fixed synthetic gains, and the singer appeared in earlier work.
"Voice-only" means no ADDED backing, not physically instrument-free ground
truth. The floor may reject quiet genuine music. The held-out report's field
`startedUtc` is mistakenly completion time; its monotonic113.781s is measured.
Keep that original receipt and the reviewer caveat, not a fabricated start.

Next use the exact cached original-six517120-frame stereo estimates from
69.823s, verified against their original manifest/source hashes. Stereo-float
interleaving and native44100Hz source windows are checked; no inference or
resampling. The SAME fixed floor/NCC/filter rejects all5 target companions:

| Companion | Best bass NCC | Candidate offset, not verified timing |
| --- | ---: | ---: |
| Riona |0.698631|+6.009ms|
| unc |0.709939|+76.145ms|
| Forte |0.109277|+5739.184ms|
| Yoshino |0.515319|+19.025ms|
| Morisray |0.677350|+21.927ms|

All6 retention ratios pass0.20, approximately0.66–0.85, so the new energy
floor does not repair actual-song similarity. The0.844s real-data check has
no independent sample-accurate offset ground truth, searches only valid
placements of one interior1–5s query and is NOT distributed/full-song or
emitted-output proof. Forte's large weak peak is not an applied offset.
The inherited helper fields `music700Ncc` and `voice980Ncc` sample fixed lags;
their names do not establish music/vocal timing ground truth in the originals.

STOP this candidate. Do not tune a new cutoff/band/model on these failures or
enable separation in the app. Preserve the controlled improvement AND actual
failure. A new material production approach needs a concrete reviewable plan
and owner confirmation. No threshold/matcher/source-quality change, media
upload, engine enablement, push or merge follows.

Evidence under the private overnight root: `RETAINED_ENERGY_PROTOCOL.md`,
`retained-energy-01/`, `retention-heldout-01/`, `REAL_RETENTION_PROTOCOL.md`,
`retention-real-six-01/report.json` and their runners/input hashes. Independent
read-only review confirms the controlled result's narrow scope, not runtime
acceptance. Private licensed fixtures/models/captures stay outside Git.

## Next actions

E11/E12 rule out two shortcuts: treating a relative library score as backing
confidence, and assuming an estimated instrumental is voice-free. Use the
per-region and known-component evidence before another model or matcher trial.

1. E31's independent-reference coarse identity is promising, but E32's fine
   candidates are STOPPED at their frozen failures. Establish independently
   falsifiable backing-event/transfer evidence before another fine architecture;
   do not tune a failed NCC/floor/band/model to these five originals.
2. Present the concrete proposal and obtain owner confirmation before a material
   production change. Keep sound and picture estimates separate.
3. Use E21's qualified raw route; preserve its original failed variable-length
   recordings and all old failed speaker paths. E33's actual full-song clock is
   not qualified: synthetic conditional projection is insufficient. Establish
   a valid emitted-source clock before inferring actual voice gain/presence/drift;
   retain E22's independently coded multi-player failure separately.
4. E29's pipeline performance variations are STOPPED. Any new investigation
   needs a directly testable cause beyond summed overlapping stage times;
   preserve all original cadence/high-rate/function gates and keep both flags
   off. A math microbenchmark or zero-underrun paused output is not acceptance.

No experiment in this notebook establishes universal or “1000%” accuracy.

### E31 — Independent instrumental time/key reference, resumed14November

**New hypothesis:** compare each raw cover with an independent backing reference,
not another foreground singer. The known retained syudou instrumental is used
only for private analysis; it is not bundled, redistributed or hardcoded in the
app. No new model or media download. The [Unisoner paper](https://staff.aist.go.jp/m.goto/PAPER/ICMCSMC2014tsuzuki.pdf) supports joint time/key
spectral registration, but its reported34.6ms median error is NOT our2ms proof.

Use16kHz analysis copies,2048Hann FFT,100ms hop and95absolute-MIDI bins24–118,
preserving octaves. Search13key shifts[-6,+6]. Source playback remains unchanged.
The first30controlled cases have12/12correct700ms backing offsets rather than
the980ms voice delay, with minimum positive score0.233357 and maximum negative
0.128504 in1.063s. A selection-calibrated0.20coarse reference floor then passes
all18retained Gospel cases and six actual-song estimated-vocal challenges in
2.922s. Those estimated-vocal challenges may contain backing bleed: they are NOT
physically instrument-free ground truth or a fresh independent corpus.

All6originals have six distributed coarse observations. Miku/Riona/Morisray
have key0 and coarse100ms; unc alternates100/200ms. Yoshino is+3semitones.
Forte is mainly-3 with inconsistent late/outro estimates, including a-4key
outro. These are analysis-reference-relative candidates, not applied offsets
or a license to call the final two covers accurately timed.

The standalone Java A55 core runs the same exact copied16kF32 inputs for all6
sources:6.219s process wall, all36coarse keys/offsets match the host. This does
NOT include decoding/resampling, SAF/native app entry, cancellation, media
operations, native timing, or mixed-output acceptance. The actual Android
decoder uses different analysis resampling, so parity cannot be assumed.
No APK installed/removed, player ownership changed, or production route enabled.

**Fine-stage failures led to a direct information check:** the original24
short-query waveform test fails two late-voice cases; both failed queries have
negligible known backing/mix RMS,0.0000469 and0.0002389. Do not force timing from
them. Choose between the SAME100ms and5100ms0.8s queries using ONLY clean-reference
RMS, before candidate loading; never retry a candidate-selected alternative.
Preserve the old24-window failures, all6planned regions and at least5/6support.

With that frozen selector, the known Gospel native map has6/6unique, correctly
timed anchors; this qualifier does NOT require the full native0.45amplitude gate.
Do not relabel it unchanged native acceptance. Its scale is
0.9999999991803834, offset699.999855ms, max residual0.0010225ms. Periodic ambiguity,
sign,4/6vs5/6and actual-anchor-center tests pass. A counterfactual coordinate
regression uses region-center reference coordinates but selected-query candidate
coordinates and creates3077.269ms endpoint error in its known-affine construction.
This is not an observed old production defect. This geometry test does NOT
prove actual time-stretch audio behavior or1%rate accuracy.

Native original observations keep the existing0.45score and0.02distinct-lag
gap, with every alternative more than2ms away considered. Only unc and Morisray
have6/6reference-relative maps: offsets152.962813ms and99.239443ms, respectively;
fit residuals0.008912ms and0.009844ms. **Fit residual is not independent timing
truth.** Miku and Riona each have only two eligible regions and no qualified
primary map. Do not subtract an uncertain primary map to publish a plausible
companion offset. Forte/Yoshino native key-shift timing remains unqualified.

Potential general product seam is an OPTIONAL local instrumental reference,
outside audible Stack slots, with URI permission/provenance/cancel/stale-result
handling. It is NOT implemented or approved as a production replacement here.
The owner-declined broad native-fit rescue remains excluded.

**Evidence:** private child
`reference-spectrum-followup-20261114-1243/`: `PROTOCOL.md`, `control-report.json`,
`holdout-02/`, `a55-core-01/`, `fine-01/`, `native-phase-01/`,
`reference-information-01/` and `selected-native-01/`, with frozen runners and
input hashes. Retain the initial metadata-field harness failure too.

### E32 — Fine alternatives stopped at their frozen failures

**Existing attack representation:** apply the app's unchanged positive
abs-difference0.8ms-fast/8ms-slow envelope against the independent reference,
with the same selected queries. Analytic different-carrier +/-3semitone events
pass, and Gospel timing lands near700ms in all6regions, but only4/6have strict
0.45eligibility. No original-six candidate adoption follows.

**One phase-whitened test:** the inspected LCAV GCC-PHAT equation is tested on
that SAME attack representation. It preserves sign/off-seed controls, but the
analytic key-change controls choose the550/850ms search boundaries rather than
700ms. All6Gospel delay values remain accurate, but their phase-domain scores
do not meet the separately declared0.45observation gate. A PHAT peak is NOT
amplitude NCC; do not silently relabel the metric or lower the guard to rescue
the test. Stop this candidate, with no actual-six phase-whitened adoption.

**Reusing the original fine spectrum after the information falsification:**
keep its FFT/bins/1ms hop/radius unchanged and change ONLY to the already-frozen
reference-only selector. One of6Gospel selected delays is697.816988ms, outside
the unchanged2ms bound; all primary scores are below0.45. This stops before
original-six observations. Do not relax2ms because the miss is only0.183ms past
the boundary. This is selection-informed regression, not a fresh blind corpus.

These failures do not justify another post-hoc band, threshold, query, model or
backend variation. The coarse identity improvement does not solve native
primary/key-shift timing. Before a new fine architecture, specify independently
falsifiable backing-event/transfer assumptions and retain every negative control.

**Evidence:** same private child: `reference-attack-01/`, `GCC_PROTOCOL.md`,
`gcc-01/`, `SELECTED_SPECTRUM_PROTOCOL.md`, `selected-spectrum-01/`. No app,
playback-quality, production matcher, new artifact, push or merge change.

### E33 — Full-song actual output and clock-registration falsification

The exact optimized1.22.30 default build, SHA2929ef6f…80c05, runs the real6
originals through normal UI: Miku primary, sound Review/live Apply of the existing
two proposals, pause, seek0, resume. Review is2.142s with warm retained data,
not cold matcher timing. The qualified raw device-output route retains
10,243,072native48k stereoS16 frames; the prospectively selected first210s has
10,080,000frames, with every native tail frame preserved. Float measurement is
EXACTLY nativeS16/32768. No resampling, repair, normalization, deletion or retiming
of the actual recording. Capture completion does NOT establish synchronization.

**Source-output measurement needs its own challenge.** The first fixed4..8s
Miku-only training correlation is only0.1325, with another placement about0.12.
Its conditional six-present/omission/wrong-sign/gain/duplicate controls pass,
but those are constructed FROM the chosen source columns: they do not prove
that the global source registration is correct. Therefore low real conditional
coefficients do NOT establish absent singers, a gain bug, or per-player drift.
Independent read-only review identified this circular-registration limitation.

The raw prefix independently has65,536exact-zero frames already at readiness,
and the first nonzero is frame215,040, capture4.48s. First nonzero is NOT source
zero truth: the source can have its own silent intro. But the old valid search
source0..12s with capture4..8s can only return origin[-4,+4]s, excluding a known
-4.48s construction. Keep that original failure and do not trim the real capture.

A prospectively declared8..12s registration corrects the search support. Its
single-Miku clock STILL fails a perfect nominal six-source synthetic mix:
-4.511854s instead of-4.48s,31.854ms wrong. This directly disproves the shared
assumption that a single voice's waveform gives the global clock of this mixture.
No real-output scoring follows that failed repair.

A separately frozen joint-template clock uses the SUM of all6known sources at
the existing UI offsets/gains, never per-voice lag/rate/gain fitting. It recovers
the constructed-4.48s origin exactly, NCC0.999999996, and passes the unchanged
synthetic held-out checks. However real joint training has NCC0.094879, below
the frozen0.90gate, so real held-out presence/gain/drift/loop conclusions remain
UNINTERPRETED. No post-hoc gain compensation or claim of a second recorder fault.
The output clock/processing model needs independent evidence before another
actual-song waveform acceptance claim. Original2ms/.90/.45/5%/.001gates remain.

The reference-run cleanup receipt initially stops after an unexpected test-package
query failure; it does not prove all restoration steps ran. Do not erase that
failure. The later separately authorized fullscreen check snapshots the package
query as already missing, verifies it unchanged, restores both temporary app-ops
toDEFAULT, retains music volume5/15 and original profile hashes, and stops the
owned A55 at07:57:26UTC. No package installed/removed in either resumed run.
This does not invent an earlier package-restore success or physical-phone QA.

**Evidence:** same private child: `actual-six30-fullsong-01/`,
`FULL_MIX_PROTOCOL.md`, `full-mix-score-01/INTERPRETATION.md`,
`capture-prefix-01/`, `PREFIX_REGISTRATION_PROTOCOL.md`, `full-mix-prefix-02/`,
`JOINT_REGISTRATION_PROTOCOL.md`, `full-mix-joint-03/`. Later restoration is in
`App/output/fullscreen-diagnosis-20261114/restoration.json`; that task's UI
findings are separate from Stack acceptance. No full-goal pass or new APK.

## E34 — post-DSP observation tracer remains incomplete; separate requested UI work

A private debug-only byte-copy tracer reproduces then passes buffer-lifetime and
partial/zero-write assertions. Its new flush-generation assertion is STILL RED;
it is not qualified or integrated. The four source/test files are preserved
byte-for-byte with failed receipts under the private E31 child
`post-dsp-observer-01/paused-src/` and `PARKED.md`. Do not mistake submitted bytes
for emitted samples or restart recorder-route qualification.

The owner then explicitly adds bottom/smaller speed hints, reverse upward unlock,
a clear expanded pull-handle gap, and app-themed useful diagnostics. Android31
is this separate UI scope, not a backing-engine change.595JVM checks and27A55
component checks pass after retained touch-target/test-fixture failures. Native
optimized delivery is separate. The original six-take backing/output acceptance,
primary/key-shift reference maps, shared-output high-resolution parity and
original public signer remain incomplete. No push, merge or full-goal pass.

**31UI delivery evidence, separate from alignment:** local source commitf6959b3
has the same355ba93d…e93d2fd app-input fingerprint as the27passing component tests.
Optimized assembly passes. Exact maintenance-signed APK SHA19658f77…fe26039
passes normal A55 native video advancement, seek/pause/resume/Home, bottom hint,
upward2x unlock, center handle spacing and the actual Developer report/picker.
Screenshots were visually inspected. One additional fullscreen round trip passes;
the later Mini check remains interrupted/unaccepted after all164QEMU threads
are observed suspended in Windows. One owned-process resume is recorded, then
the emulator exits. Do not relabel it an app crash or a Mini pass. Cleanup-only
same-profile boot restores original DEFAULT app-ops, volume5/15 and test-package
absence, verifies stored profile hashes and stops11:44:19UTC. Original signing
and the full backing/output goal remain open. Private receipts are under
`App/output/now-playing-controls-20261114/`.

## E35 — private post-DSP trace accounting, not native output acceptance

After the requested31UI work and its cleanup, the parked four Java files are
reused ONLY in a new private `post-dsp-flush-01/` child of E31's evidence folder.
No app source, package, playback path, matcher, threshold, format or APK changes.
The existing Media3 1.11.1/JUnit class files and JDK21 run a bounded JVM check.

The original THREE assertions reproduce exactly the original flush-generation
failure, then pass unchanged after per-record epoch metadata and a zero-byte
flush event recorded only AFTER successful delegation. Existing accepted bytes
remain retained and discardable; no output timestamp is invented. Six added
format/capacity/failure checks expose three genuine accounting failures: a writer,
audio write or flush exception can leave the trace marked healthy. All NINE pass
after invalidation while preserving original exception and buffer behavior.

The independent read-only source review identifies that the first concurrency
fixture never wraps its ring. The revised fixture reuses16slots for1001records
and checks owned-byte publication and epoch order. Only the TEST caller paces its
batches; the queue does not wait on the audio thread. A false write result that
still accepts bytes, followed by an empty retry, also preserves original PTS and
does not duplicate data. All ELEVEN checks pass. Original red receipts and all
earlier sources remain intact; this is not native performance evidence.

Important limits remain: `isComplete()` is only a no-recorded-failure predicate,
not producer sealing, writer drain, file integrity or emitted audio. Final counts
need producer quiescence/happens-before and persisted ordinal reconciliation.
Native configuration/encoding/rate/channels/offload and serialized write/flush
ownership are not proved. Audio-thread copy overhead is not measured. The next
test-only native instrument must predict a known premix with observer on/off on
the SAME qualified raw device-output route under unchanged2ms/.90/.45/5%/.001
checks. No new recorder route/budget, per-voice fitting or repaired media follows.
Do not copy this incomplete diagnostic into production or claim actual-six sync.

Independent source reviewer: existing `pipeline_readonly_review`; no builds or
device operations by the reviewer. Evidence: `post-dsp-flush-01/RESULT.md`, the
original/same-three red-green logs, added-control failures and
`eleven-pass-through-green-03/receipt.json`. Final private source SHA-256:

- `AcceptedPcmTraceQueue.java`: `b91932166244a8a4967c018a29107c49e4f7746c3ae45e97339e50be924e3a58`
- `AcceptedPcmTraceQueueTest.java`: `40ecf5068543e095976a83854b576419d9ad304d96ffcedcd86b4f763b364c56`
- `ObservedAudioOutput.java`: `b54c8f10ffcfe9981717893cc35abbc17f84bda79b9c53f53961ae7023935a0b`
- `ObservedAudioOutputTest.java`: `9d71323b951c62dc01e1bf56ec48f18e3aeba7c64291ff6567363f145c3ce134`

The committed app-input fingerprint stays355ba93d…e93d2fd and exact31APK remains
SHA19658f77…fe26039. No new APK, app version, branch, worktree, push, merge or
production adoption. The full original backing/output goal remains incomplete.

## E36 — existing Media3 provider compatibility and retained source-binding scope

No new provider implementation or app integration. The pinned Media3 1.11.1
`ForwardingAudioOutputProvider` is tested with its real configuration objects and
fake outputs. Sixteen PCM encoding/rate combinations through192k float retain
support/config/output identity, as do offload/tunneling requests. Buffer/session/
virtual-device/playback-parameter flags and original exception identity remain.
A deliberately48k/S16-coercing fixture fails the SAME check specifically for
encoding. This embedded negative control cannot pass from a missing-class error.

The first run does have missing-class failures: the selected transformed Guava
JAR contains only Maven metadata. Substitute the existing full pinned cached JAR
and failureaccess dependency, without changing assertions or versions. All FIVE
methods pass. The independent read-only reviewer finds no assertion false-pass,
but catches the copied runner's unused red-mode branch expecting the old flush
test. Remove that unsupported branch and bind the runner hash. All FIVE pass
again. No separate provider red launch, native negotiation/cadence or emitted-
sample qualification is claimed. This supports reusing the existing maintained
forwarder rather than rebuilding configs or forcing a lower-quality PCM route.

The native instrument remains incomplete and outside production. Evidence:
E31 child `post-dsp-provider-api-01/RESULT.md`, initial class-loading failures,
`full-cached-guava-02/receipt.json`, `provider-runner-fixed-03/receipt.json`.
Test SHA-256:`76d5d508605bcaab6fface0db043e05d97ae4e6938a945c318b608deed9066da`.
Independent read-only reviewer: existing `overnight_alignment_review`, not a
native tester. No app/source-quality, matcher, capture-route, APK, device, push,
merge or production change follows this isolated check.

A bounded read of E33's retained provenance finds no demonstrated wrong-file
cause: its adapter checks all6QA MP4 SHA-256 values against the six-entry media
manifest before normal UI selection. Its source-scoring scripts also bind all
6host PCM hashes to the earlier identifiability receipt. Those facts prove file
presence/retained measurement inputs, NOT which decoded samples each native
lane actually emits. No new decode, waveform fit or scorer follows. Future
post-DSP observation still needs an output-instance-to-source/config binding;
do not reinterpret E33's unqualified coefficients as missing singers or a gain
bug. Full original-six backing/output and native high-resolution parity remain
incomplete; unchanged app31 and private artifacts remain preserved.

**Final31UI Mini retry, separate from Stack:** the same optimized APK
SHA19658f77…fe26039 passes a clean targeted expanded → Mini → expanded flow.
PID1781stays alive; position advances12,988→31,268ms over18.281seconds with an
empty crash buffer. Actual Mini/returned-expanded screenshots are inspected.
DEFAULT access app-ops, volume5/15, baseline test-package absence and profile
hashes are preserved; the owned A55 stops12:32:14UTC. The earlier host-suspended
attempt stays recorded. This is not a fullscreen-shake repair, native tracer
qualification or actual-six backing pass. Exact receipt:
`App/output/now-playing-controls-20261114/a55-mini-retry-01/report.json`.
