# Storage migration — stage A

Source target: 1.21.30 / 253. Not a verified release or permission cutover.

`MediaSource` separates an absolute Android file path from a content URI.
`MediaReference` separates a logical item ID (including cue segments) from its
physical source and clip bounds. The Media3 adapter uses `Uri.fromFile` only for
files; content URIs remain content URIs. Parsing is not authorization.

Legacy migration preserves exact IDs, Unicode, spaces and unsupported records.
It does not rewrite preferences, normalize keys, infer SAF grants, or discard data.
The existing scanner continues producing filesystem entries. URI playback alone
does not establish parity for Stack, thumbnails, waveform or sharing.

## Remaining IO boundaries

| Component | Current limitation | Required next stage |
| --- | --- | --- |
| MediaScanner | Download traversal uses File | MediaStore index + persisted SAF roots |
| SafTreeReader | Bounded preview only | Return typed entries and explicit grant failures |
| ThumbnailRepository | Path metadata / sibling artwork | ContentResolver descriptors + document siblings |
| WaveformRepository | Path extractor / random access | Descriptor-backed extraction and cache keys |
| StackPcmStream | File source | URI descriptors with safe lifetime ownership |
| LocalLibraryCallback | Internal requests validate Download paths | Validate URI grants and typed library membership |
| Sharing / delete / import | Not migrated by this change | Android-confirmed URI operations and regression tests |

Do not remove MANAGE_EXTERNAL_STORAGE until all these paths have tested parity.
No permissions were added or removed in stage A. No network source is accepted.

## Evidence, not certification

MASVS-STORAGE-1 review: source identity isolation and lossless migration covered
by `MediaSourceTest`; device grant/revocation and scoped storage parity NOT RUN.
MASVS-PLATFORM-1 review: URI scheme restrictions tested; cross-app authorization
and malformed incoming Intent coverage remain NOT RUN. These are partial
engineering checks, not a MASVS compliance claim.

Reference: Android shared-media and Storage Access Framework documentation;
OWASP MASVS. SAF selection restrictions mean a tree grant is not equivalent to
unrestricted recursive Download access.

## Assessment record

| Control / scope | Method | Expected | Actual / status | Severity / follow-up |
| --- | --- | --- | --- | --- |
| MASVS-STORAGE-1 identity migration subset | JVM `MediaSourceTest` | Preserve exact logical IDs; reject unsupported sources without dropping records | Five tests passed; broader storage control NOT RUN | #148: broad access remains |
| MASVS-PLATFORM-1 URI adapter subset | Android `MediaSourceAndroidTest` | File encoding round-trip; content URI not converted into a file path | PASS: two tests on A55-sized API 36 emulator; not a permission-grant test | #148: resolver authorization / revocation |
| Public source secrets | `scripts/audit-public-repo.ps1 .` | No high-confidence credential / sensitive filename findings | PASS on local stage-A tree | #24: historical workstation paths still require separately scoped review |
| Release identity | Unmodified version validator | Latest verified artifact agrees with ledger | Working-tree FAIL: unrelated untracked 1.21.28 APK; preserved | No release promotion; clean tracked-tree check required |
| Production signing | Real release workflow / independent artifact verification | Proven chain from exact source to modern signed artifact | NOT RUN | #23: owner signing/provenance evidence |
| Complete MASVS assessment | All applicable controls and physical tests | Evidence-backed applicability and results | NOT RUN | #162: no compliance claim |

Retest URI adapter after scanner or resolver integration; retest legacy migration
before any preference-schema rewrite. APK identity, device and instrumentation
results must be appended after execution, never inferred from JVM results.

### Executed evidence — 2026-10-11

- Source: branch `codex/1.21.30-media-identities`, based on `ccfc0a3`;
  executable code unchanged during final verification. PR commit identifies the
  exact source; local debug artifact is not a production release.
- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`: PASS.
  323 JVM tests, zero failures/errors; lint zero errors, 31 warnings, two hints
  (one new UseKtx suggestion for explicit Uri.parse, not a security failure).
- Device: `GreaterArt_A55_API36`, emulator-5554, Android API 36; no physical phone.
- `am instrument ... MediaSourceAndroidTest`: PASS, two tests.
- Test APK: `app/build/outputs/apk/debug/app-debug.apk`, 26,795,669 bytes;
  version 1.21.30/code253; SHA-256
  `30dee35b221879b2987ea2b862ce29fdd3bd445b1553d936fc20d3a14f3b3cb2`.
- No decoder quality, bitrate, resolution or frame-rate restrictions added.
- Full playback/Stack/content grant revocation regression: NOT RUN. This PR does
  not enable a production URI-backed library or change any permission requests.

## Administrative boundaries

Historical exposure is not removed by adding a migration layer. No reachable
history, tags or immutable APKs were rewritten. A history rewrite needs explicit
owner authorization, collaborator coordination and post-rewrite checks.

ISO/IEC 27001 work is organizational: no legal operator, management approval,
internal audit, all-93-control applicability decision or certification is asserted
by this PR. Existing templates remain drafts until real organizational evidence.
