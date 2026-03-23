---
phase: 15-k3s-cluster-setup
plan: 03
subsystem: infra
tags: [kustomize, k8s, overlays, configmap, secrets, ingress, traefik]

requires:
  - phase: 15-02
    provides: "Kustomize base manifests for all 5 components"
provides:
  - "Staging Kustomize overlay with scale-to-zero (replicas: 0)"
  - "Production Kustomize overlay with replicas: 1"
  - "Per-environment ConfigMaps, Secrets, and Ingress resources"
affects: [16-backup-restore, 17-sealed-secrets, 18-dns-tls]

tech-stack:
  added: []
  patterns: [kustomize-overlays, namespace-separation, scale-to-zero]

key-files:
  created:
    - infra/k8s/overlays/staging/kustomization.yaml
    - infra/k8s/overlays/staging/configmap.yaml
    - infra/k8s/overlays/staging/secrets.yaml
    - infra/k8s/overlays/staging/ingress.yaml
    - infra/k8s/overlays/prod/kustomization.yaml
    - infra/k8s/overlays/prod/configmap.yaml
    - infra/k8s/overlays/prod/secrets.yaml
    - infra/k8s/overlays/prod/ingress.yaml
  modified: []

key-decisions:
  - "Used replicas shorthand instead of JSON patches for scale-to-zero"
  - "Standard K8s Ingress API (networking.k8s.io/v1) over Traefik IngressRoute CRD"
  - "Placeholder secrets with changeme values -- Phase 17 converts to SealedSecrets"

patterns-established:
  - "Kustomize overlay pattern: base + per-environment overlays with namespace, images, replicas"
  - "ConfigMap keys match Spring Boot env var placeholders in application-prod.yml"
  - "SPRING_PROFILES_ACTIVE set in deployment env array, not ConfigMap"

requirements-completed: [K8S-04]

duration: 5min
completed: 2026-03-23
---

# Phase 15 Plan 03: Kustomize Overlays Summary

**Staging and production Kustomize overlays with scale-to-zero staging (replicas: 0), production replicas: 1, per-environment ConfigMaps/Secrets/Ingress**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-23T14:58:26Z
- **Completed:** 2026-03-23T15:03:39Z
- **Tasks:** 2
- **Files created:** 8

## Accomplishments
- Staging overlay with all 5 components at replicas: 0 (scale-to-zero for 2GB memory constraint)
- Production overlay with all 5 components at replicas: 1
- ConfigMaps providing non-sensitive env vars (DB_HOST, REDIS_HOST, MINIO_HOST, etc.) with per-environment hostnames
- Placeholder Secrets for backend, postgres, and minio (Phase 17 converts to SealedSecrets)
- Ingress resources using standard networking.k8s.io/v1 API routing to staging.job-hunt.dev and job-hunt.dev

## Task Commits

Each task was committed atomically:

1. **Task 1: Create staging overlay with scale-to-zero** - `ae2851f` (feat)
2. **Task 2: Create production overlay with replicas: 1** - `962b25e` (feat)

## Files Created/Modified
- `infra/k8s/overlays/staging/kustomization.yaml` - Staging overlay: namespace jobhunt-staging, replicas: 0, GHCR image rewrites
- `infra/k8s/overlays/staging/configmap.yaml` - Backend and frontend ConfigMaps with staging hostnames
- `infra/k8s/overlays/staging/secrets.yaml` - Backend, postgres, minio secrets with placeholder values
- `infra/k8s/overlays/staging/ingress.yaml` - Ingress routing to staging.job-hunt.dev
- `infra/k8s/overlays/prod/kustomization.yaml` - Production overlay: namespace jobhunt-prod, replicas: 1, GHCR image rewrites
- `infra/k8s/overlays/prod/configmap.yaml` - Backend and frontend ConfigMaps with production hostnames
- `infra/k8s/overlays/prod/secrets.yaml` - Backend, postgres, minio secrets with placeholder values
- `infra/k8s/overlays/prod/ingress.yaml` - Ingress routing to job-hunt.dev

## Decisions Made
- Used Kustomize `replicas` shorthand instead of JSON patches for cleaner scale-to-zero configuration
- Standard K8s Ingress API (networking.k8s.io/v1) over Traefik IngressRoute CRD for portability
- Secrets use stringData with placeholder values (changeme) -- Phase 17 converts to SealedSecrets via kubeseal

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Kustomize manifest structure complete (base + staging + prod overlays)
- Both `kubectl kustomize` builds succeed for staging and production
- Ready for Phase 16 (backup/restore) and Phase 17 (SealedSecrets to replace placeholder values)

## Self-Check: PASSED

- All 8 overlay files exist on disk
- Commit ae2851f (staging overlay) found
- Commit 962b25e (production overlay) found

---
*Phase: 15-k3s-cluster-setup*
*Completed: 2026-03-23*
