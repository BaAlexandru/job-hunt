---
phase: 17-app-deployment-argocd
plan: 01
subsystem: infra
tags: [argocd, sealed-secrets, gitops, kustomize, kubernetes, helm]

# Dependency graph
requires:
  - phase: 15-k3s-cluster-setup
    provides: Kustomize overlays with namespace separation
  - phase: 16-data-stores-k8s
    provides: Base manifests and overlay structure for data services
provides:
  - ArgoCD Helm values with resource limits for constrained 2GB cluster
  - App-of-apps Application CRDs (root + staging auto-sync + prod manual sync)
  - Sealed Secrets seal-secrets.sh and backup-sealed-key.sh scripts
  - Placeholder SealedSecret files in both overlay directories
affects: [17-02, 18-domain-tls]

# Tech tracking
tech-stack:
  added: [argocd, sealed-secrets, kubeseal]
  patterns: [app-of-apps, gitops-sync-policy, sealed-secrets-workflow]

key-files:
  created:
    - infra/argocd/values.yaml
    - infra/argocd/root-app.yaml
    - infra/argocd/apps/staging.yaml
    - infra/argocd/apps/prod.yaml
    - infra/scripts/seal-secrets.sh
    - infra/scripts/backup-sealed-key.sh
    - infra/k8s/overlays/prod/backend-sealed-secret.yaml
    - infra/k8s/overlays/prod/postgres-sealed-secret.yaml
    - infra/k8s/overlays/prod/minio-sealed-secret.yaml
    - infra/k8s/overlays/staging/backend-sealed-secret.yaml
    - infra/k8s/overlays/staging/postgres-sealed-secret.yaml
    - infra/k8s/overlays/staging/minio-sealed-secret.yaml
  modified:
    - infra/k8s/overlays/prod/kustomization.yaml
    - infra/k8s/overlays/staging/kustomization.yaml

key-decisions:
  - "Staging auto-sync with selfHeal; prod manual sync only for safe promotion"
  - "Placeholder SealedSecret files with empty encryptedData to keep kustomize build valid pre-sealing"
  - "Old secrets.yaml renamed to .bak for reference rather than deleted"

patterns-established:
  - "App-of-apps: root Application watches infra/argocd/apps/, child apps watch overlays"
  - "Sealed Secrets workflow: generate random passwords, pipe through kubeseal, never write plaintext"
  - "ArgoCD resource limits: disable unused components (dex, notifications, applicationSet) for 2GB cluster"

requirements-completed: [ARGO-01, ARGO-02, ARGO-03]

# Metrics
duration: 3min
completed: 2026-03-24
---

# Phase 17 Plan 01: ArgoCD & Sealed Secrets Summary

**ArgoCD app-of-apps pattern with Helm values, staging auto-sync, prod manual sync, and Sealed Secrets scripts replacing plaintext credentials**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-24T13:00:28Z
- **Completed:** 2026-03-24T13:03:04Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- ArgoCD Helm values targeting ~400MB total with disabled unused components (dex, notifications, applicationSet)
- App-of-apps pattern: root Application auto-syncs child apps; staging has selfHeal, prod requires manual sync
- Sealed Secrets scripts for secure credential management without plaintext in Git
- Kustomize overlays updated to reference sealed-secret files with valid placeholder SealedSecret CRDs

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ArgoCD Helm values and app-of-apps Application manifests** - `c5fdaf3` (feat)
2. **Task 2: Create Sealed Secrets scripts and update overlay kustomizations** - `d567a6a` (feat)

## Files Created/Modified
- `infra/argocd/values.yaml` - ArgoCD Helm chart overrides with resource limits and disabled components
- `infra/argocd/root-app.yaml` - Root Application CRD pointing to infra/argocd/apps
- `infra/argocd/apps/staging.yaml` - Staging child Application with automated sync and selfHeal
- `infra/argocd/apps/prod.yaml` - Production child Application with manual sync only
- `infra/scripts/seal-secrets.sh` - Generates random passwords and pipes through kubeseal
- `infra/scripts/backup-sealed-key.sh` - Exports controller signing key for disaster recovery
- `infra/k8s/overlays/prod/kustomization.yaml` - Updated to reference sealed-secret files
- `infra/k8s/overlays/staging/kustomization.yaml` - Updated to reference sealed-secret files
- `infra/k8s/overlays/{prod,staging}/*-sealed-secret.yaml` - 6 placeholder SealedSecret files

## Decisions Made
- Staging auto-sync with selfHeal enables GitOps push-to-deploy; prod manual sync prevents accidental deployments
- Placeholder SealedSecret files with empty encryptedData keep kustomize build valid before real sealing on cluster
- Old secrets.yaml files renamed to .bak for reference rather than deleted outright

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required. Scripts will be run on the live cluster during deployment.

## Next Phase Readiness
- ArgoCD manifests ready for Helm install on K3s cluster
- Sealed Secrets scripts ready to run once controller is installed
- Plan 17-02 can proceed with backend init containers, CI image tag updates, and verification

## Self-Check: PASSED

All 13 created files verified present. Both task commits (c5fdaf3, d567a6a) confirmed in git log.

---
*Phase: 17-app-deployment-argocd*
*Completed: 2026-03-24*
