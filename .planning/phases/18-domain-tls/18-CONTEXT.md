# Phase 18: Domain & TLS - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Make the application publicly accessible at job-hunt.dev with HTTPS, staging subdomain (staging.job-hunt.dev), and ArgoCD UI subdomain (argocd.job-hunt.dev). Includes Cloudflare DNS records (via OpenTofu), Origin CA cert generation (via OpenTofu), Traefik TLS configuration, ArgoCD UI exposure, origin security hardening, and end-to-end verification. Does NOT include application feature changes or data store modifications.

</domain>

<decisions>
## Implementation Decisions

### Origin CA cert setup
- Wildcard Cloudflare Origin CA cert covering `*.job-hunt.dev` + `job-hunt.dev`
- 15-year lifetime (maximum, set-and-forget for personal project)
- Generated via OpenTofu `cloudflare_origin_ca_certificate` + `tls_private_key` resources (IaC-managed, not manual dashboard)
- Cert and private key extracted from `tofu output -raw` and sealed with kubeseal into K8s TLS Secrets
- TLS Secret in kube-system namespace for Traefik default cert (SealedSecret, cluster-wide scope)
- TLS Secret in argocd namespace for ArgoCD server cert (SealedSecret, namespace-scoped)
- Configured as Traefik's default TLS certificate via TLSStore CRD — all Ingress resources get TLS automatically without per-namespace secrets or spec.tls sections
- Existing Ingress resources remain unchanged (no TLS sections needed)

### HTTPS enforcement
- Both Cloudflare "Always Use HTTPS" AND Traefik HTTP→HTTPS redirect enabled (defense in depth)
- HSTS enabled via Cloudflare zone settings (max-age 31536000, includeSubDomains, preload, nosniff)
- Traefik configured to redirect all HTTP (port 80) traffic to HTTPS (port 443) via HelmChartConfig

### Security response headers
- Configured via Cloudflare Transform Rules (managed via OpenTofu `cloudflare_ruleset` resource, phase `http_response_headers_transform`)
- Headers applied to all proxied traffic:
  - `X-Content-Type-Options: nosniff` — prevent MIME-type sniffing
  - `X-Frame-Options: DENY` — prevent clickjacking
  - `Referrer-Policy: strict-origin-when-cross-origin` — limit referrer leakage
  - `Permissions-Policy: camera=(), microphone=(), geolocation=()` — restrict browser APIs
- Content-Security-Policy deferred — requires careful tuning per app routes, better handled at application layer

### Traefik proxy configuration
- HelmChartConfig includes `forwardedHeaders.trustedIPs` with Cloudflare IPv4 ranges — required to get real client IPs in X-Forwarded-For when behind Cloudflare proxy
- Without trusted IPs, application logs show Cloudflare edge IPs instead of real clients, and IP-based security controls are ineffective
- TLSOption CRD with `minVersion: VersionTLS12` applied as default — enforces TLS 1.2+ on origin side even if Cloudflare is bypassed

### Origin access restriction (Cloudflare-only)
- AWS security group ingress rules for ports 80 and 443 restricted to Cloudflare IPv4 ranges (https://www.cloudflare.com/ips/) instead of 0.0.0.0/0
- Managed via OpenTofu by updating `aws_security_group.main` ingress rules
- Prevents direct IP access bypassing Cloudflare — all HTTP/HTTPS traffic must flow through Cloudflare proxy
- Eliminates: WAF bypass, DDoS bypass, origin fingerprinting, direct cert inspection
- SSH (port 22) remains restricted to `var.allowed_ssh_cidr` (unchanged)

### ArgoCD UI exposure
- Exposed at argocd.job-hunt.dev
- ArgoCD built-in authentication only (admin user + strong password stored in password manager)
- **Accepted risk:** No MFA or Cloudflare Access — mitigated by: (1) Cloudflare proxy hides origin IP, (2) ArgoCD built-in account lockout after 5 consecutive failed login attempts, (3) strong randomly-generated password, (4) origin access restricted to Cloudflare IPs only (no direct access)
- **Follow-up recommendation:** Add Cloudflare Access zero-trust (free tier, 50 users) for MFA in a future phase
- Admin password changed from auto-generated default via helper script; password stored in user's password manager, not Git
- ArgoCD `argocd-secret` is Helm-managed (not managed by app-of-apps), so no `ignoreDifferences` needed — password changes persist because no ArgoCD Application syncs the secret
- TLS passthrough via Traefik IngressRouteTCP CRD (exception to standard Ingress API used elsewhere) — ArgoCD handles its own TLS termination
- ArgoCD server configured with the Cloudflare Origin CA wildcard cert (replacing its self-signed cert) to satisfy Full (Strict) mode
- Ingress resource lives in the argocd namespace directly (not managed by app-of-apps to avoid circular dependency)
- gRPC endpoint NOT exposed externally — argocd CLI access via kubectl port-forward only

### Cloudflare DNS configuration
- Managed via OpenTofu Cloudflare provider in existing `infra/tofu/main/` module
- 3 A records: `job-hunt.dev`, `staging.job-hunt.dev`, `argocd.job-hunt.dev` — all pointing to Elastic IP from `aws_eip.main`
- All records Cloudflare-proxied (orange cloud) for CDN + DDoS + TLS termination
- Only manage new A records + SSL settings — do NOT import or manage existing DNS records (MX, TXT, etc.)
- Cloudflare SSL/TLS settings managed via OpenTofu: Full (Strict) mode, Always Use HTTPS, HSTS, TLS 1.3, min TLS 1.2, automatic HTTPS rewrites
- Cloudflare WAF: Free-tier managed rulesets auto-enabled on proxied zones (OWASP Top 10, known CVEs). No custom WAF rules needed for initial deployment — Cloudflare's default protection is sufficient for a personal project
- API token provided via gitignored .tfvars file (CLOUDFLARE_API_TOKEN) — token scoped to Zone:DNS:Edit, Zone:Settings:Edit, Zone:SSL and Certificates:Edit, Zone:Zone Rulesets:Edit
- Zone ID provided as variable in same gitignored .tfvars file
- DNS records reference `aws_eip.main.public_ip` output directly (single tofu apply manages everything)
- Origin CA cert generated in same apply via `cloudflare_origin_ca_certificate` resource

### Rollout order
1. Add Cloudflare provider, variables, Origin CA cert, DNS records, SSL settings, security headers, and SG restriction to OpenTofu → `tofu apply` (creates everything in one apply)
2. Extract cert + key from tofu output → seal into K8s secrets (kube-system namespace for Traefik, argocd namespace for ArgoCD)
3. Apply K8s manifests: TLSStore CRD, TLSOption CRD, HelmChartConfig (HTTP→HTTPS redirect + trustedIPs), ArgoCD IngressRouteTCP, ArgoCD TLS cert config
4. Change ArgoCD admin password via helper script
5. Verify each endpoint (automated + manual)

### Verification approach
- Automated verification script: `infra/scripts/verify-phase18.sh`
  - DNS resolution for all 3 hosts
  - HTTPS 200 on job-hunt.dev
  - HTTPS 200 on staging.job-hunt.dev (after scaling up)
  - ArgoCD login page loads at argocd.job-hunt.dev
  - HTTP→HTTPS redirect verification
  - Cert issuer is Cloudflare Origin CA
  - HSTS header present
  - Security headers present (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
  - Direct IP access blocked (connection refused or timeout on ports 80/443 from non-Cloudflare IP)
- Manual browser verification: full app smoke test
  - Page loads with HTTPS padlock
  - Login flow works (verify cookie Secure attribute set)
  - Create a test company, verify API calls work through Cloudflare proxy (verify CORS headers correct)
  - ArgoCD login page accessible
- Staging scaled up for verification (staging-up.sh), then scaled back down after

### Claude's Discretion
- Exact Traefik TLSStore + TLSOption + HelmChartConfig YAML structure
- ArgoCD TLS cert configuration method (argocd-server args, argocd-cmd-params-cm ConfigMap, or Helm values)
- Verification script implementation details (curl flags, jq parsing, output format)
- OpenTofu Cloudflare resource structure and variable naming
- ArgoCD admin password change method (argocd CLI vs bcrypt in argocd-secret)
- Cloudflare Transform Rules expression syntax for security headers
- Whether to use individual SG ingress rules per Cloudflare CIDR or a single rule with list

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
- `infra/tofu/main/providers.tf` — Existing AWS provider (will add Cloudflare provider)
- `infra/tofu/main/main.tf` — Security group with ports 80/443 open to 0.0.0.0/0 (will restrict to Cloudflare IPs)
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
- `infra/scripts/seal-secrets.sh` — Pattern for generating SealedSecrets; Origin CA cert sealing can follow same approach (but targeting kube-system namespace with cluster-wide scope)
- `infra/tofu/main/` — Existing OpenTofu module with AWS provider; Cloudflare provider + tls provider added here
- `infra/argocd/values.yaml` — ArgoCD Helm values for configuring TLS cert on argocd-server

### Established Patterns
- SealedSecrets for all credentials in Git (Phase 17) — Origin CA cert follows same pattern
- Standard K8s Ingress API for app routing — ArgoCD is the exception (IngressRouteTCP for TLS passthrough)
- OpenTofu outputs consumed by scripts and cross-referenced between resources
- Gitignored .tfvars for sensitive values (bootstrap module already has this pattern)

### Integration Points
- Cloudflare DNS A records reference `aws_eip.main.public_ip` from existing OpenTofu module
- Cloudflare Origin CA cert generated in same module — cert/key extracted via `tofu output -raw`
- AWS security group updated in same apply to restrict ports 80/443 to Cloudflare IPs
- Traefik default cert must be in kube-system namespace where Traefik runs
- ArgoCD server TLS cert config requires updating argocd-server Deployment or ConfigMap in argocd namespace
- IngressRouteTCP CRD for ArgoCD requires Traefik CRD support (bundled with K3s default Traefik install)

</code_context>

<specifics>
## Specific Ideas

- User wants full Infrastructure-as-Code for Cloudflare — OpenTofu Cloudflare provider manages DNS records, SSL settings, Origin CA cert, and security headers
- ArgoCD TLS passthrough chosen over --insecure mode despite complexity — user values ArgoCD handling its own TLS
- Origin CA cert installed on ArgoCD server to maintain Full (Strict) across all subdomains consistently
- Defense in depth for HTTPS: both Cloudflare and Traefik enforce redirect
- Defense in depth for origin protection: AWS SG restricts to Cloudflare IPs + Cloudflare proxy hides origin
- Security response headers managed at Cloudflare edge (Transform Rules) — consistent with IaC approach
- Full app smoke test during verification (login, create company, verify API through Cloudflare proxy) — not just infrastructure checks
- Staging must be fully scaled up and tested, not just route-verified
- CORS and cookie security attributes (Secure, SameSite) verified during manual smoke test — ConfigMaps already have correct FRONTEND_BASE_URL values from Phase 15/17

</specifics>

<deferred>
## Deferred Ideas

- Cloudflare Access zero-trust for ArgoCD MFA (free tier, 50 users) — recommended follow-up after initial deployment
- Content-Security-Policy header — requires per-route tuning, better handled at application layer in a future phase
- Custom Cloudflare WAF rules — free-tier auto-protection sufficient for personal project initially
- Cloudflare IPv6 ranges in AWS SG — add when IPv6 support is needed

</deferred>

<skills>
## Skills & Documentation

### Recommended Skills

| Skill | Tier | Relevance |
|---|---|---|
| `opentofu-cloudflare` | Essential | IaC for DNS records, SSL zone settings, Origin CA cert generation via `cloudflare_origin_ca_certificate` + `tls_private_key`, provider config, .tfvars patterns (~25% of phase work) |
| `traefik-k3s` | Essential | TLSStore default cert CRD, TLSOption min TLS version, IngressRouteTCP for ArgoCD TLS passthrough, K3s HelmChartConfig for HTTP→HTTPS redirect + Cloudflare trusted IPs, entrypoint config (~20% of phase work) |
| `cloudflare` | Essential | Cloudflare platform reference — WAF managed rulesets (auto-enabled on free tier), Transform Rules for security headers (`cloudflare_ruleset` http_response_headers_transform phase), DDoS protection (auto-enabled), API token scoping (~10% of phase work) |
| `kubernetes-specialist` | Essential | Ingress resources, TLS Secrets in kube-system, namespace routing, SealedSecret for Origin CA cert (~15% of phase work) |
| `argocd-expert` | Essential | ArgoCD TLS cert configuration (argocd-cmd-params-cm or server args), IngressRouteTCP exposure at argocd.job-hunt.dev, admin password management, ignoreDifferences for argocd-secret, Helm values updates (~18% of phase) |
| `helm-chart-scaffolding` | Supporting | ArgoCD Helm values configuration for TLS cert, server flags (--insecure=false), and IngressRouteTCP resource generation |
| `sequential-thinking` | Supporting | Multi-step reasoning for TLS chain (Cloudflare → Traefik → ArgoCD), rollout ordering, debugging connectivity across DNS/proxy/ingress/pod layers |
| `systematic-debugging` | Supporting | Debugging TLS handshake failures, DNS propagation delays, Ingress routing issues, Cloudflare Full (Strict) certificate validation errors on live cluster |
| `conventional-commit` | Workflow | Structured commit messages during phase execution |
| `verification-before-completion` | Workflow | Verify all 5 DNS requirements (DNS-01..05) are met, run verify-phase18.sh + manual smoke test before marking phase complete |

### Context7 Documentation Sources

Downstream agents SHOULD query these Context7 library IDs for up-to-date documentation during planning and implementation.

| Technology | Context7 Library ID | Snippets | Trust Score | Key Topics |
|---|---|---|---|---|
| Cloudflare Terraform Provider | `/cloudflare/terraform-provider-cloudflare` | 1,586 | 7.8 | `cloudflare_dns_record` (A records, proxied), `cloudflare_zone_setting` (v5: individual settings for SSL mode, Always HTTPS, HSTS), `cloudflare_origin_ca_certificate`, `cloudflare_ruleset` (Transform Rules for security headers), zone data source |
| Traefik (official docs) | `/websites/doc_traefik_io_traefik` | 8,368 | 8.0 | TLSStore default cert, TLSOption min TLS version, IngressRouteTCP CRD for TLS passthrough, entrypoints redirect (web→websecure), HelmChartConfig for K3s Traefik customization, forwardedHeaders trustedIPs |
| Traefik Helm Chart | `/traefik/traefik-helm-chart` | 104 | 8.0 | K3s uses this chart internally — HelmChartConfig values override Traefik defaults (ports, TLS, middleware, forwardedHeaders) |
| ArgoCD (Argo Helm) | `/argoproj/argo-helm` | 122 | 9.3 | argocd-server TLS cert configuration, Helm values for `server.certificate`, `server.ingress`, `server.extraArgs`, ignoreDifferences for secrets |
| Kubernetes | `/websites/kubernetes_io` | 15,032 | 9.9 | Ingress TLS, IngressClass, TLS Secrets (kubernetes.io/tls type), cross-namespace secret references |
| Kustomize | `/kubernetes-sigs/kustomize` | 1,397 | 9.1 | Overlay patches, resource references (minimal use in this phase) |
| Sealed Secrets | `/bitnami-labs/sealed-secrets` | 246 | 7.9 | kubeseal --scope cluster-wide for cross-namespace TLS secret, SealedSecret for kube-system namespace |
| OpenTofu | `/opentofu/opentofu` | 3,996 | 7.6 | Provider configuration, S3 backend (existing), variable files, output references between resources, tls_private_key + tls_cert_request resources |
| Terraform AWS Provider | `/hashicorp/terraform-provider-aws` | 22,352 | 7.9 | `aws_eip` output referenced by Cloudflare A records, `aws_security_group` ingress rules update for Cloudflare IP restriction |
| Helm | `/websites/helm_sh` | 1,315 | 9.9 | `helm upgrade` with `--set` overrides for ArgoCD, chart values structure |

### Skill Gap Coverage

The following phase technologies have no dedicated skill — Context7 docs above fill the gap:

- **Shell scripting** — No skill needed; verify-phase18.sh is straightforward curl/dig scripting

### Important: Cloudflare Provider v5 Change

The Cloudflare Terraform provider v5 replaced the monolithic `cloudflare_zone_settings_override` resource with individual `cloudflare_zone_setting` resources (one per setting). The `opentofu-cloudflare` skill documents both patterns. Use the v5 pattern (`cloudflare_zone_setting` with `setting_id`) for new code.

</skills>

---

*Phase: 18-domain-tls*
*Context gathered: 2026-03-25*
*Security audit: 2026-03-25 — 15 findings (0 critical, 2 high, 6 medium, 7 low) — all addressed*
