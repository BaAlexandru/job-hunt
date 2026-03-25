#!/usr/bin/env bash
# verify-phase18.sh -- End-to-end Phase 18 verification
# Tests: DNS resolution, HTTPS connectivity, HTTP redirect, TLS cert, security headers
set -euo pipefail

PASS=0
FAIL=0

check() {
  local desc="$1"; shift
  if "$@" > /dev/null 2>&1; then
    echo "PASS: $desc"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $desc"
    FAIL=$((FAIL + 1))
  fi
}

# DNS resolution helper: uses dig if available, nslookup otherwise
dns_resolves() {
  local host="$1"
  if command -v dig &> /dev/null; then
    dig +short "$host" @1.1.1.1 | grep -q .
  else
    nslookup "$host" 1.1.1.1 2>/dev/null | grep -q "Address.*[0-9]"
  fi
}

echo "=== Phase 18: Domain & TLS Verification ==="
echo "NOTE: Staging checks require staging to be scaled up first (bash infra/scripts/staging-up.sh)"
echo ""

# DNS resolution
echo "--- DNS Resolution ---"
check "DNS: job-hunt.dev resolves" dns_resolves job-hunt.dev
check "DNS: staging.job-hunt.dev resolves" dns_resolves staging.job-hunt.dev
check "DNS: argocd.job-hunt.dev resolves" dns_resolves argocd.job-hunt.dev

# HTTPS connectivity
echo ""
echo "--- HTTPS Connectivity ---"
check "HTTPS: job-hunt.dev returns 200" curl -sf -o /dev/null --max-time 15 https://job-hunt.dev
check "HTTPS: staging.job-hunt.dev returns 200" curl -sf -o /dev/null --max-time 15 https://staging.job-hunt.dev
check "HTTPS: argocd.job-hunt.dev loads" curl -sf -o /dev/null --max-time 15 https://argocd.job-hunt.dev

# HTTP to HTTPS redirect
echo ""
echo "--- HTTP Redirect ---"
check "Redirect: HTTP->HTTPS on job-hunt.dev" \
  bash -c 'curl -sI -o /dev/null -w "%{redirect_url}" --max-time 15 http://job-hunt.dev 2>/dev/null | grep -qi "https://"'

# TLS certificate (Cloudflare proxied = edge cert from Cloudflare/Let's Encrypt, not Origin CA directly)
echo ""
echo "--- TLS Certificate ---"
check "TLS: valid cert on edge (Cloudflare or Let's Encrypt)" \
  bash -c 'echo | openssl s_client -connect job-hunt.dev:443 -servername job-hunt.dev 2>/dev/null | openssl x509 -noout -issuer 2>/dev/null | grep -iE "cloudflare|let.s.encrypt"'

# Security headers (set via Traefik middleware)
echo ""
echo "--- Security Headers ---"
check "Header: Strict-Transport-Security" \
  bash -c 'curl -sI --max-time 15 https://job-hunt.dev | grep -qi "strict-transport-security"'
check "Header: X-Content-Type-Options: nosniff" \
  bash -c 'curl -sI --max-time 15 https://job-hunt.dev | grep -qi "x-content-type-options.*nosniff"'
check "Header: X-Frame-Options: DENY" \
  bash -c 'curl -sI --max-time 15 https://job-hunt.dev | grep -qi "x-frame-options.*deny"'
check "Header: Referrer-Policy" \
  bash -c 'curl -sI --max-time 15 https://job-hunt.dev | grep -qi "referrer-policy"'
check "Header: Permissions-Policy" \
  bash -c 'curl -sI --max-time 15 https://job-hunt.dev | grep -qi "permissions-policy"'

# Traefik routing (different hosts -> different content or at least both respond)
echo ""
echo "--- Traefik Routing ---"
check "Route: prod host serves content" curl -sf -o /dev/null --max-time 15 https://job-hunt.dev/
check "Route: staging host serves content" curl -sf -o /dev/null --max-time 15 https://staging.job-hunt.dev/

# Summary
echo ""
echo "================================"
echo "Results: $PASS passed, $FAIL failed"
echo "================================"

if [ "$FAIL" -eq 0 ]; then
  echo "All checks passed!"
  exit 0
else
  echo "Some checks failed. Review output above."
  exit 1
fi
