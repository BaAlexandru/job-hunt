---
phase: 05-interview-management
plan: 01
subsystem: database, api
tags: [flyway, jpa, kotlin, postgresql, interviews]

requires:
  - phase: 04-application-tracking
    provides: applications table and ApplicationEntity for FK relationship
provides:
  - interviews and interview_notes Flyway migrations (V11, V12)
  - InterviewEntity and InterviewNoteEntity JPA entities
  - 5 new enums (InterviewType, InterviewStage, InterviewOutcome, InterviewResult, InterviewNoteType)
  - InterviewRepository and InterviewNoteRepository with query methods
  - Interview and InterviewNote DTO contracts (create, update, response)
  - TimelineEntry DTO for unified timeline view
affects: [05-interview-management]

tech-stack:
  added: []
  patterns: [interview entity with soft-delete archiving, interview notes via parent FK isolation]

key-files:
  created:
    - backend/src/main/resources/db/migration/V11__phase05_create_interviews.sql
    - backend/src/main/resources/db/migration/V12__phase05_create_interview_notes.sql
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/InterviewEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/InterviewNoteEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/InterviewRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/InterviewNoteRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewNoteDtos.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/TimelineDtos.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt

key-decisions:
  - "Followed existing entity patterns exactly - no architectural deviations needed"

patterns-established:
  - "Interview notes use parent FK isolation (no userId) - user access controlled via interview ownership"
  - "Timeline DTO uses generic Map<String, Any?> for flexible detail payloads per entry type"

requirements-completed: [INTV-01, INTV-02, INTV-03, INTV-04]

duration: 3min
completed: 2026-03-20
---

# Phase 5 Plan 01: Interview Data Layer Summary

**Flyway migrations for interviews/interview_notes tables, 5 interview enums, JPA entities, Spring Data repositories, and DTO contracts for CRUD and timeline**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T17:13:07Z
- **Completed:** 2026-03-20T17:15:55Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Created interviews and interview_notes PostgreSQL tables with proper FK constraints, indexes, and soft-delete support
- Added 5 interview-specific enums covering type, stage, outcome, result, and note classification
- Built JPA entities following established ApplicationEntity/ApplicationNoteEntity patterns exactly
- Defined repository interfaces with all query methods needed by Plan 02 service layer
- Established DTO contracts for interview CRUD, interview note CRUD, and timeline entries

## Task Commits

Each task was committed atomically:

1. **Task 1: Flyway migrations, enums, and JPA entities** - `cf5a9a6` (feat)
2. **Task 2: Repositories and DTO contracts** - `3fc08e8` (feat)

## Files Created/Modified
- `backend/src/main/resources/db/migration/V11__phase05_create_interviews.sql` - interviews table with 5 indexes
- `backend/src/main/resources/db/migration/V12__phase05_create_interview_notes.sql` - interview_notes table with cascade delete
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt` - Added InterviewType, InterviewStage, InterviewOutcome, InterviewResult, InterviewNoteType
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/InterviewEntity.kt` - JPA entity with all interview fields
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/InterviewNoteEntity.kt` - JPA entity following ApplicationNoteEntity pattern
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/InterviewRepository.kt` - CRUD, timeline, round counting queries
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/InterviewNoteRepository.kt` - CRUD and batch lookup queries
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewDtos.kt` - Create/Update/Response DTOs with validation
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewNoteDtos.kt` - Create/Update/Response DTOs with validation
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/TimelineDtos.kt` - TimelineEntryType enum and TimelineEntry data class

## Decisions Made
None - followed plan as specified. All patterns replicated exactly from existing application/note entities.

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Data layer complete: schema, entities, repositories, DTOs all in place
- Plan 02 can build service layer and REST controllers on top of this foundation
- All query methods pre-defined for CRUD, filtering, timeline assembly, and round number auto-increment

---
*Phase: 05-interview-management*
*Completed: 2026-03-20*
