# Backing music, not singer timing — independent diagnostics

Status: isolated host diagnostics only. Android remains1.22.22/code343;
the production matcher, acceptance rules, player and APK are unchanged.
This is evidence against calling sound alignment complete, not a release gate pass.

## A pitch-only repair is insufficient

`BackingVocalProbeV3.kt` compiles the current production feature, map-fit and
native-attack math. It uses six deterministic six-second PCM windows with a
known700ms delay. Positive cases have bit-identical backing arrays. A synthetic
voiced-harmonic layer changes independently. This is not real human vocal audio.
The negative changes both the drum pattern and the harmony, not just a random
seed applied to the same arrangement. All mixtures are finite and unclipped.

| Controlled input | Current coarse result | Existing mono confirmation |
| --- | --- | --- |
| Same backing, no voice | Correct700ms,6 anchors | Accepts; about0.4us from constructed truth |
| Same backing and voice | Correct700ms,6 anchors | Accepts |
| Same backing, vocal pitch changed in4/6 windows | All six local peaks are700ms; pitch-consensus rejects the map | Removing only pitch labels still leaves just2 native corrections; rejects |
| Same backing, only voice280ms later | Correct700ms,6 anchors | No native corrections; rejects |
| Different backing, same moderate voice | Insufficient support; rejects | Not reached |
| Different backing, same louder voice | Correctly follows the common **voice**,5 anchors | Accepts, despite the wrong backing |
| Different backing, no voice | Rejects | Not reached |

The narrow earlier idea—make pitch advisory but keep full-mix attack confirmation—
is **not sufficient**. Both stages can depend on the voice. The native confirmation
is a finer measurement of the supplied mixture, not independent proof that the
backing music matched. Do not lower thresholds or simply remove the pitch guard.

This probe mirrors the current mono confirmation boundary, not the full Android
decoder, stereo-selection policy, full-range competitor search or emitted sound.
Its700ms offset is constructed truth; it is not the six-song corpus's truth.
The printed summary counts coarse outcomes only, not final acceptance accuracy.

Evidence under `App/output/stack-alignment-audit-20261111/`:

- `BackingVocalProbeV3.kt`, SHA-256
  `6398e34d5c90ca905e2274bf70a0e8d32021140c749f8825ea0462487d583a1c`.
- `Run-BackingVocalProbeV3.ps1`, SHA-256
  `11cde27e2d799c082778afac8c77cdf2ef3620a44bb4f8433254402621a04344`.
- `backing-vocal-probe-03.log`, SHA-256
  `721dbf5ddd0b8c3d4c6daac2f697d4aaa0830e76ceb1a720fcb64b155c950a71`.

## A real instrumental reference

syudou publishes [孤独毒毒(カラオケ)](https://piapro.jp/t/w6Ht).
Its player exposes a public MP3 audition. The original download requires login;
that endpoint was not accessed. The page marks the work for non-commercial use;
see [Piapro's condition definitions](https://piapro.jp/license/pc/icon).
The public audition is retained privately for local QA only. Do not bundle or
redistribute it, or upload any of the user's takes to a separation service.

`BackingReferenceProbe.py` compares that instrumental with the six retained
PCM16 stereo44.1kHz takes. The diagnostic averages the channels and resamples
analysis only to16kHz. It searches each of the same six planned regions over
±15s, using normalized FFT correlation in three separately reported bands:
80–260Hz,600–2000Hz and2000–7000Hz. Known-delay and unrelated-noise controls
pass. Before/after SHA-256 checks confirm all six retained PCM inputs are unchanged.
There is no new application acceptance threshold or automatic offset Apply.

The first five distributed regions give these relative positions in the middle
and high bands after subtracting Miku's match to the same instrumental:

| Take | Observed position relative to Miku | Interpretation |
| --- | ---: | --- |
| Riona |+0.69..+1.06ms|Backing is already very close; does not prove the singer's delivery or actual Stack output is synchronized |
| unc |+82.94..+83.19ms|Independent support for the approximate83ms backing offset |
| Yoshino |+14.69..+15.19ms|Backing match exists despite the current rejection; not a qualification of the earlier20.77ms coarse proposal |
| Morisray |+29.19..+29.50ms|Strong repeatable backing peaks; the current full-mix rejection is not proof of different backing |
| Forte |No stable waveform match|Do not force a timing map. Pitch, arrangement and mix differences remain untested explanations |

These are measured peak ranges, **not** confidence intervals, imported timing maps
or a physical sample-accuracy claim. Band-dependent phase/EQ differences remain.
The final/outro region fails to give a comparable stable reference for several
takes. All six planned regions remain in the report; the unmatched final region
was not discarded to claim a complete-song pass. Yoshino's own high-band peaks
are consistent across all six, but Miku's last peak is not suitable for subtracting
a relative time. Bass results also contain distinct phase/mixture effects.

The host probe took11.384s including public-preview fetch, decode and analysis.
This is not a cold A55 benchmark or a speed claim for a proposed mobile engine.
The reference is a lossy public preview, not the lossless instrumental master.
Nothing in this result proves audible output, live Apply or universal rejection.

Private evidence:

- `BackingReferenceProbe.py`, SHA-256
  `06f66684fa8c20ac20d32f961e1f8a641e5abbe4b5d96b7df5e59e4191e9dc1b`.
- `backing-reference-01/reference-report.json`, SHA-256
  `14111707d2fbc59896e7472645d6a1fd7755dbcdcf51bbe4c3543380be1e3741`.
- Public audition:3023515bytes,188.9031875s decoded, SHA-256
  `e06f3754b92f4c292ae41fd73a74a77097c56c9d808dbdd059f5070299cffcdf`.

## Approved analysis-only separation trial

The owner subsequently approved an isolated offline separation-model trial:
check the license, backing/voice false matches and A55 cost before proposing
application integration. This did not approve replacing the production matcher,
adding an app dependency, publishing an APK or changing playback media.

### Model, runtime and checks

- Author-published KUIELab Track A vocal model from
  [DOI10.5281/zenodo.5717356](https://doi.org/10.5281/zenodo.5717356).
  The publisher metadata explicitly licenses the weights under CC BY4.0.
  The105MiB archive matches its published110359032bytes and MD5
  `72605d3722ebef18c353f831abdbfc07`. Only the vocal model was extracted:
  29703204bytes, SHA-256
  `daba83c2ee1afee9139766ad64c9b6808d6b6f092fff04bed3338be50baac721`.
- The MIT inference reference is pinned to
  `kuielab/mdx-net-submission@13c98c0ed9a759b82a3a6d10514738f3e8518365`.
  Source and notices are retained privately; downloaded Python was not executed.
  The ONNX graph uses standard operators, without external tensor files.
- Reused host ONNX Runtime1.20.1, NumPy, SciPy and FFmpeg. No global Python
  packages were installed. A separate permission-free Android QA package uses
  the pinned Microsoft MIT runtime1.20.0. It does not replace Greater Art.
- Model input is Float32 stereo44.1kHz with512 STFT frames. The output vocal
  estimate is subtracted from the original analysis mixture. Do not use either
  estimate as playback media, clip float estimates or advertise lossless separation.
- STFT/iSTFT round-trip retains the exact523264-sample length with maximum
  absolute error8.94e-8. This checks transform gain and sample indexing, not
  separation or acoustic accuracy.

### Measured resource and Android result

The default host allocator reached approximately2.37GiB peak private commit on
two short inputs. Disabling the CPU arena and memory-pattern cache released
memory between runs and kept three repeated predictions bit-exact, but the
transient allocation remains large: about1.14GiB peak private commit.

The unchanged A55-profile API36 x86_64 emulator ran a separate private probe,
`com.local.greaterart.separatorprobe`. It executed the exact retained Miku
tensor with CPU only, two threads, arena off and memory-pattern optimization off.
The512-frame model core took:

| Stage | Measured time |
| --- | ---: |
| Session/tensor setup |1302.1151ms|
| First inference |31754.3339ms|
| Second inference, same process/input |8030.8562ms|

Peak process RSS was1254224KiB, about1.20GiB. Checkpoint PSS is not peak PSS.
These are model-core times under the observed host/guest memory conditions,
not full-scan timings or physical Samsung hardware performance.

All4194304 returned floats match the retained host output **bit for bit**:
SHA-256 `95100091a71593295855d071cae3189c22cb0ac487bd421e568eb40e1fef8422`.
A one-float mutation is rejected by the comparison check. This qualifies that
single tensor/runtime path, not the full Android separation pipeline or song.
The helper was force-stopped after collection. No microphone, capture, network
or storage permission was granted. Installed Greater Art remains the exact
1.22.22 candidate hash `c9c1da2f5e317e5175fdd92f72660ec2abf8629ba8c04fa2a5b55768ff8a52ba`.
The driver also retained the original Power-saving setting. A55 configuration
and renderer were not changed.

### Music versus real-vocal controls

Two public seven-second MedleyDB vocal excerpts from the SigSep preview archive
provide real vocal layers: Lushlife's *Toynbee Suite* and Music Delta's *Gospel*.
Their per-track license entries are CC BY-NC-SA. They are MUSDB **training**
excerpts; the results are controlled-mixture diagnostics, not independent
held-out model-quality measurements. The media stays private and is not bundled.

Seven short controls include no voice, same voice, different singers, louder
different singers, a280ms-late voice, different backing with the same loud voice,
and different backing without voice. Constructed music delay is700ms. Mixtures
are finite and scaled below0.9 without clipping.

In the loud late-voice example, raw normalized correlation follows the voice at
980ms with score0.9211. Estimated backing instead peaks at the correct700ms,
score0.7205. With different music but the same loud voice, the peak score drops
from0.9441 to0.2796. These scores are not the app's acceptance policy.

The stronger six-region check uses the **unchanged** production feature,
map-fit, planned-region denominator and native44.1kHz confirmation functions:

| Constructed case | Raw mixtures | Estimated backing |
| --- | --- | --- |
| Same backing, voice280ms late |Incorrect confident980ms map across6/6 regions|Correct local700ms peaks, but only4/6 regions pass; global map rejects|
| Different backing, same loud voice |Incorrect confident700ms map across6/6 regions|No passing local anchors; correctly rejects|

The stem-input gate therefore remains **red**: one of its two truth conditions
fails. Do not lower the five-of-six requirement or call rejection of the wrong
music a complete repair. Separately inspected waveform peaks on estimated
backing recover700ms in all six positive regions (scores0.5865..0.7220), while
wrong-music scores remain0.2315..0.3076. That diagnoses a mismatch between the
current novelty-weighted feature gate and separated inputs; it does not approve
a new waveform threshold. This constructed search is8s, not the app's full±15s
competitor search, and does not exercise Android decoding or stereo selection.

The first private gate driver incorrectly called a native helper limited to
one second on a six-second feature window. Its retained exception is a harness
failure, not a model verdict. Native guards were preserved; feature samples were
prepared separately with anti-aliased16kHz polyphase resampling, while400ms
native checks retain44.1kHz. The completed raw/stem comparison then produced the
red result above without changing production source, thresholds or truth bounds.

### Actual six-take window and Forte clue

All six retained takes were checked in one shared real-music window. Reference
matching generally improves after vocal estimation; for example the middle-band
correlation increases from0.1915 to0.5150 for Miku,0.3143 to0.8220 for Riona,
0.3349 to0.7810 for unc,0.2141 to0.5664 for Yoshino and0.2457 to0.6379 for
Morisray. That is not whole-song or output acceptance. Earlier exact Miku/Riona
estimates were reused; the other four were generated with bounded allocator
settings. All original source hashes are unchanged.

Forte still has no useful raw-waveform match after separation. A separate
one-window chroma diagnostic gives a strong **minus-three-semitone clue**
(score0.8644; neighbouring rotations also score highly). Its synthetic
known-rotation/lag control passes. The coarse120ms peak has10ms hops and broad
spectral windows; it is **not** a fine timing map and must not be applied. Check
key/arrangement consistency across the song before deciding what matches. Do
not pitch-shift the user's playback or treat picture timing as sound truth.

### Retained trial evidence

Everything is under the same private
`App/output/stack-alignment-audit-20261111/` task folder. The trial subfolders
measured505.38MiB; only the selected model and two stem files were extracted.
Acquisition and run scripts remain beside the receipts. Notable receipts:

| Relative file | SHA-256 |
| --- | --- |
| `separator-model-01/acquisition.json` | `36d1535bc9721a563ab5608ecc72b5c3d8269b11ac6202d65b7cb1c374df6072` |
| `separator-host-memory-01/memory.json` | `1828f4fa86454f223d453a9aec4402e66e3fa621f4f40336860d6df1ae915384` |
| `separator-voice-controls-01/controls.json` | `8ccecba40835f43c6ca90aa588a2aa788323fd16ae062ab9570ad18214fe210a` |
| `separator-real-takes-01/real-takes.json` | `02037b3b57b9e37265643ee24d5c10bd291cc12c540d0adb49983e1e4fa39200` |
| `separator-a55-02/native.json` | `692234fa89a4e1dafbd1e9f3e9870cc191f90f43ee53243536e650ecb7fec37e` |
| `separator-a55-02/comparison.json` | `4558944ab5253d2df48aafba5e583c5f29c5b0bb404595eb534cf310c50666c8` |
| `separator-regional-gate-02/gate.log` | `8dc7d9489afa233d8f341d35984bbca6b5c7aeb43219f41f41d8cd3b3f862190` |
| `separator-forte-chroma-01/chroma.json` | `47c1152741ad490ffe4f27a5a839af7669902b8d49a9e641b050e71e9aa9a595` |

Retain the first Java build failure caused by the private driver's wrong SDK
folder (`android-37` instead of installed `android-37.0`) and the initial ADB
missing-package setup failure. Neither installed or tested a changed Greater Art
APK. They are not alignment, production compilation or runtime crash findings.

## Decision and limits

The backing-only reference is a useful independent QA oracle; it is not a viable
requirement that every user supply an official instrumental. A new general
music-first approach must identify backing evidence in **both** matching and
confirmation. The owner approved the isolated model/runtime trial above, not an
application policy change or integration. The tested model is **not ready to
integrate as-is**: resource cost is high and the unchanged six-region gate is
still red. Preserve the existing rejection-only repair and declined native-map
rescue; do not replace it with an unqualified stem-input swap.

Do not substitute singer alignment for backing alignment, use picture timing as
sound truth, or apply these host-derived figures automatically. Keep the sound
and picture methods separate, one selected map per track, saved/manual timing,
all group controls and original playback quality. Model-based source separation
remains offline and analysis-only. A backing-specific matcher, whole-song
key/structure verification, wider competitors and a lower mobile working set
still need independent checks before an integration proposal. No model, runtime
dependency or playback change was added to the application.

The picture Quick45s gate still fails. The existing wider picture results and UI
review/manual controls are unchanged. Both methods and actual output still need
acceptance; no100% or sample-perfect claim is supported.
