# Phase 5: Interview Management - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can schedule interviews, track multiple rounds per application, add notes per interview stage, capture final feedback, and view a chronological timeline of all interactions for an application via the REST API. No frontend UI -- API tested via Swagger/Postman.

</domain>

<decisions>
## Implementation Decisions

### Interview data model
- InterviewEntity linked to application via applicationId (plain UUID FK, same pattern as all entities)
- Interview type as enum: PHONE, VIDEO, ONSITE, TAKE_HOME
- Outcome enum: SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
- Result enum: PASSED, FAILED, PENDING, MIXED
- scheduledAt: mandatory Instant (date + time)
- durationMinutes: optional Int, default 60 -- end time derived (scheduledAt + duration), not stored
- location: single nullable text field for physical address or meeting link
- interviewerNames: optional text field (e.g., "Sarah (EM), John (Staff Eng)")
- candidateFeedback: nullable TEXT field for candidate's own feedback/reflection
- companyFeedback: nullable TEXT field for feedback received from the company
- Feedback fields intended for FINAL-stage interviews by convention (no enforcement)
- Standard fields: UUID PK, userId FK, archived/archivedAt, createdAt/updatedAt

### Round tracking
- InterviewStage enum: SCREENING, TECHNICAL, BEHAVIORAL, CULTURE_FIT, FINAL, SYSTEM_DESIGN, HOMEWORK, OTHER
- Optional free-text label for specifics (e.g., stage=TECHNICAL, label="PR review with 2 devs")
- Auto-incrementing roundNumber per application (1, 2, 3...)
- Multiple interviews allowed for the same stage (e.g., 2 TECHNICAL rounds)
- Round ordering is informational -- no enforcement of stage progression

### Interview notes
- Separate InterviewNote entity (new table, same pattern as ApplicationNoteEntity)
- Linked to interview via interviewId (plain UUID FK)
- InterviewNoteType enum: PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL
- Fields: content (TEXT), noteType, createdAt, updatedAt
- CRUD operations: create, read, update, delete notes per interview

### Timeline composition (INTV-04)
- Timeline aggregates ALL sources: interviews + application notes (including STATUS_CHANGE) + interview notes
- Unified chronological list with type discriminator (INTERVIEW, APPLICATION_NOTE, INTERVIEW_NOTE)
- Each entry has common fields (date, type, summary) plus type-specific details
- Filtering by entry type only (e.g., show only interviews, only notes)
- Served as a dedicated endpoint: GET /api/applications/{id}/timeline
- Sorted by date descending (most recent first)

### Claude's Discretion
- Exact DTO field naming and response structure details
- Repository query implementation approach
- Pagination defaults for timeline endpoint
- Index strategy for interview queries (applicationId, scheduledAt, stage)
- How to compose the unified timeline (single query with UNION vs multiple queries merged in service layer)
- Whether roundNumber auto-increment happens in DB or service layer
- Soft delete cascade behavior (archiving interview vs archiving its notes)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` -- Tech stack constraints (Kotlin, Spring Boot 4.x, Spring Data JPA, PostgreSQL)
- `.planning/REQUIREMENTS.md` -- INTV-01, INTV-02, INTV-03, INTV-04
- `.planning/ROADMAP.md` -- Phase 5 success criteria (4 criteria)

### Prior phase context
- `.planning/phases/04-application-tracking/04-CONTEXT.md` -- Application entity pattern, status state machine, notes entity pattern, search/filter approach, soft delete, user isolation
- `.planning/phases/03-company-job-domain/03-CONTEXT.md` -- Entity conventions, salary model, API response shape, error handling

### Backend conventions
- `backend/CLAUDE.md` -- Package structure, coding conventions, Flyway naming, testing approach

### Existing code to build on
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/ApplicationEntity.kt` -- Parent entity for interviews (applicationId FK target)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/ApplicationNoteEntity.kt` -- Note entity pattern to replicate for InterviewNote
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt` -- Enum file to extend with InterviewType, InterviewStage, InterviewOutcome, InterviewResult, InterviewNoteType
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/ApplicationNoteService.kt` -- Note service pattern to replicate
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt` -- Domain exceptions to extend if needed
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt` -- Error handling pattern
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt` -- Authenticated userId extraction

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ApplicationNoteEntity.kt`: Note entity pattern (content, noteType enum, timestamps, plain UUID FK) -- replicate for InterviewNoteEntity
- `ApplicationNoteService.kt`: Note CRUD service pattern -- replicate for InterviewNoteService
- `enums.kt`: Central enum file -- add InterviewType, InterviewStage, InterviewOutcome, InterviewResult, InterviewNoteType
- `DomainExceptions.kt`: NotFoundException, ConflictException, InvalidTransitionException -- reuse or extend
- `SecurityContextUtil.kt`: Extract userId from SecurityContext -- reuse in InterviewController

### Established Patterns
- Plain UUID foreign keys (no @ManyToOne) for applicationId on InterviewEntity
- Soft delete with archived boolean + archivedAt timestamp
- JPQL @Query with CAST(:param AS type) for nullable search parameters
- Next Flyway migration number: V11 (V7-V8 Phase 4, V9-V10 Phase 7)
- Batch name resolution via findAllByIdInAndUserId for N+1 prevention
- NoteType enum pattern for categorizing notes

### Integration Points
- SecurityConfig: /api/interviews/** and /api/applications/{id}/timeline protected by default (require JWT)
- Flyway: New migration files for interviews and interview_notes tables
- ApplicationEntity: FK target for applicationId on interviews table
- ApplicationNoteEntity: Timeline endpoint queries both application_notes and interview_notes tables
- Timeline: Needs to query across interviews, application_notes, and interview_notes tables for a single application

</code_context>

<specifics>
## Specific Ideas

- User wants both structured outcome tracking (SCHEDULED/COMPLETED/CANCELLED/NO_SHOW) AND result assessment (PASSED/FAILED/PENDING/MIXED) per interview
- Feedback fields (candidateFeedback, companyFeedback) are designed for the FINAL stage by convention but live on every interview entity for schema simplicity
- The InterviewStage enum includes SYSTEM_DESIGN and HOMEWORK as dedicated stages (not just OTHER) -- reflects real tech interview processes
- Timeline is the key deliverable for INTV-04: a unified chronological view that combines all interaction types for an application
- Round numbers auto-increment per application (1, 2, 3...) giving a clear progression view

</specifics>

<deferred>
## Deferred Ideas

- Google Calendar integration (GCAL-01, GCAL-02) -- sync interview events to calendar, update on reschedule. v2 feature
- Interview reminders/notifications -- v2 feature (RMND-01 scope)
- Interviewer as a separate entity with contact info -- current design uses a simple text field

</deferred>

---

*Phase: 05-interview-management*
*Context gathered: 2026-03-20*
