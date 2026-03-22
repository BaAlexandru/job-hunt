---
phase: 10-gap-closure
plan: 01
subsystem: ui
tags: [react, tanstack-query, react-dropzone, document-versions, expandable-rows]

# Dependency graph
requires:
  - phase: 08-documents
    provides: Document CRUD UI, version hooks, DocumentResponse/DocumentVersionResponse types
provides:
  - Expandable version history panel in document list
  - Inline version upload, set-current, download, delete UI
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Expandable row pattern with single-expand state and chevron rotation"
    - "Inline dropzone for sub-entity upload within expanded panel"

key-files:
  created: []
  modified:
    - frontend/components/documents/document-list.tsx

key-decisions:
  - "Replaced DataTable with custom row rendering to support expandable content without modifying shared DataTable component"
  - "Extracted VersionDownloadButton as separate component to satisfy React hooks rules for useDownloadVersionUrl"

patterns-established:
  - "Expandable row: useState<string | null>(null) for single-expand, ChevronRight with rotate(90deg) transition"
  - "VersionPanel sub-component pattern: co-located in same file, receives documentId prop, manages own mutation state"

requirements-completed: [GAP-02]

# Metrics
duration: 3min
completed: 2026-03-22
---

# Phase 10 Plan 01: Document Version History Panel Summary

**Expandable version history panel in document list with upload, set-current, download, and delete capabilities per version**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-22T14:21:31Z
- **Completed:** 2026-03-22T14:24:41Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Replaced DataTable usage with custom expandable row list supporting per-document version panels
- Wired all 5 version hooks (query, create, set-current, download URL, delete) into the UI
- Added inline dropzone for uploading new versions with optional note field
- Added delete protection when only 1 version remains

## Task Commits

Each task was committed atomically:

1. **Task 1: Add expandable version history panel to document-list.tsx** - `e01f7a1` (feat)

## Files Created/Modified
- `frontend/components/documents/document-list.tsx` - Replaced DataTable with expandable row list; added VersionPanel sub-component with version metadata display, upload dropzone, set-current/download/delete actions, and ConfirmDialog for delete confirmation

## Decisions Made
- Replaced DataTable with custom row rendering rather than modifying the shared DataTable component, since DataTable does not natively support expandable rows
- Extracted VersionDownloadButton as a separate component to call useDownloadVersionUrl per-version without violating React hooks rules (hooks must be called at component top level)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Document version management UI is complete and ready for user testing
- GAP-02 requirement closed; remaining phase 10 plans (GAP-01 interview notes, GAP-03 password reset email) can proceed independently

---
*Phase: 10-gap-closure*
*Completed: 2026-03-22*
