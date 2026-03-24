---
phase: 17
slug: app-deployment-argocd
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Shell scripts + kubectl assertions |
| **Config file** | none — validation is operational (live cluster) |
| **Quick run command** | `kubectl get pods -n jobhunt-prod -o wide` |
| **Full suite command** | `bash infra/scripts/verify-phase17.sh` |
| **Estimated runtime** | ~15 seconds (requires SSH tunnel active) |

---

## Sampling Rate

- **After every task commit:** Run `kubectl get pods -n jobhunt-prod && kubectl get applications -n argocd`
- **After every plan wave:** Run `bash infra/scripts/verify-phase17.sh`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 17-01-T1a | 01 | 1 | ARGO-01 | smoke | `kubectl get pods -n argocd` — all pods Running | No — Wave 0 | ⬜ pending |
| 17-01-T1b | 01 | 1 | ARGO-02 | smoke | `kubectl get applications -n argocd` — shows jobhunt, jobhunt-staging, jobhunt-prod | No — Wave 0 | ⬜ pending |
| 17-01-T2a | 01 | 1 | ARGO-03 | smoke | `kubectl get pods -n kube-system -l app.kubernetes.io/name=sealed-secrets` returns Running | No — Wave 0 | ⬜ pending |
| 17-01-T2b | 01 | 1 | ARGO-03 | smoke | `kubectl get secret backend-secrets -n jobhunt-prod` exists after sealing | No — Wave 0 | ⬜ pending |
| 17-02-T1a | 02 | 2 | K8S-05 | smoke | `kubectl get pods -n jobhunt-prod -l app=backend -o jsonpath='{.items[0].status.phase}'` returns Running | No — Wave 0 | ⬜ pending |
| 17-02-T1b | 02 | 2 | K8S-05 | smoke | `kubectl get pods -n jobhunt-prod -l app=frontend -o jsonpath='{.items[0].status.phase}'` returns Running | No — Wave 0 | ⬜ pending |
| 17-02-T2a | 02 | 2 | ARGO-04 | smoke | `kubectl get app jobhunt-staging -n argocd -o jsonpath='{.spec.syncPolicy.automated}'` returns non-empty | No — Wave 0 | ⬜ pending |
| 17-02-T2b | 02 | 2 | ARGO-04 | functional | CI push triggers staging kustomization.yaml update with `[skip ci]` commit | No — Wave 0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `infra/scripts/verify-phase17.sh` — comprehensive verification script checking all 5 requirements
- [ ] Script checks: Sealed Secrets controller running, ArgoCD pods healthy, Applications synced, app pods running, sync policies correct
- [ ] Script requires SSH tunnel active (`infra/scripts/connect.sh`)

*Verification is primarily operational (kubectl commands on live cluster), not unit tests.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| ArgoCD web UI accessible | ARGO-01 | Requires browser + port-forward | `kubectl port-forward svc/argocd-server -n argocd 8443:443` then open https://localhost:8443 |
| Manual prod promotion workflow | ARGO-04 | Requires human-initiated kustomize edit + ArgoCD sync | Update prod overlay image tag, commit, push, then sync in ArgoCD UI |
| Sealed Secrets key backup | ARGO-03 | Requires file system check outside Git | Run `bash infra/scripts/backup-sealed-key.sh` and verify output file exists |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
