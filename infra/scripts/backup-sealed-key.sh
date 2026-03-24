#!/usr/bin/env bash
# backup-sealed-key.sh -- Export the controller's signing key pair
# Store the output file securely OUTSIDE of Git
set -euo pipefail
BACKUP_DIR="${1:-$HOME/.sealed-secrets-backup}"
mkdir -p "$BACKUP_DIR"
kubectl get secret -n kube-system -l sealedsecrets.bitnami.com/sealed-secrets-key \
  -o yaml > "${BACKUP_DIR}/sealed-secrets-key-$(date +%Y%m%d).yaml"
echo "Key backed up to ${BACKUP_DIR}/"
echo "IMPORTANT: Store this file securely outside Git."
