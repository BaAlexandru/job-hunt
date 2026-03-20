# Phase 5: Interview Management - Research

**Researched:** 2026-03-20
**Domain:** Spring Boot 4 / Kotlin REST API -- CRUD entities, timeline composition, Flyway migrations
**Confidence:** HIGH

## Summary

Phase 5 adds interview scheduling, round tracking, interview notes, and a unified timeline endpoint to the existing backend. The technical domain is entirely within established project patterns -- the entity/service/controller/DTO/repository/migration stack used in Phases 3 and 4. No new libraries are needed; no new architectural concepts are introduced.

The primary complexity is the timeline endpoint (INTV-04), which aggregates data from three tables (interviews, application_notes, interview_notes) into a unified chronological response. The recommended approach is service-layer composition (multiple queries merged in Kotlin) rather than a SQL UNION, because the three tables have different schemas and the volume per application is low (dozens of records, not thousands).

**Primary recommendation:** Replicate the ApplicationNote entity/service/controller pattern for both InterviewEntity and InterviewNoteEntity. Implement timeline as a dedicated service that queries all three repositories and merges results in memory.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
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
- InterviewStage enum: SCREENING, TECHNICAL, BEHAVIORAL, CULTURE_FIT, FINAL, SYSTEM_DESIGN, HOMEWORK, OTHER
- Optional free-text label for specifics (e.g., stage=TECHNICAL, label="PR review with 2 devs")
- Auto-incrementing roundNumber per application (1, 2, 3...)
- Multiple interviews allowed for the same stage (e.g., 2 TECHNICAL rounds)
- Round ordering is informational -- no enforcement of stage progression
- Separate InterviewNote entity (new table, same pattern as ApplicationNoteEntity)
- Linked to interview via interviewId (plain UUID FK)
- InterviewNoteType enum: PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL
- Fields: content (TEXT), noteType, createdAt, updatedAt
- CRUD operations: create, read, update, delete notes per interview
- Timeline aggregates ALL sources: interviews + application notes + interview notes
- Unified chronological list with type discriminator (INTERVIEW, APPLICATION_NOTE, INTERVIEW_NOTE)
- Each entry has common fields (date, type, summary) plus type-specific details
- Filtering by entry type only
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

### Deferred Ideas (OUT OF SCOPE)
- Google Calendar integration (GCAL-01, GCAL-02) -- sync interview events to calendar, update on reschedule. v2 feature
- Interview reminders/notifications -- v2 feature (RMND-01 scope)
- Interviewer as a separate entity with contact info -- current design uses a simple text field
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INTV-01 | User can schedule an interview with date, time, type (phone/video/onsite), and location or meeting link | InterviewEntity with scheduledAt (Instant), InterviewType enum, location field; InterviewController POST endpoint; Flyway V11 migration |
| INTV-02 | User can track multiple interview rounds per application (screening, technical, behavioral, final) | InterviewStage enum, roundNumber auto-increment in service layer, multiple interviews per applicationId |
| INTV-03 | User can add notes and conversation details to each interview stage | InterviewNoteEntity with interviewId FK, InterviewNoteType enum, CRUD controller nested under interview; Flyway V12 migration |
| INTV-04 | User can view a timeline of all interactions and interview stages per application | TimelineService composing data from InterviewRepository + ApplicationNoteRepository + InterviewNoteRepository; dedicated GET /api/applications/{id}/timeline endpoint |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.0.4 | Application framework | Already in project |
| Spring Data JPA | (via starter) | Repository layer | Already in project |
| Flyway | (via starter) | Database migrations | Already in project |
| Jakarta Validation | (via starter) | DTO validation | Already in project |
| PostgreSQL | (via Docker Compose) | Database | Already in project |

### Supporting
No new dependencies required. All needed libraries are already present from previous phases.

**Installation:**
No new dependencies to add.

## Architecture Patterns

### Recommended Project Structure
```
backend/src/main/kotlin/com/alex/job/hunt/jobhunt/
  entity/
    InterviewEntity.kt           # New interview entity
    InterviewNoteEntity.kt       # New interview note entity
    enums.kt                     # Extended with 5 new enums
  dto/
    InterviewDtos.kt             # Create/Update/Response DTOs for interviews
    InterviewNoteDtos.kt         # Create/Update/Response DTOs for interview notes
    TimelineDtos.kt              # Timeline entry DTOs
  repository/
    InterviewRepository.kt       # JpaRepository for interviews
    InterviewNoteRepository.kt   # JpaRepository for interview notes
  service/
    InterviewService.kt          # CRUD + roundNumber logic
    InterviewNoteService.kt      # CRUD for interview notes
    TimelineService.kt           # Aggregation from 3 sources
  controller/
    InterviewController.kt       # /api/interviews/** endpoints
    InterviewNoteController.kt   # /api/interviews/{id}/notes/** endpoints
    TimelineController.kt        # /api/applications/{id}/timeline endpoint

backend/src/main/resources/db/migration/
    V11__phase05_create_interviews.sql
    V12__phase05_create_interview_notes.sql

backend/src/test/kotlin/.../interview/
    InterviewControllerIntegrationTests.kt
    InterviewNoteControllerIntegrationTests.kt
    TimelineControllerIntegrationTests.kt
```

### Pattern 1: Entity with Plain UUID FK (Established)
**What:** Entities reference parents via plain UUID columns, no @ManyToOne
**When to use:** All FK relationships in this project
**Example:**
```kotlin
// Source: Existing ApplicationNoteEntity.kt pattern
@Entity
@Table(name = "interviews")
class InterviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    // ... other fields
)
```

### Pattern 2: Service-Layer Round Number Auto-Increment
**What:** Compute next round number in service by querying max round for applicationId
**When to use:** When creating a new interview for an application
**Rationale:** Service-layer is simpler than DB sequence per-application. Volume is low (typically <20 interviews per application).
**Example:**
```kotlin
// In InterviewService.create():
val nextRound = interviewRepository.countByApplicationId(applicationId) + 1
val entity = InterviewEntity(
    applicationId = applicationId,
    roundNumber = nextRound,
    // ...
)
```
**Note:** Use `countByApplicationId` (not max+1) to handle gaps from deletions cleanly. Alternatively, use `findMaxRoundNumberByApplicationId` JPQL query returning `(maxRound ?: 0) + 1` if gap-free numbering after deletes matters -- but for informational ordering, count+1 is sufficient.

### Pattern 3: Timeline Composition in Service Layer (Recommended)
**What:** Query three repositories independently, merge results into a unified list sorted by date
**When to use:** For GET /api/applications/{id}/timeline
**Rationale:** Tables have different schemas. Per-application data volume is small (tens of records). Service-layer merge is simpler to maintain than SQL UNION with column mapping. Allows typed DTOs per source.
**Example:**
```kotlin
@Service
@Transactional(readOnly = true)
class TimelineService(
    private val interviewRepository: InterviewRepository,
    private val applicationNoteRepository: ApplicationNoteRepository,
    private val interviewNoteRepository: InterviewNoteRepository,
    private val applicationRepository: ApplicationRepository
) {
    fun getTimeline(applicationId: UUID, userId: UUID, types: List<TimelineEntryType>?): List<TimelineEntry> {
        // Verify ownership
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val entries = mutableListOf<TimelineEntry>()

        // Conditionally fetch based on type filter
        if (types == null || TimelineEntryType.INTERVIEW in types) {
            entries += interviewRepository.findByApplicationId(applicationId)
                .map { it.toTimelineEntry() }
        }
        if (types == null || TimelineEntryType.APPLICATION_NOTE in types) {
            entries += applicationNoteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .map { it.toTimelineEntry() }
        }
        if (types == null || TimelineEntryType.INTERVIEW_NOTE in types) {
            val interviewIds = interviewRepository.findIdsByApplicationId(applicationId)
            if (interviewIds.isNotEmpty()) {
                entries += interviewNoteRepository.findByInterviewIdIn(interviewIds)
                    .map { it.toTimelineEntry() }
            }
        }

        return entries.sortedByDescending { it.date }
    }
}
```

### Pattern 4: Nested Resource Controller (Established)
**What:** Interview notes nested under interviews: /api/interviews/{interviewId}/notes
**When to use:** Child resources that belong to a parent
**Example:**
```kotlin
// Source: Existing ApplicationNoteController.kt pattern
@RestController
@RequestMapping("/api/interviews/{interviewId}/notes")
class InterviewNoteController(private val noteService: InterviewNoteService) {
    // Same CRUD shape as ApplicationNoteController
}
```

### Anti-Patterns to Avoid
- **@ManyToOne relationships:** Project uses plain UUID FKs everywhere. Do not introduce JPA relationships.
- **SQL UNION for timeline:** Tempting but creates brittle column-mapping. Service-layer merge is cleaner here.
- **DB-level round number sequences:** Per-application sequences in PostgreSQL add complexity for no real gain at this data volume.
- **userId on InterviewNoteEntity:** Notes inherit user isolation through their parent interview. The interview already has userId; the note only needs interviewId.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pagination | Custom offset/limit logic | Spring Data Pageable | Built-in, consistent with existing endpoints |
| Validation | Manual null checks in controller | Jakarta @Valid + @NotBlank/@NotNull | Consistent with project pattern, auto-generates error responses |
| Error responses | Per-controller try/catch | GlobalExceptionHandler + domain exceptions | Already handles NotFoundException, ConflictException, etc. |
| UUID generation | Application-side UUID creation | DB gen_random_uuid() via GenerationType.UUID | Consistent, collision-proof |
| Timestamp management | Manual Instant.now() in all places | DB DEFAULT now() for createdAt; service-layer for updatedAt | Consistent with existing entities |

## Common Pitfalls

### Pitfall 1: FK Cleanup Order in Tests
**What goes wrong:** Integration tests fail with FK constraint violations during @BeforeEach cleanup
**Why it happens:** interview_notes references interviews, interviews references applications
**How to avoid:** Delete in order: interviewNoteRepository.deleteAll() -> interviewRepository.deleteAll() -> applicationNoteRepository.deleteAll() -> applicationRepository.deleteAll() -> ... (children before parents)
**Warning signs:** Random test failures depending on execution order

### Pitfall 2: Missing userId Check on Interview Access
**What goes wrong:** User A can see/modify User B's interviews if only interviewId is checked
**Why it happens:** Interview notes are accessed by interviewId; forgetting to verify the parent interview belongs to the requesting user
**How to avoid:** Always verify userId ownership on the parent interview before operating on notes. The service must check `interviewRepository.findByIdAndUserId(interviewId, userId)` before any note operation.
**Warning signs:** No userId in interview note queries

### Pitfall 3: Timeline InterviewNote Linkage
**What goes wrong:** Interview notes don't appear in timeline because they're linked to interviews, not directly to applications
**Why it happens:** InterviewNote has interviewId FK, not applicationId FK. Timeline needs a join through interviews.
**How to avoid:** First fetch interview IDs for the application, then fetch notes for those interview IDs. Use `interviewRepository.findIdsByApplicationId()` as an intermediary query.
**Warning signs:** Timeline returns interviews and application notes but no interview notes

### Pitfall 4: Flyway Migration Number Collision
**What goes wrong:** Migration fails because V11 or V12 already exists
**Why it happens:** CONTEXT.md says "Next Flyway migration number: V11 (V7-V8 Phase 4, V9-V10 Phase 7)" -- V9 and V10 are taken by Phase 7 (Better Auth).
**How to avoid:** Use V11 and V12 for Phase 5 migrations. Verify by checking existing migration files.
**Warning signs:** Flyway checksum errors on startup

### Pitfall 5: roundNumber Concurrency
**What goes wrong:** Two simultaneous interview creations for the same application get the same roundNumber
**Why it happens:** count+1 is not atomic
**How to avoid:** For a single-user app this is unlikely. If needed, add a unique constraint on (application_id, round_number) and handle the constraint violation with a retry. For v1, the race condition is effectively impossible since one user creates interviews sequentially.
**Warning signs:** Duplicate round numbers (acceptable for v1)

## Code Examples

### Interview Entity
```kotlin
@Entity
@Table(name = "interviews")
class InterviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "round_number", nullable = false)
    var roundNumber: Int,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = 60,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var interviewType: InterviewType,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var stage: InterviewStage,

    @Column(name = "stage_label")
    var stageLabel: String? = null,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var outcome: InterviewOutcome = InterviewOutcome.SCHEDULED,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var result: InterviewResult = InterviewResult.PENDING,

    @Column(columnDefinition = "TEXT")
    var location: String? = null,

    @Column(name = "interviewer_names")
    var interviewerNames: String? = null,

    @Column(name = "candidate_feedback", columnDefinition = "TEXT")
    var candidateFeedback: String? = null,

    @Column(name = "company_feedback", columnDefinition = "TEXT")
    var companyFeedback: String? = null,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "archived_at")
    var archivedAt: Instant? = null,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
```

### Flyway V11 Migration
```sql
CREATE TABLE interviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES users(id),
    round_number        INT          NOT NULL,
    scheduled_at        TIMESTAMPTZ  NOT NULL,
    duration_minutes    INT          DEFAULT 60,
    interview_type      VARCHAR(50)  NOT NULL,
    stage               VARCHAR(50)  NOT NULL,
    stage_label         VARCHAR(255),
    outcome             VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    result              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    location            TEXT,
    interviewer_names   VARCHAR(500),
    candidate_feedback  TEXT,
    company_feedback    TEXT,
    archived            BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interviews_application_id ON interviews(application_id);
CREATE INDEX idx_interviews_user_id ON interviews(user_id);
CREATE INDEX idx_interviews_user_archived ON interviews(user_id, archived);
CREATE INDEX idx_interviews_scheduled_at ON interviews(application_id, scheduled_at);
CREATE INDEX idx_interviews_stage ON interviews(application_id, stage);
```

### Flyway V12 Migration
```sql
CREATE TABLE interview_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id    UUID         NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    note_type       VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_notes_interview_id ON interview_notes(interview_id);
```

### Timeline DTO Structure
```kotlin
enum class TimelineEntryType { INTERVIEW, APPLICATION_NOTE, INTERVIEW_NOTE }

data class TimelineEntry(
    val id: UUID,
    val date: Instant,
    val type: TimelineEntryType,
    val summary: String,
    val details: Map<String, Any?>? = null
)
```

### Enums to Add to enums.kt
```kotlin
enum class InterviewType { PHONE, VIDEO, ONSITE, TAKE_HOME }

enum class InterviewStage { SCREENING, TECHNICAL, BEHAVIORAL, CULTURE_FIT, FINAL, SYSTEM_DESIGN, HOMEWORK, OTHER }

enum class InterviewOutcome { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }

enum class InterviewResult { PASSED, FAILED, PENDING, MIXED }

enum class InterviewNoteType { PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL }
```

## State of the Art

No changes to the technology stack. All patterns are stable and established in this project.

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| N/A | N/A | N/A | No changes -- same stack as Phase 4 |

## Open Questions

1. **Timeline pagination**
   - What we know: Timeline endpoint aggregates from 3 sources. User decided filtering by type.
   - What's unclear: Whether to paginate the timeline or return all entries. Per-application data volume is expected to be small.
   - Recommendation: Start without pagination (return all entries for the application). Add pagination later if needed. Use a reasonable limit (e.g., 100 entries max) as a safety net.

2. **Soft delete cascade for interviews**
   - What we know: Interviews and interview notes both exist. Archiving an interview should logically hide its notes too.
   - What's unclear: Whether archiving an interview should explicitly archive its notes or just filter them out in queries.
   - Recommendation: Filter by parent -- when listing interview notes, skip notes whose parent interview is archived. No explicit cascade needed. The ON DELETE CASCADE in the DB handles hard deletes.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + MockMvc |
| Config file | build.gradle.kts (test dependencies already present) |
| Quick run command | `./gradlew :backend:test --tests "*Interview*"` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INTV-01 | Schedule interview with date, time, type, location | integration | `./gradlew :backend:test --tests "*InterviewControllerIntegration*"` | No -- Wave 0 |
| INTV-02 | Track multiple rounds per application | integration | `./gradlew :backend:test --tests "*InterviewControllerIntegration*roundNumber*"` | No -- Wave 0 |
| INTV-03 | Add/edit/delete notes per interview | integration | `./gradlew :backend:test --tests "*InterviewNoteControllerIntegration*"` | No -- Wave 0 |
| INTV-04 | Chronological timeline of all interactions | integration | `./gradlew :backend:test --tests "*TimelineControllerIntegration*"` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test --tests "*Interview*"`
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `InterviewControllerIntegrationTests.kt` -- covers INTV-01, INTV-02
- [ ] `InterviewNoteControllerIntegrationTests.kt` -- covers INTV-03
- [ ] `TimelineControllerIntegrationTests.kt` -- covers INTV-04
- [ ] TestHelper needs new methods: `createInterview()`, `createInterviewNote()`

## Sources

### Primary (HIGH confidence)
- Existing codebase: ApplicationNoteEntity.kt, ApplicationNoteService.kt, ApplicationNoteController.kt, ApplicationNoteRepository.kt -- direct patterns to replicate
- Existing codebase: ApplicationEntity.kt, enums.kt -- entity and enum conventions
- Existing codebase: V7__phase04_create_applications.sql, V8__phase04_create_application_notes.sql -- migration patterns
- Existing codebase: ApplicationNoteControllerIntegrationTests.kt, TestHelper.kt -- test patterns

### Secondary (MEDIUM confidence)
- None needed -- all patterns are from the existing codebase

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, reusing existing stack
- Architecture: HIGH -- all patterns directly replicated from Phase 4 code
- Pitfalls: HIGH -- derived from actual project patterns and FK relationships
- Timeline composition: HIGH -- straightforward in-memory merge, verified approach against data volume expectations

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- no external dependencies changing)
