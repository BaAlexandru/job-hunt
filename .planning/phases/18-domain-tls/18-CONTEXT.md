# Phase 18: Domain & TLS - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Make the application publicly accessible at job-hunt.dev with HTTPS, staging subdomain (staging.job-hunt.dev), and ArgoCD UI subdomain (argocd.job-hunt.dev). Includes Cloudflare DNS records (via OpenTofu), Origin CA cert installation on the cluster, Traefik TLS configuration, ArgoCD UI exposure, and end-to-end verification. Does NOT include application feature changes or data store modifications.

</domain>

<decisions>
## Implementation Decisions

### Origin CA cert setup
- Wildcard Cloudflare Origin CA cert covering `*.job-hunt.dev` + `job-hunt.dev`
- 15-year lifetime (maximum, set-and-forget for personal project)
- Generated manually via Cloudflare dashboard, then sealed with kubeseal
- Stored as SealedSecret in kube-system namespace (consistent with Phase 17 pattern)
- Configured as Traefik's default TLS certificate — all Ingress resources get TLS automatically without per-namespace secrets or spec.tls sections
- Existing Ingress resources remain unchanged (no TLS sections needed)

### HTTPS enforcement
- Both Cloudflare "Always Use HTTPS" AND Traefik HTTP→HTTPS redirect enabled (defense in depth)
- HSTS enabled via Cloudflare zone settings
- Traefik configured to redirect all HTTP (port 80) traffic to HTTPS (port 443)

### ArgoCD UI exposure
- Exposed at argocd.job-hunt.dev
- ArgoCD built-in authentication only (admin user + password) — no Cloudflare Access or IP restrictions
- Admin password changed from auto-generated default via helper script; password stored in user's password manager, not Git
- TLS passthrough via Traefik IngressRouteTCP CRD (exception to standard Ingress API used elsewhere) — ArgoCD handles its own TLS termination
- ArgoCD server configured with the Cloudflare Origin CA wildcard cert (replacing its self-signed cert) to satisfy Full (Strict) mode
- Ingress resource lives in the argocd namespace directly (not managed by app-of-apps to avoid circular dependency)
- gRPC endpoint NOT exposed externally — argocd CLI access via kubectl port-forward only

### Cloudflare DNS configuration
- Managed via OpenTofu Cloudflare provider in existing `infra/tofu/main/` module
- 3 A records: `job-hunt.dev`, `staging.job-hunt.dev`, `argocd.job-hunt.dev` — all pointing to Elastic IP from `aws_eip.main`
- All records Cloudflare-proxied (orange cloud) for CDN + DDoS + TLS termination
- Only manage new A records + SSL settings — do NOT import or manage existing DNS records (MX, TXT, etc.)
- Cloudflare SSL/TLS settings managed via OpenTofu: Full (Strict) mode, Always Use HTTPS, HSTS
- API token provided via gitignored .tfvars file (CLOUDFLARE_API_TOKEN)
- Zone ID provided as variable in same gitignored .tfvars file
- DNS records reference `aws_eip.main.public_ip` output directly (single tofu apply manages everything)

### Rollout order
1. Generate Origin CA cert via Cloudflare dashboard + seal into cluster
2. Configure Traefik default cert + ArgoCD TLS (Origin CA cert on ArgoCD server)
3. Apply OpenTofu for DNS records + SSL settings
4. Verify each endpoint

### Verification approach
- Automated verification script: `infra/scripts/verify-domain.sh`
  - DNS resolution for all 3 hosts
  - HTTPS 200 on job-hunt.dev
  - HTTPS 200 on staging.job-hunt.dev (after scaling up)
  - ArgoCD login page loads at argocd.job-hunt.dev
  - HTTP→HTTPS redirect verification
  - Cert issuer is Cloudflare Origin CA
  - HSTS header present
- Manual browser verification: full app smoke test
  - Page loads with HTTPS padlock
  - Login flow works
  - Create a test company, verify API calls work through Cloudflare proxy
  - ArgoCD login page accessible
- Staging scaled up for verification (staging-up.sh), then scaled back down after

### Claude's Discretion
- Traefik default cert configuration method (HelmChartConfig, static config, or TLSStore CRD)
- Exact ArgoCD TLS cert configuration (argocd-server args or argocd-cmd-params-cm ConfigMap)
- Verification script implementation details (curl flags, jq parsing, output format)
- OpenTofu Cloudflare resource structure and variable naming
- ArgoCD admin password change method (argocd CLI vs bcrypt in argocd-secret)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Prior phase context
- `.planning/phases/17-app-deployment-argocd/17-CONTEXT.md` — ArgoCD full install with UI in `argocd` namespace, Sealed Secrets pattern, app-of-apps structure, CI-driven image tag updates
- `.planning/phases/15-k3s-cluster-setup/15-CONTEXT.md` — Traefik bundled with K3s (default), Kustomize directory layout, standard K8s Ingress API decision, SSH tunnel for kubectl
- `.planning/phases/14-aws-infrastructure/14-CONTEXT.md` — EC2 instance details, Elastic IP output, OpenTofu module structure (bootstrap/ + main/), security groups (ports 22, 80, 443 open)

### Existing K8s manifests
- `infra/k8s/overlays/prod/ingress.yaml` — Production Ingress with host `job-hunt.dev`, routes /api→backend:8080 and /→frontend:3000
- `infra/k8s/overlays/staging/ingress.yaml` — Staging Ingress with host `staging.job-hunt.dev`, same routing
- `infra/k8s/overlays/prod/configmap.yaml` — Backend/frontend ConfigMaps with FRONTEND_BASE_URL=https://job-hunt.dev
- `infra/k8s/overlays/staging/configmap.yaml` — Staging ConfigMaps with FRONTEND_BASE_URL=https://staging.job-hunt.dev

### Infrastructure
- `infra/tofu/main/outputs.tf` — elastic_ip output for DNS A records
- `infra/tofu/main/variables.tf` — Existing variables (will add Cloudflare vars)
- `infra/argocd/values.yaml` — ArgoCD Helm values (may need TLS config updates)
- `.planning/ROADMAP.md` §Memory Budget — m7i-flex.large (8GB), comfortable headroom for all services
- `.planning/REQUIREMENTS.md` §DNS-01..DNS-05 — All 5 DNS/TLS requirements for this phase

### Convenience scripts
- `infra/scripts/seal-secrets.sh` — Sealed Secrets helper (pattern for Origin CA cert sealing)
- `infra/scripts/staging-up.sh` / `staging-down.sh` — Staging scale scripts (used during verification)
- `infra/scripts/connect.sh` — SSH tunnel for kubectl access

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `infra/k8s/overlays/{prod,staging}/ingress.yaml` — Ingress resources already configured with correct hostnames and routing; no TLS sections needed (Traefik default cert covers it)
- `infra/scripts/seal-secrets.sh` — Pattern for generating SealedSecrets; Origin CA cert sealing can follow same approach
- `infra/tofu/main/` — Existing OpenTofu module with AWS provider; Cloudflare provider added here
- `infra/argocd/values.yaml` — ArgoCD Helm values for configuring TLS cert on argocd-server

### Established Patterns
- SealedSecrets for all credentials in Git (Phase 17) — Origin CA cert follows same pattern
- Standard K8s Ingress API for app routing — ArgoCD is the exception (IngressRouteTCP for TLS passthrough)
- OpenTofu outputs consumed by scripts and cross-referenced between resources
- Gitignored .tfvars for sensitive values (bootstrap module already has this pattern)

### Integration Points
- Cloudflare DNS A records reference `aws_eip.main.public_ip` from existing OpenTofu module
- Traefik default cert must be in kube-system namespace where Traefik runs
- ArgoCD server TLS cert config requires updating argocd-server Deployment or ConfigMap in argocd namespace
- IngressRouteTCP CRD for ArgoCD requires Traefik CRD support (bundled with K3s default Traefik install)

</code_context>

<specifics>
## Specific Ideas

- User wants full Infrastructure-as-Code for Cloudflare (not manual dashboard config) — OpenTofu Cloudflare provider manages DNS records and SSL settings
- ArgoCD TLS passthrough chosen over --insecure mode despite complexity — user values ArgoCD handling its own TLS
- Origin CA cert installed on ArgoCD server to maintain Full (Strict) across all subdomains consistently
- Defense in depth for HTTPS: both Cloudflare and Traefik enforce redirect
- Full app smoke test during verification (login, create company, verify API through Cloudflare proxy) — not just infrastructure checks
- Staging must be fully scaled up and tested, not just route-verified

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 18-domain-tls*
*Context gathered: 2026-03-25*
