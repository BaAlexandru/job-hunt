---
phase: 03-company-job-domain
plan: 03
subsystem: api
tags: [kotlin, spring-boot, jpa, rest, crud, pagination, filtering, salary-model, company-linking]

requires:
  - phase: 03-01
    provides: "Flyway V6 jobs migration, enums (WorkMode, JobType, SalaryType, SalaryPeriod), GlobalExceptionHandler, DomainExceptions"
  - phase: 03-02
    provides: "CompanyEntity, CompanyRepository (findByIdAndUserId, findAllByIdInAndUserId), CompanyService"
provides:
  - "JobEntity JPA entity for jobs table with salary model and enum fields"
  - "JobRepository with user-isolated queries, multi-parameter filtered search, company-job existence check"
  - "JobService with CRUD, company validation, batch company name resolution"
  - "JobController with 5 REST endpoints at /api/jobs"
  - "CreateJobRequest, UpdateJobRequest, JobResponse DTOs with validation"
  - "Company archive guard: 409 Conflict when company has active jobs"
  - "Integration tests covering JOBS-01 through JOBS-04 plus company archive guard"
affects: [04-application-status, job-listing, company-management]

tech-stack:
  added: []
  patterns: [company-linking-validation, batch-company-name-resolution, archive-guard-pattern, multi-parameter-jpql-filtering]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/JobEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/JobRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/JobDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/JobService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/JobController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/job/JobControllerIntegrationTests.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt

key-decisions:
  - "Plain UUID for companyId (no @ManyToOne) to keep entity simple and avoid lazy-loading issues"
  - "Batch company name resolution in list queries via findAllByIdInAndUserId for N+1 prevention"
  - "CAST(:title AS string) in JPQL consistent with company search pattern for null parameter safety"

patterns-established:
  - "Company linking validation: verify company belongs to user and is not archived before linking"
  - "Archive guard: check for active linked entities before allowing soft-delete"
  - "Batch name resolution: collect IDs from page, single query, build map for O(1) lookup"

requirements-completed: [JOBS-01, JOBS-02, JOBS-03, JOBS-04]

duration: 4min
completed: 2026-03-20
---

# Phase 3 Plan 3: Job CRUD Summary

**Job REST API with entity, repository, service, controller, company linking with validation, salary model (RANGE/FIXED/TEXT), multi-parameter filtering, and company archive guard (409 Conflict)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-20T02:40:49Z
- **Completed:** 2026-03-20T02:45:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Full Job domain slice: entity with 20 fields (enums, BigDecimal salary, TEXT description), repository with filtered JPQL query, service with company validation, controller with 5 endpoints
- Company linking: validated on create/update (must belong to user, not archived), companyName resolved in all responses, batch resolution for list queries
- Company archive guard added to CompanyService: 409 Conflict when non-archived jobs exist, preventing orphaned job-company links
- 22 integration tests covering all JOBS requirements plus company archive guard -- full suite green alongside existing auth and company tests

## Task Commits

Each task was committed atomically:

1. **Task 1: JobEntity, JobRepository, JobDtos, JobService, JobController, and CompanyService archive guard** - `8c78bc5` (feat)
2. **Task 2: Job integration tests and company archive guard test** - `aa24ff8` (test)

## Files Created/Modified
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/JobEntity.kt` - JPA entity mapping to jobs table with UUID PK, enums, BigDecimal salary, LocalDate closingDate
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/JobRepository.kt` - Spring Data JPA repository with filtered JPQL query (companyId, jobType, workMode, title search)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/JobDtos.kt` - CreateJobRequest, UpdateJobRequest, JobResponse with Jakarta validation and companyName
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/JobService.kt` - Business logic for CRUD with company validation and batch name resolution
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/JobController.kt` - REST controller with 5 endpoints under /api/jobs
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt` - Added JobRepository dependency and archive guard check
- `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/job/JobControllerIntegrationTests.kt` - 22 integration tests with helpers for job/company creation

## Decisions Made
- Used plain UUID for companyId field (no @ManyToOne JPA relationship) to keep the entity simple and avoid lazy-loading complexity
- Batch company name resolution for list queries using findAllByIdInAndUserId to prevent N+1 queries
- Applied CAST(:title AS string) in JPQL consistent with the company search pattern established in 03-02

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 3 complete: Company and Job domain fully implemented with CRUD, linking, filtering, and archive guards
- Ready for Phase 4 (Application Status tracking) which will link to jobs
- All 22 job tests + 16 company tests + auth tests passing in full suite

## Self-Check: PASSED

All 7 files found. All 2 commit hashes verified.

---
*Phase: 03-company-job-domain*
*Completed: 2026-03-20*
