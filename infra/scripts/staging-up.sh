#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="jobhunt-staging"
APP_NAME="jobhunt-staging"

echo "Scaling up staging namespace ($NAMESPACE)..."

# Pause ArgoCD auto-sync so it doesn't revert scaling
echo "Pausing ArgoCD auto-sync for $APP_NAME..."
kubectl -n argocd patch application "$APP_NAME" \
  --type json -p '[{"op":"remove","path":"/spec/syncPolicy/automated"}]' 2>/dev/null || true

# Wait a moment for ArgoCD to stop syncing
sleep 3

kubectl scale -n "$NAMESPACE" deploy --all --replicas=1
kubectl scale -n "$NAMESPACE" statefulset --all --replicas=1

echo "Waiting for all pods to be Ready (timeout: 120s)..."
kubectl wait -n "$NAMESPACE" --for=condition=Ready pod --all --timeout=120s

echo ""
echo "Staging pods:"
kubectl get pods -n "$NAMESPACE"
echo ""
echo "NOTE: Run 'bash infra/scripts/staging-down.sh' to scale down and re-enable ArgoCD sync."
