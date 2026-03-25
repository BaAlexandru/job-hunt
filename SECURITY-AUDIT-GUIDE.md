# Security Audit — Deployment Guide

## What This Branch Changes (`phase-19-security-audit`)

| # | Fix | File |
|---|-----|------|
| 1 | Refresh token cookie `secure` flag is now `true` in prod (was hardcoded `false`) | `backend/.../controller/AuthController.kt` |
| 2 | Actuator: only `/actuator/health/**` is public, `/actuator/info` and `/actuator/flyway` require auth | `backend/.../config/SecurityConfig.kt` |
| 3 | CORS `allowedHeaders` restricted to `Authorization, Content-Type, X-Internal-Secret, Cookie` | `backend/.../config/SecurityConfig.kt` |
| 4 | `seal-secrets.sh` now requires `SMTP_USERNAME` and `SMTP_PASSWORD` env vars (no more `changeme`) | `infra/scripts/seal-secrets.sh` |
| 5 | New `seal-smtp-secrets.sh` to patch SMTP creds without rotating all other secrets | `infra/scripts/seal-smtp-secrets.sh` |

---

## Step-by-Step Deployment

### Step 1 — Review and merge the code changes

```bash
# You're already on phase-19-security-audit with all changes
# Commit, push, and open a PR:
git add -A
git commit -m "fix(security): harden cookies, actuator, CORS, and SMTP secret handling"
git push -u origin phase-19-security-audit

# Open PR to master
gh pr create --title "fix(security): audit hardening" --body "..."
```

Review the PR, then merge it on GitHub.

```bash
git checkout master && git pull origin master
```

### Step 2 — Get your Gmail App Password

Before sealing SMTP secrets, you need a Gmail App Password:

1. Go to https://myaccount.google.com/apppasswords
2. Sign in (2FA must be enabled on your Google account)
3. Select app: "Mail", select device: "Other" → name it "job-hunt"
4. Copy the 16-character app password (e.g., `abcd efgh ijkl mnop`)
5. Remove spaces — your password is `abcdefghijklmnop`

### Step 3 — Seal the SMTP credentials (from a machine with cluster access)

SSH into your EC2 instance or any machine with `kubectl` and `kubeseal` access:

```bash
# Clone / pull latest master
cd job-hunt
git pull origin master

# Set your credentials
export SMTP_USERNAME="your-email@gmail.com"
export SMTP_PASSWORD="abcdefghijklmnop"   # the app password from Step 2

# Seal for prod (patches backend-sealed-secret.yaml in-place)
./infra/scripts/seal-smtp-secrets.sh prod

# If you also use staging:
# ./infra/scripts/seal-smtp-secrets.sh staging
```

**Prerequisite:** `yq` must be installed on the machine. Install with:
```bash
# On Ubuntu/Debian
sudo wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
sudo chmod +x /usr/local/bin/yq
```

### Step 4 — Commit and push the updated sealed secret

```bash
git add infra/k8s/overlays/prod/backend-sealed-secret.yaml
git commit -m "chore(infra): seal real SMTP credentials for prod"
git push origin master
```

ArgoCD will detect the change and sync the new sealed secret automatically.

### Step 5 — Verify the deployment

```bash
# Check the backend pod restarted with new secrets
kubectl -n jobhunt-prod rollout status deployment/backend

# Verify SMTP is working (trigger a password reset or email verification)
# Check pod logs for mail errors:
kubectl -n jobhunt-prod logs deployment/backend | grep -i mail
```

---

## Important Notes

- **Do NOT re-run `seal-secrets.sh`** unless you want to rotate ALL secrets (DB password, JWT secret, MinIO keys). That would require a full database password reset and session invalidation.
- **Use `seal-smtp-secrets.sh`** to patch only SMTP credentials safely.
- The sealed secret YAML files are safe to commit — they can only be decrypted by the Sealed Secrets controller in your cluster.
- After merging, the cookie `secure=true` takes effect automatically because the prod deployment uses `SPRING_PROFILES_ACTIVE=prod`.
