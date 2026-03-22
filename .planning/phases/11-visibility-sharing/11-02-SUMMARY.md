---
phase: 11-visibility-sharing
plan: 02
subsystem: api, service
tags: [kotlin, spring-boot, visibility, sharing, jpa, rest-api, tdd]

# Dependency graph
requires:
  - phase: 11-visibility-sharing plan 01
    provides: V16 migration, Visibility/ResourceType enums, ResourceShareEntity, visibility-aware repositories, DTOs, test stubs
provides:
  - ShareService with createShare/revokeShare/listShares and full validation
  - Visibility-aware CompanyService and JobService (getById, setVisibility, browsePublic, sharedWithMe)
  - BrowseController for public resource discovery
  - SharedWithMeController for shared resource access
  - Visibility/share endpoints on CompanyController and JobController
  - Full integration test suite (8 test classes, 43 tests)
  - Test infrastructure fix for Docker Compose port discovery
affects: [11-03, 11-04]

# Tech tracking
tech-stack:
  added: []
  patterns: [visibility-aware-reads, owner-only-writes, batch-fetch-emails, share-cleanup-on-delete]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/ShareService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/BrowseController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/SharedWithMeController.kt
    - backend/src/test/resources/application.yml
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/JobService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/CompanyController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/JobController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/visibility/ (8 test files)

key-decisions:
  - "Combined service and controller implementation in single TDD cycle since integration tests require endpoints"
  - "Test-specific application.yml with explicit DB config to bypass Docker Compose port discovery issue"
  - "resolveCompanyName in JobService uses findById (not findByIdAndUserId) for visibility-aware getById where viewer may not be owner"

patterns-established:
  - "Visibility-aware reads: getById uses findByIdWithVisibility, computes isOwner dynamically"
  - "Owner-only writes: update/archive/setVisibility use findByIdAndUserId"
  - "Browse public: batch-fetch owner emails via userRepository.findAllById to avoid N+1"
  - "Share cleanup: deleteByResourceTypeAndResourceId before archive to prevent orphaned shares"

requirements-completed: [VISI-01, VISI-02, VISI-03, VISI-04, VISI-05]

# Metrics
duration: 21min
completed: 2026-03-22
---

# Phase 11 Plan 02: Services & Controllers Summary

**ShareService with full share lifecycle, visibility-aware CompanyService/JobService, BrowseController, SharedWithMeController, and 43 passing integration tests**

## Performance

- **Duration:** 21 min
- **Started:** 2026-03-22T14:56:52Z
- **Completed:** 2026-03-22T15:18:37Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- ShareService handles create/revoke/list with self-share prevention, duplicate detection, ownership verification, and batch email lookup
- CompanyService and JobService support visibility-aware reads (owner, public, shared), owner-only writes, setVisibility, browsePublic with batch-fetched owner emails, and sharedWithMe
- Four controller endpoint groups: PATCH visibility, share CRUD on resources, browse public, shared-with-me
- All 43 visibility tests pass including enum, migration, default, service, and controller tests
- Fixed pre-existing Docker Compose port discovery issue with test-specific application.yml

## Task Commits

Each task was committed atomically:

1. **Task 1: ShareService, CompanyService, JobService with tests** - `3230640` (feat)
2. **Task 2: Controllers for visibility, sharing, browse, shared-with-me** - `8d30330` (feat)

## Files Created/Modified
- `ShareService.kt` - Share create/revoke/list with ownership verification and batch email lookup
- `CompanyService.kt` - Added setVisibility, browsePublic, sharedWithMe; updated getById for visibility-aware reads
- `JobService.kt` - Same visibility patterns as CompanyService, plus batch company name resolution for browse
- `CompanyController.kt` - Added PATCH /visibility, GET/POST/DELETE /shares endpoints
- `JobController.kt` - Same share/visibility endpoints as CompanyController
- `BrowseController.kt` - GET /api/browse/companies and /api/browse/jobs
- `SharedWithMeController.kt` - GET /api/shared/companies and /api/shared/jobs
- `application.yml` (test resources) - Explicit DB/Redis config bypassing Docker Compose discovery
- 8 test files in `visibility/` package - Full integration test implementations

## Decisions Made
- Combined Task 1 and Task 2 implementation because integration tests require working endpoints (TDD red phase needs controllers to test service behavior through HTTP)
- Created test-specific application.yml with explicit datasource config to work around Docker Desktop port mapping discovery issue
- JobService.resolveCompanyName changed to use findById (not findByIdAndUserId) since visibility-aware getById may be called by non-owners who still need to see company names

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created test-specific application.yml for Docker Compose port discovery**
- **Found during:** Task 1 (running tests)
- **Issue:** Spring Boot Docker Compose integration fails with "No host port mapping found for container port 5432" on Docker Desktop 4.65.0. `NetworkSettings.Ports` shows empty bindings despite correct compose.yaml port config.
- **Fix:** Created `backend/src/test/resources/application.yml` with explicit datasource URL, username, password, and Redis config. Set `spring.docker.compose.skip.in-tests: true`.
- **Files modified:** backend/src/test/resources/application.yml
- **Committed in:** 3230640

**2. [Rule 3 - Blocking] Changed resolveCompanyName to not require userId**
- **Found during:** Task 1 (implementing JobService.getById)
- **Issue:** Original resolveCompanyName(companyId, userId) uses findByIdAndUserId which fails for non-owner viewers of public/shared jobs
- **Fix:** Changed to use findById(companyId) which works regardless of viewer identity
- **Files modified:** JobService.kt
- **Committed in:** 3230640

---

**Total deviations:** 2 auto-fixed (2 blocking issues)
**Impact on plan:** Both fixes necessary for test execution and correct behavior. No scope creep.

## Issues Encountered
- Docker Desktop 4.65.0 on Windows has a known issue where `docker port` and `NetworkSettings.Ports` return empty even when ports are correctly configured in compose.yaml. The containers are accessible but Spring Boot's Docker Compose integration can't discover the mapping. Resolved by providing explicit connection properties in test config.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Full backend API for visibility and sharing is functional
- Plan 03 (frontend) can consume all endpoints: PATCH visibility, share CRUD, browse public, shared-with-me
- Plan 04 (E2E testing) has all backend endpoints ready for integration verification

## Self-Check: PASSED

All created files verified present. Both task commits verified in git log.

---
*Phase: 11-visibility-sharing*
*Completed: 2026-03-22*
