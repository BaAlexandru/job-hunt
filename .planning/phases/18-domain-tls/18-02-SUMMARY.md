---
phase: 18-domain-tls
plan: 02
subsystem: infra
tags: [traefik, tls, argocd, k8s, cloudflare, helmchartconfig, ingress]

# Dependency graph
requires:
  - phase: 18-01
    provides: Origin CA SealedSecret and seal-origin-ca.sh script
  - phase: 15-k3s-cluster-setup
    provides: Traefik bundled with K3s, Kustomize overlays, standard K8s Ingress
  - phase: 17-app-deployment-argocd
    provides: ArgoCD install in argocd namespace, app-of-apps structure
provides:
  - Traefik HelmChartConfig with HTTP->HTTPS redirect and Cloudflare trusted IPs
  - TLSStore default cert pointing to origin-ca-tls secret
  - TLSOption enforcing TLS 1.2 minimum on origin
  - ArgoCD IngressRouteTCP for TLS passthrough at argocd.job-hunt.dev
  - ArgoCD admin password change script with bcrypt hashing
affects: [18-03, verification, deployment]

# Tech tracking
tech-stack:
  added: []
  patterns: [HelmChartConfig for K3s Traefik customization, TLSStore default cert, TLSOption default, IngressRouteTCP TLS passthrough]

key-files:
  created:
    - infra/k8s/traefik/traefik-config.yaml
    - infra/k8s/traefik/tls-store.yaml
    - infra/k8s/traefik/tls-option.yaml
    - infra/k8s/argocd/ingress-route-tcp.yaml
    - infra/scripts/change-argocd-password.sh
  modified: []

key-decisions:
  - "TLSStore named 'default' in kube-system for automatic Traefik default cert"
  - "Cloudflare trusted IPs on BOTH web and websecure entrypoints for real client IPs"
  - "ArgoCD TLS passthrough (not termination) so ArgoCD handles its own TLS"
  - "ArgoCD Application specs unchanged -- argocd-secret not managed by app-of-apps"
  - "Password script uses env var for bcrypt to prevent shell injection"

patterns-established:
  - "HelmChartConfig in kube-system: K3s Traefik customization via Helm values override"
  - "TLSStore default + TLSOption default: automatic TLS for all Ingress without per-resource config"
  - "IngressRouteTCP passthrough: exception to standard Ingress API for services managing own TLS"

requirements-completed: [DNS-05]

# Metrics
duration: 3min
completed: 2026-03-25
---

# Phase 18 Plan 02: Traefik TLS & ArgoCD Passthrough Summary

**Traefik HTTPS redirect, default Origin CA cert, TLS 1.2 minimum, and ArgoCD TLS passthrough at argocd.job-hunt.dev with admin password change script**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-25T12:49:13Z
- **Completed:** 2026-03-25T12:52:00Z
- **Tasks:** 2
- **Files created:** 5

## Accomplishments
- Traefik configured with HTTP-to-HTTPS redirect, Cloudflare trusted IPs on both entrypoints, and TLS enabled on websecure
- Default TLS certificate (TLSStore) and minimum TLS 1.2 (TLSOption) applied automatically to all connections
- ArgoCD exposed at argocd.job-hunt.dev via IngressRouteTCP with TLS passthrough
- Admin password change script with secure bcrypt hashing (python3 env var + htpasswd fallback)

## Task Commits

Each task was committed atomically:

1. **Task 1: Traefik TLS configuration manifests** - `2ea498f` (feat)
2. **Task 2: ArgoCD TLS passthrough and password script** - `258d2af` (feat)

## Files Created/Modified
- `infra/k8s/traefik/traefik-config.yaml` - HelmChartConfig for HTTP redirect + Cloudflare trusted IPs
- `infra/k8s/traefik/tls-store.yaml` - TLSStore default cert pointing to origin-ca-tls secret
- `infra/k8s/traefik/tls-option.yaml` - TLSOption enforcing TLS 1.2 minimum
- `infra/k8s/argocd/ingress-route-tcp.yaml` - IngressRouteTCP for ArgoCD TLS passthrough
- `infra/scripts/change-argocd-password.sh` - Admin password change helper with bcrypt

## Decisions Made
- TLSStore and TLSOption both named "default" in kube-system namespace so Traefik applies them automatically to all TLS connections
- Cloudflare IPv4 trusted IPs configured on BOTH web and websecure entrypoints (not just websecure) so X-Forwarded-For is correct regardless of entry path
- ArgoCD uses TLS passthrough (IngressRouteTCP) rather than standard Ingress with TLS termination -- ArgoCD handles its own TLS with the Origin CA cert
- ArgoCD Application specs (prod.yaml, staging.yaml) left unchanged because argocd-secret is Helm-managed, not synced by any ArgoCD Application
- Password script passes password to python3 via environment variable (not string interpolation) to prevent shell injection

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Traefik TLS chain fully defined: default cert + TLS 1.2 + HTTPS redirect + Cloudflare trusted IPs
- ArgoCD IngressRouteTCP ready for TLS passthrough once Origin CA cert is sealed and applied
- Plan 03 (OpenTofu Cloudflare + verification) can proceed

## Self-Check: PASSED

- All 5 created files verified on disk
- Commits 2ea498f and 258d2af verified in git log

---
*Phase: 18-domain-tls*
*Completed: 2026-03-25*
