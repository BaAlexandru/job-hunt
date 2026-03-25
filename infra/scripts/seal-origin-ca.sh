#!/usr/bin/env bash
# seal-origin-ca.sh -- Extract Origin CA cert from tofu and seal into K8s secrets
# Creates two SealedSecrets:
#   1. origin-ca-tls in kube-system (cluster-wide scope) for Traefik default cert
#   2. argocd-server-tls in argocd (namespace-scoped) for ArgoCD server cert
set -euo pipefail

CONTROLLER_NAME="sealed-secrets"
CONTROLLER_NAMESPACE="kube-system"

# Extract cert and key from tofu outputs
echo "Extracting Origin CA cert and key from tofu outputs..."
CERT=$(tofu -chdir=infra/tofu/main output -raw origin_ca_cert)
KEY=$(tofu -chdir=infra/tofu/main output -raw origin_ca_key)

if [ -z "$CERT" ] || [ -z "$KEY" ]; then
  echo "ERROR: Failed to extract cert or key from tofu outputs"
  echo "Ensure 'tofu apply' has been run in infra/tofu/main/"
  exit 1
fi

# Ensure output directories exist
mkdir -p infra/k8s/traefik infra/k8s/argocd

# Write cert/key to temp files (process substitution not available on Windows Git Bash)
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT
echo "$CERT" > "$TMPDIR/tls.crt"
echo "$KEY" > "$TMPDIR/tls.key"

# 1. Traefik default cert (kube-system, cluster-wide scope)
echo "Sealing Origin CA cert for Traefik (kube-system, cluster-wide)..."
kubectl create secret tls origin-ca-tls \
  --namespace=kube-system \
  --cert="$TMPDIR/tls.crt" \
  --key="$TMPDIR/tls.key" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --scope=cluster-wide \
  --format=yaml > infra/k8s/traefik/origin-ca-sealed-secret.yaml

# 2. ArgoCD server cert (argocd namespace, namespace-scoped)
echo "Sealing Origin CA cert for ArgoCD (argocd, namespace-scoped)..."
kubectl create secret tls argocd-server-tls \
  --namespace=argocd \
  --cert="$TMPDIR/tls.crt" \
  --key="$TMPDIR/tls.key" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace=argocd \
  --format=yaml > infra/k8s/argocd/argocd-tls-sealed-secret.yaml

echo ""
echo "Sealed secrets written:"
echo "  infra/k8s/traefik/origin-ca-sealed-secret.yaml (cluster-wide)"
echo "  infra/k8s/argocd/argocd-tls-sealed-secret.yaml (argocd namespace)"
echo ""
echo "Next steps:"
echo "  1. kubectl apply -f infra/k8s/traefik/origin-ca-sealed-secret.yaml"
echo "  2. kubectl apply -f infra/k8s/argocd/argocd-tls-sealed-secret.yaml"
