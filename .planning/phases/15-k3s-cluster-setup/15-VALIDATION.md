---
phase: 15
slug: k3s-cluster-setup
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-23
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Shell script validation + kustomize build |
| **Config file** | None (shell-based validation) |
| **Quick run command** | `kustomize build infra/k8s/overlays/staging > /dev/null && kustomize build infra/k8s/overlays/prod > /dev/null` |
| **Full suite command** | `kustomize build infra/k8s/overlays/staging && kustomize build infra/k8s/overlays/prod` + `kubectl get nodes` (requires cluster access) |
| **Estimated runtime** | ~5 seconds (kustomize build only; kubectl requires SSH tunnel) |

---

## Sampling Rate

- **After every task commit:** Run `kustomize build infra/k8s/overlays/staging > /dev/null && kustomize build infra/k8s/overlays/prod > /dev/null`
- **After every plan wave:** Run full `kustomize build` output inspection for both overlays
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 15-01-01 | 01 | 1 | K8S-02 | manual | `kubectl get nodes` (requires SSH tunnel) | N/A - cluster test | ⬜ pending |
| 15-01-02 | 01 | 1 | K8S-02 | smoke | `bash -n infra/scripts/bootstrap-k3s.sh` | ❌ W0 | ⬜ pending |
| 15-01-03 | 01 | 1 | K8S-02 | smoke | `bash -n infra/scripts/setup-kubeconfig.sh` | ❌ W0 | ⬜ pending |
| 15-02-01 | 02 | 1 | K8S-03 | manual | `kubectl get ns jobhunt-staging jobhunt-prod` | N/A - cluster test | ⬜ pending |
| 15-02-02 | 02 | 1 | K8S-03 | smoke | `kubectl apply --dry-run=client -f infra/k8s/namespaces/` | ❌ W0 | ⬜ pending |
| 15-03-01 | 03 | 2 | K8S-04 | smoke | `kustomize build infra/k8s/overlays/staging > /dev/null` | ❌ W0 | ⬜ pending |
| 15-03-02 | 03 | 2 | K8S-04 | smoke | `kustomize build infra/k8s/overlays/prod > /dev/null` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `kustomize` CLI available locally (bundled with kubectl or installed separately)
- [ ] Shell scripts are syntactically valid (`bash -n` checks)
- [ ] Kustomize overlays build without errors

*No test framework to install — validation is shell-based (kustomize build, bash -n, kubectl).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| K3s node shows Ready | K8S-02 | Requires SSH to EC2 instance + running cluster | 1. Run `infra/scripts/connect.sh` 2. Run `kubectl get nodes` 3. Verify STATUS=Ready |
| Namespaces exist with LimitRange | K8S-03 | Requires running cluster | 1. Run `kubectl get ns jobhunt-staging jobhunt-prod` 2. Run `kubectl get limitrange -n jobhunt-staging` 3. Verify LimitRange has default memory/cpu |
| Staging replicas are 0 | K8S-04 | Can verify via kustomize build output | 1. Run `kustomize build infra/k8s/overlays/staging` 2. Grep for `replicas: 0` in output |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
