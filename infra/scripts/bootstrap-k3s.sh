#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

echo "Installing K3s on EC2 instance at $ELASTIC_IP..."

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new ubuntu@"$ELASTIC_IP" \
  "curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE='644' sh -"

echo "Waiting for node to be Ready (timeout: 120s)..."

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new ubuntu@"$ELASTIC_IP" \
  "sudo k3s kubectl wait --for=condition=Ready node --all --timeout=120s"

echo ""
echo "K3s installed successfully!"
echo "Next step: run ./infra/scripts/setup-kubeconfig.sh to fetch kubeconfig"
