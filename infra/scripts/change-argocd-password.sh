#!/usr/bin/env bash
# change-argocd-password.sh -- Change ArgoCD admin password
# Usage: ./change-argocd-password.sh
# Prompts for new password, hashes with bcrypt, patches argocd-secret
set -euo pipefail

echo "Enter new ArgoCD admin password:"
read -rs NEW_PASSWORD
echo ""

if [ -z "$NEW_PASSWORD" ]; then
  echo "ERROR: Password cannot be empty"
  exit 1
fi

echo "Generating bcrypt hash..."
# Use python3 bcrypt (preferred) or htpasswd fallback
# NOTE: Password passed via env var to avoid shell injection (never interpolate into code strings)
if command -v python3 &> /dev/null && python3 -c "import bcrypt" 2>/dev/null; then
  BCRYPT_HASH=$(NEW_PASSWORD="$NEW_PASSWORD" python3 -c "
import bcrypt, os
password = os.environ['NEW_PASSWORD'].encode('utf-8')
print(bcrypt.hashpw(password, bcrypt.gensalt(rounds=10)).decode('utf-8'))
")
elif command -v htpasswd &> /dev/null; then
  BCRYPT_HASH=$(htpasswd -nbBC 10 "" "$NEW_PASSWORD" | tr -d ':\n' | sed 's/$2y/$2a/')
else
  echo "ERROR: Requires python3 with bcrypt module OR htpasswd"
  echo "Install: pip3 install bcrypt OR apt-get install apache2-utils"
  exit 1
fi

MTIME=$(date -u +%FT%TZ)

echo "Patching argocd-secret..."
kubectl -n argocd patch secret argocd-secret \
  -p "{\"stringData\": {\"admin.password\": \"${BCRYPT_HASH}\", \"admin.passwordMtime\": \"${MTIME}\"}}"

echo "Restarting argocd-server to pick up change..."
kubectl -n argocd rollout restart deployment argocd-server
kubectl -n argocd rollout status deployment argocd-server --timeout=120s

echo ""
echo "ArgoCD admin password changed successfully."
echo "IMPORTANT: Store the password in your password manager."
echo "Login: https://argocd.job-hunt.dev (username: admin)"
