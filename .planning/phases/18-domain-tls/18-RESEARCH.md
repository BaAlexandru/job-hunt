# Phase 18: Domain & TLS - Research

**Researched:** 2026-03-25
**Domain:** DNS, TLS/SSL, Cloudflare, Traefik Ingress, ArgoCD TLS, OpenTofu IaC
**Confidence:** HIGH

## Summary

Phase 18 wires up the final networking layer: Cloudflare DNS records pointing to the EC2 Elastic IP, Cloudflare Origin CA certificates for Full (Strict) SSL, Traefik TLS configuration via K3s HelmChartConfig, ArgoCD UI exposure via IngressRouteTCP with TLS passthrough, and origin hardening via AWS security group restriction to Cloudflare IPs only.

The architecture is well-defined: Cloudflare terminates public TLS, proxies to the origin where Traefik terminates the origin-side TLS using a Cloudflare Origin CA wildcard certificate. ArgoCD is the exception -- it uses TLS passthrough so ArgoCD handles its own TLS with the same Origin CA cert. All infrastructure is managed via OpenTofu (Cloudflare provider v5 + existing AWS provider) and K8s manifests committed to Git as SealedSecrets.

**Primary recommendation:** Use OpenTofu Cloudflare provider ~> 5.17 with individual `cloudflare_zone_setting` resources (v5 pattern), `cloudflare_origin_ca_certificate` for cert generation, and `cloudflare_ruleset` for security headers. Traefik configuration via HelmChartConfig deployed to `/var/lib/rancher/k3s/server/manifests/`. ArgoCD TLS via `argocd-server-tls` Secret (auto-detected, no restart needed).

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Wildcard Cloudflare Origin CA cert covering `*.job-hunt.dev` + `job-hunt.dev`, 15-year lifetime, generated via OpenTofu
- Cert and private key sealed with kubeseal into K8s TLS Secrets (kube-system for Traefik, argocd for ArgoCD)
- TLSStore CRD for Traefik default cert (no per-namespace TLS sections in Ingress)
- Both Cloudflare "Always Use HTTPS" AND Traefik HTTP-to-HTTPS redirect (defense in depth)
- HSTS via Cloudflare zone settings (max-age 31536000, includeSubDomains, preload, nosniff)
- Security response headers via Cloudflare Transform Rules (`cloudflare_ruleset`, phase `http_response_headers_transform`)
- Traefik `forwardedHeaders.trustedIPs` with Cloudflare IPv4 ranges
- TLSOption CRD with minVersion TLS 1.2
- AWS SG ports 80/443 restricted to Cloudflare IPv4 ranges (not 0.0.0.0/0)
- ArgoCD exposed at argocd.job-hunt.dev via IngressRouteTCP with TLS passthrough
- ArgoCD TLS cert is the same Origin CA wildcard cert (Full Strict consistency)
- ArgoCD admin password changed via helper script, stored in password manager
- ArgoCD `argocd-secret` must have `ignoreDifferences` in Application spec
- 3 A records: job-hunt.dev, staging.job-hunt.dev, argocd.job-hunt.dev -- all Cloudflare-proxied
- Cloudflare API token in gitignored .tfvars, scoped to Zone:DNS:Edit, Zone:Settings:Edit, Zone:SSL and Certificates:Edit
- Single `tofu apply` for all Cloudflare resources + AWS SG update
- Rollout order: tofu apply -> seal certs -> K8s manifests -> ArgoCD password -> verify

### Claude's Discretion
- Exact Traefik TLSStore + TLSOption + HelmChartConfig YAML structure
- ArgoCD TLS cert configuration method
- Verification script implementation details
- OpenTofu Cloudflare resource structure and variable naming
- ArgoCD admin password change method
- Cloudflare Transform Rules expression syntax
- Whether to use individual SG ingress rules per Cloudflare CIDR or a single rule with list

### Deferred Ideas (OUT OF SCOPE)
- Cloudflare Access zero-trust for ArgoCD MFA
- Content-Security-Policy header
- Custom Cloudflare WAF rules
- Cloudflare IPv6 ranges in AWS SG

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DNS-01 | Cloudflare DNS A record pointing job-hunt.dev to EC2 Elastic IP (proxied) | OpenTofu `cloudflare_dns_record` resource with `proxied = true`, referencing `aws_eip.main.public_ip` |
| DNS-02 | Cloudflare SSL/TLS set to Full (Strict) with Origin CA cert on K8s | `cloudflare_zone_setting` with `setting_id = "ssl"`, `value = "strict"` + `cloudflare_origin_ca_certificate` + `tls_private_key` resources; cert sealed into K8s TLS Secret |
| DNS-03 | HTTPS enforced -- HTTP requests redirect to HTTPS (Cloudflare Always Use HTTPS + HSTS) | `cloudflare_zone_setting` for `always_use_https` + `security_header` (HSTS) + Traefik HelmChartConfig `ports.web.redirectTo.port: websecure` |
| DNS-04 | staging.job-hunt.dev subdomain configured for staging namespace | Second `cloudflare_dns_record` A record for `staging`; existing staging Ingress already has `host: staging.job-hunt.dev` |
| DNS-05 | Traefik ingress routes configured for prod and staging hosts | Existing Ingress resources already configured; TLSStore default cert provides TLS; HelmChartConfig enables websecure entrypoint + HTTP redirect |

</phase_requirements>

## Standard Stack

### Core

| Library/Tool | Version | Purpose | Why Standard |
|---|---|---|---|
| Cloudflare Terraform Provider | ~> 5.17 | DNS records, zone settings, Origin CA cert, Transform Rules | Official v5 GA, HSTS bug fixed post-5.12, latest stable Feb 2026 |
| hashicorp/tls Provider | ~> 4.0 | Generate RSA private key + CSR for Origin CA | Standard TF provider for TLS key material; required by `cloudflare_origin_ca_certificate` |
| hashicorp/aws Provider | ~> 6.0 (existing) | Security group update for Cloudflare IP restriction | Already in use from Phase 14 |
| Traefik (K3s bundled) | v3.x (K3s default) | Ingress controller, TLS termination, HTTP redirect | Bundled with K3s, already running |
| kubeseal | (existing) | Seal Origin CA cert into SealedSecrets | Already in use from Phase 17 |

### Supporting

| Tool | Purpose | When to Use |
|---|---|---|
| HelmChartConfig CRD | Customize K3s Traefik without editing system charts | HTTP-to-HTTPS redirect, trustedIPs, TLS entrypoint |
| TLSStore CRD (traefik.io/v1alpha1) | Set cluster-wide default TLS certificate | Applied once in kube-system namespace |
| TLSOption CRD (traefik.io/v1alpha1) | Enforce minimum TLS version on origin | Applied once as default TLS option |
| IngressRouteTCP CRD (traefik.io/v1alpha1) | TLS passthrough routing for ArgoCD | Exception to standard Ingress API (ArgoCD only) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|---|---|---|
| Cloudflare Origin CA | Let's Encrypt + cert-manager | More moving parts, renewal complexity; Origin CA is 15-year set-and-forget behind Cloudflare proxy |
| TLSStore default cert | Per-Ingress spec.tls sections | Would require TLS Secret in every namespace; TLSStore is single config point |
| IngressRouteTCP passthrough | ArgoCD --insecure + standard Ingress | Simpler but loses end-to-end TLS for ArgoCD admin UI |

## Architecture Patterns

### New Files to Create

```
infra/tofu/main/
  cloudflare.tf          # Cloudflare provider, DNS records, zone settings, Origin CA, Transform Rules
  cloudflare-variables.tf # cloudflare_api_token, cloudflare_zone_id variables
  cloudflare-outputs.tf  # Origin CA cert + key outputs (sensitive)

infra/k8s/traefik/
  traefik-config.yaml     # HelmChartConfig (HTTP redirect + trustedIPs + TLS)
  tls-store.yaml          # TLSStore default cert
  tls-option.yaml         # TLSOption min TLS 1.2

infra/k8s/argocd/
  ingress-route-tcp.yaml  # IngressRouteTCP for argocd.job-hunt.dev

infra/k8s/overlays/prod/
  origin-ca-sealed-secret.yaml   # SealedSecret for kube-system TLS (cluster-wide scope)

infra/k8s/argocd/
  argocd-tls-sealed-secret.yaml  # SealedSecret for argocd-server-tls

infra/scripts/
  seal-origin-ca.sh       # Extract cert from tofu, seal into K8s secrets
  change-argocd-password.sh # ArgoCD admin password change helper
  verify-phase18.sh       # End-to-end verification script
```

### Pattern 1: OpenTofu Cloudflare Origin CA Certificate

**What:** Generate RSA key + CSR + Origin CA cert entirely in OpenTofu
**When to use:** When you need a Cloudflare Origin CA cert managed as IaC

```hcl
# Source: Cloudflare Terraform Registry docs + rallyware/terraform-cloudflare-origin-ca-certificate
resource "tls_private_key" "origin_ca" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_cert_request" "origin_ca" {
  private_key_pem = tls_private_key.origin_ca.private_key_pem

  subject {
    common_name  = "job-hunt.dev"
    organization = "JobHunt"
  }
}

resource "cloudflare_origin_ca_certificate" "wildcard" {
  csr                = tls_cert_request.origin_ca.cert_request_pem
  hostnames          = ["job-hunt.dev", "*.job-hunt.dev"]
  request_type       = "origin-rsa"
  requested_validity = 5475  # 15 years (maximum)
}

output "origin_ca_cert" {
  value     = cloudflare_origin_ca_certificate.wildcard.certificate
  sensitive = true
}

output "origin_ca_key" {
  value     = tls_private_key.origin_ca.private_key_pem
  sensitive = true
}
```

### Pattern 2: Cloudflare Zone Settings (v5)

**What:** Individual `cloudflare_zone_setting` resources for each SSL/TLS setting
**When to use:** All Cloudflare zone configuration in v5 provider

```hcl
# Source: Cloudflare Terraform tutorial + v5 docs
resource "cloudflare_zone_setting" "ssl" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "ssl"
  value      = "strict"
}

resource "cloudflare_zone_setting" "always_use_https" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "always_use_https"
  value      = "on"
}

resource "cloudflare_zone_setting" "min_tls_version" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "min_tls_version"
  value      = "1.2"
}

resource "cloudflare_zone_setting" "tls_1_3" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "tls_1_3"
  value      = "on"
}

resource "cloudflare_zone_setting" "automatic_https_rewrites" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "automatic_https_rewrites"
  value      = "on"
}

resource "cloudflare_zone_setting" "security_header" {
  zone_id    = var.cloudflare_zone_id
  setting_id = "security_header"
  value = {
    strict_transport_security = {
      enabled            = true
      max_age            = 31536000
      include_subdomains = true
      preload            = true
      nosniff            = true
    }
  }
}
```

### Pattern 3: Cloudflare DNS Records

**What:** A records referencing existing AWS EIP output
**When to use:** DNS records that need to be Cloudflare-proxied

```hcl
# Source: Cloudflare provider docs
resource "cloudflare_dns_record" "root" {
  zone_id = var.cloudflare_zone_id
  name    = "job-hunt.dev"
  type    = "A"
  content = aws_eip.main.public_ip
  proxied = true
  ttl     = 1  # Auto when proxied
}

resource "cloudflare_dns_record" "staging" {
  zone_id = var.cloudflare_zone_id
  name    = "staging"
  type    = "A"
  content = aws_eip.main.public_ip
  proxied = true
  ttl     = 1
}

resource "cloudflare_dns_record" "argocd" {
  zone_id = var.cloudflare_zone_id
  name    = "argocd"
  type    = "A"
  content = aws_eip.main.public_ip
  proxied = true
  ttl     = 1
}
```

### Pattern 4: Cloudflare Transform Rules (Security Headers)

**What:** Response header injection via Cloudflare edge
**When to use:** Consistent security headers for all proxied traffic

```hcl
# Source: Cloudflare Transform Rules Terraform docs
resource "cloudflare_ruleset" "security_headers" {
  zone_id     = var.cloudflare_zone_id
  name        = "Security response headers"
  description = "Add security headers to all responses"
  kind        = "zone"
  phase       = "http_response_headers_transform"

  rules {
    ref         = "security_headers"
    description = "Set security response headers"
    expression  = "true"
    action      = "rewrite"

    action_parameters {
      headers {
        name      = "X-Content-Type-Options"
        operation = "set"
        value     = "nosniff"
      }
      headers {
        name      = "X-Frame-Options"
        operation = "set"
        value     = "DENY"
      }
      headers {
        name      = "Referrer-Policy"
        operation = "set"
        value     = "strict-origin-when-cross-origin"
      }
      headers {
        name      = "Permissions-Policy"
        operation = "set"
        value     = "camera=(), microphone=(), geolocation=()"
      }
    }
  }
}
```

### Pattern 5: AWS Security Group Cloudflare-Only Restriction

**What:** Replace 0.0.0.0/0 on ports 80/443 with Cloudflare IPv4 CIDRs
**When to use:** Origin hardening to prevent direct IP access

```hcl
# Source: Cloudflare IPs page (https://www.cloudflare.com/ips-v4/)
locals {
  cloudflare_ipv4_cidrs = [
    "173.245.48.0/20",
    "103.21.244.0/22",
    "103.22.200.0/22",
    "103.31.4.0/22",
    "141.101.64.0/18",
    "108.162.192.0/18",
    "190.93.240.0/20",
    "188.114.96.0/20",
    "197.234.240.0/22",
    "198.41.128.0/17",
    "162.158.0.0/15",
    "104.16.0.0/13",
    "104.24.0.0/14",
    "172.64.0.0/13",
    "131.0.72.0/22",
  ]
}

# Update existing security group -- replace the 0.0.0.0/0 ingress rules
# for ports 80 and 443 with Cloudflare CIDR blocks
```

**Recommendation for SG rules:** Use a single ingress rule per port with the full list of CIDRs in `cidr_blocks`. This is simpler than 15 individual rules per port and AWS SG supports up to 60 rules. Two rules (port 80 + port 443) with 15 CIDRs each = 30 rules total, well within limits.

### Pattern 6: Traefik HelmChartConfig

**What:** K3s-native way to customize the bundled Traefik
**When to use:** HTTP redirect, TLS config, trusted IPs

```yaml
# Source: K3s docs (https://docs.k3s.io/add-ons/helm) + Traefik Helm chart
# Deploy to /var/lib/rancher/k3s/server/manifests/traefik-config.yaml on the node
# OR apply via kubectl (K3s watches the manifest directory)
apiVersion: helm.cattle.io/v1
kind: HelmChartConfig
metadata:
  name: traefik
  namespace: kube-system
spec:
  valuesContent: |-
    ports:
      websecure:
        tls:
          enabled: true
      web:
        redirectTo:
          port: websecure
    additionalArguments:
      - "--entrypoints.websecure.forwardedHeaders.trustedIPs=173.245.48.0/20,103.21.244.0/22,103.22.200.0/22,103.31.4.0/22,141.101.64.0/18,108.162.192.0/18,190.93.240.0/20,188.114.96.0/20,197.234.240.0/22,198.41.128.0/17,162.158.0.0/15,104.16.0.0/13,104.24.0.0/14,172.64.0.0/13,131.0.72.0/22"
```

### Pattern 7: TLSStore Default Certificate

**What:** Cluster-wide default TLS cert for all Ingress resources
**When to use:** Single wildcard cert covering all hosts

```yaml
# Source: Traefik TLSStore docs (https://doc.traefik.io/traefik/reference/routing-configuration/kubernetes/crd/tls/tlsstore/)
apiVersion: traefik.io/v1alpha1
kind: TLSStore
metadata:
  name: default
  namespace: kube-system
spec:
  defaultCertificate:
    secretName: origin-ca-tls
```

**Critical:** The TLSStore named "default" is special -- Traefik uses it automatically. Only one "default" TLSStore can exist across the cluster. The referenced Secret (`origin-ca-tls`) must be a `kubernetes.io/tls` type Secret in the same namespace.

### Pattern 8: TLSOption Minimum Version

```yaml
# Source: Traefik docs
apiVersion: traefik.io/v1alpha1
kind: TLSOption
metadata:
  name: default
  namespace: kube-system
spec:
  minVersion: VersionTLS12
```

**Critical:** Like TLSStore, the TLSOption named "default" is automatically applied to all TLS connections.

### Pattern 9: ArgoCD IngressRouteTCP (TLS Passthrough)

```yaml
# Source: ArgoCD ingress docs + Traefik IngressRouteTCP docs
apiVersion: traefik.io/v1alpha1
kind: IngressRouteTCP
metadata:
  name: argocd-server
  namespace: argocd
spec:
  entryPoints:
    - websecure
  routes:
    - match: HostSNI(`argocd.job-hunt.dev`)
      services:
        - name: argocd-server
          port: 443
  tls:
    passthrough: true
```

### Pattern 10: ArgoCD TLS Certificate

**What:** ArgoCD automatically detects `argocd-server-tls` Secret for its TLS cert
**When to use:** Custom TLS cert for ArgoCD server

ArgoCD has a built-in certificate priority:
1. `argocd-server-tls` Secret (if exists with valid `tls.crt` + `tls.key`) -- **use this**
2. `argocd-secret` (deprecated fallback)
3. Auto-generated self-signed cert

```bash
# Create the Secret (will be sealed via kubeseal)
kubectl create -n argocd secret tls argocd-server-tls \
  --cert=<(tofu -chdir=infra/tofu/main output -raw origin_ca_cert) \
  --key=<(tofu -chdir=infra/tofu/main output -raw origin_ca_key) \
  --dry-run=client -o yaml | \
kubeseal --controller-name=sealed-secrets \
  --controller-namespace=kube-system \
  --namespace=argocd \
  --format=yaml > infra/k8s/argocd/argocd-tls-sealed-secret.yaml
```

**Key insight:** ArgoCD auto-detects `argocd-server-tls` and hot-reloads it -- no restart needed. This is the recommended approach from ArgoCD docs, explicitly designed for SealedSecrets compatibility.

### Anti-Patterns to Avoid

- **Adding spec.tls to existing Ingress resources:** The TLSStore default cert handles all Ingress TLS automatically. Adding `spec.tls` sections would be redundant and create maintenance burden.
- **Using `--insecure` flag on ArgoCD server:** Loses end-to-end TLS. The TLS passthrough approach preserves encryption.
- **Hardcoding the Elastic IP in DNS records:** Always reference `aws_eip.main.public_ip` -- single source of truth.
- **Manual Cloudflare dashboard changes:** Everything must be in OpenTofu for reproducibility.
- **Committing Origin CA cert or private key to Git:** Always seal via kubeseal. The raw cert/key only exist in OpenTofu state and during the sealing process.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| TLS certificate generation | Manual OpenSSL commands | OpenTofu `tls_private_key` + `cloudflare_origin_ca_certificate` | IaC-managed, reproducible, state-tracked |
| DNS record management | Cloudflare dashboard clicks | OpenTofu `cloudflare_dns_record` | Reproducible, version-controlled |
| Security headers | Traefik middleware per route | Cloudflare Transform Rules (`cloudflare_ruleset`) | Applied at edge, consistent for all traffic, single config |
| HTTP-to-HTTPS redirect | Per-Ingress annotations | HelmChartConfig + Cloudflare Always Use HTTPS | Defense in depth, single config point |
| Secret encryption for Git | Manual base64 encoding | kubeseal SealedSecrets | Encrypted at rest, safe for Git |

**Key insight:** This phase is almost entirely configuration -- no application code changes. The complexity is in getting the TLS chain correct (Cloudflare edge -> Traefik origin -> ArgoCD passthrough) and ensuring all resources reference each other correctly.

## Common Pitfalls

### Pitfall 1: TLSStore Secret Namespace Mismatch
**What goes wrong:** TLSStore references a Secret name but the Secret is in a different namespace than the TLSStore.
**Why it happens:** TLSStore is in kube-system but cert Secret might be created in wrong namespace.
**How to avoid:** Both TLSStore AND the TLS Secret (`origin-ca-tls`) must be in `kube-system` namespace where Traefik runs.
**Warning signs:** Traefik logs show "secret not found", HTTPS connections get Traefik's default self-signed cert instead of Origin CA cert.

### Pitfall 2: Cloudflare Origin CA Cert Not Trusted by Browsers
**What goes wrong:** Visiting the site shows certificate error in browser.
**Why it happens:** Origin CA certs are only trusted by Cloudflare's edge, not by browsers directly. If traffic bypasses Cloudflare (e.g., direct IP access), the cert is untrusted.
**How to avoid:** Ensure DNS records are Cloudflare-proxied (orange cloud) AND AWS SG restricts to Cloudflare IPs. The cert is valid only when traffic flows through Cloudflare.
**Warning signs:** `curl --resolve` against the direct IP shows cert error; normal domain access works fine.

### Pitfall 3: HSTS cloudflare_zone_setting Value Format
**What goes wrong:** OpenTofu plan fails or perpetual diff on `security_header` setting.
**Why it happens:** The HSTS setting requires a nested object structure with `strict_transport_security` wrapper. Earlier provider versions (<5.13) had bugs causing crashes.
**How to avoid:** Use provider ~> 5.17. The value must be structured as `{ strict_transport_security = { enabled = true, max_age = 31536000, ... } }`.
**Warning signs:** Provider crash with nil pointer dereference, perpetual plan diff.

### Pitfall 4: IngressRouteTCP Conflicts with Standard Ingress
**What goes wrong:** ArgoCD IngressRouteTCP on `websecure` entrypoint conflicts with standard Ingress routes using the same entrypoint.
**Why it happens:** Both IngressRouteTCP and standard Ingress listen on port 443. Traefik uses SNI to differentiate.
**How to avoid:** The HostSNI match (`argocd.job-hunt.dev`) ensures only ArgoCD traffic is passthrough. Other hosts go through standard TLS termination. This works because Traefik evaluates TCP routes first by SNI before falling through to HTTP routes.
**Warning signs:** ArgoCD works but app Ingress breaks, or vice versa.

### Pitfall 5: ArgoCD Sync Reverts Admin Password
**What goes wrong:** After changing ArgoCD admin password, the next ArgoCD sync resets it to the original value.
**Why it happens:** ArgoCD manages its own `argocd-secret` and syncs it from Git. Password change modifies the Secret directly.
**How to avoid:** Add `ignoreDifferences` for `argocd-secret` in the ArgoCD Application spec:
```yaml
ignoreDifferences:
  - group: ""
    kind: Secret
    name: argocd-secret
    jsonPointers:
      - /data/admin.password
      - /data/admin.passwordMtime
```
**Warning signs:** Password works initially then stops working after next sync.

### Pitfall 6: Traefik Restart Required After HelmChartConfig
**What goes wrong:** HelmChartConfig applied but Traefik doesn't pick up changes.
**Why it happens:** K3s processes HelmChartConfig asynchronously. The Traefik pod may need to restart to apply new Helm values.
**How to avoid:** After applying HelmChartConfig, verify Traefik pod restarts automatically. If not, `kubectl rollout restart deployment traefik -n kube-system`. Note: K3s manages Traefik as a HelmChart -- changes to HelmChartConfig trigger a Helm upgrade automatically, which should restart Traefik.
**Warning signs:** HTTP redirect not working despite config being applied. Check `kubectl get helmchartconfig -n kube-system` and Traefik pod age.

### Pitfall 7: SealedSecret Scope for kube-system
**What goes wrong:** SealedSecret for Origin CA cert cannot be unsealed in kube-system namespace.
**Why it happens:** Default SealedSecret scope is `strict` (namespace-locked). The TLS Secret for Traefik must be in `kube-system`.
**How to avoid:** Use `--scope cluster-wide` when sealing the Origin CA cert for kube-system. For the ArgoCD cert, namespace-scoped is fine (sealed for `argocd` namespace).
**Warning signs:** SealedSecret controller logs show "no key could decrypt secret".

### Pitfall 8: DNS Propagation Delay
**What goes wrong:** After `tofu apply`, DNS records don't resolve immediately.
**Why it happens:** Cloudflare DNS is fast but not instant. Proxied records typically propagate in under 5 minutes but local DNS cache may take longer.
**How to avoid:** Use `dig @1.1.1.1 job-hunt.dev` to check against Cloudflare's own DNS. Flush local cache if needed. Build wait/retry into verification script.
**Warning signs:** `nslookup` returns NXDOMAIN right after apply but works minutes later.

## Code Examples

### Origin CA Cert Sealing Script

```bash
#!/usr/bin/env bash
# seal-origin-ca.sh -- Extract Origin CA cert from tofu and seal into K8s secrets
set -euo pipefail

CONTROLLER_NAME="sealed-secrets"
CONTROLLER_NAMESPACE="kube-system"

# Extract cert and key from tofu outputs
CERT=$(tofu -chdir=infra/tofu/main output -raw origin_ca_cert)
KEY=$(tofu -chdir=infra/tofu/main output -raw origin_ca_key)

# 1. Traefik default cert (kube-system, cluster-wide scope)
kubectl create secret tls origin-ca-tls \
  --namespace=kube-system \
  --cert=<(echo "$CERT") \
  --key=<(echo "$KEY") \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --scope=cluster-wide \
  --format=yaml > infra/k8s/traefik/origin-ca-sealed-secret.yaml

# 2. ArgoCD server cert (argocd namespace, namespace-scoped)
kubectl create secret tls argocd-server-tls \
  --namespace=argocd \
  --cert=<(echo "$CERT") \
  --key=<(echo "$KEY") \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace=argocd \
  --format=yaml > infra/k8s/argocd/argocd-tls-sealed-secret.yaml

echo "Sealed secrets written."
echo "  infra/k8s/traefik/origin-ca-sealed-secret.yaml (cluster-wide)"
echo "  infra/k8s/argocd/argocd-tls-sealed-secret.yaml (argocd namespace)"
```

### ArgoCD Admin Password Change Script

```bash
#!/usr/bin/env bash
# change-argocd-password.sh -- Change ArgoCD admin password
# Usage: ./change-argocd-password.sh
set -euo pipefail

echo "Enter new ArgoCD admin password:"
read -rs NEW_PASSWORD

# Generate bcrypt hash (requires python3 + bcrypt or htpasswd)
BCRYPT_HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw(b'${NEW_PASSWORD}', bcrypt.gensalt()).decode())")

# Patch argocd-secret
kubectl -n argocd patch secret argocd-secret \
  -p "{\"stringData\": {\"admin.password\": \"${BCRYPT_HASH}\", \"admin.passwordMtime\": \"$(date +%FT%T%Z)\"}}"

# Restart argocd-server to pick up change
kubectl -n argocd rollout restart deployment argocd-server

echo "ArgoCD admin password changed. Store in your password manager."
```

**Alternative without Python:** Use `htpasswd -nbBC 10 "" "$NEW_PASSWORD" | tr -d ':\n' | sed 's/$2y/$2a/'` if htpasswd is available.

### Verification Script Structure

```bash
#!/usr/bin/env bash
# verify-phase18.sh -- End-to-end Phase 18 verification
set -euo pipefail

PASS=0; FAIL=0

check() {
  local desc="$1"; shift
  if "$@" > /dev/null 2>&1; then
    echo "PASS: $desc"; ((PASS++))
  else
    echo "FAIL: $desc"; ((FAIL++))
  fi
}

# DNS resolution
check "DNS: job-hunt.dev resolves" dig +short job-hunt.dev @1.1.1.1
check "DNS: staging.job-hunt.dev resolves" dig +short staging.job-hunt.dev @1.1.1.1
check "DNS: argocd.job-hunt.dev resolves" dig +short argocd.job-hunt.dev @1.1.1.1

# HTTPS connectivity
check "HTTPS: job-hunt.dev returns 200" curl -sf -o /dev/null https://job-hunt.dev
check "HTTPS: staging.job-hunt.dev returns 200" curl -sf -o /dev/null https://staging.job-hunt.dev
check "HTTPS: argocd.job-hunt.dev loads login" curl -sf -o /dev/null https://argocd.job-hunt.dev

# HTTP redirect
check "Redirect: HTTP->HTTPS on job-hunt.dev" \
  bash -c 'curl -sI http://job-hunt.dev | grep -qi "location.*https"'

# TLS cert issuer
check "TLS: Origin CA cert" \
  bash -c 'echo | openssl s_client -connect job-hunt.dev:443 -servername job-hunt.dev 2>/dev/null | openssl x509 -noout -issuer | grep -i cloudflare'

# Security headers
check "Header: HSTS present" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "strict-transport-security"'
check "Header: X-Content-Type-Options" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "x-content-type-options.*nosniff"'
check "Header: X-Frame-Options" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "x-frame-options.*deny"'
check "Header: Referrer-Policy" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "referrer-policy"'

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|---|---|---|---|
| `cloudflare_zone_settings_override` (monolithic) | Individual `cloudflare_zone_setting` per setting | Cloudflare provider v5 (Feb 2025 GA) | Each setting is its own resource; more granular control |
| Traefik v2 IngressRoute API (`traefik.containo.us/v1alpha1`) | Traefik v3 API (`traefik.io/v1alpha1`) | Traefik v3 (2024) | API group changed; K3s bundles v3 |
| ArgoCD core-mode (no UI) | Full install with UI | Phase 17 decision | ArgoCD UI available for exposure at argocd.job-hunt.dev |
| `web.redirectTo: websecure` (string) | `web.redirectTo.port: websecure` (object) | Traefik Helm chart recent versions | Syntax changed for redirect configuration |

**Deprecated/outdated:**
- `cloudflare_zone_settings_override`: Replaced by individual `cloudflare_zone_setting` in v5
- `traefik.containo.us/v1alpha1` API group: Replaced by `traefik.io/v1alpha1` in Traefik v3
- ArgoCD `argocd-secret` for TLS cert: Deprecated in favor of `argocd-server-tls` Secret

## Open Questions

1. **Cloudflare provider v5 HSTS value format**
   - What we know: The `security_header` setting_id requires a nested `strict_transport_security` object. Bug was fixed post-5.12.
   - What's unclear: Exact HCL syntax may vary between v5.13-v5.17 releases. The `nosniff` field position (inside or outside `strict_transport_security`) is ambiguous in some sources.
   - Recommendation: Test with `tofu plan` before apply. If HSTS fails, fall back to configuring HSTS manually in Cloudflare dashboard and import the state. Use provider ~> 5.17 minimum.

2. **HelmChartConfig deployment method**
   - What we know: Can be placed at `/var/lib/rancher/k3s/server/manifests/` or applied via `kubectl apply`.
   - What's unclear: Whether `kubectl apply` persists across K3s restarts for HelmChartConfig specifically.
   - Recommendation: Deploy via SSH to manifests directory for persistence. Also keep a copy in Git for reference/ArgoCD management.

3. **Traefik CRD availability in K3s**
   - What we know: K3s bundles Traefik with CRDs. TLSStore, TLSOption, IngressRouteTCP should all be available.
   - What's unclear: Whether the specific K3s version installed has Traefik v3 CRDs (API group `traefik.io/v1alpha1`).
   - Recommendation: Verify with `kubectl get crd | grep traefik` before applying CRDs. If API group is `traefik.containo.us`, adjust manifests accordingly.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Shell script (bash) + curl/dig/openssl |
| Config file | `infra/scripts/verify-phase18.sh` (Wave 0) |
| Quick run command | `bash infra/scripts/verify-phase18.sh` |
| Full suite command | `bash infra/scripts/verify-phase18.sh` + manual browser smoke test |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DNS-01 | DNS A record resolves for job-hunt.dev | smoke | `dig +short job-hunt.dev @1.1.1.1` | Wave 0 |
| DNS-02 | Full (Strict) SSL + Origin CA cert | smoke | `openssl s_client` cert issuer check | Wave 0 |
| DNS-03 | HTTP redirects to HTTPS + HSTS header | smoke | `curl -sI http://job-hunt.dev` redirect check | Wave 0 |
| DNS-04 | staging.job-hunt.dev resolves and loads | smoke | `curl -sf https://staging.job-hunt.dev` | Wave 0 |
| DNS-05 | Traefik routes to correct namespace by host | smoke + manual | `curl` both hosts + verify different content | Wave 0 |

### Sampling Rate
- **Per task commit:** N/A (infrastructure changes, not code)
- **Per wave merge:** Run `verify-phase18.sh` after each wave of K8s/tofu changes
- **Phase gate:** Full `verify-phase18.sh` pass + manual browser smoke test (login, create company, verify API through Cloudflare)

### Wave 0 Gaps
- [ ] `infra/scripts/verify-phase18.sh` -- automated verification script
- [ ] Manual test checklist in PLAN.md for browser smoke test

## Sources

### Primary (HIGH confidence)
- [Traefik TLSStore CRD docs](https://doc.traefik.io/traefik/reference/routing-configuration/kubernetes/crd/tls/tlsstore/) -- TLSStore default cert configuration
- [ArgoCD TLS configuration docs](https://argo-cd.readthedocs.io/en/stable/operator-manual/tls/) -- `argocd-server-tls` Secret format, auto-detection, SealedSecrets compatibility
- [ArgoCD Ingress configuration docs](https://argo-cd.readthedocs.io/en/stable/operator-manual/ingress/) -- Traefik IngressRoute/IngressRouteTCP patterns
- [Cloudflare IPs page](https://www.cloudflare.com/ips-v4/) -- 15 IPv4 CIDR blocks (verified 2026-03-25)
- [K3s Helm add-ons docs](https://docs.k3s.io/add-ons/helm) -- HelmChartConfig for Traefik customization
- [Cloudflare Terraform HTTPS settings tutorial](https://developers.cloudflare.com/terraform/tutorial/configure-https-settings/) -- v5 `cloudflare_zone_setting` pattern
- [Cloudflare Transform Rules Terraform docs](https://developers.cloudflare.com/terraform/additional-configurations/transform-rules/) -- `cloudflare_ruleset` for response headers

### Secondary (MEDIUM confidence)
- [Cloudflare Origin CA docs](https://developers.cloudflare.com/ssl/origin-configuration/origin-ca/) -- Origin CA cert capabilities and limitations
- [Cloudflare provider v5 HSTS issue #6436](https://github.com/cloudflare/terraform-provider-cloudflare/issues/6436) -- HSTS bug fixed, provider ~> 5.17 recommended
- [Traefik Helm chart examples](https://github.com/traefik/traefik-helm-chart/blob/master/EXAMPLES.md) -- HelmChartConfig values structure
- [OneUptime ArgoCD TLS passthrough guide](https://oneuptime.com/blog/post/2026-02-26-argocd-tls-passthrough/view) -- IngressRouteTCP YAML example (Feb 2026)
- [rallyware/terraform-cloudflare-origin-ca-certificate](https://github.com/rallyware/terraform-cloudflare-origin-ca-certificate) -- OpenTofu module pattern for Origin CA

### Tertiary (LOW confidence)
- Cloudflare provider HSTS `security_header` exact value format -- multiple conflicting examples found across sources; recommend testing with `tofu plan` before apply

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- Cloudflare provider v5 is GA, Traefik CRDs well-documented, ArgoCD TLS pattern is official
- Architecture: HIGH -- All patterns verified against official docs; existing project infrastructure is well-understood
- Pitfalls: HIGH -- Based on known issues (provider bugs, namespace scoping, sync conflicts) documented in official repos
- HSTS zone_setting value format: MEDIUM -- Bug was fixed but exact nested object syntax needs validation during implementation

**Research date:** 2026-03-25
**Valid until:** 2026-04-25 (stable infrastructure, 30 days)
