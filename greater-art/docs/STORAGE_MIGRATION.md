# Storage migration — stages A and B

Current source target: 1.21.31 / 254. Not a verified release or permission cutover.

## Stage B — real metadata discovery, controlled inspection

`ScopedMediaDiscovery` produces actual `MediaFile`/typed-reference records.
`AndroidScopedDiscoveryAccess` queries the authorized Audio and Video MediaStore
collections under Download and enumerates only persistently granted SAF trees.
The Settings inspection action invokes this implementation and shows actual
media names. It is not a production Library storage-mode switch.

- No provider URI is converted to a filesystem path. On API26–28, DATA is only a
  Download query predicate, never a returned playable location or ID.
- Pending items are excluded on API29+, trashed items on API30+. MediaStore
  visibility depends on current Android grants; an empty index does not prove
  Download is empty. No READ_MEDIA permission is added or requested here.
- SAF entries use authority + document ID as stable identity and keep their tree
  URI as the readable location. Renaming preserves identity while the provider
  keeps its document ID. Provider reinstallation/ID reassignment is not promised
  to preserve identity forever.
- Same document IDs from overlapping grants are deduplicated. Same-name distinct
  documents are retained. Cross-provider byte-equivalence deduplication is not
  attempted; doing so by filename/size would risk hiding different user files.
- Every scan is a new snapshot. Failed or revoked roots do not reuse cached
  records. A source failure is reported while other authorized sources can remain.
- Cancellation reaches ContentResolver queries through CancellationSignal;
  cursors close on completion, failure or cancellation. Media bytes are not
  opened/decoded merely to list them. URI readability must be checked again by
  the future playback/operation resolver; metadata visibility is not a guarantee
  that a later open succeeds.
- Document, folder and depth limits produce explicit partial-result warnings,
  rather than silently presenting a truncated library as complete.

### Stage B verification procedure

Run JVM tests plus lint/build first. On the API36 A55-sized emulator, install
both debug APKs and run `ScopedDiscoveryAndroidTest`. Its MediaStore fixture is a
generated silent WAV in `Download/GreaterArtScannerTest`, with exact-URI cleanup.
The tests exercise pending exclusion, stable rescans, removal, ungranted trees,
partial audio/video denial, projection/selection and provider cancellation.

For actual SAF grant evidence, use the system folder picker to select **only**
`Download/GreaterArtScannerTest`, then invoke instrumentation with:

```text
-e safTree content://com.android.externalstorage.documents/tree/primary%3ADownload%2FGreaterArtScannerTest
-e class com.local.listentomusic.data.ScopedDiscoveryAndroidTest
```

The owned-fixture test additionally indexes that folder, opens the generated
WAV through its granted document URI, checks stable identity/removal, releases
the test grant and verifies denial on rescan. No arbitrary user folder is
accepted by this optional test path. Without this argument, these SAF-success
assertions do not run; do not claim real granted-SAF coverage from the default
four-test result alone. Temporarily disable All Files Access for this test and
restore the emulator's previous AppOps mode afterward.

The production filesystem scanner, Library queue and Stack remain unchanged.
Stage C must adapt PCM extraction, artwork, waveforms, metadata, lyrics/cue and
sharing/deletion before these entries become playable Library items. All Files
Access remains declared; this PR has **not** reduced the requested permission.

Android references: [shared media](https://developer.android.com/training/data-storage/shared/media),
[SAF](https://developer.android.com/training/data-storage/shared/documents-files),
[cancellable queries](https://developer.android.com/reference/android/content/ContentResolver).
SAF cannot select the Download root on Android11+, though eligible child folders
can be selected by the system picker. Do not advertise a grant as full Download
coverage.

### Stage B executed evidence — 2026-10-11

- Source 1.21.31/code254 on `codex/1.21.31-scoped-scanner`, based on `417f440`.
- Final `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`: PASS.
  337 JVM tests, zero failures/errors (14 discovery regressions). Lint zero errors,
  32 warnings and two hints. No permission, decoder or media-quality configuration
  changed. Initial Android fixture compile error was fixed before final checks.
- A55-sized API36 emulator (`emulator-5554`), All Files Access AppOps temporarily
  `ignore`, restored to prior `allow` after each test run. Default four Android
  scanner tests PASS on the final build. The preceding build also passed with
  the real `safTree` argument, including grant-backed discovery/open/rescan/delete/
  revocation assertions. The final change adds a snapshot-wide grant recheck,
  covered by the fourteenth JVM regression; real-tree assertions were not rerun
  after that final change.
- System picker refused Download root as expected. It granted only the generated
  `Download/GreaterArtScannerTest` child. The test released that grant and deleted
  only its exact generated WAV URI. No personal media was uploaded or deleted.
- Test APK: `app/build/outputs/apk/debug/app-debug.apk`, 27,976,832 bytes;
  SHA-256 `4b15bbe88ce4db378a81ea0bd7a88d945d073ebdeda358eab3dba543b55ce401`.
  This is a debug verification artifact, not a promoted release.
- Version guard and public-repository audit PASS on the Stage B working tree.
- Production URI Library/Stack/peripheral parity, API26–28 device coverage and
  physical-phone acceptance: NOT RUN. Latest recorded verified APK stays 1.21.18.

| MASVS subset | Method / expected | Actual | Remaining gate |
| --- | --- | --- | --- |
| STORAGE-1 / grant-bound discovery | JVM revoked-root tests + actual SAF grant/revocation, no broad access | PASS for metadata discovery subset | #148: production URI IO parity, not full control compliance |
| PLATFORM-1 / provider cancellation and denial | Real Android query path, missing grant / partially denied collections / cancellation | Four Android tests PASS; preceding build also passed real-tree assertions | Cross-app Intent/service attack tests remain NOT RUN |
| Local privacy / fixture handling | Generated owned file, exact cleanup, no personal media export | PASS within this bounded test | Production sharing/delete/privacy acceptance remains open |

Retest after Stage C connects discovered entries to playback. A typed URI and
successful scan must never substitute for a fresh permission/readability check.

## Stage A — identity foundation (1.21.30 / 253)

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
| AndroidScopedDiscoveryAccess | Typed metadata discovery, Settings inspection | Production Library and URI-operation parity |
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
