# Phase 13: CI Pipeline - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Every merge to master automatically builds, tests, scans, and publishes container images to GHCR. Covers GitHub Actions workflow, test execution in CI, Docker image build+push, and vulnerability scanning. Deployment to K8s is Phase 17. Dockerfiles are from Phase 12.

</domain>

<decisions>
## Implementation Decisions

### Workflow triggers & structure
- Single workflow file (`.github/workflows/ci.yml`)
- Triggers on merge to master only — no PR checks
- Concurrency control: cancel in-flight runs when a new merge lands (concurrency group)
- Add CI status badge to README.md

### Job structure & dependencies
- Parallel test jobs: `test-backend` and `test-frontend` run simultaneously
- Sequential gating: `build-push` job depends on both test jobs passing — no images pushed for broken code
- `scan` job runs after `build-push` completes
- Flow: `[test-backend, test-frontend]` -> `build-push` -> `scan`

### Test scope
- Backend: `./gradlew :backend:test` with JDK 24 (setup-java, Temurin distribution)
- Frontend: `pnpm test:ci` (unit/component tests with verbose reporter, no service containers needed)
- No SMTP service container needed — email tests are unit tests with MockK; EmailService graceful degradation prevents failures even in integration tests
- No linting/formatting checks in CI — stays local
- No test artifacts or coverage uploads — pass/fail visible in job logs

### Image naming & tagging
- Backend: `ghcr.io/baalexandru/jobhunt-backend`
- Frontend: `ghcr.io/baalexandru/jobhunt-frontend`
- Tags per build: short Git SHA (e.g., `sha-abc1234`), date stamp (e.g., `2026-03-22`), and `latest`
- GHCR packages are public — no imagePullSecrets needed on K8s cluster
- No automatic image cleanup — GHCR free tier is generous for public repos
- No image size enforcement in CI — 200MB target validated in Phase 12

### Vulnerability scanning
- Scanner: Trivy (aquasecurity/trivy-action)
- Scan both backend and frontend images
- Report only — never fail the build on CVEs (avoids false-positive blocking on personal project)
- Results surfaced as workflow summary table
- Image CVEs only — no Dockerfile misconfiguration scanning
- Scanning happens after images are pushed to GHCR

### Caching
- Cache Gradle dependencies (actions/cache)
- Cache pnpm store
- Cache Docker layers (docker/build-push-action with GitHub Actions cache)

### Claude's Discretion
- Frontend `NEXT_PUBLIC_API_URL` build-time strategy — Claude picks best approach given Next.js constraints and K8s deployment patterns
- Exact GitHub Actions versions for actions (checkout, setup-java, setup-node, docker/build-push-action, etc.)
- Exact Trivy action configuration (severity filter, output format)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Docker build configuration
- `backend/Dockerfile` — Multi-stage Spring Boot build (Temurin JDK 24, layer extraction, JRE runtime)
- `frontend/Dockerfile` — Multi-stage Next.js build (Node 22 Alpine, standalone output)
- `.planning/phases/12-production-docker-images/12-CONTEXT.md` — Phase 12 decisions: build context paths, base images, JVM tuning flags

### Build configuration
- `backend/build.gradle.kts` — Gradle build with Java 24 toolchain, all dependencies
- `settings.gradle.kts` — Root settings for monorepo Gradle build
- `frontend/package.json` — Frontend dependencies, build/test scripts, pnpm config

### Test configuration
- `backend/src/test/resources/application.yml` — Test config with explicit localhost connections (postgres:5432, redis:6379, minio:9000, mail:1025)
- `compose.yaml` — Service definitions (postgres:17, redis:7-alpine, minio) for port/config reference

### Email & storage (from Phase 10/12)
- `backend/src/main/kotlin/.../service/EmailService.kt` — Try/catch graceful degradation; no SMTP = logged error, not a test failure
- `backend/src/test/kotlin/.../service/EmailServiceTests.kt` — Pure unit test with MockK, does NOT connect to SMTP
- `backend/src/test/kotlin/.../service/PasswordResetServiceTests.kt` — Pure unit test with MockK, does NOT connect to SMTP
- `backend/src/main/kotlin/.../config/StorageConfig.kt` — `ensureBucketExists` CommandLineRunner auto-creates MinIO bucket on app startup
- `compose.prod.yaml` — Local production testing compose (Phase 12); missing SMTP env vars but not used in CI

### Requirements
- `.planning/REQUIREMENTS.md` — DOCK-03 (GitHub Actions builds, tests, pushes to GHCR), DOCK-04 (vulnerability scanning)
- `.planning/ROADMAP.md` §Phase 13 — Success criteria and dependency on Phase 12

### Module conventions
- `backend/CLAUDE.md` — Kotlin/Spring Boot patterns, test commands
- `frontend/CLAUDE.md` — Next.js patterns, pnpm, test commands

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `backend/Dockerfile` — Already exists with multi-stage build, Eclipse Temurin JDK 24 Alpine builder + JRE 24 Alpine runtime, Spring Boot layer extraction, JVM tuning flags, non-root user, HEALTHCHECK
- `frontend/Dockerfile` — Already exists with multi-stage build, Node 22 Alpine, Next.js standalone output, non-root user, HEALTHCHECK
- `compose.yaml` — Service definitions provide reference for CI service container config (postgres:17, redis:7-alpine, minio)
- `backend/src/test/resources/application.yml` — Test config connects to localhost with fixed ports, skips Docker Compose auto-discovery
- `backend/src/main/kotlin/.../config/StorageConfig.kt` — `ensureBucketExists` CommandLineRunner auto-creates MinIO bucket on startup — no CI setup step needed
- `spring-boot-starter-mail` already in build.gradle.kts — EmailService with try/catch graceful degradation (no SMTP = logged error, not a crash)

### Established Patterns
- All backend tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)` — full integration tests requiring PostgreSQL, Redis, MinIO
- Test config uses `spring.docker.compose.skip.in-tests: true` — expects services already running (matches CI service container approach)
- Email tests (`EmailServiceTests`, `PasswordResetServiceTests`) are pure unit tests using MockK — they do NOT connect to localhost:1025 and require no SMTP service container
- EmailService has try/catch graceful degradation — integration tests that load full Spring context won't fail even without an SMTP server (JavaMailSender doesn't eagerly connect)
- MinIO bucket auto-creation already implemented via `StorageConfig.ensureBucketExists` CommandLineRunner — CI just needs MinIO service container running, bucket is created on app startup
- `textValue()` deprecation warnings (Jackson JsonNode) appear across 26+ test locations (TestHelper.kt, all integration tests) — cosmetic, doesn't fail tests but will appear in CI logs
- Frontend tests use Vitest + React Testing Library — pure unit/component tests, no backend dependencies
- Frontend has `test:ci` script (`vitest run --reporter=verbose`) — preferable over `pnpm test` for CI verbose output

### Integration Points
- GHCR images (named here) will be referenced by K8s Kustomize manifests in Phase 15
- ArgoCD (Phase 17) will watch GHCR for new images — tagging convention (SHA + date + latest) must align
- Backend Docker build context is project root (`docker build -f backend/Dockerfile .`), frontend context is `frontend/`

</code_context>

<specifics>
## Specific Ideas

- User chose SHA + date + latest tagging — wants both traceability (SHA) and at-a-glance age identification (date)
- User wants a README badge for CI status — visual indicator of build health
- Email tests already use MockK (no SMTP refactoring needed) and EmailService has graceful degradation — CI needs no SMTP service container at all
- MinIO bucket auto-creation already implemented in StorageConfig CommandLineRunner — CI just needs MinIO service container, bucket is auto-created on app startup
- compose.prod.yaml (Phase 12) is missing SMTP env vars (MAIL_FROM, FRONTEND_BASE_URL, INTERNAL_API_SECRET) — not a CI blocker since CI doesn't use compose.prod.yaml, but noted for completeness
- textValue() deprecation warnings (26+ occurrences in test suite) will appear in CI logs — cosmetic only, tests still pass

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 13-ci-pipeline*
*Context gathered: 2026-03-22*
