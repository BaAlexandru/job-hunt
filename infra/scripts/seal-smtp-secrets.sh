#!/usr/bin/env bash
# Usage: SMTP_USERNAME=you@gmail.com SMTP_PASSWORD=your-app-password ./seal-smtp-secrets.sh <staging|prod>
# Patches ONLY the SMTP credentials in the existing backend-secrets SealedSecret.
# Does NOT rotate DB, JWT, MinIO, or other secrets.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ENV="${1:?Usage: seal-smtp-secrets.sh <staging|prod>}"
NAMESPACE="jobhunt-${ENV}"
CONTROLLER_NAME="sealed-secrets"
CONTROLLER_NAMESPACE="kube-system"
OUTPUT_DIR="${PROJECT_ROOT}/infra/k8s/overlays/${ENV}"
SEALED_SECRET_FILE="${OUTPUT_DIR}/backend-sealed-secret.yaml"

: "${SMTP_USERNAME:?ERROR: Set SMTP_USERNAME env var before running}"
: "${SMTP_PASSWORD:?ERROR: Set SMTP_PASSWORD env var before running}"

if [[ ! -f "$SEALED_SECRET_FILE" ]]; then
  echo "ERROR: ${SEALED_SECRET_FILE} not found. Run seal-secrets.sh first."
  exit 1
fi

# Create a partial secret with only SMTP keys, seal it, and merge into existing sealed secret
kubectl create secret generic backend-secrets \
  --namespace="$NAMESPACE" \
  --from-literal=SMTP_USERNAME="$SMTP_USERNAME" \
  --from-literal=SMTP_PASSWORD="$SMTP_PASSWORD" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace="$NAMESPACE" \
  --format=yaml \
  --merge-into "$SEALED_SECRET_FILE"

echo "SMTP credentials sealed into ${SEALED_SECRET_FILE}"
echo "Commit and push to deploy via ArgoCD."
