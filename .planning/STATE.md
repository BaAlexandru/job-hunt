---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Infrastructure & Deployment
status: in_progress
stopped_at: Completed 18-01-PLAN.md
last_updated: "2026-03-25"
last_activity: 2026-03-25 — Completed Phase 18 Plan 01 (Cloudflare IaC)
progress:
  total_phases: 9
  completed_phases: 8
  total_plans: 22
  completed_plans: 21
  percent: 95
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-22)

**Core value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.
**Current focus:** v1.1 Infrastructure & Deployment — Phase 18 (Domain & TLS)

## Current Position

Phase: 18 (Domain & TLS) — Plan 01 of 03 complete
Next: Phase 18 Plan 02 (Traefik TLS & ArgoCD Passthrough) - already executed
Last activity: 2026-03-25 — Completed Phase 18 Plan 01 (Cloudflare IaC)

Progress: [█████████░] 95%

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
- [Phase 17-01]: Staging auto-sync with selfHeal; prod manual sync only for safe promotion
- [Phase 17-01]: Placeholder SealedSecret files with empty encryptedData to keep kustomize build valid pre-sealing
- [Phase 18-domain-tls]: TLSStore+TLSOption named 'default' in kube-system for automatic Traefik TLS on all connections
- [Phase 18-domain-tls]: ArgoCD TLS passthrough via IngressRouteTCP (exception to standard Ingress API)
- [Phase 18-01]: Cloudflare provider v5 individual cloudflare_zone_setting resources (not monolithic override)
- [Phase 18-01]: Cloudflare ruleset rules attribute uses map syntax for headers (v5 schema requirement)
- [Phase 18-01]: AWS SG ports 80/443 restricted to Cloudflare IPv4 CIDRs via locals block

### Pending Todos

None yet.

### Blockers/Concerns

- t3.small has only 2GB RAM; memory budget is tight (~1,610MB estimated usage, ~438MB headroom)
- Mitigation: staging namespace defaults to replicas=0 (scale-to-zero), only production runs continuously
- Fallback: upgrade to t3.medium ($30/mo) if sustained memory pressure after deployment

## Session Continuity

Last session: 2026-03-25T12:56:29Z
Stopped at: Completed 18-01-PLAN.md
Resume file: None
