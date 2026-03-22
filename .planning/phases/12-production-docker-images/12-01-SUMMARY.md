---
phase: 12-production-docker-images
plan: 01
subsystem: infra
tags: [docker, multi-stage-build, eclipse-temurin, spring-boot, alpine, jre, layer-extraction]

# Dependency graph
requires:
  - phase: none
    provides: existing Spring Boot backend with Gradle build
provides:
  - "Production-ready backend Docker image (multi-stage, ~165MB)"
  - "Spring Boot layer extraction for optimal Docker layer caching"
  - "Non-root container execution (app user)"
  - ".dockerignore for lean build context"
affects: [13-ghcr-ci-pipeline, 14-kubernetes-manifests, compose-prod]

# Tech tracking
tech-stack:
  added: [eclipse-temurin-24-jre-alpine, docker-multi-stage-build]
  patterns: [spring-boot-layer-extraction, non-root-container, jvm-container-tuning]

key-files:
  created:
    - backend/Dockerfile
    - backend/.dockerignore
  modified: []

key-decisions:
  - "Used --chown on COPY instead of separate RUN chown to avoid layer duplication and reduce image by ~85MB"
  - "Eclipse Temurin 24 JRE Alpine for minimal runtime footprint (~165MB total)"
  - "Spring Boot jarmode=tools layer extraction for optimal Docker cache on dependency changes"

patterns-established:
  - "Multi-stage Dockerfile: builder (JDK) -> extractor (layer split) -> runtime (JRE)"
  - "Non-root app user via addgroup/adduser in Alpine"
  - "JVM container flags: MaxRAMPercentage=75%, UseContainerSupport, ExitOnOutOfMemoryError"
  - "HEALTHCHECK using wget to /actuator/health"

requirements-completed: [DOCK-01]

# Metrics
duration: 7min
completed: 2026-03-22
---

# Phase 12 Plan 01: Backend Dockerfile Summary

**Multi-stage Docker build with Eclipse Temurin 24 JRE Alpine, Spring Boot layer extraction, and non-root execution at ~165MB**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-22T14:09:58Z
- **Completed:** 2026-03-22T14:17:17Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Three-stage multi-stage Dockerfile: JDK builder, layer extractor, JRE runtime
- Spring Boot layer extraction for optimal Docker layer caching (dependencies cached separately from application code)
- Final image ~165MB, well under the 200MB target
- Non-root `app` user execution with proper ownership via --chown on COPY
- HEALTHCHECK on /actuator/health with wget
- JVM container-aware tuning flags for K8s deployment

## Task Commits

Each task was committed atomically:

1. **Task 1: Create backend Dockerfile with multi-stage build and layer extraction** - `744654a` (feat)

**Plan metadata:** [pending final commit]

## Files Created/Modified
- `backend/Dockerfile` - Three-stage multi-stage build: JDK builder, layer extractor, JRE Alpine runtime
- `backend/.dockerignore` - Excludes frontend/, .git/, build artifacts, planning docs, IDE files

## Decisions Made
- Used `--chown=app:app` on COPY directives instead of a separate `RUN chown -R` to avoid layer duplication. This reduced image size from ~250MB to ~165MB because Docker doesn't need to duplicate the dependencies layer for ownership changes.
- Kept the extractor as a separate stage (not combined with builder) for clarity and to allow the builder cache to be reused independently.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed image size exceeding 200MB target due to chown layer duplication**
- **Found during:** Task 1 (verification)
- **Issue:** `RUN chown -R app:app /application` in a separate layer caused Docker to duplicate the 94MB dependencies layer, inflating the image to ~250MB
- **Fix:** Replaced separate `RUN chown` with `--chown=app:app` on each `COPY --from=extractor` directive
- **Files modified:** backend/Dockerfile
- **Verification:** Image size dropped from 249MB to 165MB; `docker run --rm --entrypoint whoami` still outputs `app`
- **Committed in:** 744654a (part of Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug fix)
**Impact on plan:** Essential fix to meet the 200MB size target. No scope creep.

## Issues Encountered
None beyond the chown layer duplication addressed above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Backend Docker image ready for CI pipeline integration (Phase 13 GHCR push)
- Image tested locally: builds, runs as non-root, HEALTHCHECK configured
- Plan 12-02 (frontend Dockerfile or compose-prod) can proceed independently

---
*Phase: 12-production-docker-images*
*Completed: 2026-03-22*

## Self-Check: PASSED

- backend/Dockerfile: FOUND
- backend/.dockerignore: FOUND
- 12-01-SUMMARY.md: FOUND
- Commit 744654a: FOUND
