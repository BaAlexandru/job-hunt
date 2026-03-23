#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="jobhunt-staging"

echo "Scaling up staging namespace ($NAMESPACE)..."

kubectl scale -n "$NAMESPACE" deploy --all --replicas=1
kubectl scale -n "$NAMESPACE" statefulset --all --replicas=1

echo "Waiting for all pods to be Ready (timeout: 120s)..."
kubectl wait -n "$NAMESPACE" --for=condition=Ready pod --all --timeout=120s

echo ""
echo "Staging pods:"
kubectl get pods -n "$NAMESPACE"
