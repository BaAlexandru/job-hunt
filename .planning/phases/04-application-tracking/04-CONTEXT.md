# Phase 4: Application Tracking - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can track job applications through a full status lifecycle with notes, dates, and search/filter capabilities via the REST API. Includes application CRUD, an 8-status state machine with validated transitions, a typed notes log, date tracking (applied, last activity, next action), and cross-table search/filtering. No frontend UI -- API tested via Swagger/Postman.

</domain>

<decisions>
## Implementation Decisions

### Status model
- 8 fixed statuses as an enum: INTERESTED, APPLIED, PHONE_SCREEN, INTERVIEW, OFFER, REJECTED, ACCEPTED, WITHDRAWN
- Predefined state machine with validated transitions -- not every status can transition to every other
- Withdrawn is available from any active status (reflects real-world flexibility)
- Rejected is available from any status after APPLIED (rejection can happen at any stage)
- Terminal statuses (REJECTED, ACCEPTED, WITHDRAWN) are reversible -- user can reopen to any active status
- Custom interview stages are deferred to Phase 5 (Interview Management)

### Application data model
- Required: linked to a job posting (jobId foreign key, required)
- Status: enum field, default INTERESTED
- Quick notes: free-text field directly on the application entity (for at-a-glance info)
- Dates: appliedDate (LocalDate, nullable), lastActivityDate (Instant), nextActionDate (LocalDate, nullable)
- Soft delete: archived boolean + archivedAt timestamp (same pattern as Company/Job)
- UUID primary key, userId foreign key for ownership
- User isolation: all queries filter by userId, 404 for unauthorized access

### Notes log (separate entity)
- Separate ApplicationNote entity/table with many-to-one relationship to application
- Fields: content (TEXT), noteType enum, createdAt, updatedAt
- Note types: GENERAL, PHONE_CALL, EMAIL, FOLLOW_UP, STATUS_CHANGE
- STATUS_CHANGE notes auto-created on status transitions (e.g., "Status changed: Applied -> Phone Screen")
- User can edit and delete individual note entries
- Notes have their own UUID primary key

### Date tracking behavior
- appliedDate: auto-set to current date when status transitions to APPLIED; user can override manually
- lastActivityDate: auto-updated on status change and note creation (not on field edits)
- nextActionDate: informational only -- just a date field the user sets; no enforcement or notifications
- createdAt, updatedAt: standard timestamps (same as all entities)

### Search & filtering
- Text search across: job title, company name, application quick notes, notes log entries, job description
- Search is case-insensitive contains (same pattern as Company/Job)
- Cross-table joins needed: applications -> jobs -> companies, applications -> notes
- Filters:
  - Status (enum, multi-value)
  - Company (UUID)
  - Date range (applied date or created date)
  - Job type (enum from JobEntity)
  - Work mode (enum from JobEntity)
  - Has next action date (boolean -- useful for follow-up tracking)
  - Note type (filter applications that have notes of a specific type)
- Sort options: createdAt (default, desc), lastActivityDate, nextActionDate, company name
- Pagination: Spring Pageable (same as Company/Job)

### Deletion rules
- Soft delete (archive) as default -- same pattern as Company/Job
- No cascade constraint from applications -- archiving a job does NOT auto-archive its applications
- Default list queries exclude archived records
- Separate query param to include archived items

### Claude's Discretion
- Exact state machine transition map (which specific transitions are allowed between non-terminal statuses)
- DTO field naming and response structure details
- Repository query implementation (JPQL @Query vs Spring Data method naming vs Specifications)
- Search implementation approach (single search param vs separate fields)
- Pagination defaults (page size)
- Index strategy for filter/search columns
- Whether to use PATCH vs PUT for updates
- How to handle the cross-table search performance (joins, subqueries, or denormalization)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` -- Tech stack constraints (Kotlin, Spring Boot 4.x, Spring Data JPA, PostgreSQL)
- `.planning/REQUIREMENTS.md` -- APPL-01, APPL-02, APPL-05, APPL-06, APPL-07
- `.planning/ROADMAP.md` -- Phase 4 success criteria (4 criteria)

### Prior phase context
- `.planning/phases/03-company-job-domain/03-CONTEXT.md` -- Entity patterns, soft delete, user isolation, pagination, error handling, salary model, API response shape
- `backend/CLAUDE.md` -- Package structure, coding conventions, Flyway naming, testing approach

### Existing code to build on
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/JobEntity.kt` -- Entity pattern, field conventions, relationship to Company via plain UUID
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt` -- Enum pattern (WorkMode, JobType, SalaryType, SalaryPeriod)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/JobService.kt` -- Service pattern, cross-table name resolution, batch loading for N+1 prevention
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt` -- NotFoundException, ConflictException
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt` -- Error handling pattern
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt` -- Extracting authenticated userId
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/ErrorResponse.kt` -- Standardized error response

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `JobEntity.kt`: Entity pattern with UUID, timestamps, archived/archivedAt, enums -- replicate for ApplicationEntity
- `enums.kt`: Enum file for domain enums -- add ApplicationStatus and NoteType here
- `DomainExceptions.kt`: NotFoundException/ConflictException -- add InvalidTransitionException or similar
- `GlobalExceptionHandler.kt`: Maps domain exceptions to HTTP responses -- extend for new exceptions
- `SecurityContextUtil.kt`: Extracts userId from SecurityContext -- reuse in ApplicationController
- `JobService.kt`: Service pattern with cross-table name resolution and batch loading -- replicate for application's job/company resolution

### Established Patterns
- Plain UUID foreign keys (no @ManyToOne) to keep entities simple -- use for jobId on ApplicationEntity
- Batch name resolution via findAllByIdInAndUserId for N+1 prevention in list queries
- JPQL @Query with CAST(:param AS string) for nullable search parameters
- Flyway: next migration number is V8 (V5-V7 used in Phase 3)
- Soft delete with archived boolean + archivedAt timestamp

### Integration Points
- SecurityConfig: /api/applications/** endpoints protected by default (require JWT)
- Flyway: New migration files for applications and application_notes tables
- JobEntity: Foreign key target for jobId on applications table
- CompanyEntity: Referenced transitively through JobEntity for search/filtering

</code_context>

<specifics>
## Specific Ideas

- User wants both a quick-notes field AND a full notes log -- the quick-notes field is for at-a-glance info visible in list views, while the notes log is a detailed journal
- STATUS_CHANGE notes create an automatic activity timeline -- this is the foundation for the timeline view in Phase 5 (INTV-04)
- Search needs to span applications, jobs, companies, and notes -- this is the most complex query in the app so far
- Terminal status reversal is important -- the user wants flexibility to reopen applications (e.g., a rejected application gets a second chance)

</specifics>

<deferred>
## Deferred Ideas

- Custom interview stages -- Phase 5 handles interview rounds (screening, technical, behavioral, final) with full customization
- Notifications/reminders for nextActionDate -- v2 feature (RMND-01), Phase 4 just stores the date
- Tags/labels for applications -- v2 feature (TAGS-01)
- Salary comparison across applications -- v2 feature (SALA-01)

</deferred>

---

*Phase: 04-application-tracking*
*Context gathered: 2026-03-20*
