# Phase 11: Visibility & Sharing - Research

**Researched:** 2026-03-22
**Domain:** Multi-user resource visibility, sharing, and access control in Spring Boot + Next.js
**Confidence:** HIGH

## Summary

Phase 11 adds visibility controls (PRIVATE/PUBLIC/SHARED) to companies and jobs, a sharing mechanism by email, a public browse page, and a "Shared with me" view. The implementation is a standard multi-tenant access control pattern using an enum column on existing entities plus a join table for shares.

The codebase already has well-established patterns for entities (UUID PKs, audit columns, `@Enumerated(EnumType.STRING)`), service-layer authorization (`findByIdAndUserId`), DTOs (separate request/response data classes), repository `@Query` methods, and frontend TanStack Query hooks. This phase extends those patterns rather than introducing new ones.

**Primary recommendation:** Add a `visibility` VARCHAR column (defaulting to `PRIVATE`) on `companies` and `jobs` tables via V16 migration, create a `resource_shares` join table in the same migration, then widen all service-layer queries to include visibility/share checks using `@Query` JPQL. Frontend adds `visibility` to response types, new hooks for sharing/browse/shared-with-me, and two new pages (`/browse`, `/shared`).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- View-only always -- shared users can view but never edit or delete (matches VISI-05)
- Share target identified by email -- if user exists, share is created immediately; if not, error returned ("user not found")
- No invite flow -- recipient must already have an account
- Owner sees list of shared users on the resource detail page with revoke button per share
- Self-share prevented -- API rejects sharing with yourself
- Duplicate shares prevented -- API rejects re-sharing with same user (not idempotent)
- Separate `/browse` page for discovering public resources -- not mixed into existing lists
- Login required to browse public items -- no anonymous API endpoints needed
- Full details shown on public items -- same fields as owner sees
- Owner email displayed on public items ("Shared by user@example.com")
- Visibility control lives on the detail/edit page only -- not on cards
- Small icon badge on cards: globe icon for PUBLIC, users icon for SHARED, no icon for PRIVATE (non-interactive, informational only)
- Separate "Shared with me" section/page for items others have shared with you -- not mixed into your own lists
- Confirmation dialog required when setting visibility to PUBLIC
- Default visibility is PRIVATE -- no behavior change for existing data
- Flyway migration V16 adds visibility column with DEFAULT 'PRIVATE' to companies and jobs tables
- No cascade -- company and job visibility are fully independent
- A job CAN be public/shared even if its parent company is private
- Visibility only applies to companies and jobs -- applications, interviews, documents remain strictly private
- Shares persist when a resource is archived -- not revoked; shared users see it as archived
- Use @Query annotations for visibility-aware repository queries (avoid ambiguous derived method names)
- Phase verification MUST confirm end-to-end UI wiring, not just backend API + hook existence

### Claude's Discretion
- API endpoint structure for visibility and sharing operations
- Database schema for shares table (join table design)
- How the browse page is laid out (grid, list, search/filter)
- Visibility enum implementation (Kotlin enum, JPA mapping)
- Share management UI component design on detail page
- "Shared with me" page/tab navigation placement

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| VISI-01 | User can set visibility (PRIVATE/PUBLIC/SHARED) on companies and jobs | Visibility enum on entities, PATCH endpoint for visibility, V16 migration with DEFAULT 'PRIVATE' |
| VISI-02 | User can share specific companies or jobs with other users (by email) | ResourceShareEntity join table, ShareService with email lookup via UserRepository, share/revoke endpoints |
| VISI-03 | User can browse public companies and jobs from other users | Browse controller with `@Query` filtering by visibility=PUBLIC, /browse frontend page |
| VISI-04 | User can view items shared with them | "Shared with me" queries joining resource_shares table, /shared frontend page |
| VISI-05 | Shared users can only VIEW (not edit/delete) -- view-only always | Service-layer authorization: write operations check `userId == ownerId`, read operations widen to include shared/public |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.0.4 | REST API framework | Already in use |
| Spring Data JPA | (via Boot) | Repository queries | Already in use, `@Query` for visibility-aware queries |
| Flyway | (via Boot starter) | Schema migration | Already in use, V16 next |
| Next.js | (existing) | Frontend framework | Already in use |
| TanStack Query | (existing) | Data fetching/caching | Already in use for all API hooks |
| Lucide React | (existing) | Icon library | Already in use, has Globe and Users icons needed for badges |
| shadcn/ui | (existing) | UI components | Already in use, AlertDialog for confirm, Card for listings |

### Supporting
No new libraries needed. This phase uses only existing dependencies.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JPQL @Query | Spring Data derived method names | CONTEXT.md explicitly says use @Query to avoid ambiguous names -- locked decision |
| Single shares table | Separate company_shares + job_shares | Single polymorphic table is simpler; discriminator column (`resource_type`) avoids schema duplication |
| Enum column on entity | Separate visibility table | Enum column is simpler, matches existing pattern (e.g., `WorkMode`, `JobType` on JobEntity) |

## Architecture Patterns

### Recommended Project Structure
```
backend/src/main/kotlin/com/alex/job/hunt/jobhunt/
  entity/
    Visibility.kt             # enum: PRIVATE, PUBLIC, SHARED
    ResourceShareEntity.kt    # join table entity
  repository/
    ResourceShareRepository.kt
  service/
    ShareService.kt           # share/revoke/list logic
    VisibilityService.kt      # set visibility, visibility-aware getById
  controller/
    ShareController.kt        # /api/companies/{id}/shares, /api/jobs/{id}/shares
    BrowseController.kt       # /api/browse/companies, /api/browse/jobs
    SharedWithMeController.kt # /api/shared/companies, /api/shared/jobs
  dto/
    ShareDtos.kt              # ShareRequest, ShareResponse
    VisibilityDtos.kt         # SetVisibilityRequest
    BrowseDtos.kt             # BrowseCompanyResponse, BrowseJobResponse (includes ownerEmail)

frontend/
  types/api.ts                # Add Visibility type, ShareResponse, BrowseResponse types
  hooks/
    use-visibility.ts         # useSetVisibility mutation
    use-shares.ts             # useShares, useCreateShare, useRevokeShare
    use-browse.ts             # useBrowseCompanies, useBrowseJobs
    use-shared-with-me.ts     # useSharedCompanies, useSharedJobs
  components/
    shared/
      visibility-badge.tsx    # Globe/Users icon badge for cards
      visibility-control.tsx  # Select + confirm dialog for detail pages
      share-manager.tsx       # List shared users + add/revoke on detail page
  app/(dashboard)/
    browse/page.tsx           # Public browse page
    shared/page.tsx           # Shared with me page
```

### Pattern 1: Visibility Enum on Entities
**What:** Add a `visibility` field to CompanyEntity and JobEntity as a Kotlin enum mapped with `@Enumerated(EnumType.STRING)`.
**When to use:** Every entity that supports visibility control.
**Example:**
```kotlin
// Visibility.kt
enum class Visibility {
    PRIVATE, PUBLIC, SHARED
}

// On CompanyEntity / JobEntity:
@Column(name = "visibility", nullable = false, length = 20)
@Enumerated(EnumType.STRING)
var visibility: Visibility = Visibility.PRIVATE
```

### Pattern 2: Polymorphic Shares Table
**What:** Single `resource_shares` table with `resource_type` discriminator column, rather than separate tables per resource type.
**When to use:** Sharing companies and jobs with the same mechanism.
**Example:**
```sql
-- V16__phase11_visibility_and_shares.sql
ALTER TABLE companies ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';
ALTER TABLE jobs ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

CREATE TABLE resource_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(20) NOT NULL,  -- 'COMPANY' or 'JOB'
    resource_id UUID NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    shared_with_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (resource_type, resource_id, shared_with_id)
);

CREATE INDEX idx_resource_shares_resource ON resource_shares(resource_type, resource_id);
CREATE INDEX idx_resource_shares_shared_with ON resource_shares(shared_with_id, resource_type);
```

### Pattern 3: Visibility-Aware Repository Queries
**What:** Widen read queries to allow access when: (a) user owns it, (b) it's PUBLIC, or (c) visibility is SHARED AND user has a share record.
**When to use:** Any getById or list operation that should respect visibility.
**Example:**
```kotlin
// CompanyRepository - visibility-aware get
@Query("""
    SELECT c FROM CompanyEntity c
    WHERE c.id = :id
    AND (
        c.userId = :userId
        OR c.visibility = 'PUBLIC'
        OR (c.visibility = 'SHARED' AND EXISTS (
            SELECT 1 FROM ResourceShareEntity s
            WHERE s.resourceType = 'COMPANY'
            AND s.resourceId = c.id
            AND s.sharedWithId = :userId
        ))
    )
""")
fun findByIdWithVisibility(id: UUID, userId: UUID): CompanyEntity?
```

### Pattern 4: Separate Browse Endpoints
**What:** Dedicated `/api/browse/*` endpoints that only return PUBLIC resources (not mixed with owner's list).
**When to use:** The browse page fetches from these, keeping the existing `/api/companies` and `/api/jobs` unchanged for the owner.
**Example:**
```kotlin
@RestController
@RequestMapping("/api/browse")
class BrowseController(
    private val companyService: CompanyService,
    private val jobService: JobService
) {
    @GetMapping("/companies")
    fun browseCompanies(pageable: Pageable): ResponseEntity<Page<BrowseCompanyResponse>> {
        return ResponseEntity.ok(companyService.browsePublic(pageable))
    }
}
```

### Pattern 5: Service-Layer Authorization for Writes
**What:** Write operations (update, archive, set visibility, manage shares) continue to check `userId == ownerId`. Read operations widen to include public/shared access.
**When to use:** All mutation endpoints.
**Example:**
```kotlin
// Only owner can set visibility
fun setVisibility(resourceId: UUID, visibility: Visibility, userId: UUID) {
    val company = companyRepository.findByIdAndUserId(resourceId, userId)
        ?: throw NotFoundException("Company not found")
    company.visibility = visibility
    company.updatedAt = Instant.now()
    // If changing from SHARED to something else, optionally clean up shares
    companyRepository.save(company)
}
```

### Anti-Patterns to Avoid
- **Mixing owner list with browse results:** Owner's `/api/companies` MUST NOT include other users' public companies. Browse is a separate endpoint.
- **N+1 queries for share checks:** Use EXISTS subquery or JOIN, never load shares collection per entity.
- **Trusting client-sent visibility:** Always validate visibility transitions server-side.
- **Cascade delete on shares:** Shares should be cleaned up via explicit query when a resource is deleted, not via JPA cascade (resource_shares uses raw UUIDs, not entity references).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Confirmation dialog | Custom modal | Existing `ConfirmDialog` component | Already in `frontend/components/shared/confirm-dialog.tsx`, supports `variant="default"` |
| Pagination | Custom pagination | Spring `Pageable` + existing `PaginatedResponse<T>` type | Already established pattern in all list endpoints |
| Query key management | Ad-hoc query keys | TanStack Query key factory pattern (see `companyKeys` in `use-companies.ts`) | Consistent invalidation across mutations |
| Error handling | Custom error parsing | Existing `ApiError` class + `GlobalExceptionHandler` | Reuse `ConflictException` for duplicate shares, `NotFoundException` for user-not-found |

**Key insight:** This phase is additive -- it extends existing patterns. No new framework concepts are needed.

## Common Pitfalls

### Pitfall 1: Forgetting to Update Existing getById to Check Visibility
**What goes wrong:** After adding visibility, the existing `CompanyService.getById` still only checks `findByIdAndUserId`. Shared users get 404.
**Why it happens:** Easy to add new endpoints but forget to widen existing ones.
**How to avoid:** Replace `findByIdAndUserId` with `findByIdWithVisibility` in getById. Keep `findByIdAndUserId` for write operations only.
**Warning signs:** Shared user clicks a link and gets "not found".

### Pitfall 2: SHARED Visibility Without Any Shares
**What goes wrong:** User sets visibility to SHARED but hasn't added any share recipients yet. The resource is effectively invisible to everyone except the owner.
**Why it happens:** SHARED is a visibility state, not a constraint that requires shares to exist.
**How to avoid:** This is acceptable behavior -- allow it. The UI should show "Shared with 0 users" as a hint. Do NOT prevent setting SHARED without shares.
**Warning signs:** None -- this is expected.

### Pitfall 3: Breaking Existing Owner Queries
**What goes wrong:** Adding visibility logic to the owner's list endpoint causes it to also return public/shared items from other users.
**Why it happens:** Accidentally widening the WHERE clause on the owner's list.
**How to avoid:** Owner's `/api/companies` and `/api/jobs` list endpoints MUST still filter by `userId = currentUser`. Only getById and browse endpoints respect visibility.
**Warning signs:** Owner sees other users' companies in their own list.

### Pitfall 4: Leaking Owner Info on Private Resources
**What goes wrong:** A visibility-aware getById returns a resource that was supposed to be PRIVATE, exposing it to non-owners.
**Why it happens:** Query logic error in the OR conditions.
**How to avoid:** Test explicitly: create a PRIVATE company as user A, attempt to fetch as user B, assert 404.
**Warning signs:** Integration test failure.

### Pitfall 5: Missing Query Invalidation on Share/Visibility Changes
**What goes wrong:** After sharing a resource or changing visibility, the browse page or shared-with-me page shows stale data.
**Why it happens:** TanStack Query cache not invalidated for the right keys.
**How to avoid:** Share mutations should invalidate `browseKeys` and `sharedWithMeKeys`. Visibility changes should invalidate the resource detail key AND browse keys.
**Warning signs:** User has to refresh to see changes.

### Pitfall 6: N+1 on Owner Email for Browse Page
**What goes wrong:** Browse page shows "Shared by user@example.com" -- loading owner email per resource creates N+1 queries.
**Why it happens:** Owner email not included in the entity, requires a join to users table.
**How to avoid:** Use a JPQL JOIN to fetch owner email in the browse query, or use a DTO projection.
**Warning signs:** Slow browse page with many public items.

## Code Examples

### Flyway Migration V16
```sql
-- V16__phase11_visibility_and_shares.sql

-- Add visibility column to companies (all existing rows default to PRIVATE)
ALTER TABLE companies ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

-- Add visibility column to jobs (all existing rows default to PRIVATE)
ALTER TABLE jobs ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

-- Shares join table (polymorphic: resource_type + resource_id)
CREATE TABLE resource_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(20) NOT NULL,
    resource_id UUID NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_with_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resource_share UNIQUE (resource_type, resource_id, shared_with_id)
);

-- Index for finding shares of a specific resource
CREATE INDEX idx_shares_resource ON resource_shares(resource_type, resource_id);

-- Index for finding all resources shared with a specific user
CREATE INDEX idx_shares_shared_with ON resource_shares(shared_with_id, resource_type);
```

### ResourceShareEntity
```kotlin
@Entity
@Table(name = "resource_shares")
class ResourceShareEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "resource_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val resourceType: ResourceType,

    @Column(name = "resource_id", nullable = false)
    val resourceId: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(name = "shared_with_id", nullable = false)
    val sharedWithId: UUID,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class ResourceType {
    COMPANY, JOB
}
```

### Visibility-Aware getById Service Pattern
```kotlin
fun getById(id: UUID, userId: UUID): CompanyResponse {
    // Try visibility-aware query first (owner, public, or shared)
    val entity = companyRepository.findByIdWithVisibility(id, userId)
        ?: throw NotFoundException("Company not found")
    val isOwner = entity.userId == userId
    return entity.toResponse(isOwner = isOwner)
}
```

### Share Creation with Validations
```kotlin
fun createShare(resourceType: ResourceType, resourceId: UUID, email: String, ownerId: UUID): ShareResponse {
    // 1. Verify ownership
    val resource = when (resourceType) {
        ResourceType.COMPANY -> companyRepository.findByIdAndUserId(resourceId, ownerId)
        ResourceType.JOB -> jobRepository.findByIdAndUserId(resourceId, ownerId)
    } ?: throw NotFoundException("Resource not found")

    // 2. Look up target user by email
    val targetUser = userRepository.findByEmail(email)
        ?: throw NotFoundException("User not found")

    // 3. Prevent self-share
    if (targetUser.id == ownerId) {
        throw ConflictException("Cannot share with yourself")
    }

    // 4. Prevent duplicate
    if (shareRepository.existsByResourceTypeAndResourceIdAndSharedWithId(resourceType, resourceId, targetUser.id!!)) {
        throw ConflictException("Already shared with this user")
    }

    // 5. Create share
    val share = ResourceShareEntity(
        resourceType = resourceType,
        resourceId = resourceId,
        ownerId = ownerId,
        sharedWithId = targetUser.id
    )
    return shareRepository.save(share).toResponse(targetUser.email)
}
```

### Frontend Visibility Badge Component
```tsx
import { Globe, Users } from "lucide-react"
import type { Visibility } from "@/types/api"

export function VisibilityBadge({ visibility }: { visibility: Visibility }) {
  if (visibility === "PUBLIC") {
    return <Globe className="size-3.5 text-muted-foreground" aria-label="Public" />
  }
  if (visibility === "SHARED") {
    return <Users className="size-3.5 text-muted-foreground" aria-label="Shared" />
  }
  return null // PRIVATE shows no icon
}
```

### API Endpoint Structure (Recommended)
```
# Visibility
PATCH /api/companies/{id}/visibility    { "visibility": "PUBLIC" }
PATCH /api/jobs/{id}/visibility         { "visibility": "SHARED" }

# Sharing (nested under resource)
GET    /api/companies/{id}/shares       -> List<ShareResponse>
POST   /api/companies/{id}/shares       { "email": "user@example.com" }
DELETE /api/companies/{id}/shares/{shareId}
GET    /api/jobs/{id}/shares            -> List<ShareResponse>
POST   /api/jobs/{id}/shares            { "email": "user@example.com" }
DELETE /api/jobs/{id}/shares/{shareId}

# Browse (public resources)
GET    /api/browse/companies            -> Page<BrowseCompanyResponse>
GET    /api/browse/jobs                 -> Page<BrowseJobResponse>

# Shared with me
GET    /api/shared/companies            -> Page<CompanyResponse>
GET    /api/shared/jobs                 -> Page<JobResponse>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate tables per shared resource type | Polymorphic shares table with resource_type discriminator | Standard pattern | Less schema duplication, single service |
| `@PreAuthorize` annotations | Service-layer userId checks | Project convention from Phase 2 | Consistent with existing codebase |

**Deprecated/outdated:**
- None relevant -- this phase uses stable, well-established patterns.

## Open Questions

1. **Should browse page support search/filter?**
   - What we know: Browse shows public items from all users, paginated.
   - What's unclear: Whether search by name/title is needed on first pass.
   - Recommendation: Include a simple text search (`q` param) on browse -- matches existing pattern on owner list endpoints. Low effort, high utility.

2. **Should changing visibility from SHARED to PRIVATE/PUBLIC delete existing shares?**
   - What we know: Shares persist when archived. CONTEXT.md doesn't specify behavior when visibility changes.
   - What's unclear: Whether changing to PRIVATE should auto-revoke shares.
   - Recommendation: Changing to PRIVATE or PUBLIC should NOT auto-delete shares. Owner can manually revoke. This avoids accidental data loss if user toggles back to SHARED.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + MockK |
| Config file | `backend/src/test/resources/application-test.yml` (if exists), otherwise default Spring Boot test config |
| Quick run command | `./gradlew :backend:test --tests "*.visibility.*" --tests "*.share.*" -x check` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| VISI-01 | Set visibility on company/job | integration | `./gradlew :backend:test --tests "*VisibilityServiceTests*" -x check` | No - Wave 0 |
| VISI-01 | PATCH visibility endpoint | controller | `./gradlew :backend:test --tests "*CompanyControllerIntegrationTests*" -x check` | Partial (exists, needs new tests) |
| VISI-02 | Share company/job by email | integration | `./gradlew :backend:test --tests "*ShareServiceTests*" -x check` | No - Wave 0 |
| VISI-02 | Self-share prevention | integration | `./gradlew :backend:test --tests "*ShareServiceTests*" -x check` | No - Wave 0 |
| VISI-02 | Duplicate share prevention | integration | `./gradlew :backend:test --tests "*ShareServiceTests*" -x check` | No - Wave 0 |
| VISI-03 | Browse public companies/jobs | integration | `./gradlew :backend:test --tests "*BrowseServiceTests*" -x check` | No - Wave 0 |
| VISI-04 | Shared-with-me list | integration | `./gradlew :backend:test --tests "*SharedWithMeTests*" -x check` | No - Wave 0 |
| VISI-05 | Shared user cannot update/delete | integration | `./gradlew :backend:test --tests "*VisibilityAuthorizationTests*" -x check` | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test --tests "*phase11*" -x check` (or targeted test class)
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/kotlin/.../visibility/VisibilityServiceTests.kt` -- covers VISI-01
- [ ] `backend/src/test/kotlin/.../visibility/ShareServiceTests.kt` -- covers VISI-02
- [ ] `backend/src/test/kotlin/.../visibility/BrowseServiceTests.kt` -- covers VISI-03
- [ ] `backend/src/test/kotlin/.../visibility/SharedWithMeTests.kt` -- covers VISI-04
- [ ] `backend/src/test/kotlin/.../visibility/VisibilityAuthorizationTests.kt` -- covers VISI-05
- [ ] Test helper: method to create a second test user for cross-user testing

## Sources

### Primary (HIGH confidence)
- Direct code inspection of existing entities: `CompanyEntity.kt`, `JobEntity.kt`, `UserEntity.kt`
- Direct code inspection of services: `CompanyService.kt`, `JobService.kt`
- Direct code inspection of repositories: `CompanyRepository.kt`, `JobRepository.kt`
- Direct code inspection of controllers: `CompanyController.kt`
- Direct code inspection of frontend hooks: `use-companies.ts`
- Direct code inspection of frontend components: `company-card.tsx`, `confirm-dialog.tsx`, `sidebar.tsx`
- Direct code inspection of security config: `SecurityConfig.kt`, `SecurityContextUtil.kt`
- Direct code inspection of exception handling: `GlobalExceptionHandler.kt`, `DomainExceptions.kt`
- Project skills: `entity-patterns.md`, `api-patterns.md`, `security-patterns.md`, `testing-patterns.md`
- Flyway migrations directory: V1-V15 confirmed, V16 is next

### Secondary (MEDIUM confidence)
- Spring Data JPA `@Query` annotation behavior -- well-established, HIGH confidence via project patterns already in use

### Tertiary (LOW confidence)
- None -- all patterns are verified against existing codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - No new libraries needed; extends existing patterns
- Architecture: HIGH - Follows established entity/service/controller/DTO conventions exactly
- Pitfalls: HIGH - Derived from direct code inspection of the codebase and common access control patterns
- Validation: HIGH - Test patterns established in project skills

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (stable patterns, no fast-moving dependencies)
