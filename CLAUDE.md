# JobHunt - Claude Code Instructions

## Project Structure

Gradle multi-project monorepo:
- /backend - Spring Boot 4.0.4 + Kotlin 2.2.21 REST API
- /frontend - Next.js (not yet initialized, Phase 7)
- /infra - Docker and deployment config
- compose.yaml at root for Spring Boot docker-compose auto-discovery

## Key Commands

- Build: `./gradlew :backend:classes`
- Test: `./gradlew :backend:test`
- Run: `./gradlew :backend:bootRun` (auto-starts PostgreSQL via Docker Compose)
- Format check: `./gradlew :backend:check`

## Conventions

- Kotlin data classes, never Lombok
- UUID primary keys via gen_random_uuid() for all tables
- Flyway migrations: V{N}__{phaseNN}_{description}.sql
- JPA hibernate.ddl-auto=validate (Flyway manages schema)
- No database connection properties in application.yml (docker-compose auto-configures)
- Dependencies added only when their phase starts (no preloading)

## Module-Specific Instructions

See each module's CLAUDE.md for detailed conventions:
- backend/CLAUDE.md - Kotlin, Spring Boot, JPA, API patterns
- frontend/CLAUDE.md - Next.js, React (Phase 7+)
- infra/CLAUDE.md - Docker, deployment
