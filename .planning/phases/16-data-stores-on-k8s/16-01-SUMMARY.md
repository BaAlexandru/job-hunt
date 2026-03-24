---
phase: 16-data-stores-on-k8s
plan: 01
subsystem: infra
tags: [kubernetes, storage, statefulset, redis, postgres, minio, kustomize, persistence]

# Dependency graph
requires:
  - phase: 15-k3s-cluster-setup
    provides: Base K8s manifests with Kustomize overlays, PostgreSQL/MinIO StatefulSets, Redis Deployment
provides:
  - Custom StorageClass local-path-retain with reclaimPolicy Retain
  - Redis StatefulSet with RDB persistence replacing Deployment
  - Updated PostgreSQL/MinIO StatefulSets with retention policies
  - Two-layer data protection (StorageClass + StatefulSet retention policy)
affects: [16-data-stores-on-k8s, 17-deploy-pipeline]

# Tech tracking
tech-stack:
  added: []
  patterns: [persistentVolumeClaimRetentionPolicy for StatefulSet data protection, headless Service for StatefulSet DNS, RDB save policy for Redis persistence]

key-files:
  created:
    - infra/k8s/base/storage-class.yaml
    - infra/k8s/base/redis/statefulset.yaml
  modified:
    - infra/k8s/base/postgres/statefulset.yaml
    - infra/k8s/base/minio/statefulset.yaml
    - infra/k8s/base/redis/service.yaml
    - infra/k8s/base/kustomization.yaml

key-decisions:
  - "local-path-retain StorageClass is explicit-only (no default annotation) to avoid accidental use"
  - "Redis RDB save policy: 3600 1 300 100 60 10000 (hourly if 1 change, 5min if 100, 1min if 10000)"
  - "MinIO image pinned to RELEASE.2025-04-22T22-12-26Z for reproducibility"

patterns-established:
  - "Two-layer retention: StorageClass reclaimPolicy Retain + StatefulSet persistentVolumeClaimRetentionPolicy"
  - "Headless services (clusterIP: None) for all StatefulSets"

requirements-completed: [DATA-01, DATA-02, DATA-03]

# Metrics
duration: 3min
completed: 2026-03-24
---

# Phase 16 Plan 01: Storage Foundation Summary

**Custom StorageClass with Retain policy, Redis converted to StatefulSet with RDB persistence, PostgreSQL/MinIO updated with two-layer data retention**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-24T11:35:26Z
- **Completed:** 2026-03-24T11:38:20Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Created local-path-retain StorageClass with reclaimPolicy Retain and WaitForFirstConsumer binding
- Converted Redis from ephemeral Deployment to persistent StatefulSet with RDB save policy and 1Gi PVC
- Updated PostgreSQL and MinIO StatefulSets with local-path-retain storageClassName and persistentVolumeClaimRetentionPolicy
- Pinned MinIO image to specific release tag for reproducibility
- Both staging and prod kustomize overlays build cleanly

## Task Commits

Each task was committed atomically:

1. **Task 1: Create StorageClass and update PostgreSQL/MinIO StatefulSets** - `d919f0b` (feat)
2. **Task 2: Convert Redis from Deployment to StatefulSet with RDB persistence** - `1b3d5a5` (feat)

## Files Created/Modified
- `infra/k8s/base/storage-class.yaml` - Custom StorageClass with Retain reclaim policy
- `infra/k8s/base/redis/statefulset.yaml` - Redis StatefulSet with RDB persistence and 1Gi PVC (replaces deployment.yaml)
- `infra/k8s/base/redis/service.yaml` - Converted to headless service (clusterIP: None)
- `infra/k8s/base/postgres/statefulset.yaml` - Added retention policy, switched to local-path-retain
- `infra/k8s/base/minio/statefulset.yaml` - Added retention policy, switched to local-path-retain, pinned image
- `infra/k8s/base/kustomization.yaml` - Added storage-class.yaml resource, updated redis reference

## Decisions Made
- local-path-retain StorageClass is explicit-only (no is-default-class annotation) to prevent accidental use by unrelated workloads
- Redis RDB save policy uses graduated thresholds: hourly for low-traffic, 5min for moderate, 1min for high-traffic scenarios
- MinIO pinned to RELEASE.2025-04-22T22-12-26Z instead of :latest for deterministic deployments

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Storage foundation complete for all three data stores
- Ready for plan 16-02 (secrets, ConfigMaps, and deployment verification)
- All PVCs will use local-path-retain with two-layer retention protection

## Self-Check: PASSED

All 7 files verified present. Both commit hashes (d919f0b, 1b3d5a5) confirmed in git log.

---
*Phase: 16-data-stores-on-k8s*
*Completed: 2026-03-24*
