# Issue #148 — SAF folder-scoped storage migration

This branch is the implementation workspace for GitHub issue #148.

## Goal

Make the normal app flow work with user-selected Storage Access Framework folders instead of depending on broad MANAGE_EXTERNAL_STORAGE access.

## Work to implement

1. Folder selection
   - ACTION_OPEN_DOCUMENT_TREE
   - persist read/write URI permission where required
   - clear/reselect handling

2. Media abstraction
   - stop assuming every media source is a raw filesystem path
   - support ContentResolver / DocumentFile traversal
   - preserve stable media identity for playlists, cache keys and rescans

3. Operations
   - scan/index
   - playback
   - metadata/artwork extraction
   - thumbnail generation
   - open/share
   - deletion
   - configured-folder migration

4. Permission posture
   - fresh install must operate using scoped folder access
   - revoked permission fails safely and offers re-selection
   - do not expand access beyond the selected tree
   - keep the app local/offline

5. Compatibility
   - if a broad-access sideload/power-user mode remains necessary, isolate and document it rather than making it the normal architecture.

## Acceptance

A fresh install can select a folder and complete the normal media workflow without MANAGE_EXTERNAL_STORAGE.

## Implemented first stage — 1.21.25/code248 (SOURCE_ONLY)

This PR now includes a **bounded SAF permission + preview slice**, not only a handoff:

- Settings lets the user choose a folder using the Android system `OpenDocumentTree` picker and persist a **read-only** tree URI grant.
- A scoped, `ContentResolver`/`DocumentsContract`-only reader enumerates descendants under the granted tree (maximum 2,000 documents, 128 folders, depth 24); it refuses missing/revoked persisted grants.
- Preview reports the number of media candidates without mapping provider URIs to raw filesystem paths or touching files outside the chosen tree. Unsupported documents are ignored; optional MIME + extension candidate tests are included.
- Settings can revoke the previous grant or select a replacement. The device-specific URI key stays outside the portable settings-backup allowlist and is cleared on reset.

**This is deliberately NOT a completed SAF migration:** production Library scanning, playback, metadata, artwork, sharing, deletion, playlists and Stack still assume filesystem paths. The existing broad-permission flow remains in the app and no new claim of permission-free full Library playback is made. The chosen tree is currently a local preview only.

## Next implementation acceptance stages

1. Introduce stable provider URI-backed media identities in the shared model and scan/index repository, with revocation-state tests and library parity.
2. Adapt Media3 playback and metadata/artwork/thumbnail operations to ContentResolver streams/file descriptors without assuming `File(sourcePath)`.
3. Migrate playlist, history, Stack and external share/deletion flows; test reconnect/rescan and stable identities across app restart.
4. Make scoped folder selection the default once the entire end-to-end flow works. Keep broad access isolated to an explicit power-user mode if necessary.
5. Require exact-head CI, app/device testing and explicit permission review before merging this change as a production migration.

The initial bootstrap was documentation-only. This source stage remains in PR #155 for further development.
