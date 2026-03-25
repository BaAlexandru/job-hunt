---
phase: 18-domain-tls
plan: 01
subsystem: infra
tags: [cloudflare, opentofu, dns, tls, origin-ca, security-headers, aws-sg]

# Dependency graph
requires:
  - phase: 14-aws-infrastructure
    provides: "EC2 instance, Elastic IP, security group, OpenTofu main module"
provides:
  - "Cloudflare DNS A records (root, staging, argocd) pointing to Elastic IP"
  - "Cloudflare Origin CA wildcard cert (*.job-hunt.dev + job-hunt.dev)"
  - "Cloudflare zone settings (Full Strict SSL, HSTS, TLS 1.2+, Always HTTPS)"
  - "Cloudflare Transform Rules for security response headers"
  - "AWS SG restriction to Cloudflare IPv4 CIDRs for ports 80/443"
  - "seal-origin-ca.sh script for extracting and sealing cert into K8s secrets"
  - "verify-phase18.sh end-to-end verification script"
affects: [18-02-PLAN, 18-03-PLAN]

# Tech tracking
tech-stack:
  added: [cloudflare-terraform-provider-5.17, hashicorp-tls-provider-4.0]
  patterns: [cloudflare-zone-setting-v5-individual-resources, cloudflare-ruleset-transform-rules, sg-cidr-restriction]

key-files:
  created:
    - infra/tofu/main/cloudflare.tf
    - infra/tofu/main/cloudflare-variables.tf
    - infra/tofu/main/cloudflare-outputs.tf
    - infra/scripts/seal-origin-ca.sh
    - infra/scripts/verify-phase18.sh
  modified:
    - infra/tofu/main/providers.tf
    - infra/tofu/main/main.tf
    - infra/tofu/main/.terraform.lock.hcl

key-decisions:
  - "Cloudflare provider v5 uses individual cloudflare_zone_setting resources (not monolithic cloudflare_zone_settings_override)"
  - "Cloudflare ruleset rules attribute uses map syntax for headers (keyed by header name)"
  - "Origin CA cert with 5475-day (15-year) validity for set-and-forget personal project"

patterns-established:
  - "Cloudflare zone settings: one cloudflare_zone_setting resource per setting with setting_id"
  - "Cloudflare Transform Rules: rules as list attribute with headers as map keyed by header name"
  - "SG restriction: local.cloudflare_ipv4_cidrs referenced from cloudflare.tf locals block"

requirements-completed: [DNS-01, DNS-02, DNS-03, DNS-04]

# Metrics
duration: 8min
completed: 2026-03-25
---

# Phase 18 Plan 01: Cloudflare IaC Summary

**OpenTofu Cloudflare infrastructure with DNS records, Origin CA cert, zone settings, security headers, AWS SG restriction, and helper scripts**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-25T12:48:58Z
- **Completed:** 2026-03-25T12:56:29Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Cloudflare provider v5 integrated with DNS A records for root, staging, and argocd subdomains
- Origin CA wildcard certificate generation via OpenTofu (tls_private_key + tls_cert_request + cloudflare_origin_ca_certificate)
- Zone settings configured: Full (Strict) SSL, Always Use HTTPS, HSTS with preload, TLS 1.2 minimum, TLS 1.3, automatic HTTPS rewrites
- Security response headers via Cloudflare Transform Rules (X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy)
- AWS security group ports 80/443 restricted from 0.0.0.0/0 to Cloudflare IPv4 CIDRs only
- seal-origin-ca.sh creates two SealedSecrets (cluster-wide for Traefik, namespace-scoped for ArgoCD)
- verify-phase18.sh tests DNS, HTTPS, redirect, TLS cert, and security headers end-to-end

## Task Commits

Each task was committed atomically:

1. **Task 1: OpenTofu Cloudflare infrastructure** - `2c42fb6` (feat)
2. **Task 2: Seal-origin-ca script and verification script** - `21882f0` (feat)
3. **Task 2 followup: Make scripts executable** - `cd5fd88` (chore)
4. **Task 1 followup: Update terraform lock file** - `9421701` (chore)

## Files Created/Modified
- `infra/tofu/main/providers.tf` - Added cloudflare ~> 5.17 and tls ~> 4.0 providers
- `infra/tofu/main/cloudflare-variables.tf` - Cloudflare API token (sensitive) and zone ID variables
- `infra/tofu/main/cloudflare.tf` - DNS records, Origin CA cert, zone settings, Transform Rules, Cloudflare IPv4 CIDRs
- `infra/tofu/main/cloudflare-outputs.tf` - Sensitive outputs for origin_ca_cert and origin_ca_key
- `infra/tofu/main/main.tf` - SG ports 80/443 restricted to local.cloudflare_ipv4_cidrs
- `infra/tofu/main/.terraform.lock.hcl` - Lock file updated with cloudflare and tls provider hashes
- `infra/scripts/seal-origin-ca.sh` - Extracts cert from tofu, seals into K8s TLS secrets
- `infra/scripts/verify-phase18.sh` - End-to-end DNS, HTTPS, TLS, and header verification

## Decisions Made
- Cloudflare provider v5 syntax: individual `cloudflare_zone_setting` resources with `setting_id` attribute (not the deprecated monolithic `cloudflare_zone_settings_override`)
- Cloudflare ruleset `rules` uses list assignment syntax (`rules = [{}]`) not block syntax, and `headers` uses map syntax keyed by header name (v5 provider schema requirement)
- All 15 Cloudflare IPv4 CIDRs defined in locals block in cloudflare.tf and referenced from main.tf SG

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Cloudflare ruleset v5 syntax**
- **Found during:** Task 1 (OpenTofu Cloudflare infrastructure)
- **Issue:** Plan specified block syntax for `rules {}` but Cloudflare provider v5 requires attribute assignment syntax `rules = [{}]` and headers as map
- **Fix:** Converted to `rules = [{ ... }]` list assignment with headers as `{ "Header-Name" = { operation = "set", value = "..." } }` map syntax
- **Files modified:** infra/tofu/main/cloudflare.tf
- **Verification:** `tofu validate` passes successfully
- **Committed in:** 2c42fb6 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Syntax adaptation required for Cloudflare provider v5. No scope creep.

## Issues Encountered
None beyond the syntax fix documented above.

## User Setup Required
Before running `tofu apply`, add Cloudflare credentials to dev.tfvars:
```
cloudflare_api_token = "your-cloudflare-api-token"
cloudflare_zone_id   = "your-zone-id"
```
The API token needs permissions: Zone:DNS:Edit, Zone:Settings:Edit, Zone:SSL and Certificates:Edit, Zone:Zone Rulesets:Edit.

## Next Phase Readiness
- OpenTofu configuration validated and ready for `tofu apply`
- seal-origin-ca.sh ready for use in Plan 02 after `tofu apply` creates the cert
- verify-phase18.sh ready for end-to-end testing in Plan 03
- Plan 02 can proceed with Traefik TLS and ArgoCD passthrough configuration

---
*Phase: 18-domain-tls*
*Completed: 2026-03-25*
