---
phase: 03-company-job-domain
verified: 2026-03-20T04:00:00Z
status: passed
score: 15/15 must-haves verified
re_verification: false
---

# Phase 3: Company and Job Domain — Verification Report

**Phase Goal:** Company and Job domain models with full CRUD, soft-delete, search, and cross-entity business rules
**Verified:** 2026-03-20T04:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| #  | Truth                                                                                     | Status     | Evidence                                                                                   |
|----|-------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------|
| 1  | SecurityContext provides authenticated userId (UUID) without extra DB query               | VERIFIED   | AppUserDetails.getUserId() returns stored UUID; SecurityContextUtil.getCurrentUserId() casts principal to AppUserDetails |
| 2  | Companies and jobs tables exist in PostgreSQL with correct schema                          | VERIFIED   | V5 and V6 migrations present with all columns, FKs, and composite indexes                  |
| 3  | All error responses use standardized ErrorResponse shape                                   | VERIFIED   | GlobalExceptionHandler returns ErrorResponse for all exception types including 404, 409, 500 |
| 4  | Domain enums (WorkMode, JobType, SalaryType, SalaryPeriod) are defined and reusable       | VERIFIED   | enums.kt contains all four enums; used by JobEntity and JobDtos                            |
| 5  | User can create a company with name, website, location, and notes via POST /api/companies  | VERIFIED   | CompanyController @PostMapping calls companyService.create(); 4 integration tests cover this |
| 6  | User can update a company via PUT /api/companies/{id}                                      | VERIFIED   | CompanyController @PutMapping delegates to companyService.update(); test confirms 200 |
| 7  | User can soft-delete (archive) a company via DELETE /api/companies/{id}                    | VERIFIED   | archive() sets archived=true, archivedAt=Instant.now(); returns 204; test confirms |
| 8  | User can list their companies with pagination and name search via GET /api/companies        | VERIFIED   | findFiltered JPQL with CAST(:name AS string) for null safety; tests confirm name search |
| 9  | User can get a single company via GET /api/companies/{id}                                  | VERIFIED   | getById throws NotFoundException for missing/other-user; test confirms 200 |
| 10 | Accessing another user's company returns 404                                               | VERIFIED   | findByIdAndUserId enforces user isolation; two isolation tests pass |
| 11 | Archived companies are excluded from list by default                                       | VERIFIED   | findFiltered: includeArchived=false by default; test confirms 1 of 2 returned |
| 12 | User can create a job with title, salary, work mode, job type, and closing date            | VERIFIED   | JobEntity has all fields @Enumerated; CreateJobRequest accepts all; test with all fields passes |
| 13 | User can link a job to an existing company (nullable, mutable)                             | VERIFIED   | JobService.validateCompany() checks ownership; companyName resolved in response; 4 tests cover this |
| 14 | Company cannot be archived if it has non-archived linked jobs (409 Conflict)               | VERIFIED   | CompanyService.archive() calls jobRepository.existsByCompanyIdAndUserIdAndArchivedFalse(); throws ConflictException; two tests confirm 409 and 204 after job archive |
| 15 | User can store and retrieve the full job description text                                  | VERIFIED   | description mapped to TEXT column; test stores 2000-char string and retrieves it unchanged |

**Score:** 15/15 truths verified

---

### Required Artifacts

| Artifact                                                                                          | Provides                                    | Status     | Details                                                             |
|---------------------------------------------------------------------------------------------------|---------------------------------------------|------------|---------------------------------------------------------------------|
| `backend/src/main/kotlin/.../security/AppUserDetails.kt`                                          | Custom UserDetails carrying userId UUID      | VERIFIED   | Contains `class AppUserDetails`, `fun getUserId(): UUID`             |
| `backend/src/main/kotlin/.../security/SecurityContextUtil.kt`                                     | Utility to extract userId from SecurityContext | VERIFIED | Contains `object SecurityContextUtil`, `fun getCurrentUserId(): UUID` |
| `backend/src/main/kotlin/.../entity/enums.kt`                                                     | WorkMode, JobType, SalaryType, SalaryPeriod  | VERIFIED   | All four enums present; used in JobEntity and JobDtos                |
| `backend/src/main/resources/db/migration/V5__phase03_create_companies.sql`                        | Companies table DDL                          | VERIFIED   | Contains CREATE TABLE companies with all 10 columns and 2 indexes   |
| `backend/src/main/resources/db/migration/V6__phase03_create_jobs.sql`                            | Jobs table DDL                               | VERIFIED   | Contains CREATE TABLE jobs with all 22 columns and 5 indexes; NUMERIC(15,2) for salary |
| `backend/src/main/kotlin/.../config/GlobalExceptionHandler.kt`                                    | Unified error handling for all controllers   | VERIFIED   | @RestControllerAdvice; handles NotFoundException, ConflictException, MethodArgumentNotValidException, plus auth and generic exceptions |
| `backend/src/main/kotlin/.../dto/ErrorResponse.kt`                                                | Standardized error DTO                       | VERIFIED   | data class with status, error, message, path, timestamp, fieldErrors |
| `backend/src/main/kotlin/.../service/DomainExceptions.kt`                                         | NotFoundException, ConflictException         | VERIFIED   | Both classes defined; used by CompanyService and JobService          |
| `backend/src/main/kotlin/.../entity/CompanyEntity.kt`                                             | JPA entity for companies table               | VERIFIED   | @Table(name="companies"), userId UUID, archived Boolean, equals/hashCode |
| `backend/src/main/kotlin/.../repository/CompanyRepository.kt`                                     | User-isolated Spring Data repository         | VERIFIED   | findByIdAndUserId, findFiltered JPQL, findAllByIdInAndUserId all present |
| `backend/src/main/kotlin/.../service/CompanyService.kt`                                           | Business logic for company CRUD              | VERIFIED   | create, getById, list, update, archive with JobRepository guard      |
| `backend/src/main/kotlin/.../controller/CompanyController.kt`                                     | REST endpoints for /api/companies            | VERIFIED   | @RequestMapping("/api/companies"); 5 endpoints; SecurityContextUtil called in every handler |
| `backend/src/test/kotlin/.../company/CompanyControllerIntegrationTests.kt`                        | Integration tests COMP-01, COMP-02, COMP-03 | VERIFIED   | 16 tests covering create, update, archive, list, search, isolation   |
| `backend/src/main/kotlin/.../entity/JobEntity.kt`                                                 | JPA entity for jobs table                    | VERIFIED   | @Table(name="jobs"); enums with @Enumerated(STRING); BigDecimal salary; LocalDate closingDate |
| `backend/src/main/kotlin/.../repository/JobRepository.kt`                                         | Spring Data repository with filtering        | VERIFIED   | existsByCompanyIdAndUserIdAndArchivedFalse, findFiltered JPQL with 5 filters |
| `backend/src/main/kotlin/.../service/JobService.kt`                                               | Business logic for job CRUD                  | VERIFIED   | validateCompany uses findByIdAndUserIdAndArchivedFalse; batch company name resolution via findAllByIdInAndUserId |
| `backend/src/main/kotlin/.../controller/JobController.kt`                                         | REST endpoints for /api/jobs                 | VERIFIED   | @RequestMapping("/api/jobs"); 5 endpoints; SecurityContextUtil called in every handler |
| `backend/src/test/kotlin/.../job/JobControllerIntegrationTests.kt`                                | Integration tests JOBS-01 through JOBS-04    | VERIFIED   | 22 tests plus company archive guard; all scenarios from plan present |

**AuthExceptionHandler.kt:** Confirmed deleted (replaced by GlobalExceptionHandler). File does not exist.

---

### Key Link Verification

| From                     | To                      | Via                                       | Status  | Evidence                                                              |
|--------------------------|-------------------------|-------------------------------------------|---------|-----------------------------------------------------------------------|
| UserDetailsServiceImpl   | AppUserDetails          | Returns AppUserDetails instead of Spring User | WIRED | `return AppUserDetails(userId = user.id!!, ...)` — no User.builder() |
| SecurityContextUtil      | AppUserDetails          | Casts principal to AppUserDetails          | WIRED   | `authentication.principal as AppUserDetails`                          |
| GlobalExceptionHandler   | ErrorResponse           | Returns standardized error shape           | WIRED   | buildResponse() creates ErrorResponse; all 9 handlers use it         |
| CompanyController        | SecurityContextUtil     | Extracts userId for all operations         | WIRED   | SecurityContextUtil.getCurrentUserId() in all 5 handler methods       |
| CompanyService           | CompanyRepository       | All queries filter by userId               | WIRED   | findByIdAndUserId used in getById, update, archive; findFiltered in list |
| CompanyController        | CompanyService          | Delegates business logic                   | WIRED   | companyService.create/getById/list/update/archive called in handlers  |
| JobController            | SecurityContextUtil     | Extracts userId for all operations         | WIRED   | SecurityContextUtil.getCurrentUserId() in all 5 handler methods       |
| JobService               | CompanyRepository       | Validates company ownership when linking   | WIRED   | companyRepository.findByIdAndUserIdAndArchivedFalse() in validateCompany() |
| CompanyService           | JobRepository           | Checks for linked jobs before archiving    | WIRED   | jobRepository.existsByCompanyIdAndUserIdAndArchivedFalse(id, userId) in archive() |
| JobRepository            | JobEntity               | JPQL filtering with 5 parameters           | WIRED   | findFiltered JPQL uses j.companyId, j.jobType, j.workMode, j.title   |

---

### Requirements Coverage

| Requirement | Source Plan | Description                                                    | Status    | Evidence                                                                              |
|-------------|-------------|----------------------------------------------------------------|-----------|---------------------------------------------------------------------------------------|
| COMP-01     | 03-01, 03-02 | User can add a company with name, website, location, and notes | SATISFIED | CompanyController POST + CompanyService.create(); 4 creation tests pass              |
| COMP-02     | 03-02        | User can edit and delete companies                             | SATISFIED | PUT /{id} updates, DELETE /{id} soft-archives; 5 update/delete tests pass            |
| COMP-03     | 03-02        | User can view all companies in a list                          | SATISFIED | GET /api/companies with pagination, name filter, include-archived; 7 list/get tests  |
| JOBS-01     | 03-01, 03-03 | User can add a job posting with all fields                     | SATISFIED | JobController POST; all 20 fields mapped; 4 creation tests pass including salary variants |
| JOBS-02     | 03-03        | User can link a job posting to a company                       | SATISFIED | companyId nullable; validateCompany checks ownership and archived state; companyName resolved in response; 4 linking tests |
| JOBS-03     | 03-03        | User can edit and delete job postings                          | SATISFIED | PUT /{id} with PUT semantics (null clears); DELETE /{id} soft-archives; 7 update/delete tests |
| JOBS-04     | 03-03        | User can store the full job description text                   | SATISFIED | description mapped to TEXT column (unlimited length); test stores 2000-char string and retrieves unchanged |

All 7 requirements satisfied. No orphaned requirements for Phase 3.

---

### Anti-Patterns Found

None detected. Scanned CompanyEntity, JobEntity, CompanyService, JobService, CompanyController, JobController for TODO/FIXME, placeholder comments, empty implementations, and stub return values.

---

### Human Verification Required

**1. Schema Validation at Startup**

**Test:** Start the application with `./gradlew :backend:bootRun` against a fresh database.
**Expected:** Flyway runs V5 and V6 migrations successfully; JPA ddl-auto=validate confirms entity fields match table columns exactly (no startup exception).
**Why human:** Cannot run Docker + PostgreSQL in this verification context.

**2. Full Test Suite Green**

**Test:** Run `./gradlew :backend:test` with a live PostgreSQL instance.
**Expected:** All auth tests (Phase 2), 16 company tests, and 22 job tests pass. Zero failures.
**Why human:** Requires a running PostgreSQL container. The integration tests use @SpringBootTest and MockMvc against the real database.

---

### Gaps Summary

No gaps. All 15 observable truths verified. All 18 artifacts exist, are substantive, and are wired. All 10 key links confirmed. All 7 requirement IDs (COMP-01, COMP-02, COMP-03, JOBS-01, JOBS-02, JOBS-03, JOBS-04) satisfied. No anti-patterns found. Two items flagged for human verification require a live database environment — they are process checks, not code defects.

---

## Commit Evidence

All 6 phase commits verified present in git history:

| Commit    | Type | Description                                                       |
|-----------|------|-------------------------------------------------------------------|
| `97cbb3b` | feat | AppUserDetails, SecurityContextUtil, domain enums, and exceptions  |
| `a3aa2b9` | feat | Flyway migrations V5/V6, ErrorResponse DTO, and GlobalExceptionHandler |
| `248ca35` | feat | Company domain slice: entity, repository, service, controller, DTOs |
| `139b219` | test | Company CRUD integration tests (COMP-01, COMP-02, COMP-03)        |
| `8c78bc5` | feat | Job domain slice with entity, repository, service, controller, archive guard |
| `aa24ff8` | test | Job integration tests and company archive guard tests (JOBS-01–04) |

---

_Verified: 2026-03-20T04:00:00Z_
_Verifier: Claude (gsd-verifier)_
