#!/usr/bin/env bash
# verify-phase17.sh -- Verify all Phase 17 requirements
# Requires: active SSH tunnel (infra/scripts/connect.sh) and KUBECONFIG set
set -euo pipefail

PASS=0
FAIL=0

check() {
  local desc="$1"
  shift
  if eval "$@" > /dev/null 2>&1; then
    echo "PASS: $desc"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $desc"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Phase 17 Verification ==="
echo ""

# ARGO-01: ArgoCD installed with web UI
echo "--- ARGO-01: ArgoCD Installation ---"
check "ArgoCD server pod running" \
  "kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-server -o jsonpath='{.items[0].status.phase}' | grep -q Running"
check "ArgoCD controller pod running" \
  "kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-application-controller -o jsonpath='{.items[0].status.phase}' | grep -q Running"
check "ArgoCD repo-server pod running" \
  "kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-repo-server -o jsonpath='{.items[0].status.phase}' | grep -q Running"

# ARGO-02: App-of-apps pattern
echo ""
echo "--- ARGO-02: App-of-Apps ---"
check "Root application exists" \
  "kubectl get application jobhunt -n argocd"
check "Staging application exists" \
  "kubectl get application jobhunt-staging -n argocd"
check "Production application exists" \
  "kubectl get application jobhunt-prod -n argocd"

# ARGO-03: Sealed Secrets
echo ""
echo "--- ARGO-03: Sealed Secrets ---"
check "Sealed Secrets controller running" \
  "kubectl get pods -n kube-system -l app.kubernetes.io/name=sealed-secrets -o jsonpath='{.items[0].status.phase}' | grep -q Running"
check "Backend secrets decrypted in prod" \
  "kubectl get secret backend-secrets -n jobhunt-prod"
check "Postgres secrets decrypted in prod" \
  "kubectl get secret postgres-secrets -n jobhunt-prod"
check "MinIO secrets decrypted in prod" \
  "kubectl get secret minio-secrets -n jobhunt-prod"

# ARGO-04: Sync policies
echo ""
echo "--- ARGO-04: Sync Policies ---"
check "Staging has auto-sync enabled" \
  "kubectl get application jobhunt-staging -n argocd -o jsonpath='{.spec.syncPolicy.automated}' | grep -q selfHeal"
check "Production has no auto-sync" \
  "test -z \$(kubectl get application jobhunt-prod -n argocd -o jsonpath='{.spec.syncPolicy.automated}')"

# K8S-05: Application pods healthy
echo ""
echo "--- K8S-05: Application Pods ---"
check "Backend pod running in prod" \
  "kubectl get pods -n jobhunt-prod -l app=backend -o jsonpath='{.items[0].status.phase}' | grep -q Running"
check "Frontend pod running in prod" \
  "kubectl get pods -n jobhunt-prod -l app=frontend -o jsonpath='{.items[0].status.phase}' | grep -q Running"
check "Backend pod ready" \
  "kubectl get pods -n jobhunt-prod -l app=backend -o jsonpath='{.items[0].status.conditions[?(@.type==\"Ready\")].status}' | grep -q True"
check "Frontend pod ready" \
  "kubectl get pods -n jobhunt-prod -l app=frontend -o jsonpath='{.items[0].status.conditions[?(@.type==\"Ready\")].status}' | grep -q True"

echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="
exit $FAIL
