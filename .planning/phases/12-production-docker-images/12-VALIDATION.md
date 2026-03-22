---
phase: 12
slug: production-docker-images
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Docker CLI + shell assertions |
| **Config file** | infra/backend/Dockerfile, infra/frontend/Dockerfile |
| **Quick run command** | `docker build -t jobhunt-backend:test -f infra/backend/Dockerfile .` |
| **Full suite command** | `docker build -t jobhunt-backend:test -f infra/backend/Dockerfile . && docker build -t jobhunt-frontend:test -f infra/frontend/Dockerfile .` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick build for the relevant Dockerfile
- **After every plan wave:** Run full suite (both images build)
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | DOCK-01 | integration | `docker build -t jobhunt-backend:test -f infra/backend/Dockerfile .` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | DOCK-01 | integration | `docker run --rm jobhunt-backend:test java -version` | ❌ W0 | ⬜ pending |
| 12-02-01 | 02 | 1 | DOCK-02 | integration | `docker build -t jobhunt-frontend:test -f infra/frontend/Dockerfile .` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 1 | DOCK-02 | integration | `docker run --rm jobhunt-frontend:test node -v` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `infra/backend/Dockerfile` — multi-stage build for Spring Boot backend
- [ ] `infra/frontend/Dockerfile` — multi-stage build for Next.js frontend

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Image size < 200MB | DOCK-01, DOCK-02 | Requires `docker images` inspection | `docker images jobhunt-backend:test --format '{{.Size}}'` — verify under 200MB |
| Container serves traffic | DOCK-01, DOCK-02 | Requires running container with network | `docker run -d -p 8080:8080 jobhunt-backend:test && curl localhost:8080/actuator/health` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
