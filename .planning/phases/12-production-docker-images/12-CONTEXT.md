# Phase 12: Production Docker Images - Context

**Gathered:** 2026-03-22
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

### Runtime configuration
- Spring profiles with YAML files per environment: `application-local.yml`, `application-staging.yml`, `application-prod.yml`
- Non-sensitive config (DB host, ports, JPA settings) goes in YAML files
- Sensitive values (passwords, keys) fetched from AWS secrets management at runtime — NO env vars for secrets, NO secrets stored on disk or in images
- AWS secrets service selection is Claude's discretion (SSM Parameter Store vs Secrets Manager)
- Frontend: build-time env injection via `--build-arg NEXT_PUBLIC_API_URL` — separate image per environment
- Profile activated via `SPRING_PROFILES_ACTIVE` env var (the only env var used)

### Base images
- Backend builder: Amazon Corretto JDK 24 Alpine (build stage)
- Backend runtime: Amazon Corretto JRE 24 Alpine (run stage) — chosen for AWS optimization since deploying to EC2
- Frontend: Claude's discretion for Node.js version (Node 22 or 24 Alpine based on Next.js 16 compatibility)
- Both containers run as non-root user (dedicated `app` user created in Dockerfile)

### JVM tuning (from ROADMAP.md — locked)
- Flags: `-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError`
- K8s resource limits: `requests: 384Mi / limits: 512Mi`

### Claude's Discretion
- AWS secrets service choice (SSM Parameter Store vs Secrets Manager)
- Node.js version for frontend (22 vs 24 Alpine)
- Exact .dockerignore patterns per module
- HEALTHCHECK implementation details (endpoint, interval, timeout)
- Loading skeleton design for dependency caching layers
- Exact compose.prod.yaml structure and service naming

</decisions>

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
- `infra/CLAUDE.md` — Infrastructure conventions and directory purpose

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
- `infra/docker/` — Empty directory with .gitkeep, ready for helper scripts or compose overrides
- Backend Actuator health endpoint (`/actuator/health`) — ready for HEALTHCHECK usage

### Established Patterns
- Gradle multi-project build: root `settings.gradle.kts` + `backend/build.gradle.kts` — Docker build context must be project root for backend
- Spring Boot docker-compose auto-discovery: `spring-boot-docker-compose` is `testAndDevelopmentOnly` — won't be in prod image
- Frontend uses pnpm (not npm/yarn) — Dockerfile must install pnpm

### Integration Points
- `frontend/next.config.ts` needs `output: "standalone"` for Next.js standalone deployment mode
- Backend `application.yml` — current dev config, new profile-specific YAMLs extend this
- GHCR image registry (Phase 13 will push these images)
- K8s deployments (Phase 15+) will reference these images

</code_context>

<specifics>
## Specific Ideas

- User explicitly wants NO env vars for secrets — "I do not want to have anything stored in my PC or in the server"
- Secrets must come from AWS secrets management service, fetched at runtime
- YAML files per environment are preferred over env-var-based configuration
- Amazon Corretto chosen specifically because deploying to AWS EC2

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 12-production-docker-images*
*Context gathered: 2026-03-22*
