# Project Research Summary

**Project:** JobHunt - Job Application Tracker
**Domain:** Full-Stack Web App (Kotlin/Spring Boot + Next.js)
**Researched:** 2026-03-19
**Confidence:** HIGH

## Executive Summary

JobHunt is a personal job application tracker with AI features — a category that existing SaaS products (Huntr, Teal, Seekario) have validated heavily but uniformly gatekeep behind paid tiers. The research confirms that self-hosting is itself the primary differentiator: unlimited applications, unlimited AI usage at API cost, and full data ownership, versus competitors charging $40+/month for features that are now cheap to build. The recommended approach is a monorepo with a Kotlin/Spring Boot backend and Next.js frontend, structured package-by-feature, with a clear six-phase build order that keeps the backend ahead of the frontend and defers AI integration until the document and job management foundations are solid.

> **Update (2026-03-19):** The user decided to keep Spring Boot 4.0.4 + Spring AI 2.0.0-M3 (accepting milestone risks) rather than the 3.5.9 recommendation below. The ROADMAP.md (8 phases) supersedes the 6-phase ordering in "Implications for Roadmap" below. See `.planning/phases/01-foundation-infrastructure/01-CONTEXT.md` for the rationale.

The stack choices are well-aligned and production-grade for 2026. The primary architectural risk is over-engineering early: trying to build the kanban board before auth works, or adding AI before documents are stored. The feature dependency chain is strict — auth enables company tracking, which enables job tracking, which enables applications, which enables AI analysis. Every shortcut in that chain creates retrofit work. The research is explicit: add `user_id` to every entity on day one even though it is currently single-user, enforce Kotlin compiler plugins from the first commit, and configure CORS/JWT filter ordering correctly before touching the frontend. These are cheap to do upfront and expensive to fix later.

The most important build constraint comes from PITFALLS.md: the project currently has Spring Boot at the root rather than in a `backend/` subdirectory. The architecture research provides a concrete migration path, and this restructuring should happen before any feature work begins. Three pitfalls require Phase 1 attention regardless of the feature being built: Kotlin compiler plugins, multi-tenancy data model scoping, and CORS/JWT filter chain ordering. Skipping any of these creates either a security vulnerability or a data-layer rewrite.

---

## Key Findings

### Recommended Stack

The user's chosen stack is validated with specific version guidance. The critical decision is **Spring Boot 3.5.9 over 4.0.x** — Spring AI 1.1.1 (GA) requires Boot 3.x, Boot 4.0 is still maturing, and there is no benefit to taking on migration risk for a greenfield project. Spring AI 2.0 (requiring Boot 4.0) is still a milestone release; migrate to it after the GA ships (expected April 2026+).

**Core technologies:**
- **Kotlin 2.3.20**: Primary language — null safety, coroutines, Spring Boot first-class support
- **Spring Boot 3.5.9**: Application framework — latest 3.5.x patch, production-proven, Boot 4.0 avoided due to Spring AI incompatibility
- **Spring AI 1.1.1 GA**: AI provider abstraction — `ChatModel` interface works with Anthropic Claude and OpenAI, provider-agnostic
- **PostgreSQL + Flyway 10.x**: Database — JSONB, full-text search, production-grade; Flyway for schema migration with strict immutability discipline
- **Next.js 16.2.x (App Router)**: Frontend framework — Turbopack now default and stable, App Router is the standard
- **TanStack Query 5.91.x**: Server state management — handles caching, optimistic updates, removes Redux boilerplate for API-driven apps
- **Tailwind CSS 4.1.x + shadcn/ui CLI v4**: Styling — Rust-based Oxide engine, CSS-first config, shadcn copies components into the project
- **MockK + Testcontainers**: Testing — MockK replaces Mockito (Kotlin-native), Testcontainers replaces H2 (eliminates dialect mismatch bugs)

**Key avoidances:** Spring Boot 4.0, Spring AI 2.0.0-M2, JJWT library (Nimbus is already bundled), Mockito with Kotlin, H2 for integration tests, `react-beautiful-dnd` (deprecated), moment.js, `pages/` directory.

### Expected Features

All major competitors (Huntr, Teal, Eztrackr) use Kanban as the primary UI paradigm. Users expect drag-and-drop status changes as a first-class interaction, not buried in a detail page. The feature dependency chain is strict: auth -> company -> job -> application -> AI.

**Must have (table stakes):**
- Application CRUD with 8-status flow (Interested, Applied, Phone Screen, Interview, Offer, Accepted, Rejected, Withdrawn)
- Kanban board with drag-and-drop status changes — this is how users mentally model their pipeline
- List/table view with sort and filter — dual views are standard across all competitors
- Company management — multiple roles per company; company-level metadata required
- Job posting management (title, description, URL, salary range, location)
- Application date tracking (applied date, last activity, next action date)
- Notes per application — essential for interview prep and recruiter context
- Document upload (PDF/DOCX) with per-application linking — knowing which CV you sent where is critical
- Basic search and filtering — necessary at 50+ applications
- User authentication (JWT) — protects personal job search data

**Should have (competitive differentiators):**
- Application analytics dashboard — most free trackers lack this; meaningful after accumulating data
- Tags and custom labels — essential once tracking 20+ applications
- Salary tracking fields — useful during offer comparison
- Timeline/activity log per application — richer than status changes alone
- Reminders for stale applications — no competitor has this; strong differentiator
- Contact/networking tracker — link recruiters and contacts to applications

**Defer (v2+):**
- AI CV optimization and cover letter generation — requires stable document + job management foundation; HIGH complexity
- Job posting URL scraping — fragile, site-dependent; v2 per PROJECT.md
- Browser extension — separate codebase, high effort, high convenience

### Architecture Approach

The system follows a monorepo structure with three directories: `backend/` (Spring Boot/Kotlin), `frontend/` (Next.js), and `infra/` (Docker Compose, K8s manifests). The backend uses package-by-feature organization — each domain module (`auth/`, `company/`, `job/`, `application/`, `document/`, `ai/`) contains its own controller, service, entity, repository, and DTOs. The REST API is the contract between frontend and backend; they are independently deployable. The current project structure (Spring Boot at root) must be migrated to this monorepo layout before feature work begins.

**Major components:**
1. **Next.js Frontend** — App Router pages, shadcn/ui components, TanStack Query for all server state, JWT token handling
2. **Spring Boot Backend** — REST API with Spring Security JWT filter chain, layered service/repository architecture per domain module
3. **PostgreSQL** — All structured data; Flyway manages all schema changes (never manual edits)
4. **StorageService abstraction** — `LocalStorageService` for dev (filesystem), `S3StorageService` for prod; metadata in PostgreSQL, binaries on storage
5. **Spring AI ChatClient** — Provider-agnostic; configured via `AiConfig`, isolated from business logic; Anthropic Claude by default

**Key patterns:**
- Package-by-feature with strict Controller -> Service -> Repository dependency direction
- Application status as an explicit state machine with validated transitions (not free-form string updates)
- TanStack Query for all server state; no Redux; only minimal client state in React context
- Flat `/api/` prefix for all endpoints (no versioning — monorepo means frontend/backend evolve together)

### Critical Pitfalls

1. **Kotlin compiler plugins missing** — Without `kotlin("plugin.spring")` and `kotlin("plugin.jpa")` in `build.gradle.kts`, `@Transactional` silently does nothing (classes are final by default in Kotlin), lazy loading breaks, and Spring cannot proxy service classes. Add these on day one. Recovery is LOW cost before any entities exist; HIGH cost after.

2. **No user scoping in data model from day one** — Every business entity needs a `user_id` foreign key from the first migration. Without it, every repository method, service call, and file path must be retrofitted when multi-user support is added — effectively a data-layer rewrite. `findByIdAndUserId(id, userId)` not `findById(id)` everywhere.

3. **CORS + JWT filter chain ordering** — Spring Security evaluates authentication before CORS by default. Browser preflight `OPTIONS` requests carry no Bearer token, so they receive 401. CORS errors in browsers hide the real response. Configure `CorsConfigurationSource` as a bean and register via `http.cors { }` so CORS is evaluated before authentication. Explicitly permit `OPTIONS "/**"`.

4. **Flyway migration immutability** — Never edit an already-applied migration file. Flyway detects checksum mismatches and refuses to start. Always create a new migration to fix issues. Document this rule in `CLAUDE.md` for the backend module.

5. **File upload security — path traversal and unrestricted types** — Never use `MultipartFile.getOriginalFilename()` for storage. Generate UUID-based filenames; store original name in the database. Whitelist only `.pdf`, `.docx`, `.doc`. Set 10MB max file size in `application.properties`. Store files outside the web root or use object storage.

---

## Implications for Roadmap

> **Note:** The phase ordering below was the initial research recommendation. The actual ROADMAP.md has 8 phases with finer granularity (auth separated from foundation, application tracking separated from company/job, interview management added). Refer to ROADMAP.md for the authoritative phase plan.

The architecture research provides an explicit build order based on technical dependencies. The backend can be fully built and tested before writing frontend code. AI features depend on ALL core domain entities existing first.

### Phase 1: Foundation and Authentication

**Rationale:** Nothing else can be built without auth. The Kotlin compiler plugins, data model user scoping, CORS/JWT configuration, and Flyway discipline must all be established here — they cannot be retrofitted. This phase also includes the monorepo restructuring (Spring Boot at root -> `backend/` directory).
**Delivers:** Runnable monorepo, working registration/login, JWT auth, PostgreSQL via Docker Compose, Flyway migrations infrastructure, OpenAPI docs
**Addresses:** User authentication (table stakes)
**Avoids:** Kotlin final class pitfall, single-tenancy hardcoding, CORS/JWT filter ordering, Flyway checksum mismatches, JWT security basics (httpOnly cookies, env var secret, 30-minute expiry)

### Phase 2: Core Domain (Backend)

**Rationale:** Company -> Job -> Application is a strict dependency chain. Application tracking is the entire reason the product exists. Build the backend domain completely (including the state machine) before starting the frontend, so the API can be tested via Swagger/Postman.
**Delivers:** Full REST API for companies, jobs, and applications; status state machine with validated transitions; search and filtering; Flyway migrations V2-V4
**Addresses:** Company management, job posting management, application tracking, notes, date tracking, search/filter (all P1 table stakes)
**Avoids:** N+1 queries (use `@EntityGraph`/`JOIN FETCH` from the start), status as String instead of enum

### Phase 3: Document Management (Backend)

**Rationale:** Documents must be implemented with security from the start. The `StorageService` abstraction (local dev -> S3 prod) must be established here so switching storage backends later is a config change, not a rewrite. AI features in a later phase depend on this being solid.
**Delivers:** File upload/download API, `StorageService` interface with `LocalStorageService` implementation, document-to-application linking, Flyway V5
**Addresses:** Document upload and linking (P1 table stakes)
**Avoids:** File upload path traversal, missing type validation, binary data stored in PostgreSQL, files without user-scoped paths

### Phase 4: Frontend Shell and Auth

**Rationale:** Start the frontend only after the backend API is stable and tested. Auth pages and the API client layer must come before any feature pages. JWT handling (httpOnly cookies, 401 interceptor, token refresh) must be established here.
**Delivers:** Next.js App Router setup, login/register pages, AuthProvider, API client layer with JWT interceptor, TanStack Query setup, shadcn/ui component base
**Addresses:** Responsive design, auth UX
**Avoids:** TanStack Query hydration mismatches (configure `QueryClient` in React state, not module scope; start with client-only fetching for this authenticated app), hardcoded `localhost:8080` (use `NEXT_PUBLIC_API_URL`)

### Phase 5: Frontend Feature Pages

**Rationale:** With the API complete and the frontend shell working, build feature pages by connecting UI to existing API endpoints. Kanban board (drag-and-drop) is the highest-value view and the most complex; build list view first as the simpler baseline.
**Delivers:** Company/job CRUD pages, application list/table view, kanban board with drag-and-drop, document upload/management UI, search/filter UI
**Addresses:** Kanban board, list view, full frontend experience for all P1 features
**Avoids:** Loading all applications at once without pagination (kanban can freeze at 100+ items), status transitions requiring detail page navigation (support inline and drag-and-drop)

### Phase 6: AI Features

**Rationale:** AI features depend on ALL of: stored documents, job descriptions, and company context. This is the correct deferral point per PROJECT.md and validated by the dependency chain in FEATURES.md. Design the `AiService` abstraction layer before writing the first AI feature to avoid provider lock-in.
**Delivers:** CV analysis against job description, cover letter generation, async AI processing (returns job ID, polls or SSE for result), centralized `AiConfig`
**Addresses:** AI CV optimization, AI cover letter generation (P3 differentiators)
**Avoids:** Synchronous AI calls blocking request threads (use `@Async` or coroutines), provider lock-in through hardcoded model names in service classes, missing rate limiting and cost controls on AI endpoints

### Phase Ordering Rationale

- **Backend before frontend:** The REST API is the contract. Building the backend first lets it be validated via Swagger/Postman before any UI depends on it.
- **Documents before AI:** AI requires stored CVs and job descriptions. No shortcuts here.
- **Auth on day one:** Every entity needs `user_id` from the first migration. This is foundational, not a feature.
- **v1.x features (analytics, tags, contacts, reminders) slot between Phase 5 and Phase 6** — they require accumulated application data and benefit from the full frontend foundation being stable first.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 6 (AI Features):** Async AI processing patterns in Spring Boot (WebFlux vs `@Async` vs coroutines), prompt engineering for structured output, token counting and cost controls, SSE vs polling for long-running results
- **Phase 3 (Document Management):** MIME type validation by magic bytes (not Content-Type header), PDF/DOCX text extraction for AI context assembly

Phases with standard patterns (skip research-phase):
- **Phase 1 (Foundation):** Kotlin + Spring Boot setup is extremely well-documented; follow the STACK.md build.gradle.kts exactly
- **Phase 2 (Core Domain):** Standard Spring Boot CRUD with JPA; package-by-feature is established convention
- **Phase 4 (Frontend Shell):** Next.js App Router setup is well-documented; follow TanStack Query SSR guidance from PITFALLS.md
- **Phase 5 (Frontend Features):** dnd-kit has strong documentation; shadcn/ui component copying is straightforward

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All versions verified against official release notes and changelogs. Spring AI / Spring Boot version compatibility explicitly confirmed. |
| Features | HIGH | Multiple competitor products analyzed (Huntr, Teal, Eztrackr, Seekario, Built In). Feature prioritization backed by direct product comparison. |
| Architecture | HIGH | Package-by-feature with Spring Boot is a well-documented, widely adopted pattern. Data flow diagrams based on actual Spring Security and JPA semantics. |
| Pitfalls | HIGH | Each pitfall sourced from official documentation (Kotlin plugin docs, Spring Security JWT docs, TanStack Query SSR docs) plus community-validated bug reports. |

**Overall confidence:** HIGH

### Gaps to Address

- **Auth.js (next-auth v5) vs custom JWT handling:** The research leaves this as "evaluate in Phase 1." For a single-user app with a Spring Boot backend issuing its own JWTs, a custom `fetch` wrapper with httpOnly cookies is likely simpler. Decide in Phase 1 planning before building auth pages.
- **Kanban board pagination strategy:** The research flags that loading all applications at once breaks at 100+ items, but does not prescribe a specific pagination approach for kanban columns. This needs a decision during Phase 5 planning (load per-column, virtual scrolling, or total application cap for v1).
- **File storage in production:** The research recommends MinIO locally and S3 in production. The specific deployment target (VPS, cloud provider, self-hosted K8s) is not defined in PROJECT.md. Decide before Phase 3 ends so the `StorageService` prod implementation is the right one.
- **Spring Boot 4.0 migration timing:** Spring AI 2.0 GA is expected ~April 2026. After the project is functional on 3.5.x, assess migrating to Boot 4.0 for access to Spring AI 2.0's improved agentic patterns.

---

## Sources

### Primary (HIGH confidence)
- [Spring Boot Releases](https://github.com/spring-projects/spring-boot/releases) — version verification
- [Spring AI 1.1 GA Released](https://spring.io/blog/2025/11/12/spring-ai-1-1-GA-released/) — Spring AI version and Boot compatibility
- [Spring AI 2.0.0-M2](https://spring.io/blog/2026/01/23/spring-ai-2-0-0-M2-available-now) — Boot 4.0 requirement confirmed
- [Next.js 16 Blog Post](https://nextjs.org/blog/next-16) — Turbopack stable as default bundler
- [Kotlin 2.3.20 Released](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) — latest Kotlin version
- [Tailwind CSS v4.0 Announcement](https://tailwindcss.com/blog/tailwindcss-v4) — v4 architecture changes, CSS-first config
- [Kotlin All-Open Plugin Documentation](https://kotlinlang.org/docs/all-open-plugin.html) — Kotlin final class pitfall
- [TanStack Query SSR Documentation](https://tanstack.com/query/latest/docs/framework/react/guides/ssr) — hydration pitfall
- [Spring Security JWT Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) — Nimbus JWT built-in
- [Secure File Upload with Spring Boot](https://www.oussemasahbeni.com/blog/secure-file-upload-spring-boot) — file upload security patterns

### Secondary (MEDIUM confidence)
- [ApplyArc - Best Job Application Trackers](https://applyarc.com/blog/best-job-application-trackers) — competitor feature analysis
- [Huntr Pricing](https://huntr.co/pricing) — paid tier feature gating confirmed
- [Teal - Job Tracker](https://www.tealhq.com/tools/job-tracker) — AI feature comparison
- [Cypress vs Playwright 2026](https://bugbug.io/blog/test-automation-tools/cypress-vs-playwright/) — E2E testing comparison
- [PropelAuth: Avoiding CORS Issues in React/Next.js](https://www.propelauth.com/post/avoiding-cors-issues-in-react-next-js) — CORS + JWT filter chain ordering

### Tertiary (LOW confidence)
- [Hexagonal Architecture with Spring Boot + Kotlin](https://medium.com/@hieunv/understanding-hexagonal-architecture-through-a-practical-application-2f2d28f604d9) — architecture pattern reference (not adopted directly)

---

*Research completed: 2026-03-19*
*Ready for roadmap: yes*
