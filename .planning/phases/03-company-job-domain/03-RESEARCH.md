# Phase 3: Company & Job Domain - Research

**Researched:** 2026-03-20
**Domain:** Spring Boot REST CRUD with JPA, pagination, filtering, soft delete, user isolation
**Confidence:** HIGH

## Summary

Phase 3 builds two domain aggregates (Company, Job) on top of the existing Spring Boot 4 + Kotlin backend with JWT authentication from Phase 2. The core work is standard Spring Data JPA CRUD with pagination, filtering, soft delete (archive), and strict user isolation. No new dependencies are required -- everything needed (Spring Data JPA, validation, security) is already in build.gradle.kts.

The most important technical discovery is that the current `UserDetailsServiceImpl` creates a Spring Security `User` principal that does NOT include the userId. The JWT contains a `userId` claim, but the `JwtAuthenticationFilter` stores a Spring `UserDetails` (email-based) as the principal. Controllers need the userId (UUID) to filter domain queries. This must be resolved -- either by creating a custom `UserDetails` implementation that carries the userId, or by adding a utility that looks up the user by email from the SecurityContext. The custom UserDetails approach is cleaner and avoids an extra DB query per request.

**Primary recommendation:** Follow existing entity/service/controller patterns exactly. Resolve the userId extraction gap first (custom UserDetails or SecurityContext utility), then build Company CRUD, then Job CRUD with company linking, then the global exception handler refactoring.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Company fields: name (required), website (nullable), location (free text, nullable), notes (free text, nullable)
- Company: UUID PK, userId FK, soft delete (archived + archivedAt), createdAt/updatedAt
- Job fields: title (required), description (plain text, nullable), url (nullable), notes (nullable), deadline/closingDate (nullable date)
- Job location: free text + workMode enum (ONSITE, REMOTE, HYBRID)
- Job type enum: FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP
- Job company link: nullable companyId FK, mutable via update
- Salary model: salaryType enum (RANGE, FIXED, TEXT), salaryMin/salaryMax (BigDecimal), salaryText (free text), currency (string), period enum (ANNUAL, MONTHLY, HOURLY, DAILY) -- all nullable
- Soft delete: archived=true + archivedAt timestamp; default lists exclude archived
- Block company deletion when linked non-archived jobs exist (409 Conflict)
- Strict per-user isolation: all queries filter by userId; unauthorized access returns 404 (not 403)
- Pagination: Spring Pageable with page/size/sort params, default sort createdAt desc
- Job responses embed companyId + companyName (not full company object)
- Filters: companies search by name (case-insensitive contains); jobs filter by companyId, jobType, workMode, search by title
- Standardized error response format globally; retrofit Phase 2 auth errors
- Field-level validation errors: map of field name to error message
- Plain text job descriptions (TEXT column)

### Claude's Discretion
- Exact DTO field naming and response structure details
- Repository query method implementation (Spring Data naming vs @Query)
- Pagination defaults (page size)
- Index strategy for filter/search columns
- Whether to use PATCH (partial update) vs PUT (full replace) or both
- Archived items retrieval approach (query param vs separate endpoint)
- Global exception handler implementation details
- How to extract authenticated userId in controllers (SecurityContext utility)

### Deferred Ideas (OUT OF SCOPE)
- Public/private visibility on companies and jobs
- Rich text job descriptions (frontend concern, Phase 8)
- Full-text search across all fields

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| COMP-01 | User can add a company with name, website, location, and notes | Entity pattern from UserEntity, Flyway migration V5, CreateCompanyRequest DTO with validation |
| COMP-02 | User can edit and delete companies | PUT/PATCH update endpoint, soft delete (archive) with 409 Conflict guard for linked jobs |
| COMP-03 | User can view all companies in a list | Pageable list endpoint with name search filter, exclude archived by default |
| JOBS-01 | User can add a job posting with title, description, URL, salary range, location, and job type | JobEntity with enums (WorkMode, JobType, SalaryType, SalaryPeriod), BigDecimal salary fields |
| JOBS-02 | User can link a job posting to a company | Nullable companyId FK on jobs table, validated that company exists and belongs to user |
| JOBS-03 | User can edit and delete job postings | PUT/PATCH update, soft delete (archive), company link mutable via update |
| JOBS-04 | User can store the full job description text | TEXT column for description, plain text, nullable |

</phase_requirements>

## Standard Stack

### Core (already in build.gradle.kts -- no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Data JPA | (managed by Boot 4.0.4) | Repository, Pageable, Page | Already present; provides CRUD + pagination out of box |
| Spring Boot Validation | (managed by Boot 4.0.4) | @Valid, @NotBlank, @Size | Already present; Jakarta Bean Validation |
| Spring Security | (managed by Boot 4.0.4) | SecurityContext, userId extraction | Already present; JWT filter sets authentication |
| Jackson Kotlin Module | (managed by Boot 4.0.4) | JSON serialization of data classes | Already present; handles Kotlin data classes |
| Flyway | (managed by Boot 4.0.4) | Schema migrations V5, V6 | Already present; manages all DDL |

### No New Dependencies Needed

This phase uses only what Phase 1 and Phase 2 already installed. No additions to build.gradle.kts.

## Architecture Patterns

### Package Structure (new files)

```
backend/src/main/kotlin/com/alex/job/hunt/jobhunt/
  entity/
    CompanyEntity.kt          # NEW
    JobEntity.kt              # NEW
    enums.kt                  # NEW - WorkMode, JobType, SalaryType, SalaryPeriod enums
  repository/
    CompanyRepository.kt      # NEW
    JobRepository.kt          # NEW
  service/
    CompanyService.kt         # NEW
    JobService.kt             # NEW
  controller/
    CompanyController.kt      # NEW
    JobController.kt          # NEW
  dto/
    CompanyDtos.kt            # NEW - CreateCompanyRequest, UpdateCompanyRequest, CompanyResponse
    JobDtos.kt                # NEW - CreateJobRequest, UpdateJobRequest, JobResponse
    ErrorResponse.kt          # NEW - standardized error shape
  config/
    GlobalExceptionHandler.kt # REFACTORED from AuthExceptionHandler.kt
  security/
    SecurityContextUtil.kt    # NEW - extract userId from SecurityContext
    UserDetailsServiceImpl.kt # MODIFIED - return custom UserDetails with userId
```

### Pattern 1: User Isolation via Repository Queries

**What:** Every repository method filters by userId to enforce data isolation.
**When to use:** All domain entity queries (companies, jobs, and future aggregates).

```kotlin
interface CompanyRepository : JpaRepository<CompanyEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): CompanyEntity?
    fun findByUserIdAndArchivedFalse(userId: UUID, pageable: Pageable): Page<CompanyEntity>
    fun existsByIdAndUserIdAndArchivedFalse(id: UUID, userId: UUID): Boolean
}
```

**Key rule:** Never expose `findById` without userId filtering. Accessing another user's resource must return 404 (not 403) to prevent enumeration.

### Pattern 2: Extracting Authenticated UserId

**What:** The current JWT filter stores a Spring Security `User` (email-based) as principal. We need the UUID userId for domain queries.
**Problem:** `UserDetailsServiceImpl` returns `User.builder().username(email)...build()` which does NOT carry userId.
**Solution:** Create a custom UserDetails that wraps userId.

```kotlin
// Custom UserDetails carrying userId
class AppUserDetails(
    private val userId: UUID,
    private val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val enabled: Boolean
) : UserDetails {
    fun getUserId(): UUID = userId
    override fun getUsername(): String = email
    override fun getPassword(): String = password
    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun isEnabled(): Boolean = enabled
    // Other methods return true
}

// Utility to extract from SecurityContext
object SecurityContextUtil {
    fun getCurrentUserId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth.principal as AppUserDetails
        return principal.getUserId()
    }
}
```

**Alternative:** Look up user by email from SecurityContext each time -- simpler but adds a DB query per request. The custom UserDetails approach is better.

### Pattern 3: Soft Delete (Archive)

**What:** Entities have `archived: Boolean` and `archivedAt: Instant?` fields. "Delete" sets these instead of removing the row.
**When to use:** Company and Job delete operations.

```kotlin
// Entity fields
@Column(nullable = false)
var archived: Boolean = false

@Column(name = "archived_at")
var archivedAt: Instant? = null

// Service method
fun archive(id: UUID, userId: UUID) {
    val entity = repository.findByIdAndUserId(id, userId)
        ?: throw NotFoundException("Company not found")
    entity.archived = true
    entity.archivedAt = Instant.now()
    entity.updatedAt = Instant.now()
    repository.save(entity)
}
```

### Pattern 4: Pagination with Filtering

**What:** Spring Data Pageable + repository query methods for combined filter + page.
**When to use:** All list endpoints.

```kotlin
// Repository with combined filters
@Query("""
    SELECT j FROM JobEntity j
    WHERE j.userId = :userId AND j.archived = false
    AND (:companyId IS NULL OR j.companyId = :companyId)
    AND (:jobType IS NULL OR j.jobType = :jobType)
    AND (:workMode IS NULL OR j.workMode = :workMode)
    AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
""")
fun findFiltered(
    userId: UUID,
    companyId: UUID?,
    jobType: JobType?,
    workMode: WorkMode?,
    title: String?,
    pageable: Pageable
): Page<JobEntity>
```

### Pattern 5: Global Exception Handler

**What:** Refactor AuthExceptionHandler into a GlobalExceptionHandler that serves all controllers with a standardized error shape.
**When to use:** All error responses across the application.

```kotlin
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {
    // Handles NotFoundException, ConflictException, validation errors,
    // plus existing auth exceptions (RegistrationException, AuthenticationException, etc.)
}
```

### Anti-Patterns to Avoid

- **Bidirectional JPA relationships:** Do NOT add a `@OneToMany jobs` collection on CompanyEntity. Use companyId as a simple FK column on JobEntity, query jobs by companyId when needed. Bidirectional adds complexity with no benefit here.
- **Exposing entities in responses:** Always map Entity -> Response DTO in the controller/service boundary. Never return entities from controllers.
- **Trusting client-sent userId:** Always derive userId from SecurityContext, never accept it in request bodies.
- **Eager fetching:** All `@ManyToOne` must be `FetchType.LAZY`. The Job entity's user relationship should be lazy.

## Discretion Recommendations

These are Claude's Discretion areas from CONTEXT.md with research-backed recommendations:

### PATCH vs PUT
**Recommendation: Use PUT only (full replace).** PATCH with partial updates requires either nullable-everything DTOs or JsonMergePatch, adding complexity for little benefit in a personal tool. PUT is simpler -- the client sends all fields, nulls clear optional fields. This matches the existing AuthController pattern of full request objects.

### Pagination Defaults
**Recommendation: page size 20, max 100.** This aligns with the api-patterns skill. Default sort: `createdAt,desc`.

### Archived Items Retrieval
**Recommendation: Query parameter `?includeArchived=true` on list endpoints.** Simpler than a separate endpoint, keeps the API surface small. Default is `false` (exclude archived).

### Repository Query Implementation
**Recommendation: Use `@Query` JPQL for filtered queries, Spring Data method naming for simple lookups.** The job filtering query (companyId + jobType + workMode + title) is too complex for method naming. Simple lookups like `findByIdAndUserId` work fine with method naming.

### Index Strategy
**Recommendation:** Add indexes in Flyway migrations for:
- `companies(user_id)` -- all queries filter by user
- `companies(user_id, archived)` -- list queries exclude archived
- `jobs(user_id)` -- all queries filter by user
- `jobs(user_id, archived)` -- list queries exclude archived
- `jobs(company_id)` -- company link lookups and delete guard check
- `jobs(user_id, job_type)` -- filter by job type
- `jobs(user_id, work_mode)` -- filter by work mode

### Extracting userId
**Recommendation: Custom AppUserDetails class.** Modify `UserDetailsServiceImpl` to return a custom UserDetails carrying the UUID. Add a `SecurityContextUtil` companion object for controllers. Avoids extra DB query per request.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pagination | Custom offset/limit logic | Spring `Pageable` + `Page<T>` | Handles page/size/sort params, total counts, edge cases |
| Input validation | Manual if-checks in service | Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`) | Declarative, tested, consistent error format |
| Error response formatting | Per-controller try-catch | `@RestControllerAdvice` global handler | Single place for all error mapping |
| SQL schema | JPA auto-DDL | Flyway migrations | Project convention: ddl-auto=validate |
| JSON serialization | Manual mapping | Jackson Kotlin module (already present) | Handles data classes, enums, nulls automatically |
| User isolation | Request interceptor/AOP | Repository-level userId filtering | Simple, explicit, no magic -- each query declares its filter |

## Common Pitfalls

### Pitfall 1: userId Not Available in SecurityContext
**What goes wrong:** Current UserDetails is Spring's `User` which only has username (email). Calling `(principal as AppUserDetails).getUserId()` throws ClassCastException.
**Why it happens:** Phase 2 used Spring's built-in `User` object; userId was in the JWT but never extracted into the principal.
**How to avoid:** Modify `UserDetailsServiceImpl` to return custom `AppUserDetails` with userId BEFORE building any controllers. Update `JwtAuthenticationFilter` if needed.
**Warning signs:** Tests fail with ClassCastException on SecurityContext access.

### Pitfall 2: JPA Entity Not Matching Flyway Schema
**What goes wrong:** Application fails to start with `SchemaManagementException` because entity columns don't match the table.
**Why it happens:** ddl-auto=validate means JPA validates entity mappings against the actual DB schema on startup.
**How to avoid:** Write Flyway migration FIRST, then write entity to match exactly. Column names, types, nullability must match. Use `@Column(name = "snake_case")` explicitly.
**Warning signs:** Application fails on startup; Flyway migration succeeds but validation fails.

### Pitfall 3: BigDecimal Precision in PostgreSQL
**What goes wrong:** Salary values lose precision or fail validation.
**Why it happens:** PostgreSQL NUMERIC without precision allows arbitrary precision; JPA BigDecimal maps need explicit scale.
**How to avoid:** Use `NUMERIC(15, 2)` in Flyway migration and `@Column(precision = 15, scale = 2)` on the entity. This supports values up to trillions with 2 decimal places.
**Warning signs:** Rounding errors in salary display.

### Pitfall 4: N+1 Queries on Job List with Company Name
**What goes wrong:** Listing 20 jobs fires 21 queries (1 for jobs + 20 for each company name).
**Why it happens:** Job response includes companyName, and if the Job entity has a `@ManyToOne` company relationship with LAZY fetch, accessing `job.company.name` triggers individual loads.
**How to avoid:** Two options: (1) Store companyId as a plain UUID column (no JPA relationship) and use a JOIN query to fetch companyName. (2) Use `@ManyToOne(fetch = LAZY)` but use a JPQL `JOIN FETCH` in the list query. Option 1 is simpler for this use case.
**Warning signs:** Slow list endpoints, many SQL queries in logs.

### Pitfall 5: Soft Delete Leaking Archived Records
**What goes wrong:** Archived companies/jobs appear in lists, dropdown selections, or search results.
**Why it happens:** Forgetting `AND archived = false` in a new query method.
**How to avoid:** Default all repository list methods to `archived = false`. Use the `includeArchived` parameter explicitly when needed. Consider naming convention: all `findBy...` methods that return lists include `AndArchivedFalse` unless they explicitly need archived items.
**Warning signs:** "Deleted" items reappearing in the UI.

### Pitfall 6: Company Delete Bypassing Job Check
**What goes wrong:** Company is archived but its jobs now have a dangling companyId reference (conceptually orphaned).
**Why it happens:** Forgetting to check for non-archived jobs linked to the company before archiving.
**How to avoid:** In CompanyService.archive(), ALWAYS check `jobRepository.existsByCompanyIdAndUserIdAndArchivedFalse(companyId, userId)` and throw 409 Conflict if true.
**Warning signs:** Jobs referencing archived companies.

## Code Examples

### Flyway Migration: Companies Table (V5)

```sql
-- V5__phase03_create_companies.sql
CREATE TABLE companies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    name        VARCHAR(255) NOT NULL,
    website     VARCHAR(500),
    location    VARCHAR(255),
    notes       TEXT,
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_companies_user_id ON companies(user_id);
CREATE INDEX idx_companies_user_archived ON companies(user_id, archived);
```

### Flyway Migration: Jobs Table (V6)

```sql
-- V6__phase03_create_jobs.sql
CREATE TABLE jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    company_id   UUID         REFERENCES companies(id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    url          VARCHAR(2000),
    notes        TEXT,
    location     VARCHAR(255),
    work_mode    VARCHAR(50),
    job_type     VARCHAR(50),
    salary_type  VARCHAR(50),
    salary_min   NUMERIC(15, 2),
    salary_max   NUMERIC(15, 2),
    salary_text  VARCHAR(255),
    currency     VARCHAR(10),
    salary_period VARCHAR(50),
    closing_date DATE,
    archived     BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_user_id ON jobs(user_id);
CREATE INDEX idx_jobs_user_archived ON jobs(user_id, archived);
CREATE INDEX idx_jobs_company_id ON jobs(company_id);
```

### Entity Pattern: CompanyEntity

```kotlin
@Entity
@Table(name = "companies")
class CompanyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(length = 500)
    var website: String? = null,

    @Column(length = 255)
    var location: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

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
        if (other !is CompanyEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
```

### Controller Pattern with User Isolation

```kotlin
@RestController
@RequestMapping("/api/companies")
class CompanyController(
    private val companyService: CompanyService
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateCompanyRequest): ResponseEntity<CompanyResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val company = companyService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(company)
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        pageable: Pageable
    ): ResponseEntity<Page<CompanyResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.list(userId, q, includeArchived, pageable))
    }
}
```

### Standardized Error Response

```kotlin
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `@Where(clause = "archived = false")` Hibernate annotation | Explicit repository query filtering | Hibernate 6+ deprecated `@Where` | Use JPQL WHERE clauses, not annotation-based filters |
| Spring Security `User` principal | Custom UserDetails with domain fields | Project-specific need | Avoids extra DB lookup per authenticated request |
| `com.fasterxml.jackson` | `tools.jackson.module` | Spring Boot 4 / Jackson 3 | Jackson group ID changed; already handled in build.gradle.kts |

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + MockK 1.14.2 + SpringMockK 4.0.2 |
| Config file | build.gradle.kts (test dependencies already present) |
| Quick run command | `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.company.*"` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| COMP-01 | Create company with valid fields | integration | `./gradlew :backend:test --tests "*CompanyController*create*"` | Wave 0 |
| COMP-02 | Edit and soft-delete company, 409 on delete with linked jobs | integration | `./gradlew :backend:test --tests "*CompanyController*update*" --tests "*CompanyController*delete*"` | Wave 0 |
| COMP-03 | List companies with pagination and name search | integration | `./gradlew :backend:test --tests "*CompanyController*list*"` | Wave 0 |
| JOBS-01 | Create job with all fields including salary and enums | integration | `./gradlew :backend:test --tests "*JobController*create*"` | Wave 0 |
| JOBS-02 | Link job to company, validate company ownership | integration | `./gradlew :backend:test --tests "*JobController*company*"` | Wave 0 |
| JOBS-03 | Edit and soft-delete job | integration | `./gradlew :backend:test --tests "*JobController*update*" --tests "*JobController*delete*"` | Wave 0 |
| JOBS-04 | Store and retrieve full job description text | integration | `./gradlew :backend:test --tests "*JobController*description*"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test`
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/company/CompanyControllerIntegrationTests.kt` -- covers COMP-01, COMP-02, COMP-03
- [ ] `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/job/JobControllerIntegrationTests.kt` -- covers JOBS-01, JOBS-02, JOBS-03, JOBS-04
- [ ] Test helper: `registerAndVerifyAndLogin()` utility to get a valid JWT for authenticated endpoint tests (extend pattern from AuthControllerIntegrationTests)

## Open Questions

1. **Company-Job relationship: JPA @ManyToOne vs plain UUID column?**
   - What we know: Job responses need companyId + companyName. Using `@ManyToOne` gives ORM navigation but risks N+1. Plain UUID column with JOIN queries is simpler.
   - Recommendation: Use plain `companyId: UUID?` column on JobEntity (no JPA relationship). For list queries that need companyName, use a JPQL JOIN. This avoids lazy-loading complexity and keeps the entity simple. For the delete guard (checking if company has jobs), a simple `existsByCompanyIdAndArchivedFalse` query suffices.

2. **Should UserDetailsServiceImpl modification go in Phase 3 or be a separate concern?**
   - What we know: Phase 3 is the first phase that needs userId in controllers. Phase 2 never needed it (auth endpoints use email).
   - Recommendation: Include it in Phase 3 as prerequisite work. It's a small change and directly enables the domain feature.

## Sources

### Primary (HIGH confidence)
- Existing codebase: UserEntity.kt, AuthExceptionHandler.kt, AuthController.kt, SecurityConfig.kt, JwtAuthenticationFilter.kt, UserDetailsServiceImpl.kt, AuthControllerIntegrationTests.kt
- Project skills: entity-patterns.md, api-patterns.md, testing-patterns.md
- build.gradle.kts: confirmed all needed dependencies present
- Flyway migrations V1-V4: confirmed next migration is V5

### Secondary (MEDIUM confidence)
- Spring Data JPA pagination/Pageable patterns: well-established, verified against project's Spring Boot 4.0.4 version
- Jakarta Bean Validation: standard, version-agnostic patterns

### Tertiary (LOW confidence)
- None -- this phase uses entirely standard Spring patterns with no novel libraries

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all dependencies already present, no new additions
- Architecture: HIGH - follows established patterns from Phase 2 exactly
- Pitfalls: HIGH - identified from direct code inspection (userId gap, N+1, schema validation)

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable Spring Boot patterns, no fast-moving dependencies)
