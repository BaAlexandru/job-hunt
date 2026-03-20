# Phase 3: Company & Job Domain - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can manage companies and job postings through the REST API, with jobs optionally linked to companies. Includes CRUD operations, soft delete (archive), pagination, basic filtering, and user data isolation. No frontend UI — API tested via Swagger/Postman.

</domain>

<decisions>
## Implementation Decisions

### Data model — Company
- Fields: name (required), website (nullable), location (free text, nullable), notes (free text, nullable)
- Work mode not on Company — it's per job posting
- UUID primary key, userId foreign key for ownership
- Soft delete: archived boolean + archivedAt timestamp
- createdAt, updatedAt timestamps (same pattern as Phase 2 entities)

### Data model — Job
- Fields: title (required), description (plain text, nullable), url (nullable), notes (free text, nullable), deadline/closingDate (nullable date)
- Location: free text field + workMode enum (ONSITE, REMOTE, HYBRID)
- Job type enum: FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP
- Company link: nullable companyId foreign key (jobs can exist without a company)
- Company link is mutable — can be changed via update endpoint
- Soft delete: archived boolean + archivedAt timestamp
- UUID primary key, userId foreign key for ownership

### Data model — Salary (on Job)
- Flexible salary model with salaryType enum: RANGE, FIXED, TEXT
- RANGE: salaryMin (BigDecimal) + salaryMax (BigDecimal)
- FIXED: salaryMin used as the single amount (salaryMax null)
- TEXT: salaryText (free text field, e.g., "Competitive")
- Currency: string field (e.g., "GBP", "USD"), nullable
- Period enum: ANNUAL, MONTHLY, HOURLY, DAILY — nullable
- All salary fields nullable (salary info often not disclosed)

### Deletion rules
- Soft delete (archive) as default — sets archived=true and archivedAt timestamp
- Block company deletion when it has linked (non-archived) jobs: return 409 Conflict
- Default list queries exclude archived records
- Separate endpoint or query param to include/view archived items

### User isolation
- Strict per-user isolation: all queries filter by authenticated userId
- Accessing another user's resource returns 404 (not 403) to prevent enumeration
- Every company and job row has a userId column

### API response shape
- Pagination: Spring Pageable with page/size/sort query params
- Default sort: createdAt descending (newest first)
- Job responses embed companyId + companyName (not full company object)
- Basic filters from day one:
  - Companies: search by name (case-insensitive contains)
  - Jobs: filter by companyId, jobType, workMode; search by title

### Error responses
- Standardize error response format globally (status, error, message, timestamp, path)
- Retrofit Phase 2 auth errors to use the same format
- Field-level validation errors: map of field name to error message
- Not-found: 404 for missing or unauthorized resources
- Conflict: 409 when deleting company with linked jobs

### Job description format
- Plain text for now (TEXT column)
- Rich text (Markdown/HTML) deferred to frontend phase — rendering is a frontend concern

### Claude's Discretion
- Exact DTO field naming and response structure details
- Repository query method implementation (Spring Data naming vs @Query)
- Pagination defaults (page size)
- Index strategy for filter/search columns
- Whether to use PATCH (partial update) vs PUT (full replace) or both
- Archived items retrieval approach (query param vs separate endpoint)
- Global exception handler implementation details
- How to extract authenticated userId in controllers (SecurityContext utility)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` — Tech stack constraints (Kotlin, Spring Boot 4.x, Spring Data JPA, PostgreSQL)
- `.planning/REQUIREMENTS.md` — COMP-01 through COMP-03, JOBS-01 through JOBS-04
- `.planning/ROADMAP.md` — Phase 3 success criteria (5 criteria)

### Prior phase context
- `.planning/phases/01-foundation-infrastructure/01-CONTEXT.md` — Monorepo structure, Flyway conventions, UUID PKs, dependency management
- `.planning/phases/02-authentication/02-CONTEXT.md` — JWT strategy, user entity structure, security config, rate limiting, exception handling patterns

### Backend conventions
- `backend/CLAUDE.md` — Package structure, coding conventions, Flyway naming, testing approach

### Existing code to build on
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/UserEntity.kt` — Entity pattern to follow (UUID, timestamps, equals/hashCode)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/AuthExceptionHandler.kt` — Exception handler to generalize into global handler
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/SecurityConfig.kt` — Security filter chain (new endpoints need JWT protection)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt` — Controller patterns (ResponseEntity, DTOs, validation)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/` — DTO patterns (data classes with jakarta.validation)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `UserEntity.kt`: Entity pattern with UUID GenerationType.UUID, Instant timestamps, custom equals/hashCode — replicate for Company and Job entities
- `AuthExceptionHandler.kt`: @RestControllerAdvice pattern — refactor into global handler, add domain exceptions
- DTOs in `dto/` package: Data classes with @NotBlank, @Email, @Size validation annotations — same pattern for Company/Job request/response DTOs
- `JwtAuthenticationFilter`: Already sets SecurityContext with authenticated user — extract userId from principal for ownership queries

### Established Patterns
- Flyway: V{N}__phase03_{description}.sql — next migration number is V5
- JPA: hibernate.ddl-auto=validate — entities must exactly match Flyway schema
- Repositories: JpaRepository<Entity, UUID> with Spring Data query method naming
- Services: @Service + @Transactional, constructor injection, throw domain exceptions
- Controllers: @RestController, constructor injection, @Valid on request bodies, return ResponseEntity

### Integration Points
- SecurityConfig: New /api/companies/** and /api/jobs/** endpoints are protected by default (require JWT) — no config changes needed
- Flyway: New migration files for companies and jobs tables
- UserEntity: Foreign key target for userId on companies and jobs tables
- Global exception handler: Refactor AuthExceptionHandler → GlobalExceptionHandler serving all controllers

</code_context>

<specifics>
## Specific Ideas

- User wants to learn Kotlin — keep idiomatic patterns (data classes, enums, extension functions)
- Description starts as plain text; user wants rich text in the future — design the column as TEXT so it's format-agnostic
- Salary model is intentionally flexible (range/fixed/text) because different companies disclose salary differently
- User mentioned wanting public/private visibility and sharing — deferred to a future phase

</specifics>

<deferred>
## Deferred Ideas

- Public/private visibility on companies and jobs — user wants the ability to make data public, private, or shared with specific users. This is a sharing/access-control feature that belongs in its own phase
- Rich text job descriptions — rendering concern for frontend (Phase 8). Store as plain text for now, column type (TEXT) is compatible with Markdown/HTML later
- Full-text search across all fields — basic filters now, PostgreSQL full-text search can be added later

</deferred>

---

*Phase: 03-company-job-domain*
*Context gathered: 2026-03-20*
