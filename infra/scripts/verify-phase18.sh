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
    ((PASS++))
  else
    echo "FAIL: $desc"
    ((FAIL++))
  fi
}

echo "=== Phase 18: Domain & TLS Verification ==="
echo "NOTE: Staging checks require staging to be scaled up first (bash infra/scripts/staging-up.sh)"
echo ""

# DNS resolution (use Cloudflare DNS to avoid local cache issues)
echo "--- DNS Resolution ---"
check "DNS: job-hunt.dev resolves" dig +short job-hunt.dev @1.1.1.1
check "DNS: staging.job-hunt.dev resolves" dig +short staging.job-hunt.dev @1.1.1.1
check "DNS: argocd.job-hunt.dev resolves" dig +short argocd.job-hunt.dev @1.1.1.1

# HTTPS connectivity
echo ""
echo "--- HTTPS Connectivity ---"
check "HTTPS: job-hunt.dev returns 200" curl -sf -o /dev/null -w '%{http_code}' https://job-hunt.dev
check "HTTPS: staging.job-hunt.dev returns 200" curl -sf -o /dev/null -w '%{http_code}' https://staging.job-hunt.dev
check "HTTPS: argocd.job-hunt.dev loads" curl -sf -o /dev/null https://argocd.job-hunt.dev

# HTTP to HTTPS redirect
echo ""
echo "--- HTTP Redirect ---"
check "Redirect: HTTP->HTTPS on job-hunt.dev" \
  bash -c 'curl -sI -o /dev/null -w "%{redirect_url}" http://job-hunt.dev 2>/dev/null | grep -qi "https://"'

# TLS certificate issuer (should be Cloudflare on the edge)
echo ""
echo "--- TLS Certificate ---"
check "TLS: Cloudflare cert on edge" \
  bash -c 'echo | openssl s_client -connect job-hunt.dev:443 -servername job-hunt.dev 2>/dev/null | openssl x509 -noout -issuer 2>/dev/null | grep -i cloudflare'

# Security headers
echo ""
echo "--- Security Headers ---"
check "Header: Strict-Transport-Security" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "strict-transport-security"'
check "Header: X-Content-Type-Options: nosniff" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "x-content-type-options.*nosniff"'
check "Header: X-Frame-Options: DENY" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "x-frame-options.*deny"'
check "Header: Referrer-Policy" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "referrer-policy"'
check "Header: Permissions-Policy" \
  bash -c 'curl -sI https://job-hunt.dev | grep -qi "permissions-policy"'

# Traefik routing (different hosts -> different content or at least both respond)
echo ""
echo "--- Traefik Routing ---"
check "Route: prod host serves content" curl -sf -o /dev/null https://job-hunt.dev/
check "Route: staging host serves content" curl -sf -o /dev/null https://staging.job-hunt.dev/

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
