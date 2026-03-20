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
- Health: `curl http://localhost:8080/actuator/health` (DB, Flyway, disk space)
- Format check: `./gradlew :backend:check`

## Conventions

- Kotlin data classes, never Lombok
- UUID primary keys via gen_random_uuid() for all tables
- Flyway migrations: V{N}__{phaseNN}_{description}.sql
- JPA hibernate.ddl-auto=validate (Flyway manages schema)
- No database connection properties in application.yml (docker-compose auto-configures)
- Dependencies added only when their phase starts (no preloading)

## Git Workflow

- **Branch per phase**: Before executing any phase, create a branch `phase-{NN}-{slug}` from **remote master** (`git fetch origin && git checkout -b phase-{NN}-{slug} origin/master`)
- **All phase work on the branch**: Every commit during phase execution goes to the phase branch, never directly to master
- **PR to merge**: When phase execution completes, open a GitHub PR via `gh pr create` — never merge locally with `git merge`
- **Review before merge**: PR must be reviewed — if changes are requested, apply them on the phase branch before merging
- **After merge**: Switch to master and pull remote: `git checkout master && git pull origin master`
- **Planning docs exception**: Planning artifacts (CONTEXT.md, RESEARCH.md, PLAN.md, VALIDATION.md) may be committed to master since they don't affect code
- **Branch naming**: `phase-{NN}-{slug}` where NN is zero-padded phase number and slug is the phase name (e.g., `phase-02-auth`, `phase-03-company-crud`)

## Module-Specific Instructions

See each module's CLAUDE.md for detailed conventions:
- backend/CLAUDE.md - Kotlin, Spring Boot, JPA, API patterns
- frontend/CLAUDE.md - Next.js, React (Phase 7+)
- infra/CLAUDE.md - Docker, deployment
