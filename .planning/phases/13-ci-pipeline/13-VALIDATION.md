---
phase: 13
slug: ci-pipeline
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | GitHub Actions (workflow validation via `act` dry-run or manual trigger) |
| **Config file** | `.github/workflows/ci.yml` |
| **Quick run command** | `cat .github/workflows/ci.yml` (syntax check) |
| **Full suite command** | `./gradlew :backend:test` (local equivalent of CI test job) |
| **Estimated runtime** | ~60 seconds (local tests); CI run ~3-5 minutes |

---

## Sampling Rate

- **After every task commit:** Validate YAML syntax with `python -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"`
- **After every plan wave:** Run `./gradlew :backend:test` to confirm tests still pass
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | DOCK-03 | file | `test -f .github/workflows/ci.yml` | ❌ W0 | ⬜ pending |
| 13-01-02 | 01 | 1 | DOCK-03 | content | `grep 'gradlew' .github/workflows/ci.yml` | ❌ W0 | ⬜ pending |
| 13-01-03 | 01 | 1 | DOCK-03 | content | `grep 'ghcr.io' .github/workflows/ci.yml` | ❌ W0 | ⬜ pending |
| 13-01-04 | 01 | 1 | DOCK-04 | content | `grep 'trivy' .github/workflows/ci.yml` | ❌ W0 | ⬜ pending |
| 13-01-05 | 01 | 1 | DOCK-03 | content | `grep 'badge' README.md` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements. Phase 13 creates new CI config files rather than modifying testable application code.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CI triggers on merge to master | DOCK-03 | Requires actual GitHub Actions runner | Merge a test PR and verify workflow runs in Actions tab |
| Images pushed to GHCR | DOCK-03 | Requires GHCR authentication and push | Check `ghcr.io/baalexandru/jobhunt-backend` after CI run |
| Trivy results in workflow summary | DOCK-04 | Requires actual workflow run output | Check Actions summary tab for vulnerability table |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
