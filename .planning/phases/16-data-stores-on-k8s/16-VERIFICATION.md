---
phase: 16-data-stores-on-k8s
verified: 2026-03-24T12:00:00Z
status: passed
score: 11/11 must-haves verified
re_verification: false
---

# Phase 16: Data Stores on K8s Verification Report

**Phase Goal:** Deploy persistent data stores (PostgreSQL, Redis, MinIO) on K8s with retention policies, backup automation, and operational tooling
**Verified:** 2026-03-24
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All three data stores use local-path-retain StorageClass with reclaimPolicy Retain | VERIFIED | `storage-class.yaml` has `reclaimPolicy: Retain`; postgres, redis, minio statefulsets all set `storageClassName: local-path-retain` |
| 2 | Redis is a StatefulSet with RDB persistence enabled and 1Gi PVC | VERIFIED | `redis/statefulset.yaml` is `kind: StatefulSet` with `--save 3600 1 300 100 60 10000 --dir /data` and `storage: 1Gi` |
| 3 | PostgreSQL and MinIO PVCs reference local-path-retain StorageClass | VERIFIED | Both statefulsets have `storageClassName: local-path-retain` in volumeClaimTemplates |
| 4 | All StatefulSets have persistentVolumeClaimRetentionPolicy set to Retain | VERIFIED | `whenDeleted: Retain` and `whenScaled: Retain` present in all three StatefulSet specs |
| 5 | Kustomize build succeeds for both staging and prod overlays | VERIFIED | `kubectl kustomize infra/k8s/overlays/prod` and `infra/k8s/overlays/staging` both exit 0 |
| 6 | MinIO buckets (jobhunt-documents, jobhunt-backups) are created by a Kubernetes Job | VERIFIED | `minio/bucket-init-job.yaml` contains `mc mb myminio/jobhunt-documents --ignore-existing` and `mc mb myminio/jobhunt-backups --ignore-existing` |
| 7 | jobhunt-backups bucket has 7-day lifecycle expiry policy | VERIFIED | `mc ilm rule add myminio/jobhunt-backups --expire-days 7` with idempotency check present |
| 8 | Daily pg_dump CronJob runs at 02:00 UTC and uploads compressed backup to MinIO | VERIFIED | `schedule: "0 2 * * *"`, `pg_dump ... | gzip`, `mc cp /tmp/...` all present in `backup-cronjob.yaml` |
| 9 | User can download backups from MinIO to local machine via convenience script | VERIFIED | `download-backups.sh` exists, is executable, contains `mc mirror k8s-minio/jobhunt-backups` via SSH tunnel |
| 10 | Staging overlay suspends the backup CronJob to prevent failed runs against scale-to-zero pods | VERIFIED | `suspend-jobs.yaml` patch applied; `kubectl kustomize overlays/staging` renders `suspend: true` on CronJob |
| 11 | Bucket init Job lifecycle rule addition is idempotent | VERIFIED | `mc ilm rule ls ... --json \| grep -q '"expiration"' \|\| mc ilm rule add ...` conditional present |

**Score:** 11/11 truths verified

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `infra/k8s/base/storage-class.yaml` | Custom StorageClass with Retain reclaim policy | VERIFIED | 7 lines; `reclaimPolicy: Retain`, `provisioner: rancher.io/local-path`, `volumeBindingMode: WaitForFirstConsumer`; no `is-default-class` annotation |
| `infra/k8s/base/redis/statefulset.yaml` | Redis StatefulSet with RDB persistence | VERIFIED | `kind: StatefulSet`, `serviceName: redis`, `--save 3600 1 300 100 60 10000`, `storageClassName: local-path-retain`, `storage: 1Gi`, `persistentVolumeClaimRetentionPolicy: Retain` |
| `infra/k8s/base/postgres/statefulset.yaml` | PostgreSQL with local-path-retain and retention policy | VERIFIED | `storageClassName: local-path-retain`, `whenDeleted: Retain`, `whenScaled: Retain` |
| `infra/k8s/base/minio/statefulset.yaml` | MinIO with local-path-retain, retention policy, pinned image | VERIFIED | `storageClassName: local-path-retain`, `whenDeleted: Retain`, `image: minio/minio:RELEASE.2025-04-22T22-12-26Z` |
| `infra/k8s/base/minio/bucket-init-job.yaml` | Idempotent bucket creation and lifecycle policy | VERIFIED | `kind: Job`, `backoffLimit: 5`, `mc mb` both buckets, idempotent lifecycle check, `secretRef: minio-secrets`, `image: minio/mc:RELEASE.2025-09-07T16-10-49Z` |
| `infra/k8s/base/postgres/backup-cronjob.yaml` | Daily pg_dump backup to MinIO | VERIFIED | `kind: CronJob`, `schedule: "0 2 * * *"`, `concurrencyPolicy: Forbid`, `pg_dump ... | gzip`, `mc cp`, refs `postgres-secrets` and `minio-secrets` |
| `infra/scripts/download-backups.sh` | Convenience script for local backup download | VERIFIED | Executable, `set -euo pipefail`, tofu outputs, SSH tunnel, `mc mirror k8s-minio/jobhunt-backups`, validates `mc` CLI and env vars, trap cleanup |
| `infra/k8s/overlays/staging/suspend-jobs.yaml` | Staging CronJob suspension patch | VERIFIED | `kind: CronJob`, `name: postgres-backup`, `suspend: true` |
| `infra/k8s/base/redis/deployment.yaml` | Must NOT exist (deleted) | VERIFIED | File absent from codebase |
| `infra/k8s/base/kustomization.yaml` | All resources registered including new Job and CronJob | VERIFIED | `storage-class.yaml`, `redis/statefulset.yaml`, `postgres/backup-cronjob.yaml`, `minio/bucket-init-job.yaml` all present; `redis/deployment.yaml` absent |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `redis/statefulset.yaml` | `storage-class.yaml` | `storageClassName: local-path-retain` in volumeClaimTemplates | WIRED | Line 67: `storageClassName: local-path-retain` |
| `postgres/statefulset.yaml` | `storage-class.yaml` | `storageClassName: local-path-retain` in volumeClaimTemplates | WIRED | Line 62: `storageClassName: local-path-retain` |
| `minio/statefulset.yaml` | `storage-class.yaml` | `storageClassName: local-path-retain` in volumeClaimTemplates | WIRED | Line 68: `storageClassName: local-path-retain` |
| `kustomization.yaml` | `redis/statefulset.yaml` | resources list | WIRED | `redis/statefulset.yaml` in resources |
| `minio/bucket-init-job.yaml` | `minio-secrets` | `envFrom secretRef` | WIRED | `secretRef: name: minio-secrets` at line 30 |
| `postgres/backup-cronjob.yaml` | `postgres-secrets` | `envFrom secretRef` for pg_dump credentials | WIRED | `secretRef: name: postgres-secrets` at lines 46, 41 (also PGPASSWORD secretKeyRef) |
| `postgres/backup-cronjob.yaml` | `minio-secrets` | `envFrom secretRef` for mc upload | WIRED | `secretRef: name: minio-secrets` at line 48 |
| `kustomization.yaml` | `minio/bucket-init-job.yaml` | resources list | WIRED | `minio/bucket-init-job.yaml` in resources |
| `staging/kustomization.yaml` | `suspend-jobs.yaml` | patches list | WIRED | `- path: suspend-jobs.yaml` in patches |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| DATA-01 | 16-01 | PostgreSQL deployed as StatefulSet with persistent volume (reclaimPolicy: Retain) | SATISFIED | `postgres/statefulset.yaml` is StatefulSet with `local-path-retain` StorageClass and `persistentVolumeClaimRetentionPolicy: Retain` |
| DATA-02 | 16-01 | Redis deployed on K8s with persistence | SATISFIED | `redis/statefulset.yaml` replaces Deployment with StatefulSet, RDB save policy, 1Gi PVC on `local-path-retain` |
| DATA-03 | 16-01, 16-02 | MinIO deployed as StatefulSet with persistent volume | SATISFIED | `minio/statefulset.yaml` has `local-path-retain`, retention policy, pinned image; bucket-init-job creates buckets with lifecycle |
| DATA-04 | 16-02 | Automated daily pg_dump backup CronJob to S3 | SATISFIED | `backup-cronjob.yaml` runs at `0 2 * * *`, dumps via `pg_dump | gzip`, uploads to MinIO `jobhunt-backups` bucket |

All four requirements satisfied. No orphaned requirements found for Phase 16.

---

### Anti-Patterns Found

No anti-patterns detected. Scanned all 8 created/modified files:
- No TODO/FIXME/PLACEHOLDER comments
- No stub implementations (empty returns, static responses)
- No unwired resources
- No `redis/deployment.yaml` left behind
- `backups/` correctly gitignored

One notable operational note (not a blocker): The `postgres/backup-cronjob.yaml` downloads the `mc` binary at runtime via `curl` from `dl.min.io`. This is a deliberate design decision documented in the SUMMARY (avoids maintaining a custom image). It introduces a runtime dependency on external network access during backup execution, and the downloaded binary is unversioned (latest). This is a warning-level concern for production hardening but does not block the phase goal.

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `postgres/backup-cronjob.yaml` | `curl -sSL https://dl.min.io/.../mc` downloads unversioned binary at runtime | Warning | Backup could silently break if dl.min.io changes binary ABI; no pinning of the downloaded mc version |

---

### Human Verification Required

The following items cannot be verified without a live cluster:

#### 1. Bucket Init Job Execution

**Test:** Apply the manifests to the cluster and verify the `minio-bucket-init` Job completes successfully.
**Expected:** Job reaches `Completed` status; `jobhunt-documents` and `jobhunt-backups` buckets exist in MinIO; `jobhunt-backups` has a lifecycle rule showing 7-day expiry.
**Why human:** Requires live MinIO pod and actual `mc` CLI execution against it.

#### 2. Backup CronJob Execution

**Test:** Manually trigger the `postgres-backup` CronJob once (`kubectl create job --from=cronjob/postgres-backup`) and monitor the pod logs.
**Expected:** pg_dump completes, gzipped file appears in MinIO `jobhunt-backups` bucket with timestamped filename.
**Why human:** Requires live PostgreSQL and MinIO, and actual `mc cp` upload.

#### 3. Data Survival Across Pod Restart

**Test:** Write data to PostgreSQL, delete the postgres pod, verify data persists after the pod reschedules.
**Expected:** All data present after restart; PVC not deleted.
**Why human:** Requires live cluster and actual PVC/pod lifecycle.

#### 4. download-backups.sh End-to-End

**Test:** Run `infra/scripts/download-backups.sh` with `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` set.
**Expected:** SSH tunnel established, backups synced to local `backups/` directory.
**Why human:** Requires live EC2 instance, tofu state, and MinIO data.

---

### Gaps Summary

No gaps. All 11 observable truths verified, all artifacts substantive and wired, all 4 requirements satisfied, both kustomize overlays render cleanly, all 5 documented commit hashes confirmed in git history.

The one warning (unversioned mc binary download in the CronJob) is a future hardening concern, not a blocker for the phase goal.

---

_Verified: 2026-03-24_
_Verifier: Claude (gsd-verifier)_
