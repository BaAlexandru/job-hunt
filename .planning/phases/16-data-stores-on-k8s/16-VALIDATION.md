---
phase: 16
slug: data-stores-on-k8s
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-24
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | kustomize build (YAML validation) + kubectl dry-run |
| **Config file** | infra/k8s/base/kustomization.yaml |
| **Quick run command** | `kustomize build infra/k8s/overlays/prod` |
| **Full suite command** | `kustomize build infra/k8s/overlays/prod && kustomize build infra/k8s/overlays/staging` |
| **Estimated runtime** | ~5 seconds |

---

## Sampling Rate

- **After every task commit:** Run `kustomize build infra/k8s/overlays/prod > /dev/null && kustomize build infra/k8s/overlays/staging > /dev/null`
- **After every plan wave:** Run `kustomize build infra/k8s/overlays/prod && kustomize build infra/k8s/overlays/staging`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 16-01-01 | 01 | 1 | DATA-01 | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A5 "local-path-retain"` | N/A | ⬜ pending |
| 16-01-02 | 01 | 1 | DATA-02 | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A20 "kind: StatefulSet" \| grep -A20 "name: redis"` | N/A | ⬜ pending |
| 16-02-01 | 02 | 2 | DATA-03, DATA-04 | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep "bucket-init"` | N/A | ⬜ pending |
| 16-02-02 | 02 | 2 | DATA-04 | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A5 "CronJob" \| grep "schedule"` | N/A | ⬜ pending |
| 16-02-03 | 02 | 2 | — | manifest validation | `kustomize build infra/k8s/overlays/staging \| grep -A5 "CronJob" \| grep "suspend: true"` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.*

None — Kustomize is already available, base manifests exist from Phase 15, no test framework installation needed. Validation is YAML rendering, not runtime testing (cluster validation happens at deployment time in Phase 17).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| PostgreSQL data survives pod restart | DATA-01 | Requires running cluster | Deploy, insert row, delete pod, verify row exists |
| Redis data survives pod restart | DATA-02 | Requires running cluster | Deploy, SET key, delete pod, GET key |
| MinIO accessible via S3 API | DATA-03 | Requires running cluster | Deploy, upload file via mc, verify via presigned URL |
| pg_dump CronJob runs successfully | DATA-04 | Requires running cluster + time | Trigger manual job, verify backup file in MinIO bucket |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 5s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved (review-audit 2026-03-24)
