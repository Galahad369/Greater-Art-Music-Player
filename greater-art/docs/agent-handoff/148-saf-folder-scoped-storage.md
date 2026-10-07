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

This bootstrap commit is documentation-only. Executable migration commits must follow VERSION_RULES.md.
