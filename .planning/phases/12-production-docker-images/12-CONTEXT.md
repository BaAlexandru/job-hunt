# Phase 12: Production Docker Images - Context

**Gathered:** 2026-03-22
**Audited:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Both backend (Spring Boot/Kotlin) and frontend (Next.js) produce optimized, production-ready container images via multi-stage Dockerfiles. Images must be under 200MB each and serve traffic correctly when run with `docker run`. CI pipeline and K8s deployment are separate phases.

</domain>

<decisions>
## Implementation Decisions

### Dockerfile placement
- Dockerfiles live in project roots: `backend/Dockerfile` and `frontend/Dockerfile`
- Build context is the project root (monorepo) for backend: `docker build -f backend/Dockerfile .`
- Build context is `frontend/` for frontend: `docker build -f frontend/Dockerfile frontend/`
- Add `.dockerignore` files for each module to keep build context lean
- **Audit note:** `infra/CLAUDE.md` previously stated Dockerfiles go in `infra/docker/` — updated to reflect module-root placement as the standard pattern for multi-stage builds

### Build approach
- Full build inside Docker (multi-stage) — no pre-built artifacts
- Backend: COPY gradlew, gradle/, settings.gradle.kts, build.gradle.kts, backend/ into builder stage, run `./gradlew :backend:bootJar`
- Frontend: COPY package.json, pnpm-lock.yaml first for dependency caching, then source, run `pnpm build`
- Dependency layers cached separately from source for faster rebuilds

### Local build & test
- Docker Compose for local production testing: `compose.prod.yaml` extends existing `compose.yaml`
- Reuses postgres/redis/minio definitions from `compose.yaml`
- Run: `docker compose -f compose.yaml -f compose.prod.yaml up`
- HEALTHCHECK instructions included in both Dockerfiles for local testing (K8s probes added in Phase 15+)

### Runtime configuration — SCOPED FOR PHASE 12
- Phase 12 images run with existing `application.yml` dev config — no environment-specific profiles needed yet
- `SPRING_PROFILES_ACTIVE` env var can be passed at `docker run` time but defaults to dev-compatible config
- Frontend: build-time env injection via `--build-arg NEXT_PUBLIC_API_URL` — defaults to `http://localhost:8080/api` for local testing
- **Deferred to Phase 14:** Spring profile YAML files per environment (`application-staging.yml`, `application-prod.yml`), all AWS infrastructure config files, and secrets management strategy. The user will create these files and handle secret management in Phase 14.

### Base images
- Backend builder: Eclipse Temurin JDK 24 Alpine (build stage)
- Backend runtime: Eclipse Temurin JRE 24 Alpine (`eclipse-temurin:24-jre-alpine`) — chosen because Amazon Corretto does not ship separate JRE-only Alpine images for Java 17+; Temurin provides true JRE Alpine images and is the proven Docker pattern from official Docker documentation
- Frontend builder + runtime: Node.js 22 Alpine (`node:22-alpine`) — Node 22 is active LTS (until April 2027); Node 24 is not yet LTS and risky for production with Next.js 16
- Both containers run as non-root user (dedicated `app` user created in Dockerfile)

### JVM tuning (from ROADMAP.md — locked)
- Flags: `-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError`
- K8s resource limits: `requests: 384Mi / limits: 512Mi`

### Claude's Discretion
- Exact .dockerignore patterns per module
- HEALTHCHECK implementation details (endpoint, interval, timeout)
- Exact compose.prod.yaml structure and service naming
- Spring Boot layer extraction strategy (layertools vs fat JAR)

</decisions>

<audit>
## Review Audit Results (2026-03-22)

### Aligned (6 items)

| # | Area | Verification |
|---|------|-------------|
| 1 | Requirements mapping | DOCK-01 + DOCK-02 correctly scoped to Phase 12 per REQUIREMENTS.md |
| 2 | Gradle build context | Monorepo root context + `./gradlew :backend:bootJar` matches settings.gradle.kts multi-project setup |
| 3 | Frontend standalone | pnpm + `output: "standalone"` is the documented Next.js Docker pattern; next.config.ts correctly identified as needing the change |
| 4 | JVM tuning | `-XX:MaxRAMPercentage=75.0` with 512Mi limit = ~384Mi heap — matches ROADMAP.md memory budget (locked) |
| 5 | compose.prod.yaml | Extends compose.yaml with multi-file override pattern — correct reuse of postgres/redis/minio |
| 6 | HEALTHCHECK | Backend `/actuator/health` already exposed per application.yml; Docker-level check is correct (K8s probes deferred to Phase 15+) |

### Resolved Concerns (3 items)

#### 1. Base Image: Corretto JRE does not exist — RESOLVED: Use Eclipse Temurin
- **Original decision:** Amazon Corretto JRE 24 Alpine for runtime
- **Problem:** Amazon Corretto does not ship separate JRE-only Alpine images for Java 17+. Available images are `amazoncorretto:24-alpine` (full JDK) and `amazoncorretto:24-headless-alpine` (headless JDK, not a true JRE). Full JDK as runtime would likely exceed 200MB target.
- **Resolution:** Use `eclipse-temurin:24-jre-alpine` as runtime base image. Temurin provides true JRE Alpine images and is the proven pattern from official Docker documentation (docker.com/guides/java). Builder stage uses `eclipse-temurin:24-jdk-alpine`.

#### 2. Scope Creep: AWS Secrets & Profile YAMLs — RESOLVED: Deferred to Phase 14
- **Original decision:** Context included AWS Secrets Manager/SSM Parameter Store details, per-environment YAML files
- **Problem:** Phase 12 success criteria only require images that build (<200MB) and serve traffic (`docker run`). None requires AWS secrets integration or environment-specific profiles.
- **Resolution:** All secrets management, environment-specific profile YAML files (`application-staging.yml`, `application-prod.yml`), and AWS infrastructure config deferred to Phase 14 where the user will create these files. Phase 12 images work with existing `application.yml` dev config.

#### 3. infra/CLAUDE.md Contradicts Dockerfile Placement — RESOLVED: Update infra/CLAUDE.md
- **Original state:** `infra/CLAUDE.md` stated "/docker - Dockerfiles for production builds (future phases)"
- **Problem:** Context decision places Dockerfiles at module roots (`backend/Dockerfile`, `frontend/Dockerfile`), not in `infra/docker/`
- **Resolution:** Module-root placement is the standard pattern for multi-stage builds (keeps Dockerfiles close to code they build). `infra/CLAUDE.md` will be updated during Phase 12 implementation. `infra/docker/` directory can be used for helper scripts or compose overrides if needed.

### Recommendations Applied

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Node.js 22 Alpine for frontend | Active LTS until April 2027; Node 24 not yet LTS, risky for production with Next.js 16 |
| 2 | Eclipse Temurin JRE 24 Alpine for backend runtime | True JRE image exists; Corretto only has JDK Alpine variants |
| 3 | Keep plans to 2-3 max | Per RETROSPECTIVE.md: small focused plans > large rework |
| 4 | Defer secrets/profiles to Phase 14 | Phase 12 success criteria don't require them; user handles in Phase 14 |

</audit>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Build configuration
- `backend/build.gradle.kts` — Gradle build with Java 24 toolchain, Spring Boot plugins, all dependencies
- `settings.gradle.kts` — Root settings needed for monorepo Gradle build inside Docker
- `frontend/package.json` — Frontend dependencies, build scripts, pnpm config
- `frontend/next.config.ts` — Needs `output: "standalone"` added for Docker deployment

### Existing infrastructure
- `compose.yaml` — Current dev compose (postgres, redis, minio) — compose.prod.yaml must extend this
- `infra/CLAUDE.md` — Infrastructure conventions and directory purpose (will be updated in this phase)

### Memory constraints
- `.planning/ROADMAP.md` §Memory Budget — JVM flags, K8s resource limits, 2GB total budget

### Module conventions
- `backend/CLAUDE.md` — Kotlin/Spring Boot patterns, package structure
- `frontend/CLAUDE.md` — Next.js patterns, pnpm, build commands

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `compose.yaml` — Dev services (postgres:17, redis:7-alpine, minio) with volume definitions — compose.prod.yaml extends this
- `infra/docker/` — Empty directory with .gitkeep, available for helper scripts or compose overrides
- Backend Actuator health endpoint (`/actuator/health`) — ready for HEALTHCHECK usage

### Established Patterns
- Gradle multi-project build: root `settings.gradle.kts` + `backend/build.gradle.kts` — Docker build context must be project root for backend
- Spring Boot docker-compose auto-discovery: `spring-boot-docker-compose` is `testAndDevelopmentOnly` — won't be in prod image
- Frontend uses pnpm (not npm/yarn) — Dockerfile must install pnpm

### Integration Points
- `frontend/next.config.ts` needs `output: "standalone"` for Next.js standalone deployment mode
- Backend `application.yml` — current dev config, sufficient for Phase 12 local Docker testing
- GHCR image registry (Phase 13 will push these images)
- K8s deployments (Phase 15+) will reference these images

</code_context>

<specifics>
## Specific Ideas

- User explicitly wants NO env vars for secrets in production — "I do not want to have anything stored in my PC or in the server"
- Secrets must come from AWS secrets management service, fetched at runtime — but this is deferred to Phase 14, not Phase 12
- Amazon Corretto was originally chosen for AWS optimization but replaced with Eclipse Temurin for JRE availability; Temurin is equally performant on AWS EC2
- Profile YAML files and all secret management files will be created by the user in Phase 14

</specifics>

<deferred>
## Deferred Ideas

- **AWS secrets management integration** — Deferred to Phase 14 (user will create config files and handle secret management there)
- **Environment-specific profile YAMLs** (`application-staging.yml`, `application-prod.yml`) — Deferred to Phase 14
- **AWS Secrets Manager vs SSM Parameter Store choice** — Deferred to Phase 14

</deferred>

---

*Phase: 12-production-docker-images*
*Context gathered: 2026-03-22*
*Audit applied: 2026-03-22*
