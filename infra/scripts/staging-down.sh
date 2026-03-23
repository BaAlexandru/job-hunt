#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="jobhunt-staging"

echo "Scaling down staging namespace ($NAMESPACE)..."

kubectl scale -n "$NAMESPACE" deploy --all --replicas=0
kubectl scale -n "$NAMESPACE" statefulset --all --replicas=0

echo "Waiting for pods to terminate..."
kubectl wait -n "$NAMESPACE" --for=delete pod --all --timeout=60s 2>/dev/null || true

echo ""
echo "Staging pods (should be empty):"
kubectl get pods -n "$NAMESPACE"
