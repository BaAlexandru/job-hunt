# Phase 2: Authentication - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can create accounts, log in with persistent sessions, log out, and reset forgotten passwords via the REST API. Includes email verification, token invalidation, CORS configuration, and securing existing actuator endpoints. No frontend UI in this phase — API tested via Swagger/Postman.

</domain>

<decisions>
## Implementation Decisions

### Token strategy
- Access + Refresh token pair
- Access token: 15 minute lifetime, sent in Authorization header
- Refresh token: 7 day lifetime, stored in HTTP-only cookie (XSS-safe)
- Refresh endpoint rotates both tokens
- Token invalidation via database blocklist table (store invalidated token IDs, check on each request)
- Logout adds both tokens to blocklist

### Password reset flow
- Log reset link to application console in local dev (no external email dependency)
- Reset token valid for 1 hour, single-use (invalidated after use)
- Rate limiting via Redis: max 3 reset requests per email per hour
- Redis added to Docker Compose stack for rate limiting

### Registration rules
- Password complexity: minimum 8 characters, at least one uppercase, one lowercase, one number
- Generic error on duplicate email ("Registration failed") — prevents email enumeration
- Email verification required: send verification token, account locked until confirmed
- Verification email logged to console (same as password reset in local dev)

### Endpoint security
- Public endpoints (no auth required): /api/auth/** (register, login, refresh, verify, reset), all /actuator/** endpoints
- All other endpoints require valid JWT in Authorization header
- CORS: allow http://localhost:3000 only (Next.js dev server)
- CORS filter ordering: OPTIONS requests pass without auth (JWT filter chain ordering)

### Role-based access (RBAC)
- Users table includes role column: USER and ADMIN roles
- Spring Security annotations for role-based endpoint protection
- Default role on registration: USER
- Role checks via @PreAuthorize or method security

### Claude's Discretion
- BCrypt rounds/configuration for password hashing
- Exact JWT claims structure and signing algorithm
- Database blocklist cleanup strategy (scheduled job vs lazy cleanup)
- Redis configuration details and Spring Data Redis setup
- Email verification token format and URL structure
- Spring Security filter chain configuration details
- Exception handling and error response format for auth endpoints

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` — Tech stack constraints (Kotlin, Spring Boot 4.x, Spring Security + JWT, PostgreSQL)
- `.planning/REQUIREMENTS.md` — AUTH-01 through AUTH-04
- `.planning/ROADMAP.md` — Phase 2 success criteria (5 criteria including CORS)

### Prior phase context
- `.planning/phases/01-foundation-infrastructure/01-CONTEXT.md` — Monorepo structure, Flyway conventions, UUID PKs, dependency management approach

### Backend conventions
- `backend/CLAUDE.md` — Package structure, coding conventions, Flyway naming, testing approach, actuator security notes

### Existing code
- `backend/build.gradle.kts` — Current dependencies (no Spring Security yet — must be added)
- `backend/src/main/resources/application.yml` — Current config (actuator exposed, Docker Compose integration)
- `compose.yaml` — Docker Compose stack (PostgreSQL only — Redis must be added)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `JobHuntApplication.kt`: Spring Boot main class — add @EnableMethodSecurity or security config here
- `build.gradle.kts`: Kotlin plugin.spring already configured — Spring Security proxying will work
- `application.yml`: Actuator already configured with show-details: always

### Established Patterns
- Flyway migrations: V{N}__{phaseNN}_{description}.sql — next migration is V2__phase02_create_users.sql
- JPA with hibernate.ddl-auto=validate — Flyway manages all schema, entities must match
- Docker Compose auto-discovery from compose.yaml at project root
- Constructor injection via Kotlin primary constructors

### Integration Points
- `compose.yaml`: Add Redis service alongside existing PostgreSQL
- `build.gradle.kts`: Add spring-boot-starter-security, spring-boot-starter-data-redis, jjwt or spring-security-oauth2-jose
- Flyway: New migrations for users table, roles, token blocklist, verification tokens, password reset tokens
- Actuator: Currently open — Spring Security will auto-secure; must explicitly permit /actuator/**

</code_context>

<specifics>
## Specific Ideas

- User is learning Kotlin — keep idiomatic patterns (data classes for DTOs, sealed classes for auth results, extension functions where natural)
- Backend-first: all auth flows tested via Swagger/Postman before any frontend exists
- Multi-user ready from day one: user ownership on all future tables references the users table created here
- STATE.md notes: "Add MockK, SpringMockK, and Testcontainers to backend dependencies when Phase 2 planning starts"

</specifics>

<deferred>
## Deferred Ideas

- Auth.js vs custom JWT handling for frontend — Phase 7 decision (noted in STATE.md blockers)
- OAuth2 social login (Google, GitHub) — future enhancement, not v1
- Account deletion/deactivation — not in current requirements
- Two-factor authentication (2FA) — potential v2 feature

</deferred>

---

*Phase: 02-authentication*
*Context gathered: 2026-03-20*
