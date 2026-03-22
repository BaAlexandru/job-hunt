---
phase: 12-production-docker-images
plan: 02
subsystem: infra
tags: [docker, next.js, standalone, node-alpine, compose]

requires:
  - phase: 12-production-docker-images (plan 01)
    provides: backend Dockerfile pattern and conventions
provides:
  - frontend production Docker image (three-stage, node:22-alpine, standalone output)
  - compose.prod.yaml for local production testing of full stack
affects: [13-github-container-registry, 14-kubernetes-manifests, 16-ci-cd-pipeline]

tech-stack:
  added: [node:22-alpine, next.js-standalone-output]
  patterns: [three-stage-docker-build, compose-override-files, docker-network-hostnames]

key-files:
  created:
    - frontend/Dockerfile
    - frontend/.dockerignore
    - frontend/public/.gitkeep
    - compose.prod.yaml
  modified:
    - frontend/next.config.ts

key-decisions:
  - "Created public/.gitkeep to ensure COPY --from=builder works when public/ directory is empty"
  - "Used compose override pattern (compose.prod.yaml extends compose.yaml) rather than standalone file"

patterns-established:
  - "Frontend Docker: three-stage build (deps, builder, runner) with standalone output"
  - "Compose override: compose.prod.yaml extends compose.yaml for local production testing"
  - "Docker network hostnames: use service names (postgres, redis, minio) not localhost"

requirements-completed: [DOCK-02]

duration: 5min
completed: 2026-03-22
---

# Phase 12 Plan 02: Frontend Docker Image & Compose Summary

**Three-stage frontend Docker image (70MB, node:22-alpine) with standalone output and compose.prod.yaml for full-stack local production testing**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-22T14:10:09Z
- **Completed:** 2026-03-22T14:15:21Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Frontend Docker image builds at ~70MB (well under 200MB target) using three-stage build
- Container runs as non-root `app` user with HEALTHCHECK
- compose.prod.yaml enables full-stack local production testing with correct Docker network hostnames

## Task Commits

Each task was committed atomically:

1. **Task 1: Add standalone output to next.config.ts and create frontend Dockerfile** - `17d86af` (feat)
2. **Task 2: Create compose.prod.yaml and update infra/CLAUDE.md** - `94692a2` (feat)

## Files Created/Modified
- `frontend/next.config.ts` - Added `output: "standalone"` for Docker-optimized builds
- `frontend/Dockerfile` - Three-stage build: deps (pnpm install), builder (next build), runner (node server.js)
- `frontend/.dockerignore` - Excludes .next/, node_modules/, tests, Docker files
- `frontend/public/.gitkeep` - Ensures public/ directory exists for Docker COPY
- `compose.prod.yaml` - Extends compose.yaml with backend and frontend app services

## Decisions Made
- Created `frontend/public/.gitkeep` because the project has no `public/` directory, which caused `COPY --from=builder /app/public` to fail. This is a minimal fix that works regardless of whether assets are added later.
- infra/CLAUDE.md already contained correct Dockerfile placement conventions from the context session, so no modifications were needed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created missing public/ directory**
- **Found during:** Task 1 (Frontend Dockerfile build)
- **Issue:** `COPY --from=builder --chown=app:app /app/public ./public` failed because frontend has no public/ directory
- **Fix:** Created `frontend/public/.gitkeep` to ensure the directory exists in the build context
- **Files modified:** frontend/public/.gitkeep (created)
- **Verification:** Docker build completed successfully after fix
- **Committed in:** 17d86af (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary fix for Docker build to succeed. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviation above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Both backend and frontend Docker images are ready for registry push (Phase 13)
- compose.prod.yaml can be used for full integration testing: `docker compose -f compose.yaml -f compose.prod.yaml up --build`
- Kubernetes manifests (Phase 14) can reference both images

---
*Phase: 12-production-docker-images*
*Completed: 2026-03-22*
