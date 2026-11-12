# Stack music timing and A55 review — 12 November 2026

Status: diagnostic evidence, not a new matcher or verified release. Android remains
1.22.21/code342. Production source is unchanged from the local checkpoint
`c114441e04ef4dab7a7aca3ab21879402f5cdf80`. Nothing was pushed or merged.

## Target: shared backing music, not the singer

Align the shared instrumental timeline. Do not try to make different singers'
syllables, attacks or phrasing identical. Picture matching remains a separate
method and cannot establish matching sound.

The current sound path already uses stereo-side evidence and bounded
harmonic/percussive separation. This is not an isolated instrumental stem.
HPSS separates sustained and transient content, not specifically vocals and
accompaniment; see the [maintainer's explanation](https://github.com/librosa/librosa/issues/1352)
and [HPSS documentation](https://librosa.org/doc/main/api/generated/librosa.decompose.hpss.html).
Adding the same separation again is not a fix.

## A confirmed pitch-vote veto, with a limited conclusion

The private `BackingPitchVoteProbe.kt` compiles the current
`StackAlignmentMath.kt` and `StackInstrumentMath.kt`. It replays every retained
candidate, including alternatives and missing regions, from the earlier
six-take analysis. It does not decode again or change app code.

For Miku → Yoshino:

- The real candidates cover five of six planned regions. Their nearby timing
  peaks have four pitch-rotation votes of3 and one of0.
- The unchanged production function rejects the result. The pitch-consensus
  filter drops the final region, leaving only four of the required five.
- Marking only the pitch labels unknown, without changing timing, scores,
  alternatives, planned-region denominator or map tolerances, yields five
  supporting regions: offset20.769100ms, scale0.9999833593321745 and maximum
  residual1.098549ms. This is a coarse counterfactual, not an approved offset.
- The existing evidence selector would still require native-rate confirmation.
  That exact counterfactual was not run through the native confirmation in this
  probe. It is not permission to adopt the separately declined native-map rescue.

The probe covered11 retained pairs in9.308s including compilation. Its controls
retain rejection of insufficient regions and contradictory timing. Riona and
unc are unchanged. Forte still lacks distributed support; Morisray still has
inadequate mono support and competing side maps. Removing pitch labels alone
does not solve those tracks.

This isolates a pitch-consensus veto. It does **not** prove that vocals caused
the pitch disagreement, establish that the proposed timing is physically exact,
or qualify removal of the guard for unrelated songs. Before changing that
policy, test backing-identical/vocal-different mixtures and wrong-backing,
repeated-section and partial-match controls. A music-first rule must preserve
independent backing evidence rather than simply lower confidence thresholds.

Private evidence under `App/output/stack-alignment-audit-20261111/`:

- `BackingPitchVoteProbe.kt`: SHA-256
  `9867f7367ba2d789a1fce2f7dbb1b0c2190ee2b960164e7c4e799beb0d5e0c04`.
- `Run-BackingPitchVoteProbe.ps1`: SHA-256
  `6a87c18ddf9406b29873a1ff657407990737c09869ab9cbb98546b7c5c3b002d`.
- `pitch-vote-probe-01.log`: SHA-256
  `948ef934b4e9a71e89549a1fa1656505ec23b2c73262fd1743246cc8f7c08fe6`.
- Current source and the retained candidate log hash the same before and after.

## Six-take checks through the actual optimized app

The normal Library → Stack flow selected all six local *孤独毒毒* takes, reached
6/6 ready, set Miku as reference and paused before analysis. The tested app was
the existing optimized1.22.21 candidate, SHA-256
`439c3b87ccf0cf2db6c1db9c5f41a1fa974aaf553fd5b7588ce62dcf2b777cb2`.
These are mixed-cache UI intervals, **not cold backend benchmarks**.

| Method | Time to review | Observed result |
| --- | ---: | --- |
| Sound |122.636s|2/5 proposals: Riona about+0.65ms, unc+83.216ms; Forte, Yoshino and Morisray unchanged |
| Quick picture |48.349s|45s backend deadline reached;3/5 proposals, Riona unmatched and Yoshino unfinished |
| Explicit Wider picture |341.251s|4/5 proposals: Forte, Morisray and unc0ms; Yoshino+17ms; Riona unmatched |

The wider result meets the unchanged corpus ranges, including Yoshino+12..+28ms
and the Riona non-match. Quick remains a failed acceptance gate; do not extend
the deadline or reclassify the unfinished take as a confident rejection.

The review exposed all five companions, current → proposed timing, reasons,
manual controls and the method-specific explanation. The list scrolls internally;
Close and Apply stay in the footer. Actual sound top/bottom and wider picture
top/bottom screenshots were visually inspected. No result was applied in these
normal-flow runs. Closing left playback paused, and temporary storage/overlay
app-ops were restored to their original default values in the completed drivers.

Private logs and captures:

- `native-rate-hold/six-sound-normal-21.log`.
- `native-rate-hold/six-picture-normal-21b.log` and
  `sixpicture21b-{quick,wide}-rows.json` in that same directory.
- `a55-normal-flow/sixsound21-review-{top,bottom}.png` and
  `sixpicture21b-wide-review-{0,2}.png`.

The first picture driver failed in its private PowerShell result channel: progress
text contaminated an XML/JSON return value. Offline checks reproduced that exact
adapter failure, then passed after progress was separated from returned data.
The app, matcher, corpus ranges and deadlines were unchanged. Keep that failed
run separate from the final completed run; it was not a product crash or pass.

## Capture qualification: streaming is not speaker-output proof

The native MODE_STREAM two-tone calibration completed with `OK (1 test)`.
Two continuous PCM16 stereo44.1kHz writers emitted441Hz and733Hz tones with
source amplitude0.15. Phases commanded gains0/0,0.1/0,0/0.1,0.1/0.1 and0/0.
All gain calls succeeded. Recorded tone amplitudes stayed approximately0.149994
and0.149995 in every phase, including both-zero phases. The unchanged analyzer's
honored/bypassed/no-signal controls passed. Logged phase checkpoints report zero
underruns on both tracks; there is no final underrun measurement after the last
checkpoint.

This extends the previous native MODE_STATIC finding to native streaming on this
AVD. It is not a direct Media3 Solo test, a physical speaker recording or an
alignment verdict. The AudioFlinger dump for this streaming run was taken after
the runner ended and contains retired tracks, not simultaneous active-track proof.
Do not infer that Solo is broken or compensate guessed output lag from these
recordings. The owner asked to prioritize music matching; do not repeat recording
experiments before resolving the backing-evidence issue.

Evidence: `native-rate-hold/stream-volume-{driver,observer,analysis}-01.log` and
`capture-stream-volume-01.wav`, SHA-256
`1c60f2e2f721334aa116b9e8215b843353e245dc4d7c07e30401b742061dc60c`.
No microphone was used. The standard test package and optimized app were restored
before the six-track UI checks. A later final read-only ADB check found the A55
absent; do not claim a fresh installed-byte or permission recheck after that point.

## Preserve and continue

Keep the approved rejection-only repair, one selected timing map per track,
manual adjustments, saves, group readiness, full playback quality and separate
sound/picture methods. Do not enable the unfinished shared-output prototype or
adopt the declined native rescue. Any material new backing-evidence policy needs
the owner's agreement and unchanged false-match tests before app integration.

The exact1.22.19 and1.22.21 APKs were rehashed on disk and remain unchanged. No
new APK was assembled for these documentation and isolated diagnostic changes.
Alignment acceptance remains open. Never label this universally or sample-perfect.
