---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: "07-03 Task 2 checkpoint: human-verify responsive layout and auth flow"
last_updated: "2026-03-20T13:41:16.815Z"
last_activity: 2026-03-20 -- Completed 07-03 dashboard layout with responsive sidebar, topbar, and placeholder pages
progress:
  total_phases: 9
  completed_phases: 4
  total_plans: 13
  completed_plans: 12
  percent: 92
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-19)

**Core value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.
**Current focus:** Phase 7: Frontend Shell & Auth UI

## Current Position

Phase: 7 of 9 (Frontend Shell & Auth UI)
Plan: 3 of 3 in current phase (07-03 Task 1 complete, awaiting human-verify checkpoint)
Status: In Progress
Last activity: 2026-03-20 -- Completed 07-03 dashboard layout with responsive sidebar, topbar, and placeholder pages

Progress: [█████████░] 92%

## Performance Metrics

**Velocity:**
- Total plans completed: 12
- Average duration: 7 min
- Total execution time: 1.4 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-infrastructure | 2 | 13 min | 7 min |
| 02-authentication | 3 | 16 min | 5 min |
| 03-company-crud | 3 | 13 min | 4 min |
| 04-application-tracking | 2 | 40 min | 20 min |
| 07-frontend-shell-auth-ui | 3 | 13 min | 4 min |

**Recent Trend:**
- Last 5 plans: 04-01 (8 min), 04-02 (27 min), 07-01 (4 min), 07-02 (3 min), 07-03 (13 min)
- Trend: variable

*Updated after each plan completion*
| Phase 03 P01 | 3 | 2 tasks | 10 files |
| Phase 03 P02 | 6 | 2 tasks | 7 files |
| Phase 03 P03 | 4 | 2 tasks | 7 files |
| Phase 04 P01 | 8 | 2 tasks | 15 files |
| Phase 04 P02 | 27 | 2 tasks | 9 files |
| Phase 07 P01 | 4 | 2 tasks | 43 files |
| Phase 07 P02 | 3 | 1 task | 4 files |
| Phase 07 P03 | 13 | 1 task | 9 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Backend-first development -- build and test full API before frontend
- [Roadmap]: 8 phases at fine granularity; 6 backend phases then 2 frontend phases
- [Context]: Keep Spring Boot 4.0.4 + Spring AI 2.0.0-M3 (overriding initial research recommendation of 3.5.9; accept milestone risks for access to latest features)
- [Phase 01]: Added foojay-resolver-convention plugin to auto-download Java 24 toolchain
- [Phase 01]: Set spring.docker.compose.skip.in-tests=false for test DB connectivity
- [Phase 01]: CLAUDE.md content verified against actual build.gradle.kts and application.yml from Phase 01-01
- [Audit]: No API versioning -- flat `/api/` prefix for all endpoints
- [Audit]: Spring Actuator added -- /actuator/health (DB, Flyway, disk), /actuator/info, /actuator/flyway exposed
- [Phase 02]: Used jjwt-gson instead of jjwt-jackson to avoid Jackson 2/3 classpath conflict with Spring Boot 4
- [Phase 02]: JWT config as custom properties (jwt.secret, jwt.access-expiration-ms) not under spring namespace
- [Phase 02]: Redis auto-configured via Docker Compose integration, no explicit host/port properties
- [Phase 02]: Used PasswordEncoderFactories.createDelegatingPasswordEncoder() for future-proof password hashing
- [Phase 02]: Refresh cookie scoped to /api/auth/refresh path for security
- [Phase 02]: Custom AuthenticationException to avoid name collision with Spring Security's AuthenticationException
- [Phase 02]: @Transactional on services accessing lazy-loaded JPA relationships
- [Phase 02]: AuthenticationEntryPoint returning 401 (not default 403) for unauthenticated REST API requests
- [Phase 02]: Spring Boot 4 AutoConfigureMockMvc moved to spring-boot-webmvc-test module
- [Phase 03]: Replaced AuthExceptionHandler with GlobalExceptionHandler for unified error handling
- [Phase 03]: CAST(:name AS string) in JPQL to fix PostgreSQL lower(bytea) error with null parameters
- [Phase 03]: Plain UUID for companyId (no @ManyToOne) to keep entity simple and avoid lazy-loading issues
- [Phase 03]: Batch company name resolution in list queries via findAllByIdInAndUserId for N+1 prevention
- [Phase 04]: JPQL with LEFT JOINs and EXISTS subquery for 4-table cross-entity search
- [Phase 04]: CAST(:param AS type) IS NULL pattern for null-safe optional JPQL parameters
- [Phase 04]: FK-safe test cleanup order: delete child tables before parent tables in @BeforeEach
- [Phase 04]: Status state machine uses Map-based transition validation with InvalidTransitionException (422)
- [Phase 04]: Terminal statuses (REJECTED, ACCEPTED, WITHDRAWN) can reopen to any active status
- [Phase 07]: Better Auth with own PostgreSQL tables (user, session, account, verification) via Flyway V9
- [Phase 07]: Sidebar exports navItems array reused by MobileNav for single source of truth
- [Phase 07]: Route group (dashboard) for shared layout without URL prefix
- [Phase 07]: Controlled Sheet state so mobile nav closes on link click

### Roadmap Evolution

- Phase 6.1 inserted after Phase 6: Visibility & Sharing (INSERTED)

### Pending Todos

- RESOLVED: No API versioning -- flat `/api/` prefix
- RESOLVED: MockK and SpringMockK added in 02-01
- RESOLVED: Interview Management added to FEATURES.md and ARCHITECTURE.md
- RESOLVED: Auth.js vs custom JWT -- chose Better Auth with own DB tables

### Blockers/Concerns

- [01-01]: RESOLVED -- Restructured project into /backend monorepo layout
- [Research]: RESOLVED -- Chose Better Auth over Auth.js for frontend auth

## Session Continuity

Last session: 2026-03-20T13:41:16.811Z
Stopped at: Completed 07-02-PLAN.md
Resume file: None
