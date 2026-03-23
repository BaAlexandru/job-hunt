#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

echo "Fetching kubeconfig from $ELASTIC_IP..."

scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new \
  ubuntu@"$ELASTIC_IP":/etc/rancher/k3s/k3s.yaml \
  "$PROJECT_ROOT/k3s-kubeconfig.yaml"

# Verify server URL points to localhost (for SSH tunnel usage)
if ! grep -q "https://127.0.0.1:6443" "$PROJECT_ROOT/k3s-kubeconfig.yaml"; then
  echo "WARNING: kubeconfig server URL is not https://127.0.0.1:6443"
  echo "You may need to update it manually for SSH tunnel access."
fi

# Merge or copy kubeconfig
mkdir -p ~/.kube

if [ -f ~/.kube/config ]; then
  echo "Merging with existing kubeconfig..."
  KUBECONFIG=~/.kube/config:"$PROJECT_ROOT/k3s-kubeconfig.yaml" \
    kubectl config view --flatten > ~/.kube/config.merged
  mv ~/.kube/config.merged ~/.kube/config
else
  echo "No existing kubeconfig found, copying directly..."
  cp "$PROJECT_ROOT/k3s-kubeconfig.yaml" ~/.kube/config
fi

# Clean up temp file
rm -f "$PROJECT_ROOT/k3s-kubeconfig.yaml"

# Rename context for clarity
kubectl config rename-context default jobhunt-k3s

echo ""
echo "Kubeconfig setup complete! Context 'jobhunt-k3s' is now available."
echo "Run ./infra/scripts/connect.sh before using kubectl commands."
