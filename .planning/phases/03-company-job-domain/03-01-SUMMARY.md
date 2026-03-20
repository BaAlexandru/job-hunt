---
phase: 03-company-job-domain
plan: 01
subsystem: api
tags: [spring-boot, kotlin, flyway, jpa, security, error-handling]

# Dependency graph
requires:
  - phase: 02-authentication
    provides: "UserDetailsServiceImpl, JwtAuthenticationFilter, SecurityContext setup"
provides:
  - "AppUserDetails carrying userId UUID in SecurityContext"
  - "SecurityContextUtil.getCurrentUserId() for controllers"
  - "Domain enums: WorkMode, JobType, SalaryType, SalaryPeriod"
  - "Companies table (V5 migration) with user_id FK and indexes"
  - "Jobs table (V6 migration) with salary, work mode, and job type fields"
  - "ErrorResponse standardized DTO for all error responses"
  - "GlobalExceptionHandler for unified exception handling"
  - "NotFoundException and ConflictException domain exceptions"
affects: [03-company-job-domain]

# Tech tracking
tech-stack:
  added: []
  patterns: [standardized-error-response, custom-userdetails, security-context-util]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/AppUserDetails.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/ErrorResponse.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt
    - backend/src/main/resources/db/migration/V5__phase03_create_companies.sql
    - backend/src/main/resources/db/migration/V6__phase03_create_jobs.sql
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/UserDetailsServiceImpl.kt

key-decisions:
  - "Replaced AuthExceptionHandler with GlobalExceptionHandler for unified error handling across all controllers"
  - "ErrorResponse includes optional fieldErrors map for validation errors"

patterns-established:
  - "ErrorResponse pattern: all error handlers return ErrorResponse(status, error, message, path, timestamp, fieldErrors)"
  - "SecurityContextUtil pattern: controllers call SecurityContextUtil.getCurrentUserId() for authenticated user ID"
  - "Domain exceptions pattern: NotFoundException (404) and ConflictException (409) for service-layer errors"

requirements-completed: [COMP-01, JOBS-01]

# Metrics
duration: 3min
completed: 2026-03-20
---

# Phase 3 Plan 01: Foundation Summary

**Custom UserDetails with userId extraction, companies/jobs Flyway migrations, domain enums, and standardized ErrorResponse handling**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T02:24:35Z
- **Completed:** 2026-03-20T02:27:43Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- AppUserDetails carries userId UUID through SecurityContext, eliminating extra DB queries in controllers
- Flyway V5 and V6 create companies and jobs tables with all columns, constraints, and composite indexes
- GlobalExceptionHandler provides unified error handling with standardized ErrorResponse shape for all exceptions
- Domain enums (WorkMode, JobType, SalaryType, SalaryPeriod) and exceptions (NotFoundException, ConflictException) ready for Plans 02 and 03

## Task Commits

Each task was committed atomically:

1. **Task 1: Custom UserDetails, SecurityContextUtil, domain enums, and domain exceptions** - `97cbb3b` (feat)
2. **Task 2: Flyway migrations V5/V6, ErrorResponse DTO, and GlobalExceptionHandler** - `a3aa2b9` (feat)

## Files Created/Modified
- `backend/src/main/kotlin/.../security/AppUserDetails.kt` - Custom UserDetails carrying userId UUID
- `backend/src/main/kotlin/.../security/SecurityContextUtil.kt` - Extracts userId from SecurityContext
- `backend/src/main/kotlin/.../security/UserDetailsServiceImpl.kt` - Returns AppUserDetails instead of Spring User
- `backend/src/main/kotlin/.../entity/enums.kt` - WorkMode, JobType, SalaryType, SalaryPeriod enums
- `backend/src/main/kotlin/.../service/DomainExceptions.kt` - NotFoundException, ConflictException
- `backend/src/main/kotlin/.../dto/ErrorResponse.kt` - Standardized error response DTO
- `backend/src/main/kotlin/.../config/GlobalExceptionHandler.kt` - Unified exception handler
- `backend/src/main/resources/db/migration/V5__phase03_create_companies.sql` - Companies table DDL
- `backend/src/main/resources/db/migration/V6__phase03_create_jobs.sql` - Jobs table DDL
- `backend/src/main/kotlin/.../config/AuthExceptionHandler.kt` - DELETED (replaced by GlobalExceptionHandler)

## Decisions Made
- Replaced AuthExceptionHandler with GlobalExceptionHandler to provide a single unified error handler for all controllers (auth + domain + validation + generic)
- ErrorResponse includes optional `fieldErrors` map for validation errors rather than a separate response shape
- Generic Exception handler logs full stack trace but returns "Internal server error" to never leak exception details

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Companies and jobs tables ready for JPA entities in Plan 02
- SecurityContextUtil ready for controller-level userId extraction in Plans 02 and 03
- GlobalExceptionHandler ready to catch NotFoundException/ConflictException from company and job services
- Domain enums ready for entity field mapping

---
*Phase: 03-company-job-domain*
*Completed: 2026-03-20*
