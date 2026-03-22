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
| **Config file** | backend/Dockerfile, frontend/Dockerfile |
| **Quick run command** | `docker build -t jobhunt-backend:test -f backend/Dockerfile .` |
| **Full suite command** | `docker build -t jobhunt-backend:test -f backend/Dockerfile . && docker build -t jobhunt-frontend:test -f frontend/Dockerfile frontend/` |
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
| 12-01-01 | 01 | 1 | DOCK-01 | integration | `docker build -t jobhunt-backend:test -f backend/Dockerfile .` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | DOCK-01 | integration | `docker run --rm jobhunt-backend:test java -version` | ❌ W0 | ⬜ pending |
| 12-01-03 | 01 | 1 | DOCK-01 | smoke | `docker image inspect jobhunt-backend:test --format '{{.Size}}'` — must be under 209715200 | ❌ W0 | ⬜ pending |
| 12-01-04 | 01 | 1 | DOCK-01 | smoke | `docker run --rm jobhunt-backend:test whoami` — must output `app` | ❌ W0 | ⬜ pending |
| 12-02-01 | 02 | 1 | DOCK-02 | integration | `docker build -t jobhunt-frontend:test -f frontend/Dockerfile frontend/` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 1 | DOCK-02 | integration | `docker run --rm jobhunt-frontend:test node -v` | ❌ W0 | ⬜ pending |
| 12-02-03 | 02 | 1 | DOCK-02 | smoke | `docker image inspect jobhunt-frontend:test --format '{{.Size}}'` — must be under 209715200 | ❌ W0 | ⬜ pending |
| 12-02-04 | 02 | 1 | DOCK-02 | smoke | `docker run --rm jobhunt-frontend:test whoami` — must output `app` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/Dockerfile` — multi-stage build for Spring Boot backend
- [ ] `backend/.dockerignore` — build context exclusions
- [ ] `frontend/Dockerfile` — multi-stage build for Next.js frontend
- [ ] `frontend/.dockerignore` — build context exclusions
- [ ] `frontend/next.config.ts` — needs `output: "standalone"` added
- [ ] `compose.prod.yaml` — extends compose.yaml with backend + frontend services

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Image size < 200MB | DOCK-01, DOCK-02 | Requires `docker images` inspection | `docker images jobhunt-backend:test --format '{{.Size}}'` — verify under 200MB |
| Backend serves traffic | DOCK-01 | Requires running container with network + DB | `docker compose -f compose.yaml -f compose.prod.yaml up --build -d && sleep 60 && curl localhost:8080/actuator/health` |
| Frontend serves traffic | DOCK-02 | Requires running container with backend | `curl localhost:3000/` after compose.prod.yaml is up |
| Non-root user | DOCK-01, DOCK-02 | Requires container execution | `docker run --rm jobhunt-backend:test whoami` and `docker run --rm jobhunt-frontend:test whoami` — both must output `app` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
