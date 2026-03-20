---
phase: 03-company-job-domain
plan: 02
subsystem: api
tags: [kotlin, spring-boot, jpa, rest, crud, pagination]

requires:
  - phase: 03-01
    provides: "Flyway V5 companies migration, GlobalExceptionHandler, SecurityContextUtil, DomainExceptions"
provides:
  - "CompanyEntity JPA entity for companies table"
  - "CompanyRepository with user-isolated queries and filtered search"
  - "CompanyService with CRUD + soft-delete archive"
  - "CompanyController with 5 REST endpoints at /api/companies"
  - "CreateCompanyRequest, UpdateCompanyRequest, CompanyResponse DTOs"
  - "Integration tests covering COMP-01, COMP-02, COMP-03"
affects: [03-03-job-crud, company-job-linking]

tech-stack:
  added: []
  patterns: [user-isolated-repository-queries, soft-delete-archive-pattern, jpql-filtered-pagination]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/CompanyEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/CompanyRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/CompanyDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/CompanyController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/company/CompanyControllerIntegrationTests.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/CompanyRepository.kt

key-decisions:
  - "CAST(:name AS string) in JPQL to fix PostgreSQL lower(bytea) error with null parameters"

patterns-established:
  - "User-isolated repository: all queries filter by userId, never expose cross-user data"
  - "Soft-delete pattern: archived boolean + archivedAt timestamp, excluded from list by default"
  - "Integration test helper: registerAndGetToken() for authenticated API test setup"

requirements-completed: [COMP-01, COMP-02, COMP-03]

duration: 6min
completed: 2026-03-20
---

# Phase 3 Plan 2: Company CRUD Summary

**Company REST API with entity, repository, service, controller, DTOs, validation, soft-delete archive, paginated name search, and 16 integration tests**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-20T02:31:01Z
- **Completed:** 2026-03-20T02:37:20Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Full Company domain slice: entity, repository, service, controller, DTOs all compiling and validated against Flyway V5 schema
- 5 REST endpoints: POST, GET /{id}, GET (list), PUT /{id}, DELETE /{id} with user isolation via SecurityContextUtil
- 16 integration tests covering create (4), update+delete (5), list+get (7) -- all green alongside existing auth tests

## Task Commits

Each task was committed atomically:

1. **Task 1: CompanyEntity, CompanyRepository, CompanyDtos, CompanyService, and CompanyController** - `248ca35` (feat)
2. **Task 2: Company integration tests** - `139b219` (test)

## Files Created/Modified
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/CompanyEntity.kt` - JPA entity mapping to companies table with UUID PK, userId, soft-delete fields
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/CompanyRepository.kt` - Spring Data JPA repository with user-isolated queries and JPQL filtered search
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/CompanyDtos.kt` - CreateCompanyRequest, UpdateCompanyRequest, CompanyResponse with Jakarta validation
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt` - Business logic for create, getById, list, update, archive operations
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/CompanyController.kt` - REST controller with 5 endpoints under /api/companies
- `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/company/CompanyControllerIntegrationTests.kt` - 16 integration tests with auth helper

## Decisions Made
- Used CAST(:name AS string) in JPQL query to prevent PostgreSQL lower(bytea) type error when the name search parameter is null -- Hibernate passes null as untyped parameter which PostgreSQL can't apply lower() to

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed JPQL null parameter type casting for PostgreSQL**
- **Found during:** Task 2 (Company integration tests)
- **Issue:** JPQL query `LOWER(CONCAT('%', :name, '%'))` fails when `:name` is null because PostgreSQL receives it as bytea type and cannot apply `lower()` function
- **Fix:** Added `CAST(:name AS string)` in the CONCAT expression to ensure proper type handling
- **Files modified:** `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/CompanyRepository.kt`
- **Verification:** All 16 integration tests pass including null name search scenarios
- **Committed in:** `139b219` (part of Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Essential fix for query correctness with null parameters. No scope creep.

## Issues Encountered
None beyond the auto-fixed JPQL bug above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Company CRUD complete and tested, ready for Job CRUD (Plan 03-03)
- CompanyRepository.findAllByIdInAndUserId available for job-company linking validation
- Archive pattern established for reuse in Job entity

## Self-Check: PASSED

All 6 files found. All 2 commit hashes verified.

---
*Phase: 03-company-job-domain*
*Completed: 2026-03-20*
