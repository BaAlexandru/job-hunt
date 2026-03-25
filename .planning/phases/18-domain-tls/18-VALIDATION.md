---
phase: 18
slug: domain-tls
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
---

# Phase 18 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Shell script (bash) + curl/dig/openssl |
| **Config file** | `infra/scripts/verify-phase18.sh` (Wave 0) |
| **Quick run command** | `bash infra/scripts/verify-phase18.sh` |
| **Full suite command** | `bash infra/scripts/verify-phase18.sh` + manual browser smoke test |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** N/A (infrastructure changes, not code)
- **After every plan wave:** Run `bash infra/scripts/verify-phase18.sh`
- **Before `/gsd:verify-work`:** Full suite must be green + manual browser smoke test
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 18-01-01 | 01 | 1 | DNS-01 | smoke | `dig +short job-hunt.dev @1.1.1.1` | ❌ W0 | ⬜ pending |
| 18-01-02 | 01 | 1 | DNS-02 | smoke | `openssl s_client -connect job-hunt.dev:443` cert issuer check | ❌ W0 | ⬜ pending |
| 18-01-03 | 01 | 1 | DNS-03 | smoke | `curl -sI http://job-hunt.dev` redirect + HSTS check | ❌ W0 | ⬜ pending |
| 18-01-04 | 01 | 1 | DNS-04 | smoke | `curl -sf https://staging.job-hunt.dev` | ❌ W0 | ⬜ pending |
| 18-01-05 | 01 | 1 | DNS-05 | smoke + manual | `curl` both hosts + verify different content | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `infra/scripts/verify-phase18.sh` — automated verification script (curl, dig, openssl checks)
- [ ] Manual test checklist in PLAN.md for browser smoke test (login, create company, verify API)

*Infrastructure phase — no unit test framework needed. Verification is via smoke tests against live infrastructure.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Browser loads job-hunt.dev with valid cert | DNS-01, DNS-02 | Full browser rendering + cert chain validation | Open https://job-hunt.dev, verify green lock, check cert issuer is "Cloudflare Inc" |
| Login flow works through Cloudflare | DNS-01 | End-to-end auth through proxy | Login, create company, verify API calls succeed |
| staging.job-hunt.dev shows staging env | DNS-04 | Visual confirmation of separate environment | Open https://staging.job-hunt.dev, verify staging indicator |
| ArgoCD UI accessible | DNS-05 | Browser-based UI verification | Open https://argocd.job-hunt.dev, verify ArgoCD login page |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
