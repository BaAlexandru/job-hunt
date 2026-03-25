---
phase: 18-domain-tls
verified: 2026-03-25T14:30:00Z
status: passed
score: 14/14 must-haves verified
re_verification: false
human_verification:
  - test: "Open https://job-hunt.dev in browser and confirm padlock, login flow, and API calls work through Cloudflare"
    expected: "HTTPS padlock, login succeeds, no CORS errors, Secure cookie attribute set"
    why_human: "Browser UI and DevTools inspection required to confirm TLS indicator and cookie flags"
  - test: "Scale up staging (bash infra/scripts/staging-up.sh), visit https://staging.job-hunt.dev, scale back down"
    expected: "Staging environment loads (may show empty state), scales down cleanly without ArgoCD selfHeal reverting"
    why_human: "Requires live cluster interaction; staging is scale-to-zero by design during normal operation"
  - test: "Visit https://argocd.job-hunt.dev and log in with admin credentials"
    expected: "ArgoCD login page loads, login succeeds, all Applications visible"
    why_human: "Requires live cluster and knowledge of admin password set during execution"
---

# Phase 18: Domain & TLS Verification Report

**Phase Goal:** Domain & TLS — Configure job-hunt.dev domain with Cloudflare DNS, Origin CA TLS certificates, Traefik HTTPS ingress, and ArgoCD subdomain exposure
**Verified:** 2026-03-25T14:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | OpenTofu creates 3 Cloudflare A records (root, staging, argocd) pointing to Elastic IP | VERIFIED | `cloudflare.tf` lines 32-57: `cloudflare_dns_record.root` (name=job-hunt.dev), `.staging`, `.argocd`, all with `content = aws_eip.main.public_ip`, `proxied = true` |
| 2 | OpenTofu generates a wildcard Origin CA cert covering `*.job-hunt.dev` + `job-hunt.dev` | VERIFIED | `cloudflare.tf` lines 62-82: `tls_private_key`, `tls_cert_request` with `dns_names = ["job-hunt.dev", "*.job-hunt.dev"]`, `cloudflare_origin_ca_certificate.wildcard` with `requested_validity = 5475` |
| 3 | Cloudflare SSL/TLS is set to Full (Strict) with HSTS and Always Use HTTPS enabled | VERIFIED | `cloudflare.tf` lines 87-129: `cloudflare_zone_setting.ssl` value=`"strict"`, `.always_use_https` value=`"on"`, `.security_header` with `max_age = 31536000`, `include_subdomains = true`, `preload = true` |
| 4 | AWS security group ports 80/443 are restricted to Cloudflare IPv4 CIDRs only | VERIFIED | `main.tf` lines 65-79: ports 80 and 443 both use `cidr_blocks = local.cloudflare_ipv4_cidrs`; 0.0.0.0/0 only appears on route table default route and egress rule (both correct) |
| 5 | Security response headers are configured (X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy) | VERIFIED | `security-headers-middleware.yaml`: Traefik Middleware in kube-system with all 4 headers plus `stsSeconds: 31536000`; both prod and staging ingress.yaml reference `kube-system-security-headers@kubernetescrd` |
| 6 | seal-origin-ca.sh can extract cert/key from tofu output and seal into SealedSecrets | VERIFIED | `seal-origin-ca.sh`: calls `tofu -chdir=infra/tofu/main output -raw origin_ca_cert/origin_ca_key`, uses temp files (Windows-compatible), produces two SealedSecrets with `--scope=cluster-wide` for kube-system and `--namespace=argocd` for ArgoCD |
| 7 | Traefik redirects all HTTP traffic to HTTPS via HelmChartConfig | VERIFIED | `traefik-config.yaml`: `kind: HelmChartConfig`, `ports.web.redirectTo.port: websecure`, both web and websecure entrypoints have `forwardedHeaders.trustedIPs` with all 15 Cloudflare CIDRs |
| 8 | Traefik uses Origin CA cert as default TLS certificate via TLSStore | VERIFIED | `tls-store.yaml`: `kind: TLSStore`, `name: default`, `namespace: kube-system`, `spec.defaultCertificate.secretName: origin-ca-tls` |
| 9 | Traefik enforces TLS 1.2 minimum via TLSOption | VERIFIED | `tls-option.yaml`: `kind: TLSOption`, `name: default`, `namespace: kube-system`, `spec.minVersion: VersionTLS12` |
| 10 | ArgoCD is accessible at argocd.job-hunt.dev via TLS passthrough | VERIFIED | `ingress-route-tcp.yaml`: `kind: IngressRouteTCP`, `namespace: argocd`, `match: HostSNI(\`argocd.job-hunt.dev\`)`, `tls.passthrough: true`, service `argocd-server` port 443 |
| 11 | Origin CA cert is sealed into K8s SealedSecrets for both Traefik and ArgoCD | VERIFIED | `infra/k8s/traefik/origin-ca-sealed-secret.yaml` (6289 bytes, `kind: SealedSecret`), `infra/k8s/argocd/argocd-tls-sealed-secret.yaml` (6147 bytes, `kind: SealedSecret`) — both substantive files, not stubs |
| 12 | HTTP requests redirect to HTTPS | VERIFIED | Cloudflare zone setting `always_use_https = "on"` + Traefik HelmChartConfig `redirectTo.port: websecure`; SUMMARY confirms `PASS: Redirect: HTTP->HTTPS on job-hunt.dev` in verify-phase18.sh output |
| 13 | staging.job-hunt.dev subdomain is configured | VERIFIED | `cloudflare_dns_record.staging` (name=staging, proxied=true), staging ingress.yaml host=staging.job-hunt.dev, staging overlay with security-headers middleware annotation |
| 14 | ArgoCD admin password persists (no Application manages argocd-secret) | VERIFIED | `infra/argocd/apps/prod.yaml` and `staging.yaml` target `jobhunt-prod`/`jobhunt-staging` namespaces only; `change-argocd-password.sh` uses `kubectl -n argocd patch secret argocd-secret` with bcrypt via env var |

**Score:** 14/14 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `infra/tofu/main/cloudflare.tf` | DNS records, Origin CA cert, zone settings | VERIFIED | 138 lines; 3 DNS records, Origin CA, 6 zone settings, all 15 CIDRs in locals |
| `infra/tofu/main/cloudflare-variables.tf` | Cloudflare API token (sensitive) and zone ID | VERIFIED | `cloudflare_api_token` with `sensitive = true`, `cloudflare_zone_id` |
| `infra/tofu/main/cloudflare-outputs.tf` | Sensitive outputs for cert and key | VERIFIED | `origin_ca_cert` and `origin_ca_key` both `sensitive = true` |
| `infra/tofu/main/providers.tf` | Cloudflare ~> 5.17 and tls ~> 4.0 providers | VERIFIED | Both providers present with correct versions; `provider "cloudflare"` block with `api_token` |
| `infra/tofu/main/main.tf` | SG ports 80/443 restricted to Cloudflare CIDRs | VERIFIED | Both ingress rules use `local.cloudflare_ipv4_cidrs`; descriptions updated to "(Cloudflare only)" |
| `infra/scripts/seal-origin-ca.sh` | SealedSecret generation for Origin CA cert | VERIFIED | Executable; uses temp files (Windows-compatible); `--scope=cluster-wide` for Traefik; `--namespace=argocd` for ArgoCD |
| `infra/scripts/verify-phase18.sh` | End-to-end automated verification | VERIFIED | Executable; DNS helper with dig/nslookup fallback; arithmetic fix (`PASS=$((PASS+1))`); all header checks present; exits 0 on all pass |
| `infra/k8s/traefik/traefik-config.yaml` | HelmChartConfig for HTTP redirect + trusted IPs | VERIFIED | `kind: HelmChartConfig`; `redirectTo.port: websecure`; 15 CIDRs on both web and websecure entrypoints |
| `infra/k8s/traefik/tls-store.yaml` | TLSStore default cert | VERIFIED | `name: default`, `namespace: kube-system`, `secretName: origin-ca-tls` |
| `infra/k8s/traefik/tls-option.yaml` | TLSOption TLS 1.2 minimum | VERIFIED | `name: default`, `namespace: kube-system`, `minVersion: VersionTLS12` |
| `infra/k8s/argocd/ingress-route-tcp.yaml` | IngressRouteTCP for ArgoCD TLS passthrough | VERIFIED | `HostSNI(\`argocd.job-hunt.dev\`)`, `passthrough: true`, service `argocd-server` port 443 |
| `infra/scripts/change-argocd-password.sh` | ArgoCD admin password change helper | VERIFIED | Executable; `read -rs NEW_PASSWORD`; bcrypt via env var; `kubectl -n argocd patch secret argocd-secret`; rollout restart |
| `infra/k8s/traefik/security-headers-middleware.yaml` | Traefik Middleware for security headers | VERIFIED | `kind: Middleware`; all 4 custom headers plus `stsSeconds: 31536000`, `stsIncludeSubdomains: true`, `stsPreload: true` |
| `infra/k8s/traefik/origin-ca-sealed-secret.yaml` | SealedSecret for Traefik TLS cert (cluster-wide) | VERIFIED | 6289 bytes; `kind: SealedSecret`; substantive encrypted data |
| `infra/k8s/argocd/argocd-tls-sealed-secret.yaml` | SealedSecret for ArgoCD TLS cert (namespace-scoped) | VERIFIED | 6147 bytes; `kind: SealedSecret`; substantive encrypted data |
| `infra/k8s/overlays/prod/ingress.yaml` | Ingress with security-headers middleware annotation | VERIFIED | `traefik.ingress.kubernetes.io/router.middlewares: kube-system-security-headers@kubernetescrd` |
| `infra/k8s/overlays/staging/ingress.yaml` | Ingress with security-headers middleware annotation | VERIFIED | `traefik.ingress.kubernetes.io/router.middlewares: kube-system-security-headers@kubernetescrd`; host=staging.job-hunt.dev |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `infra/tofu/main/cloudflare.tf` | `infra/tofu/main/main.tf` | `aws_eip.main.public_ip` in DNS records | WIRED | DNS records use `content = aws_eip.main.public_ip`; `aws_eip.main` defined in main.tf lines 119-126 |
| `infra/scripts/seal-origin-ca.sh` | `infra/tofu/main/cloudflare-outputs.tf` | `tofu output -raw origin_ca_cert/key` | WIRED | `CERT=$(tofu -chdir=infra/tofu/main output -raw origin_ca_cert)`, `KEY=...origin_ca_key`; outputs declared as sensitive in cloudflare-outputs.tf |
| `infra/k8s/traefik/tls-store.yaml` | `origin-ca-tls` Secret in kube-system | `secretName: origin-ca-tls` | WIRED | TLSStore references `origin-ca-tls`; SealedSecret `origin-ca-sealed-secret.yaml` creates this secret in kube-system with cluster-wide scope |
| `infra/k8s/argocd/ingress-route-tcp.yaml` | `argocd-server` Service | `HostSNI(argocd.job-hunt.dev)` + service reference | WIRED | `match: HostSNI(\`argocd.job-hunt.dev\`)`, service `name: argocd-server`, `port: 443` |
| `infra/scripts/change-argocd-password.sh` | `argocd-secret` in argocd namespace | `kubectl patch argocd-secret` | WIRED | `kubectl -n argocd patch secret argocd-secret -p "{\"stringData\": ...}"` |
| Security headers middleware | prod/staging Ingress | `traefik.ingress.kubernetes.io/router.middlewares` annotation | WIRED | Both ingress.yaml files annotate `kube-system-security-headers@kubernetescrd`; Middleware defined in kube-system namespace |
| Cloudflare DNS A records | Cloudflare zone settings (Full Strict SSL) | Same `cloudflare_zone_id` variable | WIRED | All resources in cloudflare.tf reference `var.cloudflare_zone_id` |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| DNS-01 | 18-01, 18-03 | Cloudflare DNS A record pointing job-hunt.dev to EC2 Elastic IP (proxied) | SATISFIED | `cloudflare_dns_record.root` with `name="job-hunt.dev"`, `content=aws_eip.main.public_ip`, `proxied=true`; REQUIREMENTS.md marked `[x]` |
| DNS-02 | 18-01, 18-03 | Cloudflare SSL/TLS set to Full (Strict) with Origin CA cert on K8s | SATISFIED | `cloudflare_zone_setting.ssl` value=`"strict"`; `tls-store.yaml` references `origin-ca-tls` secret from sealed Origin CA cert; REQUIREMENTS.md marked `[x]` |
| DNS-03 | 18-01, 18-03 | HTTPS enforced — HTTP requests redirect to HTTPS | SATISFIED | `cloudflare_zone_setting.always_use_https` value=`"on"`; `traefik-config.yaml` `redirectTo.port: websecure`; verify-phase18.sh PASS confirmed in SUMMARY; REQUIREMENTS.md marked `[x]` |
| DNS-04 | 18-01, 18-03 | staging.job-hunt.dev subdomain configured for staging namespace | SATISFIED | `cloudflare_dns_record.staging` (proxied); staging ingress.yaml host=staging.job-hunt.dev; security headers middleware applied; REQUIREMENTS.md marked `[x]` |
| DNS-05 | 18-02, 18-03 | Traefik ingress routes configured for prod and staging hosts | SATISFIED | `traefik-config.yaml` (HelmChartConfig), `tls-store.yaml`, `tls-option.yaml`; prod and staging ingress.yaml updated with middleware; ArgoCD IngressRouteTCP; REQUIREMENTS.md marked `[x]` |

No orphaned requirements: all 5 DNS-01 through DNS-05 are mapped to Phase 18 in REQUIREMENTS.md and all are verified.

### Anti-Patterns Found

No blockers or warnings found. Notable items:

| File | Note | Severity |
|------|------|---------|
| `infra/tofu/main/cloudflare.tf` | Cloudflare `cloudflare_ruleset` resource was removed (replaced by Traefik middleware due to API permission limitations); comment explains the decision | Info |
| `infra/scripts/seal-origin-ca.sh` | Uses temp files instead of process substitution (`<(...)`) for Windows Git Bash compatibility; trap ensures cleanup | Info |
| `infra/scripts/verify-phase18.sh` | Uses `PASS=$((PASS+1))` instead of `((PASS++))` to avoid `set -e` exit on zero arithmetic; `dns_resolves()` helper for Windows nslookup fallback | Info |

### Human Verification Required

The following items were confirmed by the user during Plan 03 execution (checkpoint:human-verify task). They are documented here for completeness but do not block phase acceptance — the user approved them on 2026-03-25.

#### 1. Browser HTTPS and application smoke test

**Test:** Open https://job-hunt.dev in browser
**Expected:** HTTPS padlock present, login works, API calls succeed through Cloudflare proxy (no CORS errors), cookie has `Secure` attribute
**Why human:** Browser UI, DevTools Network/Application inspection, TLS indicator — not verifiable by grep or curl alone
**SUMMARY status:** User confirmed approved 2026-03-25

#### 2. Security headers in browser DevTools

**Test:** DevTools Network tab, click any request, inspect Response Headers
**Expected:** `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=()`
**Why human:** Exact header values and formatting best confirmed in browser DevTools context
**SUMMARY status:** User confirmed all 5 headers present

#### 3. ArgoCD UI at argocd.job-hunt.dev

**Test:** Visit https://argocd.job-hunt.dev, log in with admin credentials
**Expected:** Login page loads, login succeeds with password set by change-argocd-password.sh, all Applications visible
**Why human:** Requires live cluster access and admin password
**SUMMARY status:** User confirmed approved 2026-03-25

#### 4. Staging environment (scale-to-zero by design)

**Test:** `bash infra/scripts/staging-up.sh`, visit https://staging.job-hunt.dev, then `bash infra/scripts/staging-down.sh`
**Expected:** Staging loads (may show empty state), ArgoCD selfHeal does not immediately revert scale-up
**Why human:** Requires live cluster, ArgoCD sync pause/resume behavior
**Note:** verify-phase18.sh shows 2 expected staging failures (scale-to-zero is intended design); staging DNS and ingress routing are confirmed configured

### Gaps Summary

No gaps. All 14 observable truths verified against actual codebase. All 17 required artifacts exist and are substantive. All 7 key links are wired. All 5 requirements (DNS-01 through DNS-05) are satisfied. The phase delivered its stated goal: job-hunt.dev domain is configured with Cloudflare DNS, Origin CA TLS certificates, Traefik HTTPS ingress, and ArgoCD subdomain exposure.

Key architectural decision from execution: security response headers were moved from Cloudflare Transform Rules to a Traefik Middleware due to Cloudflare API token permission limitations. The end result (headers on all responses) is identical to what was planned, and the implementation is arguably more reliable since it is under K8s control rather than dependent on external API token scoping.

---

_Verified: 2026-03-25T14:30:00Z_
_Verifier: Claude (gsd-verifier)_
