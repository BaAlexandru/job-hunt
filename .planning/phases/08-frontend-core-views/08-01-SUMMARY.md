---
phase: 08-frontend-core-views
plan: 01
subsystem: ui
tags: [tanstack-query, tanstack-table, shadcn-ui, typescript, react, better-auth, spring-security]

# Dependency graph
requires:
  - phase: 07-frontend-shell-auth-ui
    provides: Next.js app shell, Better Auth client, apiClient, shadcn/ui foundation
  - phase: 04-application-tracking
    provides: Application CRUD and status transition API endpoints
  - phase: 05-interview-management
    provides: Interview and interview notes API endpoints
  - phase: 06-document-management
    provides: Document upload, versioning, and linking API endpoints
provides:
  - BetterAuthSessionFilter bridging frontend session cookies to backend Spring Security
  - TypeScript types mirroring all backend DTOs (types/api.ts)
  - TanStack Query hooks for applications, companies, jobs, interviews, documents
  - Shared components: DataTable, ConfirmDialog, StatusBadge
  - View preference hook for board/list toggle with localStorage
  - apiClient FormData support for document uploads
affects: [08-02, 08-03, 08-04]

# Tech tracking
tech-stack:
  added: ["@tanstack/react-table", "react-dropzone", "@dnd-kit/core", "@dnd-kit/sortable", "@dnd-kit/modifiers", "@dnd-kit/utilities"]
  patterns: [TanStack Query key factory, optimistic mutation with rollback, filter-to-query-params pattern]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/BetterAuthSessionFilter.kt
    - frontend/types/api.ts
    - frontend/hooks/use-applications.ts
    - frontend/hooks/use-companies.ts
    - frontend/hooks/use-jobs.ts
    - frontend/hooks/use-interviews.ts
    - frontend/hooks/use-documents.ts
    - frontend/hooks/use-view-preference.ts
    - frontend/components/shared/confirm-dialog.tsx
    - frontend/components/shared/data-table.tsx
    - frontend/components/applications/status-badge.tsx
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/SecurityConfig.kt
    - frontend/lib/api-client.ts
    - frontend/package.json

key-decisions:
  - "BetterAuthSessionFilter queries Better Auth session+user tables via JdbcTemplate (not JPA) since those are not JPA entities"
  - "Session filter runs before JWT filter so cookie auth is tried first, JWT as fallback"
  - "Companies hook uses paginated response (matching actual backend API) instead of plan's array suggestion"
  - "All filter interfaces defined before query key factories to satisfy TypeScript strict type checking"

patterns-established:
  - "Query key factory pattern: domain-specific key objects with all/lists/list(filters)/details/detail(id) hierarchy"
  - "Optimistic mutation pattern: cancelQueries -> snapshot -> optimistic set -> rollback on error -> invalidate on settled"
  - "FormData upload pattern: use fetch() directly with credentials include, bypassing apiClient Content-Type"

requirements-completed: [APPL-03, APPL-04]

# Metrics
duration: 10min
completed: 2026-03-21
---

# Phase 8 Plan 01: Foundation Layer Summary

**TanStack Query hooks for all 5 domains, TypeScript DTO types, Better Auth session bridge, 11 shadcn/ui components, and shared DataTable/ConfirmDialog/StatusBadge components**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-21T01:24:14Z
- **Completed:** 2026-03-21T01:34:39Z
- **Tasks:** 3
- **Files modified:** 26

## Accomplishments
- Backend now accepts Better Auth session cookies alongside JWT tokens via BetterAuthSessionFilter
- All 11 shadcn/ui components installed (dialog, badge, table, popover, calendar, textarea, skeleton, scroll-area, command, kanban, alert-dialog)
- TypeScript types mirror every backend DTO with status transition map and color/label constants
- TanStack Query hooks cover all CRUD operations for applications, companies, jobs, interviews, and documents including notes, timeline, versions, and document-application links
- Shared reusable components (DataTable with sorting/pagination, ConfirmDialog, StatusBadge) ready for page views

## Task Commits

Each task was committed atomically:

1. **Task 0: Bridge backend auth to accept Better Auth session cookies** - `dd03b6c` (feat)
2. **Task 1: Install dependencies and shadcn/ui components** - `c1c6fd0` (chore)
3. **Task 2: Create TypeScript types, API hooks, shared components, and fix apiClient** - `7315918` (feat)

## Files Created/Modified
- `backend/.../security/BetterAuthSessionFilter.kt` - Spring filter validating Better Auth session cookies
- `backend/.../config/SecurityConfig.kt` - Added BetterAuthSessionFilter before JWT filter
- `frontend/types/api.ts` - All TypeScript types mirroring backend DTOs
- `frontend/hooks/use-applications.ts` - Application CRUD, status update, notes, timeline hooks
- `frontend/hooks/use-companies.ts` - Company CRUD hooks
- `frontend/hooks/use-jobs.ts` - Job CRUD hooks
- `frontend/hooks/use-interviews.ts` - Interview CRUD and interview notes hooks
- `frontend/hooks/use-documents.ts` - Document CRUD, versions, and document-application link hooks
- `frontend/hooks/use-view-preference.ts` - Board/list view toggle with localStorage
- `frontend/components/shared/confirm-dialog.tsx` - Reusable AlertDialog confirmation wrapper
- `frontend/components/shared/data-table.tsx` - Generic TanStack Table wrapper with sorting and pagination
- `frontend/components/applications/status-badge.tsx` - Colored status badge component
- `frontend/lib/api-client.ts` - Fixed FormData handling, exported API_BASE
- `frontend/package.json` - Added @tanstack/react-table, react-dropzone

## Decisions Made
- BetterAuthSessionFilter queries Better Auth tables via JdbcTemplate since they are not JPA entities
- Session filter placed before JWT filter in chain -- cookie auth tried first, JWT as fallback
- Companies hook returns PaginatedResponse matching actual backend API (plan suggested array, but backend returns Page)
- Filter interfaces defined before query key factories for TypeScript strict type resolution

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Companies hook uses paginated response**
- **Found during:** Task 2 (API hooks creation)
- **Issue:** Plan specified `useCompanies()` returns array, but backend CompanyController returns `Page<CompanyResponse>`
- **Fix:** Changed hook to return `PaginatedResponse<CompanyResponse>` matching actual backend API
- **Files modified:** frontend/hooks/use-companies.ts
- **Verification:** Build passes, types match backend
- **Committed in:** 7315918

**2. [Rule 1 - Bug] Fixed TypeScript strict type error in query key factories**
- **Found during:** Task 2 (build verification)
- **Issue:** `Record<string, unknown>` not assignable from specific filter interfaces under strict mode
- **Fix:** Changed key factory `list()` methods to accept specific filter interface types, moved interface definitions before factory declarations
- **Files modified:** All 4 hooks files
- **Verification:** pnpm build passes with no type errors
- **Committed in:** 7315918

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All types, hooks, and shared components ready for Plans 02-04 to build page views
- Plan 02 (Applications Board & List) can consume useApplications, StatusBadge, DataTable directly
- Plan 03 (Company, Job, Interview pages) can consume respective hooks
- Plan 04 (Document management UI) can consume useDocuments with FormData upload

---
*Phase: 08-frontend-core-views*
*Completed: 2026-03-21*
