---
phase: 15-k3s-cluster-setup
plan: 02
subsystem: infra
tags: [kubernetes, kustomize, k3s, namespaces, limitrange, deployments, statefulsets]

requires:
  - phase: 14-aws-infrastructure
    provides: EC2 instance with 2GB RAM constraint for resource sizing
provides:
  - Namespace definitions for jobhunt-staging and jobhunt-prod
  - LimitRange safety nets for both namespaces
  - Kustomize base manifests for all 5 application components
  - StatefulSet definitions for PostgreSQL and MinIO with persistent storage
  - Deployment definitions for backend, frontend, and Redis
  - Headless and ClusterIP service definitions
affects: [15-03-kustomize-overlays, 16-data-store-deployment, 17-app-deployment]

tech-stack:
  added: []
  patterns: [kustomize-base-overlay, statefulset-with-headless-service, namespace-isolation]

key-files:
  created:
    - infra/k8s/namespaces/namespaces.yaml
    - infra/k8s/namespaces/limitrange-staging.yaml
    - infra/k8s/namespaces/limitrange-prod.yaml
    - infra/k8s/base/kustomization.yaml
    - infra/k8s/base/backend/deployment.yaml
    - infra/k8s/base/backend/service.yaml
    - infra/k8s/base/frontend/deployment.yaml
    - infra/k8s/base/frontend/service.yaml
    - infra/k8s/base/postgres/statefulset.yaml
    - infra/k8s/base/postgres/service.yaml
    - infra/k8s/base/redis/deployment.yaml
    - infra/k8s/base/redis/service.yaml
    - infra/k8s/base/minio/statefulset.yaml
    - infra/k8s/base/minio/service.yaml
  modified: []

key-decisions:
  - "Namespaces kept outside Kustomize overlays to avoid namespace transformer gotcha"
  - "LimitRange identical for staging and prod (safety net, not enforcement)"

patterns-established:
  - "StatefulSets paired with headless services (clusterIP: None) for stable DNS"
  - "Base manifests use placeholder image names for Kustomize images transformer"
  - "envFrom with configMapRef/secretRef for environment injection"
  - "automountServiceAccountToken: false on all app pods"

requirements-completed: [K8S-03, K8S-04]

duration: 3min
completed: 2026-03-23
---

# Phase 15 Plan 02: Namespaces & Base Manifests Summary

**Kubernetes namespace definitions with LimitRange safety nets and complete Kustomize base manifests for 5 components (backend, frontend, PostgreSQL, Redis, MinIO) sized for 2GB node**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-23T14:57:51Z
- **Completed:** 2026-03-23T15:00:37Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- Created jobhunt-staging and jobhunt-prod namespace definitions with environment labels
- Added LimitRange safety nets to both namespaces (default 256Mi/250m, max 512Mi/1 CPU)
- Built complete Kustomize base with 11 manifest files across 5 component subdirectories
- PostgreSQL and MinIO StatefulSets with volumeClaimTemplates using local-path StorageClass
- Backend deployment with Spring Boot liveness/readiness probes against actuator endpoints
- MinIO uses httpGet health probes (not mc ready) per pitfall guidance

## Task Commits

Each task was committed atomically:

1. **Task 1: Create namespace definitions and LimitRange safety nets** - `f56708a` (feat)
2. **Task 2: Create Kustomize base manifests for all 5 components** - `0f76c4e` (feat)

## Files Created/Modified

- `infra/k8s/namespaces/namespaces.yaml` - jobhunt-staging and jobhunt-prod namespace definitions
- `infra/k8s/namespaces/limitrange-staging.yaml` - LimitRange for staging namespace
- `infra/k8s/namespaces/limitrange-prod.yaml` - LimitRange for production namespace
- `infra/k8s/base/kustomization.yaml` - Base kustomization referencing all 10 resource files
- `infra/k8s/base/backend/deployment.yaml` - Spring Boot deployment with health probes and envFrom
- `infra/k8s/base/backend/service.yaml` - ClusterIP service on port 8080
- `infra/k8s/base/frontend/deployment.yaml` - Next.js deployment with probes
- `infra/k8s/base/frontend/service.yaml` - ClusterIP service on port 3000
- `infra/k8s/base/postgres/statefulset.yaml` - PostgreSQL StatefulSet with 10Gi PVC
- `infra/k8s/base/postgres/service.yaml` - Headless service for StatefulSet DNS
- `infra/k8s/base/redis/deployment.yaml` - Redis deployment with exec probes
- `infra/k8s/base/redis/service.yaml` - ClusterIP service on port 6379
- `infra/k8s/base/minio/statefulset.yaml` - MinIO StatefulSet with 10Gi PVC
- `infra/k8s/base/minio/service.yaml` - Headless service with API (9000) and console (9001) ports

## Decisions Made

- Namespaces kept outside Kustomize overlays to avoid the namespace transformer gotcha (Kustomize would incorrectly apply overlay namespace to cluster-scoped Namespace resources)
- LimitRange specs identical for staging and prod since they serve as safety nets, not enforcement

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Base manifests ready for overlay customization in plan 15-03
- Kustomize build validates successfully via `kubectl kustomize infra/k8s/base`
- Resource limits align with ROADMAP memory budget for t3.small (2GB)

## Self-Check: PASSED

- All 14 files verified present on disk
- Both task commits (f56708a, 0f76c4e) verified in git log

---
*Phase: 15-k3s-cluster-setup*
*Completed: 2026-03-23*
