---
phase: 04-application-tracking
plan: 02
subsystem: api
tags: [jpql, spring-data, integration-tests, search, filter, pagination]

requires:
  - phase: 04-application-tracking-01
    provides: Application CRUD, status state machine, notes subsystem, REST endpoints

provides:
  - Cross-table JPQL filtered search query (applications + jobs + companies + notes)
  - Multi-value status filter, date range, text search, jobType/workMode/noteType/hasNextAction filters
  - Integration tests for all 5 APPL requirements (APPL-01, APPL-02, APPL-05, APPL-06, APPL-07)

affects: [frontend-application-views, visibility-sharing]

tech-stack:
  added: []
  patterns: [JPQL cross-table LEFT JOIN with EXISTS subquery, CAST for null-safe params, batch name resolution]

key-files:
  created:
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/application/ApplicationControllerIntegrationTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/application/ApplicationNoteControllerIntegrationTests.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/ApplicationRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/JobRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/ApplicationService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/ApplicationController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/auth/AuthControllerIntegrationTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/company/CompanyControllerIntegrationTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/job/JobControllerIntegrationTests.kt

key-decisions:
  - "JPQL with LEFT JOIN and EXISTS subquery for 4-table search rather than Criteria API or Specifications"
  - "CAST(:param AS type) IS NULL pattern for null-safe optional parameters in JPQL"
  - "Empty status list converted to null to avoid SQL IN () error"

patterns-established:
  - "Cross-table search: LEFT JOIN entities in JPQL, EXISTS subquery for related collections"
  - "FK-safe test cleanup: delete child tables before parent tables in @BeforeEach"

requirements-completed: [APPL-01, APPL-02, APPL-05, APPL-06, APPL-07]

duration: 27min
completed: 2026-03-20
---

# Phase 04 Plan 02: Application Search/Filter & Integration Tests Summary

**4-table JPQL search query with LEFT JOINs across applications/jobs/companies and EXISTS subquery for notes, plus 30+ integration tests covering all APPL requirements**

## Performance

- **Duration:** 27 min
- **Started:** 2026-03-20T11:40:36Z
- **Completed:** 2026-03-20T12:07:25Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Cross-table JPQL query searching across job title, company name, quick notes, note content, and job description
- Multi-value status filter, date range, jobType, workMode, hasNextAction, noteType filters combined in single paginated endpoint
- 30+ integration tests proving APPL-01 (CRUD), APPL-02 (status transitions), APPL-05 (date auto-tracking), APPL-06 (notes + auto STATUS_CHANGE), APPL-07 (search/filter)
- Full test suite green (87 tests across auth, company, job, and application domains)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add filtered search JPQL query and wire search/filter params** - `a806682` (feat)
2. **Task 2: Integration tests for all APPL requirements** - `b69f09a` (test)

## Files Created/Modified
- `ApplicationRepository.kt` - Added findFiltered JPQL with 4-table LEFT JOINs and EXISTS subquery
- `ApplicationService.kt` - Added listFiltered with empty status list handling and batch name resolution
- `ApplicationController.kt` - Updated list endpoint with q, status, companyId, jobType, workMode, dateFrom, dateTo, hasNextAction, noteType params
- `JobRepository.kt` - Added findAllByIdIn for batch job resolution
- `ApplicationControllerIntegrationTests.kt` - 22 tests for APPL-01, 02, 05, 07
- `ApplicationNoteControllerIntegrationTests.kt` - 8 tests for APPL-06
- `AuthControllerIntegrationTests.kt` - FK-safe cleanup
- `CompanyControllerIntegrationTests.kt` - FK-safe cleanup
- `JobControllerIntegrationTests.kt` - FK-safe cleanup

## Decisions Made
- Used JPQL with LEFT JOINs rather than Criteria API for readability and consistency with existing repository patterns
- CAST(:param AS type) IS NULL pattern for null-safe optional parameters (established in Phase 3)
- Empty status list converted to null to prevent SQL IN () syntax error

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added missing findAllByIdIn to JobRepository**
- **Found during:** Task 1
- **Issue:** ApplicationService.list (from 04-01) used jobRepository.findAllByIdIn but method was not in committed JobRepository
- **Fix:** Added `fun findAllByIdIn(ids: Set<UUID>): List<JobEntity>` to JobRepository
- **Files modified:** JobRepository.kt
- **Verification:** ./gradlew :backend:classes compiles successfully
- **Committed in:** a806682

**2. [Rule 1 - Bug] Fixed FK-safe test cleanup order in Auth/Company/Job tests**
- **Found during:** Task 2
- **Issue:** Existing test @BeforeEach methods deleted jobs/companies without first deleting applications and application_notes, causing FK constraint violations
- **Fix:** Added applicationNoteRepository.deleteAll() and applicationRepository.deleteAll() before jobRepository.deleteAll() in all test classes
- **Files modified:** AuthControllerIntegrationTests.kt, CompanyControllerIntegrationTests.kt, JobControllerIntegrationTests.kt
- **Verification:** ./gradlew :backend:test passes all 87 tests
- **Committed in:** b69f09a

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Application tracking domain complete with full CRUD, status state machine, notes, and search/filter
- All APPL requirements verified with integration tests
- Ready for Phase 5 (Interview Management) or Phase 6.1 (Visibility & Sharing)

---
*Phase: 04-application-tracking*
*Completed: 2026-03-20*
