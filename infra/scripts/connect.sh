#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

echo "Opening SSH tunnel to $ELASTIC_IP for kubectl (port 6443)..."
echo "Press Ctrl+C to close the tunnel."

ssh -L 6443:localhost:6443 -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new \
  ubuntu@"$ELASTIC_IP" -N
