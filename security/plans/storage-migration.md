# Least-privilege Android media access migration — plan

**Current fact (main 1.21.27/code250):** `AndroidManifest.xml` declares `MANAGE_EXTERNAL_STORAGE`. `MainActivity.PermissionAwareApp` routes users to All Files Access. Existing `SafTreeReader` + picker offer bounded **preview only**; production `MediaScanner`, model identities, Media3 playback, Stack, thumbnails, playlists, export, sharing and delete still use raw paths.

**Goal:** A fresh install and an upgrade can browse, play and manage supported user-selected media **without All Files Access**. No fabricated `content://` → `File` conversion, unwanted scanning, loss of playlist identity, quality reduction or deletion without user confirmation.

## API decisions

- **MediaStore:** default for discoverable Android audio/video collections, particularly Download media indexed by Android; use the correct version-dependent read permission and `content://` URIs. Do not assume every arbitrary Download subfolder/file is indexed.
- **Storage Access Framework (SAF):** explicit `OpenDocument` and `OpenDocumentTree` for permitted files/folders; request and persist the minimum read/write grant needed for operations. Respect Android 11+ picker restrictions (e.g., certain root/Download folder selections) instead of pretending users can pick all paths.
- **App-private storage:** thumbnail/cache/temporary generated data. Do not silently copy full libraries into it.
- **Media3:** consume `content://` playable sources via supported data sources. Pass file descriptors/URI access correctly to metadata, lyrics, waveform and Stack analyzers; don't globally convert URI strings to filesystem paths.
- **Delete/share:** use Android-confirmed MediaStore delete requests or authorized SAF `DocumentsContract` operations as appropriate; exact content-URI grant to recipients, no blanket file-provider root.

## Staged implementation and acceptance

| Stage | Change | Evidence / no-go rule |
|---|---|---|
| M0 audit | Enumerate every file-path assumption, media type, permission decision and Android version; data-flow/threat model for media identity and grants | Baseline inventory; no behavior change |
| M1 typed identity | Add explicit `MediaRef` identity/type, opaque stable IDs, resolver abstraction; migrate persisted Library/playlist/Stack references while preserving older path data | Migration/round-trip tests; no queue/artwork regression |
| M2 scan/index | Implement MediaStore scanner and bounded SAF provider scanner, cancellation, MIME/extension validation, dedupe and explicit consent | Works on fresh Android 11–current, revocation/read-only/offline/empty folders |
| M3 playback | Media3 audio/video, queue/session, Stack primary/companions, clip/cue/lyrics/media metadata from URI or descriptor | Original sample rate/quality; full seek/loop/primary/video-follow parity |
| M4 peripherals | Artwork/cache, shares, M3U, metadata edits, file deletion/write, favourites, backup/restore and permission relocation/revoke | No unauthorized delete/share; repeated pick/revoke/change tests; visible errors |
| M5 cutover | Only after device parity, remove `MANAGE_EXTERNAL_STORAGE`, old broad-permission onboarding and unneeded legacy permissions; test merged release manifest | Fresh install + upgrade both work **without broad grant**; regression matrix signed off |

## Specific regression matrix

- Android API 26/29/30/33/34/36 or supported available equivalents, at least one physical phone; pre-33 and 33+ permissions; fresh install and upgrade.
- Browse recursive permitted folder, search/filter/sort, thumbnails, background image/video, playlists, saved Stack, 2–8 voice Stack (where supported), A–B loop, queue, PiP/mini/fullscreen, export and shares.
- Permission denied, revoked while playing, provider process unavailable, renamed/moved/deleted content, duplicate files, nonexistent URIs, slow provider, no media, >2,000 file preview boundary, full disk, interrupted delete.
- Confirm no file reads outside chosen scope, no leaked private paths/URIs to logs, background no all-files prompt, correct prompted read/write access, no persisted grants left accidentally after explicit revoke.
- Before cutover run `aapt dump permissions` / merged-manifest check on **actual APK** and repeat release-source hash and signature checks.

## Decision gate / rollback

Keep old production path behind current behavior until full end-to-end testing. A passing SAF preview is **not** permission migration. Cutover must be separately versioned, reviewed, staged and reproducible; retain a tested rollback build and migration recovery strategy. Never downgrade media quality merely to satisfy permission goals.

Tracker: [#148](https://github.com/Galahad369/Greater-Art-Music-Player/issues/148).
References: https://developer.android.com/training/data-storage/manage-all-files ; https://developer.android.com/training/data-storage/shared/media ; https://developer.android.com/training/data-storage/shared/documents-files .
