---
phase: 04-application-tracking
plan: 01
subsystem: api
tags: [spring-boot, jpa, kotlin, state-machine, rest-api]

requires:
  - phase: 03-company-job-domain
    provides: JobEntity, JobRepository, CompanyRepository, DomainExceptions, GlobalExceptionHandler

provides:
  - ApplicationEntity and ApplicationNoteEntity JPA entities
  - ApplicationStatus (8-value) and NoteType (5-value) enums
  - Status state machine with validated transitions and terminal status reopening
  - ApplicationService with CRUD, state machine, date auto-updates
  - ApplicationNoteService with CRUD, auto STATUS_CHANGE notes, delete protection
  - ApplicationController (7 REST endpoints at /api/applications)
  - ApplicationNoteController (4 REST endpoints at /api/applications/{id}/notes)
  - Flyway V7 (applications table) and V8 (application_notes table)
  - InvalidTransitionException mapped to HTTP 422
  - TestHelper.createJob and TestHelper.createApplication helpers

affects: [04-02-application-tracking, 05-interview-management, 06-documents]

tech-stack:
  added: []
  patterns: [status-state-machine, nested-resource-controllers, auto-date-tracking]

key-files:
  created:
    - backend/src/main/resources/db/migration/V7__phase04_create_applications.sql
    - backend/src/main/resources/db/migration/V8__phase04_create_application_notes.sql
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/ApplicationEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/ApplicationNoteEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/ApplicationRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/ApplicationNoteRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/ApplicationDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/ApplicationNoteDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/ApplicationService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/ApplicationNoteService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/ApplicationController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/ApplicationNoteController.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/TestHelper.kt

key-decisions:
  - "Added findByUserId and findByUserIdAndArchivedFalse to ApplicationRepository for proper user-scoped list queries"
  - "Status state machine uses Map<ApplicationStatus, Set<ApplicationStatus>> for transition validation"
  - "Terminal statuses (REJECTED, ACCEPTED, WITHDRAWN) can reopen to any active status"

patterns-established:
  - "Status state machine: Map-based transition validation with InvalidTransitionException (422)"
  - "Nested resource controller: /api/applications/{id}/notes pattern with ownership verification"
  - "Auto-date tracking: appliedDate auto-set on APPLIED, lastActivityDate on status change and note creation"
  - "STATUS_CHANGE notes: auto-created on status transitions, protected from deletion"

requirements-completed: [APPL-01, APPL-02, APPL-05, APPL-06]

duration: 5min
completed: 2026-03-20
---

# Phase 04 Plan 01: Application CRUD & Status State Machine Summary

**Application tracking domain with 8-status state machine, validated transitions, auto-date tracking, notes subsystem with STATUS_CHANGE auto-notes, and 11 REST endpoints**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-20T11:40:34Z
- **Completed:** 2026-03-20T11:45:19Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- Complete application tracking domain with Flyway migrations, entities, repositories, services, and controllers
- 8-status state machine with validated transitions and terminal status reopening
- Notes subsystem with auto STATUS_CHANGE notes on transitions and delete protection for system notes
- 11 REST endpoints (7 application + 4 notes) following existing controller patterns

## Task Commits

Each task was committed atomically:

1. **Task 1: Flyway migrations, enums, entities, repositories, DTOs, and exceptions** - `972d1c6` (feat)
2. **Task 2: Application service, note service, controllers, and TestHelper extensions** - `3dbc514` (feat)

## Files Created/Modified
- `V7__phase04_create_applications.sql` - Applications table with indexes and unique constraint
- `V8__phase04_create_application_notes.sql` - Application notes table with ON DELETE CASCADE
- `enums.kt` - Added ApplicationStatus (8 values) and NoteType (5 values) enums
- `ApplicationEntity.kt` - Application JPA entity with status, dates, archive support
- `ApplicationNoteEntity.kt` - Application note JPA entity
- `ApplicationRepository.kt` - Repository with userId-scoped queries
- `ApplicationNoteRepository.kt` - Repository with applicationId-scoped queries
- `ApplicationDtos.kt` - Create, update, status update, and response DTOs
- `ApplicationNoteDtos.kt` - Create, update, and response DTOs for notes
- `DomainExceptions.kt` - Added InvalidTransitionException
- `GlobalExceptionHandler.kt` - Added 422 handler for InvalidTransitionException
- `ApplicationService.kt` - CRUD, state machine, date auto-updates, batch name resolution
- `ApplicationNoteService.kt` - CRUD, STATUS_CHANGE auto-notes, delete protection
- `ApplicationController.kt` - 7 endpoints including PATCH status and GET transitions
- `ApplicationNoteController.kt` - 4 nested resource endpoints
- `TestHelper.kt` - Added createJob and createApplication helpers

## Decisions Made
- Added findByUserId and findByUserIdAndArchivedFalse to ApplicationRepository for proper user-scoped queries (plan only specified basic methods)
- Status state machine uses Map<ApplicationStatus, Set<ApplicationStatus>> for transition validation
- Terminal statuses (REJECTED, ACCEPTED, WITHDRAWN) can reopen to any active status
- GET /api/applications/{id}/transitions endpoint returns valid next statuses for UI consumption

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed list method to filter by userId**
- **Found during:** Task 2 (ApplicationService implementation)
- **Issue:** The list method used applicationRepository.findAll() which returns all applications regardless of user
- **Fix:** Added findByUserId and findByUserIdAndArchivedFalse methods to ApplicationRepository; updated list method to use them
- **Files modified:** ApplicationRepository.kt, ApplicationService.kt
- **Verification:** Compilation passes
- **Committed in:** 3dbc514 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Essential fix for data isolation. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All application tracking domain code compiles and is ready for integration tests in Plan 02
- TestHelper extended with createJob and createApplication for test setup
- State machine ready for validation in integration tests

## Self-Check: PASSED

- All 16 files verified present on disk
- Both task commits (972d1c6, 3dbc514) verified in git history

---
*Phase: 04-application-tracking*
*Completed: 2026-03-20*
