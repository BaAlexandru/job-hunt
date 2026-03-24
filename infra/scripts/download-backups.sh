#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

NAMESPACE="${1:-jobhunt-prod}"
LOCAL_BACKUP_DIR="${2:-$PROJECT_ROOT/backups}"
mkdir -p "$LOCAL_BACKUP_DIR"

# Read EC2 connection details from tofu outputs
ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

# Verify mc CLI is installed locally
if ! command -v mc &> /dev/null; then
  echo "Error: mc (MinIO Client) is not installed locally."
  echo "Install: https://min.io/docs/minio/linux/reference/minio-mc.html"
  exit 1
fi

# Verify MINIO_ROOT_USER and MINIO_ROOT_PASSWORD are set
if [[ -z "${MINIO_ROOT_USER:-}" ]] || [[ -z "${MINIO_ROOT_PASSWORD:-}" ]]; then
  echo "Error: MINIO_ROOT_USER and MINIO_ROOT_PASSWORD must be set."
  echo "Export them before running this script."
  exit 1
fi

echo "Setting up port-forward to MinIO in ${NAMESPACE}..."
ssh -L 9000:localhost:9000 -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new \
  ubuntu@"$ELASTIC_IP" \
  "kubectl port-forward -n ${NAMESPACE} svc/minio 9000:9000" &
SSH_PID=$!
trap "kill $SSH_PID 2>/dev/null || true" EXIT
sleep 3

echo "Configuring mc alias..."
mc alias set k8s-minio http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

echo "Downloading backups to ${LOCAL_BACKUP_DIR}..."
mc mirror k8s-minio/jobhunt-backups "$LOCAL_BACKUP_DIR/"

echo ""
echo "Backups downloaded to $LOCAL_BACKUP_DIR"
echo "Files:"
ls -lh "$LOCAL_BACKUP_DIR"
