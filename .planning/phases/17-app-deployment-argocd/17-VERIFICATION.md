---
phase: 17-app-deployment-argocd
verified: 2026-03-24T14:00:00Z
status: human_needed
score: 9/9 must-haves verified
re_verification: false
human_verification:
  - test: "Run ./infra/scripts/seal-secrets.sh staging on live K3s cluster"
    expected: "kubeseal encrypts credentials, writes 3 *-sealed-secret.yaml files to infra/k8s/overlays/staging/"
    why_human: "Requires live Sealed Secrets controller; cannot test kubeseal without cluster access"
  - test: "Apply infra/argocd/root-app.yaml after ArgoCD Helm install; check ArgoCD UI"
    expected: "Root Application 'jobhunt' appears Synced, spawns jobhunt-staging (auto-sync) and jobhunt-prod (manual) child apps"
    why_human: "Requires live ArgoCD; cannot verify Application CRD sync status without cluster"
  - test: "Push a commit to master; observe update-tags job in GitHub Actions"
    expected: "update-tags job updates staging/kustomization.yaml image tags to sha-XXXXXXX and commits with [skip ci], triggering no further CI run"
    why_human: "GitOps loop closure requires live GitHub Actions execution; cannot verify without triggering a real run"
  - test: "Wait for ArgoCD to sync staging; inspect backend pod startup logs"
    expected: "wait-postgres, wait-redis, wait-minio init containers complete before main backend container starts; no crash loops on first deployment"
    why_human: "Init container TCP wait behaviour requires live K8s pod scheduling; cannot simulate offline"
  - test: "Confirm prod sync requires manual action in ArgoCD UI/CLI after a staging promotion"
    expected: "Production application shows OutOfSync but does NOT self-heal; requires manual argocd app sync jobhunt-prod"
    why_human: "Manual vs automated sync policy enforcement requires live cluster observation"
---

# Phase 17: App Deployment & ArgoCD Verification Report

**Phase Goal:** Backend and frontend pods are running on K8s, managed by ArgoCD GitOps pipeline
**Verified:** 2026-03-24T14:00:00Z
**Status:** human_needed (all automated checks passed; live cluster required to confirm runtime behaviour)
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | ArgoCD Helm values exist with resource limits and disabled components | VERIFIED | `infra/argocd/values.yaml` — dex/notifications/applicationSet all `enabled: false`; controller limit `256Mi` |
| 2 | App-of-apps pattern defined: root Application + staging child + prod child | VERIFIED | `root-app.yaml` (name: jobhunt, path: infra/argocd/apps), `apps/staging.yaml` (name: jobhunt-staging), `apps/prod.yaml` (name: jobhunt-prod) |
| 3 | Staging Application has automated syncPolicy with selfHeal; prod has no automated sync | VERIFIED | `apps/staging.yaml` contains `selfHeal: true` under `automated:`; `apps/prod.yaml` has `syncPolicy:` with only `syncOptions` and `retry` — no `automated:` block |
| 4 | seal-secrets.sh generates random passwords and pipes through kubeseal without writing intermediate plaintext | VERIFIED | Script uses `openssl rand -base64`, pipes directly via `\|` to `kubeseal --dry-run=client`, writes only to `*-sealed-secret.yaml` output files |
| 5 | Kustomize overlays reference sealed-secret YAML files instead of plaintext secrets.yaml | VERIFIED | Both `prod/kustomization.yaml` and `staging/kustomization.yaml` list `backend-sealed-secret.yaml`, `postgres-sealed-secret.yaml`, `minio-sealed-secret.yaml` — `secrets.yaml` does not appear; old file renamed to `secrets.yaml.bak` |
| 6 | Backend pod waits for PostgreSQL, Redis, and MinIO before starting | VERIFIED | `infra/k8s/base/backend/deployment.yaml` has 3 `initContainers`: `wait-postgres` (`nc -z postgres 5432`), `wait-redis` (`nc -z redis 6379`), `wait-minio` (`nc -z minio 9000`), all using `busybox:1.37` |
| 7 | CI pipeline automatically updates staging overlay image tags after successful image push | VERIFIED | `update-tags` job in `ci.yml` runs `kustomize edit set image` with `jobhunt-backend=ghcr.io/baalexandru/jobhunt-backend:sha-${SHORT_SHA}` and frontend equivalent |
| 8 | CI image tag update commit includes [skip ci] to prevent infinite loop | VERIFIED | Commit message: `"ci: update staging image tags to sha-${GITHUB_SHA::7} [skip ci]"` |
| 9 | Verification script checks all 5 phase requirements via kubectl commands | VERIFIED | `verify-phase17.sh` contains labelled sections ARGO-01 through ARGO-04 and K8S-05; exits with `$FAIL` count |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `infra/argocd/values.yaml` | ArgoCD Helm chart overrides with resource limits | VERIFIED | Contains `notifications: enabled: false`, `applicationSet: enabled: false`, `dex: enabled: false`, `memory: 256Mi` for controller |
| `infra/argocd/root-app.yaml` | Root Application pointing to infra/argocd/apps | VERIFIED | `kind: Application`, `name: jobhunt`, `path: infra/argocd/apps`, `selfHeal: true` |
| `infra/argocd/apps/staging.yaml` | Staging child Application with auto-sync | VERIFIED | `selfHeal: true`, `path: infra/k8s/overlays/staging`, `CreateNamespace=false`, finalizer present |
| `infra/argocd/apps/prod.yaml` | Prod child Application without auto-sync | VERIFIED | No `automated:` block, `path: infra/k8s/overlays/prod`, `CreateNamespace=false`, finalizer present |
| `infra/scripts/seal-secrets.sh` | Secret sealing helper (executable) | VERIFIED | Executable (`-rwxr-xr-x`), contains `kubeseal`, `--dry-run=client`, all 3 secrets (backend/postgres/minio) |
| `infra/scripts/backup-sealed-key.sh` | Controller key backup script (executable) | VERIFIED | Executable, contains `sealedsecrets.bitnami.com/sealed-secrets-key` label selector |
| `infra/k8s/overlays/prod/kustomization.yaml` | Prod overlay without plaintext secrets | VERIFIED | References `backend-sealed-secret.yaml`, no `secrets.yaml` entry; `namespace: jobhunt-prod`, 5 replica entries |
| `infra/k8s/overlays/staging/kustomization.yaml` | Staging overlay without plaintext secrets | VERIFIED | References `backend-sealed-secret.yaml`, no `secrets.yaml` entry; `namespace: jobhunt-staging`, `suspend-jobs.yaml` patch, all replicas at 0 |
| `infra/k8s/overlays/prod/backend-sealed-secret.yaml` | Prod backend placeholder SealedSecret | VERIFIED | `kind: SealedSecret`, `name: backend-secrets`, `namespace: jobhunt-prod`, `encryptedData: {}` |
| `infra/k8s/overlays/prod/postgres-sealed-secret.yaml` | Prod postgres placeholder SealedSecret | VERIFIED | Exists with correct kind/name/namespace |
| `infra/k8s/overlays/prod/minio-sealed-secret.yaml` | Prod minio placeholder SealedSecret | VERIFIED | Exists with correct kind/name/namespace |
| `infra/k8s/overlays/staging/backend-sealed-secret.yaml` | Staging backend placeholder SealedSecret | VERIFIED | `kind: SealedSecret`, `name: backend-secrets`, `namespace: jobhunt-staging` |
| `infra/k8s/overlays/staging/postgres-sealed-secret.yaml` | Staging postgres placeholder SealedSecret | VERIFIED | Exists with correct kind/name/namespace |
| `infra/k8s/overlays/staging/minio-sealed-secret.yaml` | Staging minio placeholder SealedSecret | VERIFIED | Exists with correct kind/name/namespace |
| `infra/k8s/base/backend/deployment.yaml` | Backend Deployment with init containers | VERIFIED | `initContainers:` section with `wait-postgres`, `wait-redis`, `wait-minio`; existing probes/resources/envFrom intact |
| `.github/workflows/ci.yml` | Extended CI with update-tags job | VERIFIED | `update-tags:` job, `needs: [build-push]`, `imranismail/setup-kustomize@v2`, `[skip ci]` guard, `git diff --cached --quiet` no-op check |
| `infra/scripts/verify-phase17.sh` | Phase 17 verification script | VERIFIED | Executable, checks ARGO-01/02/03/04 and K8S-05 sections, `exit $FAIL` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `infra/argocd/root-app.yaml` | `infra/argocd/apps/` | `spec.source.path` | WIRED | `path: infra/argocd/apps` confirmed in file |
| `infra/argocd/apps/staging.yaml` | `infra/k8s/overlays/staging` | `spec.source.path` | WIRED | `path: infra/k8s/overlays/staging` confirmed |
| `infra/argocd/apps/prod.yaml` | `infra/k8s/overlays/prod` | `spec.source.path` | WIRED | `path: infra/k8s/overlays/prod` confirmed |
| `infra/scripts/seal-secrets.sh` | `infra/k8s/overlays/{env}/` | kubeseal output path | WIRED | Writes to `${OUTPUT_DIR}/backend-sealed-secret.yaml`, `postgres-sealed-secret.yaml`, `minio-sealed-secret.yaml` |
| `.github/workflows/ci.yml` | `infra/k8s/overlays/staging/kustomization.yaml` | `kustomize edit set image` in update-tags job | WIRED | `cd infra/k8s/overlays/staging` + `kustomize edit set image jobhunt-backend=...` present |
| `infra/k8s/base/backend/deployment.yaml` | postgres service port 5432 | init container TCP check | WIRED | `nc -z postgres 5432` in wait-postgres init container |
| `infra/k8s/base/backend/deployment.yaml` | redis service port 6379 | init container TCP check | WIRED | `nc -z redis 6379` in wait-redis init container |
| `infra/k8s/base/backend/deployment.yaml` | minio service port 9000 | init container TCP check | WIRED | `nc -z minio 9000` in wait-minio init container |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| ARGO-01 | 17-01 | ArgoCD installed (full install with web UI) on the cluster | NEEDS HUMAN | ArgoCD Helm values exist and are ready; actual pod running state requires cluster |
| ARGO-02 | 17-01 | App-of-apps pattern managing all K8s resources | SATISFIED (Git artifacts) | root-app.yaml + apps/staging.yaml + apps/prod.yaml form the complete app-of-apps CRD set |
| ARGO-03 | 17-01 | Sealed Secrets for managing credentials in Git | SATISFIED (Git artifacts) | seal-secrets.sh + backup-sealed-key.sh + placeholder SealedSecret files in both overlays; kustomizations no longer reference plaintext secrets.yaml |
| ARGO-04 | 17-02 | Auto-sync enabled — Git push triggers staging, manual promote to prod | SATISFIED (Git artifacts) | CI update-tags job closes GitOps loop; staging.yaml has `selfHeal: true`; prod.yaml has no `automated:` block |
| K8S-05 | 17-02 | Application pods (backend + frontend) deployed and healthy on K8s | NEEDS HUMAN | Deployment manifests are complete and correct; actual pod health requires live cluster and sealed secret sealing |

All 5 requirement IDs from plan frontmatter (ARGO-01, ARGO-02, ARGO-03, ARGO-04, K8S-05) are present in REQUIREMENTS.md and mapped to Phase 17. No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `infra/k8s/overlays/prod/backend-sealed-secret.yaml` | 9 | `encryptedData: {}` placeholder | Info | Intentional — must be replaced by running `seal-secrets.sh prod` on the cluster before ArgoCD can decrypt secrets |
| `infra/k8s/overlays/staging/backend-sealed-secret.yaml` | 9 | `encryptedData: {}` placeholder | Info | Same as above — intentional placeholder per plan design |

Note: All 6 sealed-secret placeholder files contain `encryptedData: {}`. This is documented and intentional — the plan explicitly specifies them as placeholders that will be overwritten by `kubeseal` output during cluster setup. They are NOT stubs hiding missing implementation; they are scaffolding for a cluster-side step. No blocker anti-patterns found.

### Human Verification Required

**1. Seal Secrets on Live Cluster**

**Test:** SSH to K3s node, install Sealed Secrets controller via Helm, then run `./infra/scripts/seal-secrets.sh prod` and `./infra/scripts/seal-secrets.sh staging` from the repo root.
**Expected:** Three real SealedSecret YAML files written to each overlay directory; `encryptedData` fields populated with kubeseal-encrypted values; Sealed Secrets controller decrypts them to Kubernetes Secrets in the respective namespaces.
**Why human:** Requires a live Sealed Secrets controller pod; kubeseal fetches the public cert from the running controller.

**2. ArgoCD Install and App-of-Apps Bootstrap**

**Test:** Run `helm install argocd argo/argo-cd -n argocd -f infra/argocd/values.yaml`, then `kubectl apply -f infra/argocd/root-app.yaml`. Open ArgoCD UI.
**Expected:** Root application 'jobhunt' appears Synced and Healthy; spawns jobhunt-staging and jobhunt-prod child applications pointing to the correct overlay paths.
**Why human:** Cannot verify ArgoCD Application sync status or UI without a running cluster.

**3. CI GitOps Loop End-to-End**

**Test:** Merge a commit to master and observe the GitHub Actions run.
**Expected:** update-tags job runs in parallel with scan after build-push; commits `ci: update staging image tags to sha-XXXXXXX [skip ci]` to master; no second CI run is triggered by that commit.
**Why human:** Requires a live GitHub Actions execution; cannot simulate the `[skip ci]` enforcement offline.

**4. Backend Startup Ordering**

**Test:** Deploy to staging or prod namespace. Watch `kubectl logs -n jobhunt-prod <backend-pod> -c wait-postgres` (and -redis, -minio) before the main container starts.
**Expected:** Init containers print "waiting for postgres/redis/minio" until the service is reachable, then exit 0; main backend container starts only after all three succeed; no CrashLoopBackOff on first deployment.
**Why human:** Init container TCP wait loop behaviour requires live Kubernetes pod scheduling.

**5. Production Manual-Only Sync Enforcement**

**Test:** Push a commit that changes a prod overlay file. Check ArgoCD UI for jobhunt-prod after a few minutes.
**Expected:** jobhunt-prod shows OutOfSync status but does NOT auto-apply. Sync only occurs after running `argocd app sync jobhunt-prod` or clicking Sync in the UI.
**Why human:** Automated vs manual sync policy enforcement can only be observed on a live cluster with a real Application resource.

### Commit Verification

All 4 phase commits confirmed in git log:
- `c5fdaf3` feat(17-01): add ArgoCD Helm values and app-of-apps Application manifests
- `d567a6a` feat(17-01): add Sealed Secrets scripts and update overlays for encrypted secrets
- `dbcd305` feat(17-02): add init containers to backend Deployment for startup ordering
- `7578c86` feat(17-02): extend CI with image tag update job and add verification script

---

_Verified: 2026-03-24T14:00:00Z_
_Verifier: Claude (gsd-verifier)_
