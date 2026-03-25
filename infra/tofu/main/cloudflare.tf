# =============================================================================
# Cloudflare DNS, Origin CA, Zone Settings, Security Headers, and SG Restriction
# =============================================================================

# -----------------------------------------------------------------------------
# Cloudflare IPv4 CIDRs (for AWS SG restriction)
# -----------------------------------------------------------------------------
locals {
  # Source: https://www.cloudflare.com/ips/ — review periodically for changes
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

# -----------------------------------------------------------------------------
# DNS A Records (all proxied, pointing to Elastic IP)
# -----------------------------------------------------------------------------
resource "cloudflare_dns_record" "root" {
  zone_id = var.cloudflare_zone_id
  name    = "job-hunt.dev"
  type    = "A"
  content = aws_eip.main.public_ip
  proxied = true
  ttl     = 1
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

# -----------------------------------------------------------------------------
# Origin CA Certificate (wildcard, 15-year validity)
# -----------------------------------------------------------------------------
resource "tls_private_key" "origin_ca" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_cert_request" "origin_ca" {
  private_key_pem = tls_private_key.origin_ca.private_key_pem
  dns_names       = ["job-hunt.dev", "*.job-hunt.dev"]

  subject {
    common_name  = "job-hunt.dev"
    organization = "JobHunt"
  }
}

resource "cloudflare_origin_ca_certificate" "wildcard" {
  csr                = tls_cert_request.origin_ca.cert_request_pem
  hostnames          = ["job-hunt.dev", "*.job-hunt.dev"]
  request_type       = "origin-rsa"
  requested_validity = 5475
}

# -----------------------------------------------------------------------------
# Zone Settings (Cloudflare provider v5: individual resources per setting)
# -----------------------------------------------------------------------------
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
  value = jsonencode({
    strict_transport_security = {
      enabled            = true
      max_age            = 31536000
      include_subdomains = true
      preload            = true
      nosniff            = true
    }
  })
}
# NOTE: nosniff=true in HSTS also adds X-Content-Type-Options:nosniff header.
# This is redundant with the Transform Rule below but harmless (headers are idempotent).

# -----------------------------------------------------------------------------
# Transform Rules (security response headers)
# -----------------------------------------------------------------------------
resource "cloudflare_ruleset" "security_headers" {
  zone_id     = var.cloudflare_zone_id
  name        = "Security response headers"
  description = "Add security headers to all responses"
  kind        = "zone"
  phase       = "http_response_headers_transform"

  rules = [{
    ref         = "security_headers"
    description = "Set security response headers"
    expression  = "true"
    action      = "rewrite"
    action_parameters = {
      headers = {
        "X-Content-Type-Options" = {
          operation = "set"
          value     = "nosniff"
        }
        "X-Frame-Options" = {
          operation = "set"
          value     = "DENY"
        }
        "Referrer-Policy" = {
          operation = "set"
          value     = "strict-origin-when-cross-origin"
        }
        "Permissions-Policy" = {
          operation = "set"
          value     = "camera=(), microphone=(), geolocation=()"
        }
      }
    }
  }]
}
