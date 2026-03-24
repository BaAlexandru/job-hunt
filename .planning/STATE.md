---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Completed 16-02-PLAN.md (Phase 16 complete)
last_updated: "2026-03-24T11:50:56.778Z"
last_activity: 2026-03-24 — Completed 16-02 operational tooling
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 17
  completed_plans: 17
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Completed 16-01-PLAN.md
last_updated: "2026-03-24T11:45:58.552Z"
last_activity: 2026-03-24 — Completed 16-01 storage foundation
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 17
  completed_plans: 17
  percent: 100
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Phase 16 context gathered
last_updated: "2026-03-24T11:39:25.421Z"
last_activity: 2026-03-23 — Completed 15-02 namespaces and base manifests
progress:
  [██████████] 100%
  completed_phases: 6
  total_plans: 17
  completed_plans: 16
  percent: 93
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Completed 13-01-PLAN.md
last_updated: "2026-03-23T15:01:54.482Z"
last_activity: 2026-03-22 — Completed Phase 11 Visibility & Sharing (all 4 plans)
progress:
  [█████████░] 93%
  completed_phases: 5
  total_plans: 15
  completed_plans: 14
  percent: 93
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Completed 13-01-PLAN.md
last_updated: "2026-03-22T23:41:21.252Z"
last_activity: 2026-03-22 — Completed Phase 11 Visibility & Sharing (all 4 plans)
progress:
  [█████████░] 93%
  completed_phases: 5
  total_plans: 12
  completed_plans: 12
  percent: 100
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Phase 13 context gathered
last_updated: "2026-03-22T22:15:17.671Z"
last_activity: 2026-03-22 — Completed Phase 11 Visibility & Sharing (all 4 plans)
progress:
  [██████████] 100%
  completed_phases: 4
  total_plans: 11
  completed_plans: 11
---

---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: completed
stopped_at: Completed 11-04-PLAN.md (Phase 11 complete)
last_updated: "2026-03-22T17:15:47.932Z"
last_activity: 2026-03-22 — Completed Phase 11 Visibility & Sharing (all 4 plans)
progress:
  total_phases: 9
  completed_phases: 1
  total_plans: 4
  completed_plans: 4
  percent: 100
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-22)

**Core value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.
**Current focus:** v1.1 Infrastructure & Deployment — Phase 16 (Data Stores on K8s)

## Current Position

Phase: 16 of 18 (Data Stores on K8s)
Plan: 2 of 2 (completed)
Status: Phase Complete
Last activity: 2026-03-24 — Completed 16-02 operational tooling

Progress: [██████████] 100%

## Performance Metrics

**Velocity:**
- v1.0: 30 plans in 4 days (2026-03-19 to 2026-03-22)
- v1.1: Not started

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Single K3s cluster with namespace-based staging/prod separation (not separate clusters)
- ArgoCD core-mode to save RAM on constrained t3.small (2GB)
- Cloudflare proxy TLS + Origin CA cert (not cert-manager + Let's Encrypt)
- Traefik bundled with K3s (not NGINX Ingress)
- K3s over kubeadm (lightweight, single binary)
- Polymorphic resource_shares table with resource_type discriminator (not separate share tables per entity)
- Class-level @Disabled on test stubs to prevent Spring context loading before migration exists
- Visibility field as String in DTOs for clean JSON serialization
- Used sonner toast for mutation success/error feedback in visibility/sharing components
- Test-specific application.yml with explicit DB config to bypass Docker Desktop port discovery issue
- JobService.resolveCompanyName uses findById (not findByIdAndUserId) for visibility-aware reads by non-owners
- Split sidebar into mainNavItems and secondaryNavItems with Separator for discovery features
- isOwner defaults to true when undefined for backwards compatibility with existing data
- Non-owner detail pages hide jobs section, edit/delete, visibility controls, and share manager
- [Phase 13-ci-pipeline]: MinIO health check uses curl against /minio/health/live instead of mc ready local in GH Actions service containers
- [Phase 13-ci-pipeline]: Trivy exit-code 0 (report-only) to avoid blocking builds on upstream CVEs
- [Phase 15-02]: Namespaces kept outside Kustomize overlays to avoid namespace transformer gotcha
- [Phase 15-02]: LimitRange identical for staging and prod (safety net, not enforcement)
- [Phase 15]: All scripts read tofu outputs dynamically via tofu -chdir -- no hardcoded IPs
- [Phase 15]: application-prod.yml uses env var placeholders matching future K8s ConfigMap/Secret keys
- [Phase 15]: Used replicas shorthand instead of JSON patches for scale-to-zero
- [Phase 15]: Standard K8s Ingress API (networking.k8s.io/v1) over Traefik IngressRoute CRD
- [Phase 16-01]: local-path-retain StorageClass explicit-only (no default annotation) to prevent accidental use
- [Phase 16-01]: Redis RDB save policy with graduated thresholds (3600/1, 300/100, 60/10000)
- [Phase 16-01]: MinIO pinned to RELEASE.2025-04-22T22-12-26Z for reproducibility
- [Phase 16-02]: mc CLI downloaded at runtime in backup CronJob (avoids custom image)
- [Phase 16-02]: Bucket init Job backoffLimit:5 to handle MinIO startup delay
- [Phase 16-02]: Staging suspends CronJob only; bucket init Job left to fail/retry on-demand

### Pending Todos

None yet.

### Blockers/Concerns

- t3.small has only 2GB RAM; memory budget is tight (~1,610MB estimated usage, ~438MB headroom)
- Mitigation: staging namespace defaults to replicas=0 (scale-to-zero), only production runs continuously
- Fallback: upgrade to t3.medium ($30/mo) if sustained memory pressure after deployment

## Session Continuity

Last session: 2026-03-24T11:44:56Z
Stopped at: Completed 16-02-PLAN.md (Phase 16 complete)
Resume file: Next phase
