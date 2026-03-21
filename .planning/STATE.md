---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in-progress
stopped_at: Completed 08-11-PLAN.md (gap closure - status dropdown fix + dashboard deep linking)
last_updated: "2026-03-21T19:35:56.851Z"
last_activity: 2026-03-21 -- Completed 08-09 gap closure (dialog UX + dashboard timestamps)
progress:
  total_phases: 8
  completed_phases: 8
  total_plans: 28
  completed_plans: 28
  percent: 96
---

---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in-progress
stopped_at: Completed 08-08-PLAN.md (gap closure - kanban click-through + mobile optimization)
last_updated: "2026-03-21T15:56:31Z"
last_activity: 2026-03-21 -- Completed 08-08 gap closure (kanban click-through + mobile optimization)
progress:
  [██████████] 96%
  completed_phases: 8
  total_plans: 25
  completed_plans: 25
  percent: 100
---

---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in-progress
stopped_at: Completed 08-06-PLAN.md (theme toggle, dialog UX, dark contrast)
last_updated: "2026-03-21T15:51:38.533Z"
last_activity: 2026-03-21 -- Completed 08-06 gap closure (theme toggle + dialog UX + dark contrast)
progress:
  total_phases: 8
  completed_phases: 7
  total_plans: 25
  completed_plans: 24
  percent: 96
---

---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in-progress
stopped_at: Completed 08-06-PLAN.md (theme toggle, dialog UX, dark contrast)
last_updated: "2026-03-21T15:49:07Z"
last_activity: 2026-03-21 -- Completed 08-06 gap closure (theme toggle + dialog UX + dark contrast)
progress:
  [██████████] 96%
  completed_phases: 8
  total_plans: 25
  completed_plans: 23
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-19)

**Core value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.
**Current focus:** Phase 8: Frontend Core Views

## Current Position

Phase: 8 of 8 (Frontend Core Views)
Plan: 9 of 11 in current phase
Status: in-progress
Last activity: 2026-03-21 -- Completed 08-09 gap closure (dialog UX + dashboard timestamps)

Progress: [█████████░] 26/28 plans complete

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
| Phase 05 P01 | 3 | 2 tasks | 10 files |
| Phase 05 P02 | 8 | 2 tasks | 10 files |
| Phase 06 P01 | 4 | 2 tasks | 20 files |
| Phase 06 P02 | 13 | 2 tasks | 6 files |
| Phase 08 P01 | 10 | 3 tasks | 26 files |
| Phase 08 P03 | 8 | 2 tasks | 7 files |
| Phase 08 P02 | 11 | 2 tasks | 7 files |
| Phase 08 P04 | 3 | 2 tasks | 7 files |
| Phase 08 P05 | 3 | 2 tasks | 4 files |
| Phase 08 P06 | 2 | 3 tasks | 5 files |
| Phase 08 P07 | 5 | 3 tasks | 10 files |
| Phase 08 P08 | 3 | 2 tasks | 3 files |
| Phase 08 P09 | 3 | 2 tasks | 2 files |
| Phase 08 P10 | 4 | 2 tasks | 4 files |
| Phase 08 P11 | 5 | 2 tasks | 3 files |

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
- [Phase 05]: Followed existing entity patterns exactly for interview data layer - no deviations needed
- [Phase 05]: Archived interviews excluded from timeline including child interview notes
- [Phase 05]: Timeline sorted descending with optional type filtering via query parameter
- [Phase 05]: Interview notes blocked on archived interviews (returns 404)
- [Phase 06]: forcePathStyle(true) on S3Client for MinIO compatibility
- [Phase 06]: StorageService interface abstracts S3 for testability and future provider swap
- [Phase 06]: Tika detect(bytes, filename) for accurate OOXML MIME detection
- [Phase 06]: CommandLineRunner instead of PostConstruct for S3 bucket creation to avoid circular bean dependency
- [Phase 08]: BetterAuthSessionFilter queries Better Auth tables via JdbcTemplate (not JPA entities)
- [Phase 08]: Session filter before JWT filter -- cookie auth tried first, JWT as fallback
- [Phase 08]: Companies hook returns PaginatedResponse matching actual backend Page API
- [Phase 08]: Used standardSchemaResolver instead of zodResolver for Zod v4 compatibility with react-hook-form
- [Phase 08]: Salary fields as strings in forms, converted to numbers on submit (z.coerce.number() incompatible with Standard Schema)
- [Phase 08]: Dice UI Kanban composable pattern for kanban board with drag validation and column dimming
- [Phase 08]: standardSchemaResolver instead of zodResolver for Zod v4 compatibility with @hookform/resolvers
- [Phase 08]: Client-side metric aggregation from useApplications({size:1000}) for dashboard -- v1 approach
- [Phase 08]: react-dropzone for drag-and-drop file upload with PDF/DOCX MIME validation
- [Phase 08]: Two-step session query (validate session, then lookup/create backend user) instead of single JOIN
- [Phase 08]: Empty password + enabled=true for auto-provisioned Better Auth users (session auth, not password)
- [Phase 08]: Sheet detail panel 92% width on mobile, max-w-lg on desktop for usable form display
- [Phase 08]: MouseSensor distance:5 + TouchSensor delay:200/tolerance:5 for click vs drag discrimination on kanban
- [Phase 08]: 240px kanban columns on mobile (vs 280px desktop) for more visible content
- [Phase 08]: Fixed positioning for sidebar instead of flex-based layout to prevent horizontal scroll displacement
- [Phase 08]: min-h-0 on scroll wrapper to override flex min-height:auto default for Sheet panel mobile scroll
- [Phase 08]: Radix modal={false} removes viewport overlay blocking scroll in inline table dropdowns

### Roadmap Evolution

- Phase 6.1 inserted after Phase 6: Visibility & Sharing (INSERTED) — deferred to v2 on 2026-03-21 (not needed for current milestone)

### Pending Todos

- RESOLVED: No API versioning -- flat `/api/` prefix
- RESOLVED: MockK and SpringMockK added in 02-01
- RESOLVED: Interview Management added to FEATURES.md and ARCHITECTURE.md
- RESOLVED: Auth.js vs custom JWT -- chose Better Auth with own DB tables

### Blockers/Concerns

- [01-01]: RESOLVED -- Restructured project into /backend monorepo layout
- [Research]: RESOLVED -- Chose Better Auth over Auth.js for frontend auth

## Session Continuity

Last session: 2026-03-21T19:35:56.847Z
Stopped at: Completed 08-11-PLAN.md (gap closure - status dropdown fix + dashboard deep linking)
Resume file: None
