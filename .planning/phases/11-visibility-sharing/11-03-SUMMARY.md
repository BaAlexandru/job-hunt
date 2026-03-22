---
phase: 11-visibility-sharing
plan: 03
subsystem: ui
tags: [react, tanstack-query, shadcn, visibility, sharing, browse]

# Dependency graph
requires:
  - phase: 11-visibility-sharing
    provides: "Plan 01 backend migration, entities, repos, DTOs for visibility/sharing"
provides:
  - "Visibility type and isOwner field on CompanyResponse/JobResponse"
  - "ShareResponse, BrowseCompanyResponse, BrowseJobResponse types"
  - "TanStack Query hooks for visibility, shares, browse, shared-with-me"
  - "VisibilityBadge, VisibilityControl, ShareManager, BrowseCard components"
affects: [11-04-page-integration]

# Tech tracking
tech-stack:
  added: []
  patterns: [visibility-control-with-confirm, share-manager-crud, browse-card-pattern]

key-files:
  created:
    - frontend/hooks/use-visibility.ts
    - frontend/hooks/use-shares.ts
    - frontend/hooks/use-browse.ts
    - frontend/hooks/use-shared-with-me.ts
    - frontend/components/shared/visibility-badge.tsx
    - frontend/components/shared/visibility-control.tsx
    - frontend/components/shared/share-manager.tsx
    - frontend/components/browse/browse-card.tsx
  modified:
    - frontend/types/api.ts

key-decisions:
  - "Used sonner toast for mutation success/error feedback, matching existing codebase pattern"

patterns-established:
  - "VisibilityControl: Select dropdown with confirmation dialog for PUBLIC transitions"
  - "ShareManager: CRUD pattern with inline form and revoke confirmation"
  - "browseKeys/shareKeys/sharedWithMeKeys: query key factories following existing companyKeys pattern"

requirements-completed: [VISI-01, VISI-02, VISI-03, VISI-04]

# Metrics
duration: 5min
completed: 2026-03-22
---

# Phase 11 Plan 03: Frontend Hooks & Components Summary

**TanStack Query hooks for visibility/sharing/browse APIs plus VisibilityBadge, VisibilityControl, ShareManager, and BrowseCard reusable components**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-22T14:55:32Z
- **Completed:** 2026-03-22T15:00:26Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Updated api.ts with Visibility type, isOwner field, and 3 new response interfaces
- Created 4 hook files covering all visibility/sharing/browse/shared-with-me API operations with proper query key invalidation
- Created 4 reusable components: VisibilityBadge (icon display), VisibilityControl (select + confirm), ShareManager (share CRUD), BrowseCard (browse page card)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add visibility types to api.ts and create all hooks** - `5515f6d` (feat)
2. **Task 2: Create VisibilityBadge, VisibilityControl, ShareManager, and BrowseCard components** - `74f4b6e` (feat)

## Files Created/Modified
- `frontend/types/api.ts` - Added Visibility type, isOwner on CompanyResponse/JobResponse, ShareResponse, BrowseCompanyResponse, BrowseJobResponse
- `frontend/hooks/use-visibility.ts` - Mutations for setting company/job visibility with cache invalidation
- `frontend/hooks/use-shares.ts` - Query + create + revoke share hooks with shareKeys factory
- `frontend/hooks/use-browse.ts` - Paginated browse queries for companies/jobs with browseKeys factory
- `frontend/hooks/use-shared-with-me.ts` - Paginated shared-with-me queries with sharedWithMeKeys factory
- `frontend/components/shared/visibility-badge.tsx` - Globe/Users icon for PUBLIC/SHARED, null for PRIVATE
- `frontend/components/shared/visibility-control.tsx` - Select dropdown with PUBLIC confirmation dialog
- `frontend/components/shared/share-manager.tsx` - Share list, add form, revoke with confirmation
- `frontend/components/browse/browse-card.tsx` - Card for browse pages with owner email

## Decisions Made
- Used sonner toast for mutation success/error feedback, matching existing codebase pattern
- ApiError status codes used for contextual error messages (404 = no account, 409 = already shared)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- TypeScript compiler not directly accessible via npx; resolved by using `./node_modules/.bin/tsc` from frontend directory after `pnpm install`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All frontend building blocks complete and TypeScript-verified
- Ready for Plan 04 page-level integration (wiring hooks and components into routes)

---
*Phase: 11-visibility-sharing*
*Completed: 2026-03-22*
