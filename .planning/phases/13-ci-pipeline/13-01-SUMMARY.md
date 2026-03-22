---
phase: 13-ci-pipeline
plan: 01
subsystem: infra
tags: [github-actions, docker, ghcr, trivy, ci-cd]

requires:
  - phase: 12-dockerize
    provides: Dockerfiles for backend and frontend
provides:
  - Complete GitHub Actions CI workflow (test, build-push, scan)
  - Container images published to ghcr.io on merge to master
  - Trivy vulnerability scan results in workflow summary
  - CI status badge in README.md
affects: [14-k8s-manifests, 17-deploy-pipeline]

tech-stack:
  added: [github-actions, trivy, docker-metadata-action, docker-build-push-action]
  patterns: [SHA+date+latest image tagging, GHA cache for Docker builds, service containers for integration tests]

key-files:
  created:
    - .github/workflows/ci.yml
    - README.md
  modified: []

key-decisions:
  - "MinIO health check uses curl against /minio/health/live instead of mc ready local (mc CLI unavailable in GH Actions service containers)"
  - "Trivy exit-code 0 (report-only) to avoid blocking builds on upstream CVEs"
  - "Frontend build-arg NEXT_PUBLIC_API_URL=https://job-hunt.dev/api baked at build time for single-domain Traefik routing"

patterns-established:
  - "CI workflow structure: test jobs in parallel -> build-push -> scan"
  - "Image tagging: type=sha + type=raw date + type=raw latest with flavor latest=false"
  - "GHA scoped caching: cache-from/cache-to with type=gha,scope={service}"

requirements-completed: [DOCK-03, DOCK-04]

duration: 3min
completed: 2026-03-23
---

# Phase 13 Plan 01: CI Pipeline Summary

**GitHub Actions CI workflow with 4-job pipeline: parallel backend/frontend tests, Docker build-push to GHCR with SHA+date+latest tags, and Trivy vulnerability scanning**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-22T23:30:53Z
- **Completed:** 2026-03-22T23:34:13Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Complete CI workflow with test-backend (PostgreSQL 17, Redis 7, MinIO service containers), test-frontend (pnpm + Vitest), build-push (GHCR with metadata tagging), and scan (Trivy) jobs
- Docker images tagged with short SHA, UTC date stamp, and latest on default branch pushes
- GHA-scoped Docker layer caching for both backend and frontend builds
- README.md with CI status badge linking to the workflow

## Task Commits

Each task was committed atomically:

1. **Task 1: Create GitHub Actions CI workflow** - `20365f6` (feat)
2. **Task 2: Create README.md with CI status badge** - `5a0298a` (feat)

## Files Created/Modified
- `.github/workflows/ci.yml` - Complete CI pipeline with 4 jobs (test-backend, test-frontend, build-push, scan)
- `README.md` - Project description with CI status badge

## Decisions Made
- MinIO health check uses `curl -f http://localhost:9000/minio/health/live` instead of `mc ready local` since mc CLI is not available in GH Actions service containers
- Trivy uses `exit-code: '0'` (report-only mode) to surface vulnerabilities without blocking builds on upstream CVEs
- Frontend image built with `NEXT_PUBLIC_API_URL=https://job-hunt.dev/api` baked at build time, appropriate for single-domain Traefik routing setup

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required. GHCR authentication uses the built-in `GITHUB_TOKEN` secret.

## Next Phase Readiness
- CI workflow ready to trigger on first merge to master
- Images will be available at ghcr.io/baalexandru/jobhunt-backend and ghcr.io/baalexandru/jobhunt-frontend
- Phase 14 (K8s manifests) can reference these image paths for deployment specs

---
*Phase: 13-ci-pipeline*
*Completed: 2026-03-23*
