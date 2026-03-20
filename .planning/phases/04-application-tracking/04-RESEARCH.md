# Phase 4: Application Tracking - Research

**Researched:** 2026-03-20
**Domain:** Spring Boot 4 / Kotlin / JPA -- Application lifecycle state machine, notes subsystem, cross-table search
**Confidence:** HIGH

## Summary

Phase 4 adds the core application tracking domain: an ApplicationEntity with an 8-status state machine, an ApplicationNote child entity, date tracking with auto-update behavior, and cross-table search/filtering spanning applications, jobs, companies, and notes. The codebase already has well-established patterns from Phase 3 (entities, repositories with JPQL, services with batch name resolution, controllers, integration tests) that this phase replicates and extends.

The primary complexity is the state machine (validated transitions with terminal status reversal) and the cross-table search query (4-table JOIN with text search across multiple columns). Both are solvable with patterns already present in the project -- enum-based validation in the service layer and JPQL @Query with LEFT JOINs in the repository.

**Primary recommendation:** Follow Phase 3 patterns exactly. State machine logic belongs in the service layer as a simple `Map<Status, Set<Status>>` transition map. Cross-table search uses a single JPQL query with LEFT JOINs. Notes are a nested resource under `/api/applications/{id}/notes`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- 8 fixed statuses as enum: INTERESTED, APPLIED, PHONE_SCREEN, INTERVIEW, OFFER, REJECTED, ACCEPTED, WITHDRAWN
- Predefined state machine with validated transitions
- Withdrawn available from any active status; Rejected available from any status after APPLIED
- Terminal statuses (REJECTED, ACCEPTED, WITHDRAWN) are reversible -- user can reopen to any active status
- Application requires jobId foreign key (required), default status INTERESTED
- Quick notes: free-text field on application entity; separate ApplicationNote entity for detailed log
- Note types: GENERAL, PHONE_CALL, EMAIL, FOLLOW_UP, STATUS_CHANGE
- STATUS_CHANGE notes auto-created on status transitions
- appliedDate auto-set on APPLIED transition; lastActivityDate auto-updated on status change and note creation
- nextActionDate: informational only, no enforcement
- Text search across: job title, company name, quick notes, notes log, job description
- Filters: status (multi), company, date range, job type, work mode, has-next-action-date, note type
- Sort: createdAt (default desc), lastActivityDate, nextActionDate, company name
- Soft delete (archive) pattern; no cascade from applications to jobs
- Plain UUID foreign key for jobId (no @ManyToOne), same as Phase 3 companyId pattern

### Claude's Discretion
- Exact state machine transition map (which specific transitions are allowed between non-terminal statuses)
- DTO field naming and response structure details
- Repository query implementation (JPQL @Query vs Spring Data method naming vs Specifications)
- Search implementation approach (single search param vs separate fields)
- Pagination defaults (page size)
- Index strategy for filter/search columns
- Whether to use PATCH vs PUT for updates
- How to handle cross-table search performance (joins, subqueries, or denormalization)

### Deferred Ideas (OUT OF SCOPE)
- Custom interview stages -- Phase 5
- Notifications/reminders for nextActionDate -- v2 (RMND-01)
- Tags/labels -- v2 (TAGS-01)
- Salary comparison -- v2 (SALA-01)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| APPL-01 | User can create an application linked to a job posting | ApplicationEntity with required jobId FK, validated against JobRepository; follows JobEntity/CompanyEntity creation pattern |
| APPL-02 | User can set and change application status through 8 statuses with validated transitions | ApplicationStatus enum + service-layer transition map; InvalidTransitionException for illegal moves |
| APPL-05 | User can track applied date, last activity date, next action date | appliedDate (LocalDate), lastActivityDate (Instant), nextActionDate (LocalDate) on entity; auto-update logic in service |
| APPL-06 | User can add free-text notes to each application | ApplicationNote child entity with noteType enum; nested REST resource /api/applications/{id}/notes |
| APPL-07 | User can search applications by text and filter by status, company, date range | JPQL query with LEFT JOINs across applications/jobs/companies/notes tables; multi-value status filter |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.0.4 | Framework | Already in project |
| Spring Data JPA | (Boot managed) | Repository layer | Already in project |
| Kotlin | 2.2.21 | Language | Already in project |
| PostgreSQL | (Docker Compose) | Database | Already in project |
| Flyway | (Boot managed) | Migrations | Already in project |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jakarta Validation | (Boot managed) | Request validation | @NotNull, @Size on DTOs |
| MockK | 1.13+ | Test mocking | Already in project |
| SpringMockK | 4.0+ | @MockkBean | Already in project |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JPQL @Query for search | JPA Specifications | Specifications are more composable but harder to read; JPQL matches existing pattern |
| Service-layer state machine | Spring Statemachine | Massively over-engineered for 8 statuses; adds dependency for no benefit |
| PUT for updates | PATCH with JSON Merge Patch | PATCH is more correct semantically but project uses PUT consistently |

**No new dependencies needed.** Phase 4 uses only what Phase 3 already established.

## Architecture Patterns

### Recommended Project Structure
```
backend/src/main/kotlin/com/alex/job/hunt/jobhunt/
  entity/
    ApplicationEntity.kt         # Application JPA entity
    ApplicationNoteEntity.kt     # Note JPA entity
    enums.kt                     # Add ApplicationStatus, NoteType
  repository/
    ApplicationRepository.kt     # JPQL queries for filtered search
    ApplicationNoteRepository.kt # Basic CRUD + find by applicationId
  service/
    ApplicationService.kt        # State machine, date logic, search orchestration
    ApplicationNoteService.kt    # Note CRUD, auto STATUS_CHANGE notes
    DomainExceptions.kt          # Add InvalidTransitionException
  dto/
    ApplicationDtos.kt           # Create/Update/Response + status change request
    ApplicationNoteDtos.kt       # Note create/update/response
  controller/
    ApplicationController.kt     # /api/applications endpoints
    ApplicationNoteController.kt # /api/applications/{id}/notes endpoints
  config/
    GlobalExceptionHandler.kt    # Add InvalidTransitionException handler

backend/src/main/resources/db/migration/
  V7__phase04_create_applications.sql
  V8__phase04_create_application_notes.sql
```

### Pattern 1: State Machine via Transition Map
**What:** Define allowed transitions as a `Map<ApplicationStatus, Set<ApplicationStatus>>` in the service layer.
**When to use:** Simple finite state machines with < 20 states.
**Example:**
```kotlin
// Recommended transition map (Claude's discretion area)
enum class ApplicationStatus {
    INTERESTED, APPLIED, PHONE_SCREEN, INTERVIEW, OFFER, REJECTED, ACCEPTED, WITHDRAWN
}

private val TERMINAL_STATUSES = setOf(
    ApplicationStatus.REJECTED, ApplicationStatus.ACCEPTED, ApplicationStatus.WITHDRAWN
)

private val ACTIVE_STATUSES = ApplicationStatus.entries.toSet() - TERMINAL_STATUSES

private val transitions: Map<ApplicationStatus, Set<ApplicationStatus>> = mapOf(
    ApplicationStatus.INTERESTED to setOf(
        ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN
    ),
    ApplicationStatus.APPLIED to setOf(
        ApplicationStatus.PHONE_SCREEN, ApplicationStatus.INTERVIEW,
        ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
    ),
    ApplicationStatus.PHONE_SCREEN to setOf(
        ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER,
        ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
    ),
    ApplicationStatus.INTERVIEW to setOf(
        ApplicationStatus.PHONE_SCREEN, ApplicationStatus.OFFER,
        ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
    ),
    ApplicationStatus.OFFER to setOf(
        ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
    ),
    // Terminal statuses can reopen to any active status
    ApplicationStatus.REJECTED to ACTIVE_STATUSES,
    ApplicationStatus.ACCEPTED to ACTIVE_STATUSES,
    ApplicationStatus.WITHDRAWN to ACTIVE_STATUSES,
)

fun validateTransition(from: ApplicationStatus, to: ApplicationStatus) {
    if (to !in (transitions[from] ?: emptySet())) {
        throw InvalidTransitionException("Cannot transition from $from to $to")
    }
}
```

**Design rationale:**
- INTERESTED can only go to APPLIED or WITHDRAWN (you haven't applied yet, so no phone screen/interview/offer/rejection makes sense)
- APPLIED opens the full forward pipeline plus REJECTED and WITHDRAWN
- PHONE_SCREEN and INTERVIEW can go forward, backward between each other, or to terminal
- OFFER can only go to ACCEPTED, REJECTED, or WITHDRAWN
- All terminal statuses can reopen to any active status (per user decision)

### Pattern 2: Nested Resource for Notes
**What:** Notes are a sub-resource of applications, accessed via `/api/applications/{appId}/notes`.
**When to use:** Strong parent-child ownership where the child has no meaning outside the parent.
**Example:**
```kotlin
@RestController
@RequestMapping("/api/applications/{applicationId}/notes")
class ApplicationNoteController(
    private val noteService: ApplicationNoteService
) {
    @PostMapping
    fun create(
        @PathVariable applicationId: UUID,
        @Valid @RequestBody request: CreateNoteRequest
    ): ResponseEntity<NoteResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(noteService.create(applicationId, request, userId))
    }
    // GET, PUT, DELETE follow same pattern
}
```

### Pattern 3: Cross-Table Search with JPQL LEFT JOINs
**What:** Single JPQL query that searches across applications, jobs, companies, and notes.
**When to use:** Full-text-like search across related entities.
**Example approach:**
```kotlin
@Query("""
    SELECT DISTINCT a FROM ApplicationEntity a
    LEFT JOIN JobEntity j ON j.id = a.jobId AND j.userId = a.userId
    LEFT JOIN CompanyEntity c ON c.id = j.companyId AND c.userId = a.userId
    WHERE a.userId = :userId
    AND (:includeArchived = true OR a.archived = false)
    AND (:#{#statuses == null} = true OR a.status IN :statuses)
    AND (:companyId IS NULL OR j.companyId = :companyId)
    AND (:search IS NULL OR (
        LOWER(a.quickNotes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        OR LOWER(j.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        OR EXISTS (
            SELECT 1 FROM ApplicationNoteEntity n
            WHERE n.applicationId = a.id
            AND LOWER(n.content) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
    ))
    AND (CAST(:dateFrom AS date) IS NULL OR a.appliedDate >= :dateFrom)
    AND (CAST(:dateTo AS date) IS NULL OR a.appliedDate <= :dateTo)
    AND (:jobType IS NULL OR j.jobType = :jobType)
    AND (:workMode IS NULL OR j.workMode = :workMode)
    AND (:hasNextAction IS NULL OR (:hasNextAction = true AND a.nextActionDate IS NOT NULL)
        OR (:hasNextAction = false AND a.nextActionDate IS NULL))
""")
fun findFiltered(...): Page<ApplicationEntity>
```

**Key considerations:**
- Use `CAST(:param AS string)` for nullable string parameters (same PostgreSQL bytea workaround from Phase 3)
- Use `CAST(:dateFrom AS date)` for nullable date parameters
- Use `EXISTS` subquery for notes search rather than a JOIN to avoid duplicates
- Multi-value status filter uses `IN :statuses` with a collection parameter
- Note type filter can be a separate query parameter with another EXISTS subquery
- Sort by company name requires a JOIN-based ORDER BY clause

### Pattern 4: Auto-Update Date Behavior in Service
**What:** Service methods automatically update lastActivityDate and appliedDate based on business rules.
**When to use:** Implicit business logic that should not be the caller's responsibility.
**Example:**
```kotlin
fun updateStatus(id: UUID, newStatus: ApplicationStatus, userId: UUID): ApplicationResponse {
    val entity = findOwnedApplication(id, userId)
    validateTransition(entity.status, newStatus)

    entity.status = newStatus
    entity.lastActivityDate = Instant.now()
    entity.updatedAt = Instant.now()

    // Auto-set appliedDate when transitioning to APPLIED
    if (newStatus == ApplicationStatus.APPLIED && entity.appliedDate == null) {
        entity.appliedDate = LocalDate.now()
    }

    val saved = applicationRepository.save(entity)

    // Auto-create STATUS_CHANGE note
    noteService.createStatusChangeNote(id, entity.status, newStatus, userId)

    return saved.toResponse(...)
}
```

### Anti-Patterns to Avoid
- **Bidirectional JPA relationships:** Do NOT use @OneToMany on ApplicationEntity pointing to notes. Use plain UUID `applicationId` on ApplicationNoteEntity and query separately.
- **Eager fetching notes with applications:** Notes should only load when explicitly requested via the notes endpoint or when searching.
- **State machine in the controller:** All transition validation belongs in the service layer. Controller just passes the desired status.
- **N+1 queries in list endpoints:** Batch-resolve job titles and company names using `findAllByIdInAndUserId`, same as Phase 3 pattern.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| State machine framework | Custom event/listener system | Simple Map<Status, Set<Status>> | 8 statuses doesn't warrant a framework; Spring Statemachine is for hundreds of states |
| Full-text search | Custom search indexing | PostgreSQL LIKE with JPQL | Sufficient for single-user app; add pg_trgm or Elasticsearch only if performance demands |
| Pagination | Custom offset/limit logic | Spring Pageable | Already used in Phase 3 |
| Date tracking | Custom AOP/interceptors | Explicit service-layer updates | Transparent, testable, follows existing pattern |
| Audit logging | Custom event system | Auto STATUS_CHANGE notes in service | Simple and sufficient for Phase 4 scope |

**Key insight:** This phase's complexity is in the domain logic (state machine, date auto-updates, cross-table search), not in infrastructure. All infrastructure is already solved by Phase 3 patterns.

## Common Pitfalls

### Pitfall 1: Multi-value Enum Filter in JPQL
**What goes wrong:** Passing a `List<ApplicationStatus>?` to JPQL with null check doesn't work the same as single-value params.
**Why it happens:** SpEL expression `(:#{#statuses == null} = true OR a.status IN :statuses)` requires careful null handling. Passing an empty list to `IN` causes a SQL error.
**How to avoid:** In the service layer, convert null to null and empty list to null before passing to repository. Or use two separate repository methods (one with status filter, one without).
**Warning signs:** SQL errors like "syntax error at or near ')'" when status list is empty.

### Pitfall 2: JPQL Sort by Joined Column
**What goes wrong:** Sorting by company name requires the company JOIN to be present in the query, and Spring Pageable's `Sort` parameter uses entity property names, not SQL column names.
**Why it happens:** Spring Data JPA translates Sort properties to JPQL paths. If sorting by a joined entity's field, the join alias must be stable.
**How to avoid:** For company name sort, add an explicit `ORDER BY` clause in the JPQL rather than relying on Pageable sort, OR define the sort mapping in a custom Pageable resolver.
**Warning signs:** "Could not resolve attribute" errors when sorting by companyName.

### Pitfall 3: PostgreSQL CAST for Nullable Parameters
**What goes wrong:** PostgreSQL cannot determine the type of a null parameter without explicit casting, leading to "could not determine data type of parameter" errors.
**Why it happens:** Hibernate sends null without type info; PostgreSQL needs explicit type hints.
**How to avoid:** Use `CAST(:param AS string)` for nullable string params and `CAST(:param AS date)` for nullable date params. This is an established pattern from Phase 3.
**Warning signs:** "ERROR: could not determine data type of parameter $N" at runtime.

### Pitfall 4: Status Change Note Creating Circular Update
**What goes wrong:** Status change creates a note, note creation updates lastActivityDate, which triggers another save cycle.
**Why it happens:** If note creation is done inside the same transaction as status update with auto-flush.
**How to avoid:** Update lastActivityDate once in the status change method BEFORE creating the note. The note's own lastActivityDate update is handled in the note service for manually-created notes only.
**Warning signs:** Unexpected multiple updates to lastActivityDate timestamps.

### Pitfall 5: Duplicate Results from JOIN-based Search
**What goes wrong:** Searching across notes with a JOIN can return duplicate application rows (one per matching note).
**Why it happens:** A LEFT JOIN on notes where multiple notes match the search term produces multiple result rows per application.
**How to avoid:** Use `SELECT DISTINCT` or use `EXISTS` subquery for notes search instead of a direct JOIN.
**Warning signs:** Pagination counts are wrong; same application appears multiple times in results.

## Code Examples

### Flyway Migration: applications table
```sql
-- V7__phase04_create_applications.sql
CREATE TABLE applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users(id),
    job_id              UUID         NOT NULL REFERENCES jobs(id),
    status              VARCHAR(50)  NOT NULL DEFAULT 'INTERESTED',
    quick_notes         TEXT,
    applied_date        DATE,
    last_activity_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    next_action_date    DATE,
    archived            BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_user_archived ON applications(user_id, archived);
CREATE INDEX idx_applications_user_status ON applications(user_id, status);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_user_applied_date ON applications(user_id, applied_date);
CREATE INDEX idx_applications_user_next_action ON applications(user_id, next_action_date);
-- Unique constraint: one application per job per user
CREATE UNIQUE INDEX idx_applications_user_job ON applications(user_id, job_id) WHERE archived = false;
```

### Flyway Migration: application_notes table
```sql
-- V8__phase04_create_application_notes.sql
CREATE TABLE application_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID         NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    note_type       VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_notes_application_id ON application_notes(application_id);
CREATE INDEX idx_app_notes_note_type ON application_notes(application_id, note_type);
```

### ApplicationEntity
```kotlin
@Entity
@Table(name = "applications")
class ApplicationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "job_id", nullable = false)
    val jobId: UUID,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: ApplicationStatus = ApplicationStatus.INTERESTED,

    @Column(name = "quick_notes", columnDefinition = "TEXT")
    var quickNotes: String? = null,

    @Column(name = "applied_date")
    var appliedDate: LocalDate? = null,

    @Column(name = "last_activity_date", nullable = false)
    var lastActivityDate: Instant = Instant.now(),

    @Column(name = "next_action_date")
    var nextActionDate: LocalDate? = null,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "archived_at")
    var archivedAt: Instant? = null,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApplicationEntity) return false
        return id != null && id == other.id
    }
    override fun hashCode(): Int = javaClass.hashCode()
}
```

### ApplicationNoteEntity
```kotlin
@Entity
@Table(name = "application_notes")
class ApplicationNoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "note_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var noteType: NoteType = NoteType.GENERAL,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApplicationNoteEntity) return false
        return id != null && id == other.id
    }
    override fun hashCode(): Int = javaClass.hashCode()
}
```

### Discretion Recommendations

**State machine transition map:** See Pattern 1 above. Forward-only for active statuses (with INTERVIEW <-> PHONE_SCREEN bidirectional), all terminals reopen to any active.

**DTO structure:** Use separate endpoints for status change (`PATCH /api/applications/{id}/status`) vs field update (`PUT /api/applications/{id}`). Status change is a business operation, not a field edit.

**Repository approach:** JPQL @Query for the main filtered search (matches existing pattern). Spring Data method naming for simple queries like `findByIdAndUserId`.

**Search approach:** Single `q` parameter for text search (matches Phase 3 pattern). All filters as separate query params.

**Pagination defaults:** Page size 20 (matches api-patterns skill).

**Index strategy:** See migration examples above. Key indexes: user_id+status (status filter), user_id+applied_date (date range), job_id (FK lookup), application_id on notes (note lookup).

**PUT vs PATCH:** Use PUT for full field updates (matches Phase 3 pattern). Use PATCH only for the status change endpoint since it's a targeted operation.

**Cross-table search performance:** Use LEFT JOINs in a single query with EXISTS subquery for notes. This is fine for a single-user application. No denormalization needed.

**Unique constraint:** One active (non-archived) application per job per user. Enforced with a partial unique index `WHERE archived = false`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring Statemachine for all FSMs | Simple maps for small FSMs | Always true for < 20 states | Avoids massive config overhead |
| Hibernate Envers for audit | Application-level notes | N/A | Simpler, user-facing audit trail |
| @ManyToOne relationships | Plain UUID FKs | Project decision (Phase 3) | Simpler entities, explicit queries |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + MockK |
| Config file | build.gradle.kts (already configured) |
| Quick run command | `./gradlew :backend:test --tests "*.application.*"` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| APPL-01 | Create application linked to job | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 |
| APPL-02 | Status transitions (valid + invalid) | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 |
| APPL-05 | Date tracking (auto-set, manual override) | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 |
| APPL-06 | Notes CRUD + auto STATUS_CHANGE | integration | `./gradlew :backend:test --tests "*.application.ApplicationNoteControllerIntegrationTests"` | No -- Wave 0 |
| APPL-07 | Search + filter (text, status, company, dates) | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test`
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/kotlin/.../application/ApplicationControllerIntegrationTests.kt` -- covers APPL-01, APPL-02, APPL-05, APPL-07
- [ ] `backend/src/test/kotlin/.../application/ApplicationNoteControllerIntegrationTests.kt` -- covers APPL-06
- [ ] TestHelper needs `createJob` and `createApplication` helper methods added

## Open Questions

1. **Note type filter implementation**
   - What we know: User wants to filter applications that have notes of a specific type
   - What's unclear: Should this filter return applications that have AT LEAST ONE note of that type, or ONLY notes of that type?
   - Recommendation: AT LEAST ONE (EXISTS subquery) -- more useful and consistent with other filters

2. **Sort by company name**
   - What we know: Company name is resolved via job -> company JOIN
   - What's unclear: Whether Spring Pageable can handle this or if we need a custom ORDER BY
   - Recommendation: Use explicit JPQL ORDER BY with a CASE expression for sort direction, controlled by a separate `sortBy` query param instead of Spring's default `sort` param

## Sources

### Primary (HIGH confidence)
- Existing codebase: JobEntity.kt, JobService.kt, JobRepository.kt, JobController.kt -- established patterns
- Existing codebase: enums.kt, DomainExceptions.kt, GlobalExceptionHandler.kt -- extension points
- Existing codebase: V6__phase03_create_jobs.sql -- migration pattern with indexes
- Existing codebase: JobControllerIntegrationTests.kt, TestHelper.kt -- test patterns

### Secondary (MEDIUM confidence)
- Project skills: entity-patterns.md, api-patterns.md, testing-patterns.md -- coding conventions

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new dependencies, reuses Phase 3 stack entirely
- Architecture: HIGH - follows established entity/service/controller patterns exactly
- State machine: HIGH - simple map-based approach, well-understood pattern
- Cross-table search: MEDIUM - JPQL complexity with 4-table joins needs careful testing, especially with nullable parameters
- Pitfalls: HIGH - documented from Phase 3 experience (CAST workaround, batch loading)

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- no external dependencies or fast-moving libraries)
