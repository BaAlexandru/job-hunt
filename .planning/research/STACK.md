# Stack Research

**Domain:** Job Application Tracker with AI Features (Full-Stack Web App)
**Researched:** 2026-03-19
**Confidence:** HIGH

## Validation Summary

> **Decision Override (2026-03-19):** The user chose to keep **Spring Boot 4.0.4 + Kotlin 2.2.21 + Java 24 + Gradle 9.3.1 + Spring AI 2.0.0-M3** rather than the 3.5.9 stack recommended below. This accepts milestone risks for Spring AI 2.0 but gains access to Spring Boot 4.0 features (renamed starters, modular Flyway, Jackson 3). The "Backend Core" table below reflects the original research recommendation — for actual project versions, see the Phase 1 planning docs.

The user's chosen stack is solid and well-aligned for a 2025-2026 project. All core choices validated with minor version updates and gap-fills below. ~~One significant decision point: **Spring Boot 3.5.x vs 4.0.x** -- recommendation is 3.5.x for stability.~~ **Decided: Spring Boot 4.0.4.**

---

## Recommended Stack

### Backend Core

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.3.20 | Primary language | Latest stable (March 2026). Excellent Spring Boot support, null safety, coroutines. Learning goal aligns with production use. |
| Spring Boot | 3.5.9 | Application framework | Latest 3.5.x patch (Dec 2025). Production-proven. Spring Boot 4.0 exists but requires Jakarta EE 11 baseline and Kotlin 2.2+ minimum -- unnecessary migration risk for a new project learning Kotlin. Stick with 3.5.x. |
| Spring Security | 6.5.x (managed by Boot 3.5) | Authentication/authorization | Built-in JWT support via OAuth2 Resource Server. Use Nimbus JOSE+JWT (already a transitive dependency) rather than adding JJWT. |
| Spring Data JPA | 3.5.x (managed by Boot 3.5) | Data access | Kotlin-friendly with extension functions. Pair with Hibernate 6.x (managed). |
| PostgreSQL Driver | 42.7.x (managed by Boot 3.5) | Database connectivity | Managed by Spring Boot BOM. Do not pin manually. |
| Flyway | 10.x (managed by Boot 3.5) | Database migrations | Spring Boot 3.5 manages Flyway 10.x. Requires `flyway-database-postgresql` module (separate from `flyway-core` since Flyway 10). |
| Spring AI | 1.1.1 | AI provider abstraction | Latest stable GA (Dec 2025). Supports Anthropic Claude and OpenAI out of the box with `ChatModel` abstraction. Do NOT use 2.0.0-M2 -- it requires Spring Boot 4.0. |
| Gradle | 8.14.4 | Build tool | Latest 8.x (Jan 2026). Kotlin DSL for build scripts. Compatible with Spring Boot 3.5 and Kotlin 2.3.x. Gradle 9.x exists but ecosystem plugins lag -- stay on 8.x. |
| Java | 21 LTS | JVM target | Spring Boot 3.5 requires Java 17+. Use 21 LTS for virtual threads support and long-term support. |

### Frontend Core

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Next.js | 16.2.x | React framework | Latest stable (March 2026). Turbopack is now default and stable for both dev and production builds. App Router is the standard. |
| React | 19.x | UI library | Bundled with Next.js 16. Server Components, Actions, and `use` hook are stable. |
| TypeScript | 5.7+ | Type safety | Managed by Next.js. Stricter type checks in Next.js 16. |
| TanStack Query | 5.91.x | Server state management | Actively maintained. Handles caching, background refetching, optimistic updates. Perfect for API-driven apps. |
| Tailwind CSS | 4.1.x | Utility-first CSS | Major rewrite with Rust-based Oxide engine. 5x faster full builds. CSS-first config via `@theme` directives -- no `tailwind.config.js` needed. |
| shadcn/ui | CLI v4 | Component library | Not a package -- copies components into your project. March 2026 CLI v4 supports coding agents, Radix or Base UI primitives. Use Radix primitives (more mature). |
| Node.js | 22 LTS | Runtime | Required for Next.js. Use LTS for stability. |

### Supporting Libraries -- Backend

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `spring-boot-starter-validation` | managed | Request validation | Always. Jakarta Bean Validation for DTOs. |
| `spring-boot-starter-actuator` | managed | Health checks, metrics | Always. Required for Docker health checks and K8s probes. |
| `jackson-module-kotlin` | managed | JSON serialization | Always. Registers Kotlin module for proper data class serialization. Auto-configured by Spring Boot. |
| `kotlin-reflect` | managed | Kotlin reflection | Always. Required by Spring Boot for Kotlin support. |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.x | API documentation | Always. Auto-generates OpenAPI 3.1 spec + Swagger UI. Use instead of manual API docs. |
| `aws-sdk-kotlin` (S3) or MinIO SDK | latest | Document storage | Phase 2+. For CV/cover letter file storage. MinIO for local dev (S3-compatible), AWS S3 for production. Use Spring's `MultipartFile` for upload handling. |
| `spring-boot-starter-mail` | managed | Email notifications | Phase 3+. Optional interview reminders. |
| `kotlinx-coroutines-reactor` | managed | Async support | When AI calls need to be non-blocking. Spring AI operations can be slow -- coroutines prevent thread starvation. |

### Supporting Libraries -- Frontend

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `react-hook-form` | 7.60.x | Form state management | Always. Minimal re-renders, uncontrolled components. |
| `zod` | 3.25.x | Schema validation | Always. Runtime validation matching TypeScript types. Share schemas between client and server validation. |
| `@hookform/resolvers` | 5.1.x | RHF + Zod bridge | Always. Integrates Zod schemas with react-hook-form. |
| `@dnd-kit/core` | 6.x | Drag and drop | Kanban board. Best React DnD library -- accessible, performant. Do NOT use `react-beautiful-dnd` (deprecated). |
| `lucide-react` | latest | Icons | Always. shadcn/ui default icon library. Tree-shakeable. |
| `date-fns` | 4.x | Date formatting | Timeline displays. Lightweight, tree-shakeable. Do NOT use moment.js (bloated, deprecated). |
| `nuqs` | latest | URL state management | Search/filter state. Type-safe URL search params for Next.js. |
| `next-auth` (Auth.js) | 5.x | Auth (if needed) | Only if you want frontend session management. For JWT-only backend auth, a simple fetch wrapper with token storage may suffice. Evaluate in Phase 1. |
| `axios` or native `fetch` | -- | HTTP client | TanStack Query needs a fetch function. Native `fetch` is fine in Next.js 16. Axios adds interceptors if needed for token refresh. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Docker + Docker Compose | Local dev environment | PostgreSQL, MinIO in containers. Spring Boot and Next.js can run natively for faster dev loop. |
| IntelliJ IDEA | Kotlin IDE | Best Kotlin IDE by far. Spring Boot integration, database tools built in. |
| VS Code | Frontend IDE | With Tailwind CSS IntelliSense, ESLint, Prettier extensions. |
| `ktlint` | Kotlin linting | Enforces Kotlin coding style. Gradle plugin: `org.jlleitschuh.gradle.ktlint`. |
| `detekt` | Kotlin static analysis | Catches code smells, complexity issues. Gradle plugin: `io.gitlab.arturborisov.detekt`. |
| ESLint + Prettier | Frontend linting/formatting | Next.js ships ESLint config. Add Prettier for formatting. |
| Testcontainers | Integration testing | Spin up PostgreSQL in Docker for tests. Better than H2 (which has SQL dialect differences). |
| `pgAdmin` or DBeaver | Database GUI | Query and inspect PostgreSQL during development. |

### Testing Stack

| Tool | Layer | Purpose | Notes |
|------|-------|---------|-------|
| **JUnit 5** | Backend unit/integration | Test runner | Spring Boot default. Kotlin-compatible. |
| **MockK** | Backend unit | Mocking | Kotlin-native mocking. Use instead of Mockito -- better coroutine support, idiomatic Kotlin syntax (`every`, `coEvery`, `verify`). |
| **SpringMockK** | Backend integration | Spring test mocking | `@MockkBean` and `@SpykBean` -- MockK equivalents of `@MockBean`. |
| **Kotest** | Backend unit (optional) | Test framework | Consider for readability (`shouldBe` matchers) but JUnit 5 + MockK is sufficient and has less learning curve when also learning Kotlin. |
| **Testcontainers** | Backend integration | Real database tests | PostgreSQL container for integration tests. Eliminates H2 dialect mismatch bugs. |
| **Vitest** | Frontend unit | Test runner | Fast, Vite-native. Works with React Testing Library. |
| **React Testing Library** | Frontend component | Component testing | Test behavior, not implementation. Standard for React. |
| **Playwright** | E2E | Browser testing | Cross-browser (Chrome, Firefox, Safari). Free parallelization. Next.js officially recommends it. Use over Cypress -- broader browser support, no paid cloud needed for parallel tests. |

---

## Installation

### Backend (build.gradle.kts)

```kotlin
plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.jpa") version "2.3.20"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

dependencies {
    // Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT via Nimbus

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // AI (add in AI phase)
    // implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    // implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // API Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core") // Use MockK instead
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

### Frontend (package.json dependencies)

```bash
# Core
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --turbopack

# UI Components
npx shadcn@latest init

# Data fetching & forms
npm install @tanstack/react-query @tanstack/react-query-devtools
npm install react-hook-form zod @hookform/resolvers

# Kanban board
npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities

# Utilities
npm install date-fns lucide-react nuqs

# Dev dependencies
npm install -D prettier eslint-config-prettier
npm install -D @playwright/test
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Spring Boot 3.5.x | Spring Boot 4.0.x | When Spring AI 2.0 GA ships (April 2026+) and ecosystem has stabilized. Migrating later is straightforward (3.5 -> 4.0 migration guide exists). |
| Gradle (Kotlin DSL) | Maven | If team is more familiar with Maven. Gradle is faster for incremental builds and more flexible, but Maven has simpler mental model. |
| PostgreSQL | SQLite or H2 | Never for this project. PostgreSQL's full-text search, JSONB columns, and production-grade features are needed. |
| MockK | Mockito | Only if entire team knows Mockito and refuses to learn MockK. MockK is objectively better for Kotlin. |
| Playwright | Cypress | If team has deep Cypress expertise. For greenfield, Playwright wins on browser coverage and free parallelization. |
| TanStack Query | SWR | SWR is simpler but TanStack Query has better devtools, mutation support, and cache management. Worth the slightly higher complexity. |
| react-hook-form + Zod | Formik + Yup | Formik has more re-renders, larger bundle. RHF + Zod is the modern standard. |
| Flyway | Liquibase | Liquibase is more powerful (XML/YAML/JSON formats) but Flyway's SQL-based approach is simpler and sufficient. Spring Boot has first-class Flyway support. |
| MinIO (local) + S3 (prod) | Filesystem storage | Only for absolute MVP. Filesystem storage doesn't survive container restarts and doesn't scale. MinIO is S3-compatible and trivial to set up in Docker Compose. |
| `@dnd-kit/core` | `react-beautiful-dnd` | Never. `react-beautiful-dnd` is deprecated and unmaintained by Atlassian. |
| Next.js (App Router) | Vite + React Router | If you don't need SSR/SSG. For a job tracker, App Router gives you server components for fast initial loads and built-in API routes as a BFF layer. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| ~~Spring Boot 4.0 (for now)~~ | ~~Still maturing~~ **Decision: using 4.0.4 — user accepts milestone risks for Spring AI 2.0.0-M3 access** | ~~Spring Boot 3.5.9~~ |
| ~~Spring AI 2.0.0-M2~~ | ~~Milestone release~~ **Decision: using 2.0.0-M3 with Spring Boot 4.0.4** | ~~Spring AI 1.1.1 GA~~ |
| JJWT library | Spring Security already includes Nimbus JOSE+JWT. Adding JJWT is redundant and adds a dependency. | `spring-boot-starter-oauth2-resource-server` (uses Nimbus internally) |
| Mockito (with Kotlin) | Mockito struggles with Kotlin final classes (all classes final by default), suspend functions, and DSL syntax | MockK + SpringMockK |
| H2 for integration tests | SQL dialect differences cause false positives/negatives. Tests pass on H2 but fail on PostgreSQL. | Testcontainers with PostgreSQL |
| `react-beautiful-dnd` | Deprecated, unmaintained since 2024. Atlassian abandoned it. | `@dnd-kit/core` |
| moment.js | 330KB+ bundle, mutable API, project in maintenance mode | `date-fns` (tree-shakeable, immutable) |
| Tailwind CSS v3 | v4 is a ground-up rewrite with Rust engine. shadcn/ui CLI v4 targets Tailwind v4. | Tailwind CSS v4.1 |
| `tailwind.config.js` | Tailwind v4 uses CSS-first configuration with `@theme` directives. Config file approach is v3 legacy. | CSS `@theme` configuration |
| `pages/` directory (Next.js) | Legacy routing. App Router is the standard since Next.js 13. Pages directory won't get new features. | `app/` directory |
| Redux for server state | Over-engineering for API data. Redux is for complex client state. | TanStack Query for server state, React Context or Zustand for minimal client state |

---

## Stack Patterns by Variant

**If adding more AI providers later:**
- Spring AI's `ChatModel` interface is provider-agnostic. Add new starter dependency (e.g., `spring-ai-starter-model-ollama` for local models) and configure via `application.yml` profiles. No code changes needed.

**If deploying to Vercel (frontend) + separate backend:**
- Next.js on Vercel, Spring Boot on a VPS/K8s. Use environment variables for API URLs. CORS configuration in Spring Security.
- Consider `next.config.js` rewrites to proxy API calls and avoid CORS entirely.

**If keeping everything in Docker Compose (simpler):**
- Next.js `output: 'standalone'` in `next.config.js` for optimized Docker image (~100MB vs 1GB+).
- Multi-stage Dockerfiles for both services.

**If the project grows to multi-user:**
- Add Spring Security roles (ROLE_USER, ROLE_ADMIN). JPA entity-level filtering with `@Where` or custom repository methods. No architectural change needed if tenant ID is on entities from day one.

---

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Spring Boot 3.5.9 | Kotlin 2.3.x, Java 21, Gradle 8.14.x | Original recommendation — not used |
| **Spring Boot 4.0.4 (ACTUAL)** | **Kotlin 2.2.21, Java 24, Gradle 9.3.1** | **Project uses this combination** |
| Spring AI 1.1.1 | Spring Boot 3.5.x ONLY | NOT compatible with 4.0.x. |
| **Spring AI 2.0.0-M3 (ACTUAL)** | **Spring Boot 4.0 ONLY** | **Project uses this — milestone, not GA** |
| Next.js 16.2.x | React 19.x, Node 22 LTS | React version managed by Next.js. |
| Tailwind CSS 4.1.x | Next.js 16.x | Requires PostCSS setup change from v3. `create-next-app` handles this. |
| shadcn/ui CLI v4 | Tailwind CSS 4.x, Next.js 16.x | CLI auto-detects framework and Tailwind version. |
| TanStack Query 5.x | React 18+ / 19.x | Fully compatible with React 19 and Next.js App Router. |
| Flyway 10.x | PostgreSQL 12-17 | Spring Boot 3.5 manages the version. Requires `flyway-database-postgresql` artifact. |
| Testcontainers | Docker Desktop, Podman | Requires Docker daemon. Works on Windows with Docker Desktop or WSL2. |

---

## Sources

- [Spring Boot Releases](https://github.com/spring-projects/spring-boot/releases) -- version verification (HIGH confidence)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes) -- migration requirements (HIGH confidence)
- [Next Level Kotlin Support in Spring Boot 4](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4/) -- Kotlin compatibility (HIGH confidence)
- [Spring AI 1.1 GA Released](https://spring.io/blog/2025/11/12/spring-ai-1-1-GA-released/) -- Spring AI version and features (HIGH confidence)
- [Spring AI 2.0.0-M2](https://spring.io/blog/2026/01/23/spring-ai-2-0-0-M2-available-now) -- Spring AI 2.0 requires Boot 4.0 (HIGH confidence)
- [Next.js 16 Blog Post](https://nextjs.org/blog/next-16) -- Turbopack stable, default bundler (HIGH confidence)
- [Next.js 16.1 Blog Post](https://nextjs.org/blog/next-16-1) -- Turbopack file system caching stable (HIGH confidence)
- [Kotlin 2.3.20 Released](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) -- latest Kotlin version (HIGH confidence)
- [Tailwind CSS v4.0 Announcement](https://tailwindcss.com/blog/tailwindcss-v4) -- v4 architecture changes (HIGH confidence)
- [shadcn/ui CLI v4 Changelog](https://ui.shadcn.com/docs/changelog/2026-03-cli-v4) -- latest CLI version (HIGH confidence)
- [TanStack Query npm](https://www.npmjs.com/package/@tanstack/react-query) -- v5.91.x latest (HIGH confidence)
- [Gradle Releases](https://gradle.org/releases/) -- Gradle 8.14.4 (HIGH confidence)
- [MockK Documentation](https://mockk.io/) -- Kotlin mocking (HIGH confidence)
- [SpringMockK GitHub](https://github.com/Ninja-Squad/springmockk) -- Spring Boot + MockK integration (HIGH confidence)
- [Cypress vs Playwright 2026](https://bugbug.io/blog/test-automation-tools/cypress-vs-playwright/) -- E2E testing comparison (MEDIUM confidence)
- [Spring Security JWT Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) -- Nimbus JWT built-in (HIGH confidence)

---
*Stack research for: JobHunt - Job Application Tracker*
*Researched: 2026-03-19*
