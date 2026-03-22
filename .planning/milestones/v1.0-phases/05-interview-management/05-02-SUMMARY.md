---
phase: 05-interview-management
plan: 02
subsystem: api
tags: [spring-boot, kotlin, jpa, rest-api, interviews, timeline]

requires:
  - phase: 05-interview-management-01
    provides: "Flyway migrations, JPA entities, repositories, DTOs for interviews"
  - phase: 04-application-tracking
    provides: "ApplicationRepository, ApplicationNoteRepository, ApplicationNoteService patterns"
provides:
  - "Interview CRUD REST API with round auto-increment and soft archive"
  - "Interview note CRUD REST API with parent ownership check"
  - "Timeline aggregation API from 3 sources with type filtering"
  - "26 integration tests covering INTV-01 through INTV-04"
affects: [06-document-management, 08-analytics]

tech-stack:
  added: []
  patterns: [timeline-aggregation, parent-ownership-check, archived-interview-guard]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/InterviewService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/InterviewNoteService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/TimelineService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewNoteController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/TimelineController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/interview/InterviewControllerIntegrationTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/interview/InterviewNoteControllerIntegrationTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/interview/TimelineControllerIntegrationTests.kt
  modified:
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/TestHelper.kt

key-decisions:
  - "Archived interviews excluded from timeline including their child interview notes"
  - "Timeline sorted by date descending (most recent first) with optional type filtering"
  - "Interview notes blocked on archived interviews (returns 404)"

patterns-established:
  - "Parent-ownership guard: InterviewNoteService checks interview.archived before all operations"
  - "Timeline aggregation: collect from N repositories, map to common entry type, sort unified list"
  - "TestHelper extension: createInterview and createInterviewNote for reuse across test classes"

requirements-completed: [INTV-01, INTV-02, INTV-03, INTV-04]

duration: 8min
completed: 2026-03-20
---

# Phase 5 Plan 2: Interview Management API Summary

**Interview CRUD with round auto-increment, interview notes with archived guard, and timeline aggregating 3 sources with type filtering**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-20T17:13:49Z
- **Completed:** 2026-03-20T17:21:39Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Interview service with CRUD, round auto-increment (max existing + 1), and soft archive
- Interview note service with parent interview ownership and archived check
- Timeline service aggregating interviews, application notes, and interview notes sorted descending
- 26 integration tests covering all 4 requirements (INTV-01 through INTV-04)
- Full test suite remains green (all existing + new tests)

## Task Commits

Each task was committed atomically:

1. **Task 1: Interview service/controller and interview note service/controller** - `018e5c2` (feat)
2. **Task 2: Timeline service/controller and integration tests** - `f9709fd` (feat)

## Files Created/Modified

- `backend/src/main/kotlin/.../service/InterviewService.kt` - Interview CRUD with roundNumber auto-increment
- `backend/src/main/kotlin/.../service/InterviewNoteService.kt` - Interview note CRUD with archived guard
- `backend/src/main/kotlin/.../service/TimelineService.kt` - 3-source timeline aggregation
- `backend/src/main/kotlin/.../controller/InterviewController.kt` - REST at /api/interviews
- `backend/src/main/kotlin/.../controller/InterviewNoteController.kt` - REST at /api/interviews/{id}/notes
- `backend/src/main/kotlin/.../controller/TimelineController.kt` - REST at /api/applications/{id}/timeline
- `backend/src/test/kotlin/.../TestHelper.kt` - Added createInterview and createInterviewNote helpers
- `backend/src/test/kotlin/.../interview/InterviewControllerIntegrationTests.kt` - 11 tests for INTV-01/02
- `backend/src/test/kotlin/.../interview/InterviewNoteControllerIntegrationTests.kt` - 7 tests for INTV-03
- `backend/src/test/kotlin/.../interview/TimelineControllerIntegrationTests.kt` - 8 tests for INTV-04

## Decisions Made

- Archived interviews excluded from timeline, including their child interview notes (findIdsByApplicationId only returns non-archived IDs)
- Timeline sorted by date descending with optional type filtering via query parameter
- Interview notes blocked on archived interviews -- all CRUD operations check archived flag and return 404

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Interview management API complete with full test coverage
- Ready for Phase 6 (Document Management) or frontend integration
- Timeline endpoint available for dashboard integration

---
*Phase: 05-interview-management*
*Completed: 2026-03-20*
