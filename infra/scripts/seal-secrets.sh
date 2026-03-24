#!/usr/bin/env bash
# Usage: ./seal-secrets.sh <staging|prod>
# Generates random passwords, creates Secret YAML, pipes through kubeseal
# NEVER writes plaintext secrets to disk

set -euo pipefail

ENV="${1:?Usage: seal-secrets.sh <staging|prod>}"
NAMESPACE="jobhunt-${ENV}"
CONTROLLER_NAME="sealed-secrets"
CONTROLLER_NAMESPACE="kube-system"

# Generate random values
DB_PASSWORD=$(openssl rand -base64 24)
JWT_SECRET=$(openssl rand -base64 32)
MINIO_ACCESS_KEY=$(openssl rand -base64 16)
MINIO_SECRET_KEY=$(openssl rand -base64 32)
INTERNAL_API_SECRET=$(openssl rand -base64 32)
POSTGRES_PASSWORD="$DB_PASSWORD"
MINIO_ROOT_USER="$MINIO_ACCESS_KEY"
MINIO_ROOT_PASSWORD="$MINIO_SECRET_KEY"

OUTPUT_DIR="infra/k8s/overlays/${ENV}"

# Backend secrets (8 keys)
kubectl create secret generic backend-secrets \
  --namespace="$NAMESPACE" \
  --from-literal=DB_USERNAME=jobhunt \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY" \
  --from-literal=MINIO_SECRET_KEY="$MINIO_SECRET_KEY" \
  --from-literal=SMTP_USERNAME=changeme \
  --from-literal=SMTP_PASSWORD=changeme \
  --from-literal=INTERNAL_API_SECRET="$INTERNAL_API_SECRET" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace="$NAMESPACE" \
  --format=yaml > "${OUTPUT_DIR}/backend-sealed-secret.yaml"

# Postgres secrets (3 keys)
kubectl create secret generic postgres-secrets \
  --namespace="$NAMESPACE" \
  --from-literal=POSTGRES_DB=jobhunt \
  --from-literal=POSTGRES_USER=jobhunt \
  --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace="$NAMESPACE" \
  --format=yaml > "${OUTPUT_DIR}/postgres-sealed-secret.yaml"

# MinIO secrets (2 keys)
kubectl create secret generic minio-secrets \
  --namespace="$NAMESPACE" \
  --from-literal=MINIO_ROOT_USER="$MINIO_ROOT_USER" \
  --from-literal=MINIO_ROOT_PASSWORD="$MINIO_ROOT_PASSWORD" \
  --dry-run=client -o yaml | \
kubeseal --controller-name="$CONTROLLER_NAME" \
  --controller-namespace="$CONTROLLER_NAMESPACE" \
  --namespace="$NAMESPACE" \
  --format=yaml > "${OUTPUT_DIR}/minio-sealed-secret.yaml"

echo "Sealed secrets written to ${OUTPUT_DIR}/"
echo "IMPORTANT: DB_PASSWORD and POSTGRES_PASSWORD match: $DB_PASSWORD"
echo "IMPORTANT: MINIO_ACCESS_KEY and MINIO_ROOT_USER match: $MINIO_ACCESS_KEY"
