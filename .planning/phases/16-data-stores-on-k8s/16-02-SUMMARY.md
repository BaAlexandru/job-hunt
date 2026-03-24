---
phase: 16-data-stores-on-k8s
plan: 02
subsystem: infra
tags: [minio, postgres, kubernetes, cronjob, backup, kustomize]

requires:
  - phase: 16-01
    provides: "StatefulSets for PostgreSQL, Redis, MinIO with PVCs and services"
provides:
  - "MinIO bucket initialization Job (jobhunt-documents, jobhunt-backups)"
  - "7-day lifecycle expiry on jobhunt-backups bucket"
  - "Daily PostgreSQL backup CronJob (02:00 UTC) to MinIO"
  - "download-backups.sh convenience script for local backup retrieval"
  - "Staging overlay CronJob suspension patch"
affects: [17-argocd-deployment, 18-dns-tls]

tech-stack:
  added: [minio/mc:RELEASE.2025-09-07T16-10-49Z]
  patterns: [kubernetes-job-for-init, cronjob-for-scheduled-tasks, strategic-merge-patch-for-overlay-overrides]

key-files:
  created:
    - infra/k8s/base/minio/bucket-init-job.yaml
    - infra/k8s/base/postgres/backup-cronjob.yaml
    - infra/scripts/download-backups.sh
    - infra/k8s/overlays/staging/suspend-jobs.yaml
  modified:
    - infra/k8s/base/kustomization.yaml
    - infra/k8s/overlays/staging/kustomization.yaml
    - .gitignore

key-decisions:
  - "mc CLI downloaded at runtime in backup CronJob (avoids custom image, ~25MB static binary)"
  - "Bucket init Job uses backoffLimit:5 to handle MinIO startup delay gracefully"
  - "Staging suspends CronJob only (bucket init Job left to fail/retry on-demand)"

patterns-established:
  - "Kubernetes Job for one-time initialization tasks (bucket creation)"
  - "CronJob with Forbid concurrency for scheduled database backups"
  - "Strategic merge patch in staging overlay for job suspension"
  - "SSH tunnel + kubectl port-forward pattern for local MinIO access"

requirements-completed: [DATA-03, DATA-04]

duration: 3min
completed: 2026-03-24
---

# Phase 16 Plan 02: Operational Tooling Summary

**MinIO bucket init Job with 7-day lifecycle, daily PostgreSQL backup CronJob to MinIO, and download-backups.sh for local retrieval**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-24T11:41:14Z
- **Completed:** 2026-03-24T11:44:56Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments
- MinIO bucket initialization Job creates jobhunt-documents and jobhunt-backups buckets with idempotent lifecycle policy
- Daily PostgreSQL backup CronJob at 02:00 UTC dumps gzipped SQL to MinIO jobhunt-backups bucket
- download-backups.sh script syncs backups locally via SSH tunnel with mc mirror
- Staging overlay suspends backup CronJob to prevent failures against scale-to-zero pods

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MinIO bucket init Job and PostgreSQL backup CronJob** - `a1de4a5` (feat)
2. **Task 2: Create download-backups.sh convenience script** - `bf6df03` (feat)
3. **Task 3: Suspend CronJob and skip Job in staging overlay** - `d757243` (feat)

## Files Created/Modified
- `infra/k8s/base/minio/bucket-init-job.yaml` - Idempotent bucket creation with 7-day lifecycle on backups
- `infra/k8s/base/postgres/backup-cronjob.yaml` - Daily pg_dump to gzipped SQL uploaded to MinIO
- `infra/k8s/base/kustomization.yaml` - Added backup-cronjob.yaml and bucket-init-job.yaml resources
- `infra/scripts/download-backups.sh` - SSH tunnel + mc mirror for local backup download
- `infra/k8s/overlays/staging/suspend-jobs.yaml` - Strategic merge patch suspending CronJob
- `infra/k8s/overlays/staging/kustomization.yaml` - Added suspend-jobs.yaml patch reference
- `.gitignore` - Added backups/ directory exclusion

## Decisions Made
- mc CLI downloaded at runtime in backup CronJob container (avoids building/maintaining a custom image for a ~25MB static binary)
- Bucket init Job uses backoffLimit:5 with restartPolicy:OnFailure to tolerate MinIO startup delay
- Staging overlay only suspends CronJob; bucket init Job is left to fail/retry since staging is on-demand anyway

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All data store manifests complete (StatefulSets from 16-01, operational tooling from 16-02)
- Ready for Phase 17 (ArgoCD deployment) to deploy these manifests to the K3s cluster
- Secret values in overlays/prod/secrets.yaml still contain placeholder "changeme" values -- must be replaced before production deployment

---
*Phase: 16-data-stores-on-k8s*
*Completed: 2026-03-24*
