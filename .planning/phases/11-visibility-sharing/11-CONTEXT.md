# Phase 11: Visibility & Sharing - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can control visibility of their companies and jobs as PRIVATE, PUBLIC, or SHARED with specific users. Sharing is view-only, identified by email. A separate browse page lets logged-in users discover public resources. Applications, interviews, and documents remain strictly private.

</domain>

<decisions>
## Implementation Decisions

### Sharing model
- View-only always — shared users can view but never edit or delete (matches VISI-05)
- Share target identified by email — if user exists, share is created immediately; if not, error returned ("user not found")
- No invite flow — recipient must already have an account
- Owner sees list of shared users on the resource detail page with revoke button per share
- Self-share prevented — API rejects sharing with yourself
- Duplicate shares prevented — API rejects re-sharing with same user (not idempotent)

### Public browse
- Separate `/browse` page for discovering public resources — not mixed into existing lists
- Login required to browse public items — no anonymous API endpoints needed
- Full details shown on public items — same fields as owner sees
- Owner email displayed on public items ("Shared by user@example.com")

### Visibility UX
- Visibility control lives on the detail/edit page only — not on cards
- Small icon badge on cards: globe icon for PUBLIC, users icon for SHARED, no icon for PRIVATE (non-interactive, informational only)
- Separate "Shared with me" section/page for items others have shared with you — not mixed into your own lists
- Confirmation dialog required when setting visibility to PUBLIC ("This will make [resource] visible to all users. Continue?")

### Defaults & migration
- Default visibility is PRIVATE — no behavior change for existing data
- Flyway migration V16 adds visibility column with DEFAULT 'PRIVATE' to companies and jobs tables
- All existing rows receive PRIVATE visibility automatically via column default

### Cascade behavior
- No cascade — company and job visibility are fully independent
- A job CAN be public/shared even if its parent company is private (job shows company name but company detail is not accessible)
- Visibility only applies to companies and jobs — applications, interviews, documents remain strictly private to the owner
- Shares persist when a resource is archived — not revoked; shared users see it as archived

### Claude's Discretion
- API endpoint structure for visibility and sharing operations
- Database schema for shares table (join table design)
- How the browse page is laid out (grid, list, search/filter)
- Visibility enum implementation (Kotlin enum, JPA mapping)
- Share management UI component design on detail page
- "Shared with me" page/tab navigation placement

### Verification (v1.0 retrospective lesson #3)
- Phase verification MUST confirm end-to-end UI wiring, not just backend API + hook existence
- Required E2E checkpoints: (1) set visibility on detail page → badge appears on card, (2) set PUBLIC → item appears on /browse page, (3) share with user by email → item appears on recipient's /shared page, (4) shared user can view but NOT edit/delete
- Use @Query annotations for visibility-aware repository queries (avoid ambiguous derived method names)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — VISI-01 through VISI-05 define the acceptance criteria for this phase

### Existing entity patterns
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/CompanyEntity.kt` — Company entity structure, userId ownership pattern
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/JobEntity.kt` — Job entity structure, companyId relationship
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/UserEntity.kt` — User entity with email field (used for share lookup)

### Existing authorization patterns
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/CompanyService.kt` — Owner verification pattern via userId parameter
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/JobService.kt` — Same ownership pattern, validateCompany cross-check
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt` — Current user ID extraction

### Frontend patterns
- `frontend/hooks/use-companies.ts` — TanStack Query hook pattern for companies
- `frontend/hooks/use-jobs.ts` — TanStack Query hook pattern for jobs
- `frontend/components/companies/company-card.tsx` — Card component pattern (where badge would go)
- `frontend/app/(dashboard)/companies/page.tsx` — Companies list page (separate from browse page)
- `frontend/types/api.ts` — TypeScript API type definitions
- `frontend/lib/api-client.ts` — API client with cookie auth

### Database migrations
- `backend/src/main/resources/db/migration/` — Latest is V15; next migration is V16

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CompanyEntity`/`JobEntity`: Already have `userId` field for ownership — visibility enum adds alongside it
- `SecurityContextUtil.getCurrentUserId()`: Static utility for current user — used in all controllers
- `DomainExceptions.kt`: `NotFoundException`, `ConflictException` — reuse for share validation errors
- `CompanyCard`/`JobCard` components: shadcn/ui Card with DropdownMenu — badge can be added to card header
- `ConfirmDialog` component: Already exists for delete confirmation — reuse for PUBLIC visibility confirmation
- `apiClient<T>()`: Generic fetch wrapper with cookie auth — extend for share/visibility endpoints

### Established Patterns
- Service-layer authorization: All access checks happen in service methods via `userId` parameter, not `@PreAuthorize`
- Repository naming: `findByIdAndUserId()` pattern — will need `findByIdAndUserIdOrVisibility()` variants
- Controller structure: RESTful with `SecurityContextUtil.getCurrentUserId()` at start of each method
- DTO pattern: Separate Request/Response data classes in `dto/` package
- Flyway migrations: `V{N}__{phaseNN}_{description}.sql` naming with `gen_random_uuid()` PKs
- Frontend hooks: `useQuery`/`useMutation` with `queryKey` invalidation on success

### Integration Points
- Company/Job services need visibility-aware query methods (owner OR public OR shared-with)
- Company/Job response DTOs need `visibility` field added
- Frontend types (`api.ts`) need `visibility` field and new share-related types
- Sidebar navigation needs "Browse" and "Shared with me" links
- New route: `/browse` page, `/shared` page

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 11-visibility-sharing*
*Context gathered: 2026-03-22*
