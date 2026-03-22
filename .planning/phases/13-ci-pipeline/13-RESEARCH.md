# Phase 13: CI Pipeline - Research

**Researched:** 2026-03-22
**Domain:** GitHub Actions CI/CD, Docker image build/push, vulnerability scanning
**Confidence:** HIGH

## Summary

Phase 13 implements a GitHub Actions CI workflow triggered on merge to master. The workflow runs backend integration tests (Gradle + PostgreSQL/Redis/MinIO service containers) and frontend unit tests (Vitest) in parallel, then builds and pushes Docker images to GHCR with SHA + date + latest tags, followed by Trivy vulnerability scanning with results surfaced in the workflow summary.

The project already has all prerequisites in place: multi-stage Dockerfiles for both backend and frontend (Phase 12), test suites that run against localhost services, and a test application.yml that skips Docker Compose auto-discovery. The CI workflow is a single new file (`.github/workflows/ci.yml`) plus a README badge update.

**Primary recommendation:** Use GitHub Actions service containers for PostgreSQL, Redis, and MinIO (matching compose.yaml versions), `gradle/actions/setup-gradle` for Gradle caching, `docker/metadata-action` + `docker/build-push-action` for image tagging/pushing, and `aquasecurity/trivy-action` with the GitHub markdown template for workflow summary output.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Single workflow file (`.github/workflows/ci.yml`)
- Triggers on merge to master only -- no PR checks
- Concurrency control: cancel in-flight runs when a new merge lands
- Add CI status badge to README.md
- Parallel test jobs: `test-backend` and `test-frontend` run simultaneously
- Sequential gating: `build-push` job depends on both test jobs passing
- `scan` job runs after `build-push` completes
- Flow: `[test-backend, test-frontend]` -> `build-push` -> `scan`
- Backend: `./gradlew :backend:test` with JDK 24 (setup-java, Temurin distribution)
- Frontend: `pnpm test:ci` (unit/component tests with verbose reporter, no service containers needed)
- No SMTP service container needed
- No linting/formatting checks in CI
- No test artifacts or coverage uploads
- Backend image: `ghcr.io/baalexandru/jobhunt-backend`
- Frontend image: `ghcr.io/baalexandru/jobhunt-frontend`
- Tags: short Git SHA, date stamp (YYYYMMDD), and `latest`
- GHCR packages are public
- No automatic image cleanup
- No image size enforcement in CI
- Scanner: Trivy (aquasecurity/trivy-action)
- Report only -- never fail the build on CVEs
- Results surfaced as workflow summary table
- Image CVEs only -- no Dockerfile misconfiguration scanning
- Scanning happens after images are pushed to GHCR
- Cache Gradle dependencies, pnpm store, Docker layers (GHA cache)

### Claude's Discretion
- Frontend `NEXT_PUBLIC_API_URL` build-time strategy
- Exact GitHub Actions versions for actions
- Exact Trivy action configuration (severity filter, output format)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DOCK-03 | GitHub Actions pipeline builds, tests, and pushes images to GHCR on merge to master | Full workflow structure researched: service containers for backend tests, parallel test jobs, docker/build-push-action with metadata tagging, GHCR login |
| DOCK-04 | Container images are scanned for vulnerabilities in CI | Trivy-action configuration researched: report-only mode (exit-code 0), GitHub markdown template for workflow summary output |
</phase_requirements>

## Standard Stack

### Core Actions
| Action | Version | Purpose | Why Standard |
|--------|---------|---------|--------------|
| actions/checkout | v4 | Check out repository | Standard, required for all workflows |
| actions/setup-java | v4 | Install Temurin JDK 24 | Official action, supports Temurin distribution |
| actions/setup-node | v4 | Install Node.js + pnpm cache | Built-in pnpm cache support |
| pnpm/action-setup | v4 | Install pnpm | Official pnpm installer for GH Actions |
| gradle/actions/setup-gradle | v4 | Gradle caching + execution | Superior Gradle caching vs actions/cache; caches dependencies, wrapper, build cache |
| docker/setup-buildx-action | v3 | Enable BuildKit | Required for GHA layer caching and advanced builds |
| docker/login-action | v3 | Authenticate to GHCR | Standard registry auth with GITHUB_TOKEN |
| docker/metadata-action | v5 | Generate tags + OCI labels | SHA + date + latest tagging from Git context |
| docker/build-push-action | v6 | Build and push images | Industry standard, supports GHA cache backend |
| aquasecurity/trivy-action | v0.33.1 | Vulnerability scanning | De facto standard for container scanning in GH Actions |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| gradle/actions/setup-gradle | actions/cache + manual Gradle config | setup-gradle has smarter cache management, avoids stale caches |
| trivy-action | grype-action | Trivy is more widely adopted, better GH Actions integration |
| docker/metadata-action | Manual tag scripting | metadata-action handles edge cases (SHA length, branch detection, OCI labels) |

## Architecture Patterns

### Workflow Structure
```
.github/
  workflows/
    ci.yml          # Single CI workflow
```

### Job Dependency Graph
```
test-backend ----\
                  --> build-push --> scan
test-frontend ---/
```

### Pattern 1: Service Containers for Integration Tests
**What:** GitHub Actions service containers run PostgreSQL, Redis, and MinIO alongside the backend test job
**When to use:** Backend tests that require real database/cache/storage connections
**Example:**
```yaml
# Source: GitHub Docs + project compose.yaml
services:
  postgres:
    image: postgres:17
    env:
      POSTGRES_DB: jobhunt
      POSTGRES_USER: jobhunt
      POSTGRES_PASSWORD: jobhunt
    ports:
      - 5432:5432
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
  redis:
    image: redis:7-alpine
    ports:
      - 6379:6379
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
  minio:
    image: minio/minio:latest
    ports:
      - 9000:9000
    env:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    options: >-
      --health-cmd "curl -f http://localhost:9000/minio/health/live || exit 1"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 10
```

**Key detail:** The test `application.yml` already connects to `localhost:5432`, `localhost:6379`, `localhost:9000` with `spring.docker.compose.skip.in-tests: true`, so service containers map directly. No config changes needed.

**MinIO note:** MinIO service container requires the `server /data` command. Use the full command override or the minio/minio image which defaults to server mode. The `mc ready local` healthcheck from compose.yaml will not work in GH Actions service containers (no `mc` CLI inside the image); use the HTTP health endpoint instead.

### Pattern 2: Metadata-Driven Image Tagging
**What:** Use `docker/metadata-action` to generate SHA + date + latest tags from Git context
**When to use:** Every image build
**Example:**
```yaml
# Source: docker-build-push skill
- name: Docker metadata (backend)
  id: meta-backend
  uses: docker/metadata-action@v5
  with:
    images: ghcr.io/baalexandru/jobhunt-backend
    flavor: |
      latest=false
    tags: |
      type=sha
      type=raw,value={{date 'YYYYMMDD' tz='UTC'}},enable={{is_default_branch}}
      type=raw,value=latest,enable={{is_default_branch}}
```

### Pattern 3: Trivy Scan with Workflow Summary
**What:** Scan pushed images and output results to GH Actions step summary
**When to use:** After images are pushed to GHCR
**Example:**
```yaml
# Source: trivy-action README + trivy docs
- name: Scan backend image
  uses: aquasecurity/trivy-action@0.33.1
  with:
    scan-type: image
    image-ref: ghcr.io/baalexandru/jobhunt-backend:sha-${{ github.sha }}
    format: table
    output: trivy-backend.txt
    severity: CRITICAL,HIGH,MEDIUM
    exit-code: '0'
- name: Backend scan summary
  run: |
    echo "## Backend Image Scan" >> $GITHUB_STEP_SUMMARY
    echo '```' >> $GITHUB_STEP_SUMMARY
    cat trivy-backend.txt >> $GITHUB_STEP_SUMMARY
    echo '```' >> $GITHUB_STEP_SUMMARY
```

### Pattern 4: NEXT_PUBLIC_API_URL Build-Time Strategy (Claude's Discretion)
**What:** Handle the `NEXT_PUBLIC_API_URL` environment variable that gets baked into the JS bundle at build time
**Recommendation:** Use a relative URL (`/api`) as the build-time value, and configure the K8s ingress (Traefik) to route `/api/*` to the backend service. This is the simplest approach that produces a single image deployable to any environment.

**Rationale:**
- `NEXT_PUBLIC_*` variables are inlined into the JavaScript bundle during `next build` -- they cannot be changed at runtime
- The frontend Dockerfile already has `ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api` as the default
- For production/K8s: set the build arg to the production URL (`https://job-hunt.dev/api`) or use a relative path (`/api`) if the frontend and backend share a domain
- Since the project uses a single domain (`job-hunt.dev`) with Traefik ingress routing, a relative `/api` path is cleanest
- CI should pass `--build-arg NEXT_PUBLIC_API_URL=https://job-hunt.dev/api` to produce the production image

**Alternative considered:** Runtime env injection via `window.__RUNTIME_ENV__` -- overly complex for a single-deployment target. The user has one domain, so build-time baking is fine.

### Anti-Patterns to Avoid
- **Hardcoding tags:** Never construct image tags manually; always use `docker/metadata-action` for consistent tagging
- **Running Docker Compose in CI:** Service containers are the GH Actions native approach; Docker Compose adds complexity and startup time
- **Caching everything:** Don't cache the Gradle build output directory -- `setup-gradle` handles this optimally
- **Using `actions/cache` for Gradle:** `gradle/actions/setup-gradle` has smarter cache management with automatic cleanup

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Image tagging | Shell script to generate SHA/date tags | `docker/metadata-action@v5` | Handles edge cases, OCI labels, Handlebars expressions |
| Gradle caching | Manual `actions/cache` with Gradle paths | `gradle/actions/setup-gradle@v4` | Smarter cache key generation, automatic cleanup, avoids stale caches |
| GHCR authentication | Manual `docker login` command | `docker/login-action@v3` | Secure credential handling, works with GITHUB_TOKEN |
| Container scanning | Manual `trivy` CLI install + run | `aquasecurity/trivy-action@0.33.1` | Handles DB downloads, caching, output formatting |
| pnpm installation | Manual `npm install -g pnpm` | `pnpm/action-setup@v4` | Reads packageManager from package.json, proper corepack handling |

## Common Pitfalls

### Pitfall 1: MinIO Service Container Health Check
**What goes wrong:** MinIO healthcheck using `mc ready local` (from compose.yaml) fails in GH Actions because `mc` is not installed in the minio/minio image by default
**Why it happens:** The compose.yaml healthcheck assumes mc CLI is available; GH Actions service container `options` run inside the container
**How to avoid:** Use HTTP healthcheck: `curl -f http://localhost:9000/minio/health/live || exit 1`
**Warning signs:** MinIO service shows as unhealthy, tests fail with connection refused on port 9000

### Pitfall 2: Backend Build Context Path
**What goes wrong:** Docker build fails because the backend Dockerfile expects root project context (for Gradle wrapper, settings.gradle.kts)
**Why it happens:** Backend Dockerfile COPYs `gradlew`, `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, and `backend/` from root context
**How to avoid:** Set `context: .` (repository root) and `file: ./backend/Dockerfile` in build-push-action. Frontend uses `context: ./frontend` and `file: ./frontend/Dockerfile`
**Warning signs:** COPY commands fail with "file not found" errors

### Pitfall 3: Trivy Scanning GHCR Images Requires Authentication
**What goes wrong:** Trivy cannot pull the image from GHCR to scan it
**Why it happens:** Even for public packages, recently pushed images may not be immediately public, or the scan job may not have registry access
**How to avoid:** Either scan locally before push (using `load: true`) OR ensure the scan job has `packages: read` permission and runs `docker/login-action` first. Since user chose "scan after push," ensure GHCR login in the scan job.
**Warning signs:** Trivy fails with "image not found" or authentication errors

### Pitfall 4: Concurrency Group Cancelling the Only Run
**What goes wrong:** Concurrency group set too aggressively cancels legitimate runs
**Why it happens:** If `cancel-in-progress: true` is set without a proper group key, rapid merges cancel earlier builds
**How to avoid:** Use `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }` -- this is correct for a single-branch (master) trigger. Rapid merges will cancel in-flight runs, which is the desired behavior per user decision.

### Pitfall 5: NEXT_PUBLIC_API_URL Baked as localhost
**What goes wrong:** Production frontend image makes API calls to localhost:8080 instead of the production backend
**Why it happens:** The Dockerfile default `ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api` is used if no build-arg override is provided in CI
**How to avoid:** Pass `--build-arg NEXT_PUBLIC_API_URL=https://job-hunt.dev/api` in the build-push-action
**Warning signs:** Frontend in production shows network errors, API calls go to localhost

### Pitfall 6: Short SHA Tag Mismatch
**What goes wrong:** Trivy scan step references an image tag that doesn't match what was pushed
**Why it happens:** `docker/metadata-action` generates `sha-XXXXXXX` (7-char) while `github.sha` is the full 40-char SHA
**How to avoid:** Reference the image using the `steps.meta-backend.outputs.version` or use `${{ steps.meta-backend.outputs.tags }}` to get the exact tags. Alternatively, use the digest output from build-push-action.
**Warning signs:** Trivy scan fails with "manifest unknown" error

## Code Examples

### Complete Workflow Structure (verified from project skills + official docs)
```yaml
name: CI

on:
  push:
    branches: [master]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
  packages: write

jobs:
  test-backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_DB: jobhunt
          POSTGRES_USER: jobhunt
          POSTGRES_PASSWORD: jobhunt
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      minio:
        image: minio/minio:latest
        ports:
          - 9000:9000
        env:
          MINIO_ROOT_USER: minioadmin
          MINIO_ROOT_PASSWORD: minioadmin
        options: >-
          --health-cmd "curl -f http://localhost:9000/minio/health/live || exit 1"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 10
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '24'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :backend:test

  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: pnpm
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: pnpm --dir frontend install --frozen-lockfile
      - run: pnpm --dir frontend test:ci

  build-push:
    needs: [test-backend, test-frontend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.repository_owner }}
          password: ${{ secrets.GITHUB_TOKEN }}
      # Backend metadata + build
      - uses: docker/metadata-action@v5
        id: meta-backend
        with:
          images: ghcr.io/baalexandru/jobhunt-backend
          flavor: latest=false
          tags: |
            type=sha
            type=raw,value={{date 'YYYYMMDD' tz='UTC'}},enable={{is_default_branch}}
            type=raw,value=latest,enable={{is_default_branch}}
      - uses: docker/build-push-action@v6
        with:
          context: .
          file: ./backend/Dockerfile
          push: true
          tags: ${{ steps.meta-backend.outputs.tags }}
          labels: ${{ steps.meta-backend.outputs.labels }}
          cache-from: type=gha,scope=backend
          cache-to: type=gha,scope=backend,mode=max
      # Frontend metadata + build
      - uses: docker/metadata-action@v5
        id: meta-frontend
        with:
          images: ghcr.io/baalexandru/jobhunt-frontend
          flavor: latest=false
          tags: |
            type=sha
            type=raw,value={{date 'YYYYMMDD' tz='UTC'}},enable={{is_default_branch}}
            type=raw,value=latest,enable={{is_default_branch}}
      - uses: docker/build-push-action@v6
        with:
          context: ./frontend
          file: ./frontend/Dockerfile
          push: true
          tags: ${{ steps.meta-frontend.outputs.tags }}
          labels: ${{ steps.meta-frontend.outputs.labels }}
          build-args: |
            NEXT_PUBLIC_API_URL=https://job-hunt.dev/api
          cache-from: type=gha,scope=frontend
          cache-to: type=gha,scope=frontend,mode=max

  scan:
    needs: [build-push]
    runs-on: ubuntu-latest
    steps:
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.repository_owner }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Scan backend image
        uses: aquasecurity/trivy-action@0.33.1
        with:
          scan-type: image
          image-ref: ghcr.io/baalexandru/jobhunt-backend:latest
          format: table
          output: trivy-backend.txt
          severity: CRITICAL,HIGH,MEDIUM
          exit-code: '0'
      - name: Scan frontend image
        uses: aquasecurity/trivy-action@0.33.1
        with:
          scan-type: image
          image-ref: ghcr.io/baalexandru/jobhunt-frontend:latest
          format: table
          output: trivy-frontend.txt
          severity: CRITICAL,HIGH,MEDIUM
          exit-code: '0'
      - name: Add scan results to summary
        if: always()
        run: |
          echo "## Vulnerability Scan Results" >> $GITHUB_STEP_SUMMARY
          echo "### Backend Image" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          cat trivy-backend.txt 2>/dev/null >> $GITHUB_STEP_SUMMARY || echo "Scan output not available" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          echo "### Frontend Image" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          cat trivy-frontend.txt 2>/dev/null >> $GITHUB_STEP_SUMMARY || echo "Scan output not available" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
```

### CI Status Badge (for README.md)
```markdown
[![CI](https://github.com/BaAlexandru/job-hunt/actions/workflows/ci.yml/badge.svg)](https://github.com/BaAlexandru/job-hunt/actions/workflows/ci.yml)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `actions/cache` for Gradle | `gradle/actions/setup-gradle@v4` | 2024 | Smarter caching, auto cleanup, dependency graph |
| Manual `docker build && docker push` | `docker/build-push-action@v6` | 2024 | BuildKit caching, multi-platform, metadata integration |
| `actions/setup-java@v3` | `actions/setup-java@v4` | 2024 | Better cache support, newer JDK versions |
| `pnpm/action-setup@v2` | `pnpm/action-setup@v4` | 2025 | Reads packageManager from package.json automatically |
| trivy `--format template` with custom tpl | `--format table` + `$GITHUB_STEP_SUMMARY` | 2024 | Simpler approach for workflow summaries |

## Open Questions

1. **JDK 24 Availability on Temurin**
   - What we know: setup-java@v4 supports Temurin. JDK 24 was released March 2025. The project uses JDK 24 locally.
   - What's unclear: Whether Temurin has JDK 24 builds available through setup-java. JDK 24 is not an LTS release.
   - Recommendation: Use `java-version: '24'` in setup-java. If it fails, the CI run will clearly show the error. Fallback: JDK 24-ea or use the exact Temurin version string.

2. **MinIO Service Container Command Override**
   - What we know: minio/minio image needs `server /data` command to start the S3 API
   - What's unclear: Whether GH Actions service containers automatically run the image's default CMD, or if an explicit command is needed
   - Recommendation: The minio/minio image default entrypoint should work. If not, GH Actions does not support the `command` field in service containers -- the container's default CMD is used. If MinIO doesn't start correctly, use a setup step to start it via `docker run` instead.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | GitHub Actions (workflow execution) |
| Config file | `.github/workflows/ci.yml` |
| Quick run command | `gh workflow run ci.yml` (manual trigger would need `workflow_dispatch`) |
| Full suite command | Push/merge to master triggers the workflow |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DOCK-03 | Merge to master triggers build+test+push | smoke | `gh run list --workflow=ci.yml --limit 1` (verify run exists after merge) | N/A - workflow file |
| DOCK-03 | Tests fail = build fails (no images pushed) | manual | Intentionally break a test, verify build-push job skipped | N/A - manual verification |
| DOCK-04 | Container images scanned, results in workflow summary | smoke | `gh run view <id>` (check scan job completed) | N/A - workflow file |

### Sampling Rate
- **Per task commit:** Verify YAML syntax with `actionlint` if available, otherwise visual review
- **Per wave merge:** Merge the PR to master and verify the CI workflow triggers and completes
- **Phase gate:** At least one successful end-to-end CI run (tests pass, images pushed, scans complete)

### Wave 0 Gaps
- [ ] `.github/workflows/ci.yml` -- the entire workflow file (does not exist yet)
- [ ] README.md badge -- needs CI badge added

## Sources

### Primary (HIGH confidence)
- Project skill: `docker-build-push` -- SHA + date + latest tagging strategy, action versions, complete workflow example
- Project skill: `trivy-scanning` -- Trivy CLI flags, CI/CD integration patterns, output formats
- Project files: `backend/Dockerfile`, `frontend/Dockerfile`, `compose.yaml`, `backend/src/test/resources/application.yml` -- actual project configuration
- [GitHub Docs: Creating PostgreSQL service containers](https://docs.github.com/actions/guides/creating-postgresql-service-containers) -- service container syntax
- [GitHub Docs: Using containerized services](https://docs.github.com/actions/using-containerized-services) -- service container architecture

### Secondary (MEDIUM confidence)
- [gradle/actions/setup-gradle docs](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md) -- Gradle caching strategy
- [pnpm CI docs](https://pnpm.io/continuous-integration) -- pnpm + setup-node caching
- [aquasecurity/trivy-action](https://github.com/aquasecurity/trivy-action) -- trivy-action inputs, version 0.33.1
- [docker/metadata-action@v5](https://github.com/docker/metadata-action) -- tag types, Handlebars expressions
- [Next.js env vars docs](https://nextjs.org/docs/pages/guides/environment-variables) -- NEXT_PUBLIC build-time inlining behavior

### Tertiary (LOW confidence)
- JDK 24 availability via setup-java Temurin -- not explicitly confirmed, inferred from JDK 25 support in search results

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All actions are well-documented, versions verified from official repos and project skills
- Architecture: HIGH - Job dependency graph, service containers, and tagging strategy are directly from user decisions + official docs
- Pitfalls: HIGH - Identified from real project configuration (build context paths, MinIO healthcheck, NEXT_PUBLIC baking)

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (stable domain -- GitHub Actions, Docker actions, Trivy all mature)
