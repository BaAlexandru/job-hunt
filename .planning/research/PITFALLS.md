# Pitfalls Research

**Domain:** Job Application Tracker (Kotlin/Spring Boot + Next.js monorepo)
**Researched:** 2026-03-19
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Kotlin JPA Entity Classes Are Final by Default

**What goes wrong:**
Kotlin classes are `final` by default. Hibernate/JPA needs to create proxy subclasses for lazy loading (`@ManyToOne`, `@OneToOne`). Without the `kotlin-spring` and `kotlin-jpa` compiler plugins, you get cryptic runtime errors: lazy loading silently breaks, `@Transactional` annotations are ignored because Spring cannot proxy final classes, and `@Configuration` classes fail to be enhanced.

**Why it happens:**
Developers coming from Java assume Spring "just works" with Kotlin. Java classes are open by default; Kotlin is the opposite. The errors are often silent (e.g., transactions not rolling back) rather than loud failures, so they go undetected until data corruption occurs.

**How to avoid:**
- Add `kotlin("plugin.spring")` and `kotlin("plugin.jpa")` to `build.gradle.kts` from day one. The `plugin.spring` plugin auto-opens classes annotated with `@Component`, `@Service`, `@Configuration`, `@Controller`, `@RestController`, `@Repository`, and `@Transactional`. The `plugin.jpa` plugin adds no-arg constructors to `@Entity` classes.
- Verify with a simple integration test: create an entity with a lazy `@ManyToOne`, load the parent, and assert the child loads on access.

**Warning signs:**
- `@Transactional` methods not rolling back on exceptions
- Lazy-loaded relationships returning null or throwing `LazyInitializationException`
- Build warnings about "cannot proxy final class"

**Phase to address:**
Phase 1 (Project scaffolding). This must be in the initial `build.gradle.kts`. Retrofitting is painful because every entity and service class would need manual `open` modifiers.

---

### Pitfall 2: CORS + JWT Filter Chain Ordering Breaks Authentication

**What goes wrong:**
Next.js runs on port 3000, Spring Boot on 8080. The browser sends a preflight `OPTIONS` request before the actual request. If Spring Security's `BearerTokenAuthenticationFilter` runs before the CORS filter, the `OPTIONS` request is rejected with 401 (no Bearer token on preflight). The frontend sees opaque CORS errors that give no useful debugging information.

**Why it happens:**
Spring Security's filter chain has a specific ordering. Developers configure JWT validation and CORS separately, not realizing they interact. The default filter chain evaluates security before CORS, so preflight requests fail. This is made worse because CORS errors in browsers hide the real response, showing only "CORS policy" messages.

**How to avoid:**
- Configure CORS as a `CorsConfigurationSource` bean and register it in the SecurityFilterChain via `http.cors { it.configurationSource(corsConfig) }` -- this ensures CORS is evaluated before authentication.
- Explicitly permit `OPTIONS` requests in the security config: `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()`.
- In development, consider using Next.js `rewrites` in `next.config.js` to proxy API calls to Spring Boot, eliminating cross-origin entirely during local dev.

**Warning signs:**
- Frontend login works but subsequent API calls fail silently
- Browser console shows "CORS policy" errors on authenticated endpoints
- Requests work from Postman/curl but not from the browser

**Phase to address:**
Phase 1 (Authentication setup). Must be configured correctly the moment JWT auth is introduced. Do not defer -- it blocks all frontend-backend integration.

---

### Pitfall 3: Flyway Migration Checksum Mismatches in Team/CI Environments

**What goes wrong:**
A developer modifies an already-applied migration file (fixing a typo, adding a column). Flyway detects the checksum mismatch and refuses to start the application. In CI/CD, this means builds fail. Worse, running `flyway repair` in production to fix it can mask real migration issues.

**Why it happens:**
Developers treat migration files like regular source code that can be edited. Flyway treats applied migrations as immutable history. The mismatch between these mental models causes the problem. This is especially common early in a project when the schema is still evolving rapidly.

**How to avoid:**
- Establish a strict rule from day one: never edit an applied migration. Always create a new migration to fix issues.
- Use a naming convention like `V1__create_users.sql`, `V2__create_companies.sql` that makes ordering obvious.
- In development, if you need a clean slate, use `spring.flyway.clean-disabled=false` with `flyway clean` (NEVER in production).
- Add `spring.flyway.clean-disabled=true` explicitly in production profiles.
- Consider using Flyway's `baselineOnMigrate` for initial setup.

**Warning signs:**
- Application fails to start with "Migration checksum mismatch" in logs
- Developers manually running `flyway repair` frequently
- Git history shows edits to files in `db/migration/`

**Phase to address:**
Phase 1 (Database setup). The migration discipline must be established before any schema exists. Document the rule in `CLAUDE.md` for the backend module.

---

### Pitfall 4: File Upload Security -- Path Traversal and Unrestricted Types

**What goes wrong:**
Users upload CVs and cover letters (PDF, DOCX). If the original filename from the client is used for storage, an attacker can craft a filename like `../../etc/passwd` to perform path traversal. Even without malicious intent, filenames with special characters or duplicates cause overwrites. Additionally, accepting any file type opens the door to executable uploads or oversized files that exhaust disk/memory.

**Why it happens:**
File upload feels simple ("just save the bytes") so developers implement it naively. Spring Boot's `MultipartFile.getOriginalFilename()` returns the client-provided name, which developers use directly. The attack surface is not obvious for a "personal" app, but the multi-user-ready architecture means the code should be secure from the start.

**How to avoid:**
- Never use the original filename for storage. Generate a UUID-based filename and store the original name in the database as metadata.
- Validate file type by checking both the extension AND the MIME type (magic bytes), not just `Content-Type` header (easily spoofed).
- Enforce a whitelist: only `.pdf`, `.docx`, `.doc` extensions.
- Set `spring.servlet.multipart.max-file-size=10MB` and `spring.servlet.multipart.max-request-size=10MB` in application properties.
- Store files outside the web root (or use object storage). Never serve uploaded files from a path that could be interpreted as executable.

**Warning signs:**
- Files stored with original filenames in a flat directory
- No file size limits configured
- Files served directly from the upload directory without access control

**Phase to address:**
Phase 2 (Document management). Must be implemented correctly from the first upload endpoint. Retrofitting filename sanitization requires migrating existing files.

---

### Pitfall 5: Hardcoding Single-Tenancy Despite "Multi-User Ready" Goal

**What goes wrong:**
The project says "single user initially, multi-user ready." In practice, developers skip tenant isolation because "it's just for me right now." Queries lack `WHERE user_id = ?`, file storage uses a flat directory without user scoping, and API endpoints don't check resource ownership. When multi-user is added later, every query, every file path, and every endpoint needs modification -- effectively a rewrite of the data layer.

**Why it happens:**
The immediate need is single-user, so adding `user_id` to every query feels like premature optimization. But it is actually structural -- adding it later means modifying every repository method, every service call, and every migration.

**How to avoid:**
- Add a `user_id` foreign key to every business entity from migration V1. Even with one user, the column exists.
- Use Spring Security's `SecurityContextHolder` to inject the current user ID into service methods. Create a helper like `fun currentUserId(): Long`.
- Scope all repository queries: `findByIdAndUserId(id, userId)` not `findById(id)`.
- Store files under `/{userId}/{documentId}/filename` not `/{documentId}/filename`.
- Write integration tests that create two users and verify User A cannot access User B's data.

**Warning signs:**
- Repository methods without `userId` parameter
- No `user_id` column on `companies`, `jobs`, `applications`, or `documents` tables
- File storage paths without user scoping
- Tests that only use a single test user

**Phase to address:**
Phase 1 (Data model design). The schema must include `user_id` from the first migration. This is a foundational decision that cannot be deferred.

---

### Pitfall 6: Spring AI Provider Lock-In Through Configuration Sprawl

**What goes wrong:**
Spring AI abstracts providers, but developers leak provider-specific configuration throughout the codebase: hardcoded model names ("claude-3-opus"), provider-specific parameters in prompt templates, API-key configuration scattered across properties files. When switching providers (or when a model is deprecated), changes ripple across the entire codebase.

**Why it happens:**
Spring AI's abstraction works at the API level, but each provider has different model names, token limits, pricing, and capability differences. Developers naturally optimize for their current provider without isolating the provider-specific bits.

**How to avoid:**
- Centralize all AI configuration in a single `AiConfig` class or properties group. Map logical names to provider-specific names: `app.ai.model=default` maps to `anthropic.claude-3-5-sonnet` or `openai.gpt-4o` via a config file.
- Keep prompt templates provider-agnostic. Do not use provider-specific prompt formats (e.g., Claude's XML tags) in templates stored alongside business logic.
- Create a thin adapter layer: `CvAnalysisService` calls `AiService.analyze(prompt)`, not provider APIs directly.
- Store API keys in environment variables, never in `application.yml`.

**Warning signs:**
- Provider model names appearing in service classes
- Different AI-related properties files per environment
- Prompt templates with provider-specific formatting
- Direct `ChatClient` usage in business service classes

**Phase to address:**
Phase 3 or 4 (AI integration). Design the abstraction layer before writing the first AI feature. The abstraction is cheap to build upfront but expensive to retrofit.

---

### Pitfall 7: TanStack Query Hydration Mismatches with Next.js SSR

**What goes wrong:**
When using TanStack Query with Next.js App Router, the server pre-renders HTML with data, then the client hydrates. If the `QueryClient` is shared across requests (created at module level instead of per-request), data leaks between users. If `staleTime` is 0 (default), the client immediately refetches after hydration, causing a flash of loading state and wasted API calls. Hydration mismatches produce React errors in the console.

**Why it happens:**
TanStack Query was designed for client-side SPAs. Its integration with Next.js SSR requires careful setup: `dehydrate`/`HydrationBoundary`, per-request `QueryClient` instances, and `staleTime` configuration. The "getting started" tutorials often show client-only usage, and developers add SSR later without understanding the hydration contract.

**How to avoid:**
- Create `QueryClient` inside a React state or ref, never at module scope: `const [queryClient] = useState(() => new QueryClient({...}))`.
- Set a default `staleTime` above 0 (e.g., 60 seconds for this app -- job data does not change frequently).
- For SSR pages, use `prefetchQuery` on the server, `dehydrate` the cache, and wrap client components in `HydrationBoundary`.
- For this app (single user, personal tool), consider starting without SSR prefetching entirely and using `initialData` or client-only fetching. The SEO benefit of SSR is irrelevant for an authenticated app.

**Warning signs:**
- React hydration mismatch warnings in the browser console
- Double-fetching on page load (network tab shows two identical requests)
- Flash of loading/empty state before data appears on navigation

**Phase to address:**
Phase 1 (Frontend scaffolding). The `QueryClient` setup is foundational. But SSR prefetching can be deferred -- start with client-only fetching for an authenticated app.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Skip input validation on backend (rely on frontend) | Faster API development | Any API client bypasses validation; data integrity issues | Never -- always validate server-side |
| Store files on local filesystem instead of object storage | No S3/MinIO setup needed | Cannot scale horizontally; files lost if container restarts | MVP only, with a clear migration path to object storage |
| Use `String` for status fields instead of enums | Faster initial modeling | Typos in status values, no compiler safety, inconsistent data | Never -- use Kotlin sealed classes or enums from the start |
| Skip API versioning | Simpler URLs | Breaking changes when frontend and backend evolve at different speeds | Acceptable for MVP if both are in the same monorepo and deployed together |
| Inline CSS/styles instead of design system | Faster prototyping | Inconsistent UI, harder to maintain themes | First 2-3 pages only, then establish shadcn/ui patterns |
| Raw SQL in Flyway migrations without indexes | Simpler migrations | Slow queries as data grows; adding indexes later requires careful migration | Only for first few tables; add indexes from the start on foreign keys |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Spring Security + Kotlin | Forgetting `kotlin-spring` plugin; `@Transactional` silently does nothing on final classes | Add `plugin.spring` and `plugin.jpa` in `build.gradle.kts`; verify with integration test |
| Next.js + Spring Boot local dev | Hardcoding `localhost:8080` throughout frontend code | Use environment variable `NEXT_PUBLIC_API_URL`; use Next.js rewrites for local dev to avoid CORS |
| TanStack Query + JWT | Forgetting to attach Bearer token to requests; no 401 interceptor for token refresh | Create a custom `fetch` wrapper or Axios instance with interceptors; handle 401 globally |
| Docker Compose + PostgreSQL | Database data lost on `docker-compose down -v` | Use named volumes; document the difference between `down` and `down -v` |
| Flyway + Docker | Migrations run before database is ready | Use `depends_on` with health checks in Docker Compose; Spring Boot retry handles transient connection failures |
| Spring AI + provider APIs | No rate limiting or cost controls; runaway AI calls in development | Set budget limits on provider accounts; add request throttling; log every AI API call with token count |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| N+1 queries on application list with related companies | List page takes 2-5 seconds; many small SQL queries in logs | Use `@EntityGraph` or `JOIN FETCH` in repository queries; monitor with Hibernate statistics | 50+ applications (noticeable); 500+ (unusable) |
| Loading all applications on kanban board | Kanban board freezes; high memory usage in browser | Paginate by status column; load only visible columns; virtual scrolling | 100+ applications across all statuses |
| Storing full document content in database BLOBs | Database backup becomes huge; queries slow down | Store files on filesystem/object storage; store only metadata and file path in DB | 20+ documents (backups slow); 100+ (queries degrade) |
| No database connection pooling tuning | Random connection timeout errors under load | Configure HikariCP: `maximum-pool-size=10` for single user; monitor with actuator | Multi-user (5+ concurrent users) |
| Synchronous AI API calls blocking request threads | UI freezes for 10-30 seconds during AI analysis | Use Spring WebFlux or `@Async` for AI calls; return immediately with a job ID; poll or use SSE for results | Any usage -- AI APIs inherently have high latency |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| JWT secret key too short or hardcoded in source | Token forgery; anyone with the source can create valid tokens | Use 256-bit+ key from environment variable; rotate keys periodically; use asymmetric keys (RS256) for production |
| No token expiration or excessively long expiry (days/weeks) | Stolen token usable indefinitely | Set access token expiry to 15-30 minutes; use refresh tokens for session continuity |
| Storing JWT in localStorage | XSS attack can steal the token | Use httpOnly cookies for token storage; set SameSite=Strict; add CSRF protection for cookie-based auth |
| Serving uploaded files without access control | Any authenticated user can access any document by guessing the URL | Serve files through an authenticated endpoint that checks ownership (`user_id` match); never expose direct file paths |
| Missing rate limiting on auth endpoints | Brute force attacks on login | Add rate limiting (e.g., Bucket4j or Spring Cloud Gateway rate limiter) on `/auth/login` and `/auth/register` |
| Trusting JWT `alg` header | Algorithm confusion attack allows token forgery | Always enforce algorithm server-side; never let the token dictate which algorithm to use for verification |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Requiring too many fields when adding a job application | User stops tracking because it is too much friction | Only require company name and job title; make everything else optional; allow progressive enrichment |
| No quick-add flow for applications | User falls behind on tracking during active job search | Add a "quick add" button/modal: company + title + status in 3 clicks, details later |
| Status transitions require navigating to detail page | Updating status (the most frequent action) is buried | Allow drag-and-drop on kanban board AND inline status change on list view |
| No visual distinction between application statuses | User cannot quickly scan their pipeline | Use distinct colors per status; show counts per status; highlight stale applications (no update in 2+ weeks) |
| AI features block the main workflow | User waits 15+ seconds for AI analysis before they can continue | Run AI analysis asynchronously; show results when ready; never block form submission on AI completion |
| No "stale application" indicator | Applications sit in "Applied" forever; user forgets to follow up | Auto-flag applications with no status change in 7-14 days; show "needs attention" badge |

## "Looks Done But Isn't" Checklist

- [ ] **Authentication:** Token refresh flow implemented -- not just login. Verify: can the user stay logged in across browser sessions without re-entering credentials?
- [ ] **File upload:** Virus/malware scanning considered -- even basic MIME type validation. Verify: can you upload a `.exe` renamed to `.pdf`?
- [ ] **Application status flow:** Reverse transitions allowed (e.g., Interview back to Applied). Verify: can the user fix a status they set by mistake?
- [ ] **Search/filter:** Works with empty results gracefully. Verify: does the UI show a helpful empty state, not a blank page?
- [ ] **Database migrations:** Down/rollback path exists for every migration. Verify: can you roll back the last migration and re-apply it?
- [ ] **Docker Compose:** Data persists across `docker-compose down` (without `-v`). Verify: restart containers and check if data is still there.
- [ ] **Error handling:** API errors show user-friendly messages, not stack traces. Verify: trigger a 500 error and check what the user sees.
- [ ] **Kanban board:** Handles 0 items in a column without breaking layout. Verify: move all items out of one status column.
- [ ] **AI features:** Graceful degradation when AI provider is down or API key is missing. Verify: remove the API key and check that non-AI features still work.
- [ ] **Multi-user readiness:** Every database query scoped to user. Verify: create a second user and confirm data isolation.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Missing Kotlin compiler plugins | LOW | Add plugins to `build.gradle.kts`; rebuild; verify with tests. No data impact. |
| CORS misconfiguration | LOW | Fix `SecurityFilterChain` ordering; test with browser. Usually a single-file fix. |
| Flyway checksum mismatch | MEDIUM | In dev: `flyway clean` + re-migrate. In prod: create corrective migration + `flyway repair` (risky). |
| No user scoping in data model | HIGH | Requires new migration adding `user_id` to all tables, backfilling data, modifying every repository and service method. Effectively a rewrite of the data access layer. |
| File path traversal vulnerability | MEDIUM | Rename all stored files to UUIDs; update database references; create migration script. Risk of data loss if not careful. |
| Provider lock-in in AI code | MEDIUM | Extract provider-specific code into adapter layer; update all call sites. Scope depends on how deeply provider APIs were used directly. |
| JWT stored in localStorage | MEDIUM | Switch to httpOnly cookies; update all API call code; existing sessions invalidated (users must re-login). |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Kotlin final classes | Phase 1: Project Scaffolding | `build.gradle.kts` includes `plugin.spring` and `plugin.jpa`; lazy loading works in integration test |
| CORS + JWT filter chain | Phase 1: Authentication | Frontend can call authenticated API endpoint from browser without CORS errors |
| Flyway discipline | Phase 1: Database Setup | No edited migration files in git history; CI runs migrations from clean database successfully |
| File upload security | Phase 2: Document Management | Upload endpoint rejects non-whitelisted types; stored files have UUID names; path traversal test passes |
| Single-tenancy hardcoding | Phase 1: Data Model Design | Every business table has `user_id` FK; repository methods include user scoping; two-user isolation test passes |
| AI provider lock-in | Phase 3/4: AI Integration | Can switch `application.yml` provider config and AI features still work without code changes |
| TanStack Query hydration | Phase 1: Frontend Scaffolding | No hydration warnings in browser console; no double-fetching visible in network tab |
| JWT security basics | Phase 1: Authentication | Token expiry under 30 minutes; secret from env var; httpOnly cookie storage |
| Synchronous AI calls | Phase 3/4: AI Integration | AI analysis returns immediately with job ID; UI remains responsive during AI processing |
| Missing rate limiting | Phase 1: Authentication (or Phase 2) | Login endpoint returns 429 after repeated failed attempts |

## Sources

- [Kotlin All-Open Plugin Documentation](https://kotlinlang.org/docs/all-open-plugin.html)
- [JetBrains: Common Pitfalls with JPA and Kotlin](https://blog.jetbrains.com/idea/2026/01/how-to-avoid-common-pitfalls-with-jpa-and-kotlin/)
- [Spring Framework Kotlin Projects Documentation](https://docs.spring.io/spring-framework/reference/languages/kotlin/spring-projects-in.html)
- [PropelAuth: Avoiding CORS Issues in React/Next.js](https://www.propelauth.com/post/avoiding-cors-issues-in-react-next-js)
- [Auth0 Community: Next.js + Spring Boot JWT CORS Issue](https://community.auth0.com/t/next-js-with-spring-boot-oauth2-jwt-calls-api-from-localhost-3000-with-options-with-no-bearer-causing-401/132203)
- [TanStack Query SSR Documentation](https://tanstack.com/query/latest/docs/framework/react/guides/ssr)
- [TanStack Query Advanced SSR Documentation](https://tanstack.com/query/latest/docs/framework/react/guides/advanced-ssr)
- [DevGlan: JWT Authentication Common Pitfalls](https://www.devglan.com/spring-security/jwt-authentication-spring-security)
- [Stackademic: Spring Boot Security Mistakes with JWT](https://blog.stackademic.com/5-deadly-spring-boot-security-mistakes-with-jwt-oauth2-and-how-to-fix-them-before-hackers-0f97ee0a0d58)
- [Secure File Upload with Spring Boot](https://www.oussemasahbeni.com/blog/secure-file-upload-spring-boot)
- [Common File Upload Mistakes in Spring Boot](https://javanexus.com/blog/common-file-upload-mistakes-spring-boot)
- [Flyway Repair with Spring Boot (Baeldung)](https://www.baeldung.com/spring-boot-flyway-repair)
- [Spring AI 1.1 GA Release](https://spring.io/blog/2025/11/12/spring-ai-1-1-GA-released/)
- [Spring AI Prompt Engineering Patterns](https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns/)

---
*Pitfalls research for: Job Application Tracker (Kotlin/Spring Boot + Next.js)*
*Researched: 2026-03-19*
