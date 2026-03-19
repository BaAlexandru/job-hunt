# Phase 1: Foundation & Infrastructure - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

A runnable monorepo with Docker Compose, PostgreSQL, Flyway migrations, and developer tooling -- the foundation everything else builds on. Current Spring Boot app at root must be restructured into /backend. No domain logic in this phase.

</domain>

<decisions>
## Implementation Decisions

### Monorepo structure
- Gradle multi-project build: root settings.gradle.kts includes `:backend`
- /frontend is a plain Next.js directory with pnpm (not a Gradle subproject)
- /infra contains Docker files only (Dockerfiles, no Helm/K8s yet)
- compose.yaml stays at project root for convenience and Spring Boot docker-compose auto-discovery

### Docker Compose stack
- PostgreSQL container only for local dev -- backend runs on host via Gradle for fast dev cycle
- Fixed host port 5432 mapping (not random)
- Named volume for PostgreSQL data persistence across container restarts
- Spring Boot docker-compose integration auto-discovers compose.yaml at root

### Flyway baseline migration
- V1 creates schema extensions only (e.g., CREATE EXTENSION IF NOT EXISTS "pgcrypto" for gen_random_uuid())
- No domain tables in baseline -- each phase adds its own migrations
- Phase-prefixed naming convention: V1__phase01_baseline.sql, V2__phase02_create_users.sql, etc.
- UUID primary keys (gen_random_uuid()) for all tables -- multi-user ready, no sequential ID exposure

### Spring Boot version and dependencies
- Keep Spring Boot 4.0.4 + Spring AI 2.0.0-M3 (latest, accept milestone risks)
- Trim build.gradle.kts to Phase 1 needs only: spring-boot-starter-web, data-jpa, flyway, postgresql, kotlin-reflect, jackson-kotlin, devtools, docker-compose
- Remove Lombok entirely -- Kotlin's data classes and language features replace it
- Add back dependencies (Spring Security, Spring AI, OAuth2, restdocs, session-jdbc) when their phases start

### Claude's Discretion
- Exact Gradle multi-project configuration details
- .gitignore patterns for the monorepo
- Application properties structure (application.yml vs application.properties)
- CLAUDE.md file content and structure per module
- Test configuration and any health-check endpoint for verifying the app starts

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` -- Tech stack constraints (Kotlin, Spring Boot 3.x/4.x, PostgreSQL, Docker Compose)
- `.planning/REQUIREMENTS.md` -- INFR-01 through INFR-04, DEVX-01, DEVX-02

### Existing code to restructure
- `build.gradle.kts` -- Current root-level build to move into /backend and trim
- `settings.gradle.kts` -- Needs multi-project include(":backend")
- `compose.yaml` -- Current minimal PostgreSQL config to enhance
- `src/main/kotlin/com/alex/job/hunt/jobhunt/JobHuntApplication.kt` -- Main app to move into /backend

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `JobHuntApplication.kt`: Existing Spring Boot main class -- move to /backend/src/main/kotlin/...
- `build.gradle.kts`: Has Kotlin compiler plugins (plugin.spring, plugin.jpa) and allOpen config already working -- preserve during restructure
- `compose.yaml`: Basic PostgreSQL service definition -- enhance with fixed port and named volume

### Established Patterns
- Kotlin 2.2.21 with Java 24 toolchain -- keep this
- Jackson Kotlin module configured -- keep for JSON serialization
- Spring Boot devtools and docker-compose integration already in dependencies

### Integration Points
- Root compose.yaml: Spring Boot docker-compose starter auto-discovers this on startup
- Flyway: spring-boot-starter-flyway already in dependencies, needs migration files in /backend/src/main/resources/db/migration/
- Application properties: src/main/resources/application.properties exists but is likely empty -- needs database config

</code_context>

<specifics>
## Specific Ideas

- User wants to learn Kotlin alongside building the project -- keep idiomatic Kotlin patterns
- Multi-user ready architecture: design tables with user ownership from the start (UUID PKs, no hardcoded single-tenancy)
- Backend-first development: API built and tested before any frontend code

</specifics>

<deferred>
## Deferred Ideas

None -- discussion stayed within phase scope

</deferred>

---

*Phase: 01-foundation-infrastructure*
*Context gathered: 2026-03-19*
