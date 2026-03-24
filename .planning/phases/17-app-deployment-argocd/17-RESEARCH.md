# Phase 17: App Deployment & ArgoCD - Research

**Researched:** 2026-03-24
**Domain:** Kubernetes GitOps (ArgoCD, Sealed Secrets, CI-driven image updates)
**Confidence:** HIGH

## Summary

Phase 17 installs ArgoCD (full install with web UI) and Sealed Secrets on an existing K3s cluster, replaces plaintext placeholder secrets with encrypted SealedSecret resources, configures an app-of-apps pattern to manage staging and production environments, adds init containers for startup ordering, and extends the CI pipeline to auto-update image tags in Git. The existing Kustomize manifests (base + overlays) from Phase 15/16 provide the foundation -- this phase wires them into a GitOps pipeline.

The primary technical challenge is the 2GB RAM constraint. Full ArgoCD consumes approximately 400MB, leaving only ~288MB headroom. All ArgoCD components must have explicit resource limits. The Sealed Secrets controller is lightweight (~50MB). The CI workflow extension for `kustomize edit set image` + git push is a well-established pattern with known pitfalls (infinite loop prevention, token permissions).

**Primary recommendation:** Install ArgoCD and Sealed Secrets via Helm charts with explicit low resource limits. Use the app-of-apps pattern with plain YAML Application manifests (not Helm templates) since this project only has two environments. Extend CI with a `[skip ci]` commit message to prevent infinite workflow loops.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Bitnami Sealed Secrets controller installed on the cluster
- `kubeseal` CLI used locally to encrypt secrets; encrypted SealedSecret resources committed to Git
- Replace Phase 15's plaintext placeholder secrets.yaml files with SealedSecret resources
- Separate secrets per environment (staging and production get different DB passwords, JWT secrets, etc.)
- Helper script (`infra/scripts/seal-secrets.sh`) generates random passwords via `openssl rand`, creates Secret YAML, pipes through `kubeseal`, outputs SealedSecret
- Controller signing key backed up locally (exported to a file outside Git) -- restore before resealing if cluster is rebuilt
- ~50MB RAM for the Sealed Secrets controller
- Full ArgoCD install (not core-mode) -- includes web UI and API server
- Accept tight RAM headroom (~288MB on 2GB instance); upgrade to t3.medium if needed
- ArgoCD installed in its own namespace (`argocd`)
- Web UI accessible via port-forward or Ingress (Phase 18 can add argocd.job-hunt.dev if desired)
- SSH deploy key for GitHub repo access (read-only deploy key on the repo, private key as K8s Secret)
- Root Application (`jobhunt`) points to `infra/argocd/` directory
- Two child Applications: `jobhunt-staging` and `jobhunt-prod`
- Each child Application points to the corresponding Kustomize overlay
- CI-driven image tag updates: GitHub Actions CI adds a step after pushing images -- runs `kustomize edit set image` on the staging overlay, commits and pushes to master
- ArgoCD detects the Git change and auto-syncs staging
- Staging auto-sync enabled with self-heal; staging respects replicas: 0
- Production requires manual sync -- auto-sync disabled on prod Application
- Promotion: manually update prod overlay's image tag, commit, push; then manually sync prod in ArgoCD UI or CLI
- Init containers on backend Deployment: wait for PostgreSQL (`pg_isready`), Redis (`redis-cli ping`), and MinIO (`curl /minio/health/live`)
- Frontend Deployment gets readiness and liveness probes (HTTP GET on `/`) -- already exists in base manifest
- RollingUpdate (maxUnavailable: 0, maxSurge: 1) -- already exists in base manifests
- ArgoCD auto-rollback on sync failure

### Claude's Discretion
- Exact ArgoCD Helm chart values or manifest configuration
- Init container images and exact wait scripts
- ArgoCD Application manifest details (sync policy, retry settings)
- Sealed Secrets controller installation method (Helm vs raw manifests)
- CI workflow step implementation for kustomize edit + git push
- Frontend probe endpoints, intervals, and thresholds

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| K8S-05 | Application pods (backend + frontend) deployed and healthy on K8s | Init container patterns, health probes, RollingUpdate strategy, image tag management via Kustomize overlays |
| ARGO-01 | ArgoCD installed (full install with web UI) on the cluster | Helm chart installation with resource limits, namespace config, SSH deploy key setup |
| ARGO-02 | App-of-apps pattern managing all K8s resources | Root Application + child Application CRDs pointing to Kustomize overlays |
| ARGO-03 | Sealed Secrets for managing credentials in Git | Helm install of controller, kubeseal CLI workflow, seal-secrets.sh helper, key backup |
| ARGO-04 | Auto-sync enabled -- Git push triggers deployment to staging, manual promote to production | Staging syncPolicy.automated with selfHeal, prod manual sync, CI kustomize edit + push |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| ArgoCD (Helm chart: argo-cd) | Chart ~9.x (App v2.14+) | GitOps continuous delivery | Industry standard K8s GitOps tool, native Kustomize support |
| Sealed Secrets (Helm chart: sealed-secrets) | Chart ~0.36.x | Encrypt K8s secrets for Git storage | Bitnami standard, works with any GitOps tool, lightweight |
| kubeseal CLI | Match controller version | Local secret encryption | Official companion to Sealed Secrets controller |
| Kustomize | Built into kubectl | Manifest management | Already used in Phase 15/16, ArgoCD auto-detects kustomization.yaml |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| busybox | latest | Init container for wait scripts | Lightweight image with sh, nc utilities |
| bitnami/postgresql (client) | 17 | Init container pg_isready | Matches prod PostgreSQL version |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Sealed Secrets | External Secrets Operator + AWS SSM | More complex, needs AWS IAM, overkill for single-developer project |
| App-of-apps plain YAML | ApplicationSet | More powerful but more complex; two environments don't need it |
| CI-driven image tags | ArgoCD Image Updater | Extra controller consuming RAM on constrained node |

**Installation (on cluster via Helm):**
```bash
# ArgoCD
helm repo add argo https://argoproj.github.io/argo-helm
helm install argocd argo/argo-cd --namespace argocd --create-namespace -f infra/argocd/values.yaml

# Sealed Secrets
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm install sealed-secrets sealed-secrets/sealed-secrets --namespace kube-system
```

## Architecture Patterns

### Recommended Directory Structure (new files)
```
infra/
  argocd/
    values.yaml                    # ArgoCD Helm chart overrides
    root-app.yaml                  # Root Application (app-of-apps)
    apps/
      staging.yaml                 # Child Application -> overlays/staging
      prod.yaml                    # Child Application -> overlays/prod
  k8s/
    overlays/
      prod/
        kustomization.yaml         # (existing, modified: remove secrets.yaml resource)
        sealed-secrets.yaml        # NEW: SealedSecret resources (encrypted)
        configmap.yaml             # (existing, unchanged)
        ingress.yaml               # (existing, unchanged)
      staging/
        kustomization.yaml         # (existing, modified: remove secrets.yaml resource)
        sealed-secrets.yaml        # NEW: SealedSecret resources (encrypted)
        configmap.yaml             # (existing, unchanged)
        ingress.yaml               # (existing, unchanged)
    base/
      backend/
        deployment.yaml            # (modified: add init containers)
  scripts/
    seal-secrets.sh                # NEW: Generate + encrypt secrets
    backup-sealed-key.sh           # NEW: Export controller signing key
```

### Pattern 1: ArgoCD Application CRD (Kustomize source)
**What:** Declarative Application resource pointing to a Kustomize overlay directory
**When to use:** Each environment (staging, prod) gets its own Application
**Example:**
```yaml
# Source: https://argo-cd.readthedocs.io/en/stable/operator-manual/cluster-bootstrapping/
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: jobhunt-prod
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  source:
    repoURL: git@github.com:baalexandru/job-hunt.git
    targetRevision: master
    path: infra/k8s/overlays/prod
  destination:
    server: https://kubernetes.default.svc
    namespace: jobhunt-prod
  syncPolicy:
    syncOptions:
      - CreateNamespace=false
    retry:
      limit: 3
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 1m
```

### Pattern 2: Root Application (app-of-apps)
**What:** A parent Application that points to a directory of child Application manifests
**When to use:** Bootstrapping -- one `kubectl apply` deploys the root, which deploys all children
**Example:**
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: jobhunt
  namespace: argocd
spec:
  project: default
  source:
    repoURL: git@github.com:baalexandru/job-hunt.git
    targetRevision: master
    path: infra/argocd/apps
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

### Pattern 3: SealedSecret Resource
**What:** Encrypted secret that only the controller can decrypt
**When to use:** Every secret that must live in Git
**Example:**
```yaml
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: backend-secrets
  namespace: jobhunt-prod
spec:
  encryptedData:
    DB_PASSWORD: AgBy8h...  # encrypted by kubeseal
    JWT_SECRET: AgCx9j...
    # ... other keys
  template:
    metadata:
      name: backend-secrets
    type: Opaque
```

### Pattern 4: Init Containers for Startup Ordering
**What:** Wait for dependent services before main container starts
**When to use:** Backend depends on PostgreSQL, Redis, MinIO
**Example:**
```yaml
initContainers:
  - name: wait-postgres
    image: busybox:1.37
    command: ['sh', '-c', 'until nc -z postgres 5432; do echo "waiting for postgres"; sleep 2; done']
  - name: wait-redis
    image: busybox:1.37
    command: ['sh', '-c', 'until nc -z redis 6379; do echo "waiting for redis"; sleep 2; done']
  - name: wait-minio
    image: busybox:1.37
    command: ['sh', '-c', 'until nc -z minio 9000; do echo "waiting for minio"; sleep 2; done']
```

### Pattern 5: CI Image Tag Update Step
**What:** After pushing images, update Kustomize overlay and push back to Git
**When to use:** Every CI run after successful image build+push
**Example:**
```yaml
- name: Update staging image tags
  run: |
    cd infra/k8s/overlays/staging
    kustomize edit set image \
      jobhunt-backend=ghcr.io/baalexandru/jobhunt-backend:sha-${GITHUB_SHA::7} \
      jobhunt-frontend=ghcr.io/baalexandru/jobhunt-frontend:sha-${GITHUB_SHA::7}
    cd $GITHUB_WORKSPACE
    git config user.name "github-actions[bot]"
    git config user.email "github-actions[bot]@users.noreply.github.com"
    git add infra/k8s/overlays/staging/kustomization.yaml
    git commit -m "ci: update staging image tags to sha-${GITHUB_SHA::7} [skip ci]"
    git push
```

### Anti-Patterns to Avoid
- **No `[skip ci]` in commit message:** CI pushes a commit which triggers CI again, creating an infinite loop. Always include `[skip ci]` in automated commit messages.
- **Sealing secrets without namespace scope:** kubeseal encrypts secrets bound to a specific namespace by default. Sealing for the wrong namespace means the controller cannot decrypt.
- **Using `latest` tag in production overlays:** ArgoCD cannot detect a change if the tag name does not change. Always use SHA-based tags.
- **Auto-sync on production:** Accidental pushes to prod overlay go live immediately. Production must require manual sync.
- **Storing unsealed secrets in Git:** Even temporarily. The seal-secrets.sh script should pipe directly without writing intermediate plaintext files.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Secret encryption for Git | Custom encryption scripts | Sealed Secrets + kubeseal | Handles key rotation, namespace scoping, controller-side decryption |
| GitOps sync engine | Polling scripts with kubectl apply | ArgoCD | Drift detection, auto-sync, rollback, health monitoring, UI |
| Container registry watching | Cron jobs checking for new images | CI-driven kustomize edit (user's choice) | Deterministic, Git as source of truth |
| Service dependency ordering | Custom entrypoint scripts in app containers | K8s init containers | Standard K8s pattern, clear separation of concerns |
| Helm chart value management | kubectl create/apply with inline YAML | Helm install with values.yaml file | Reproducible, version-controlled, upgradable |

**Key insight:** ArgoCD + Sealed Secrets + Kustomize is the established GitOps stack. Each tool handles one concern well. Custom solutions introduce drift, lack rollback, and miss edge cases.

## Common Pitfalls

### Pitfall 1: ArgoCD OOM on 2GB Instance
**What goes wrong:** ArgoCD components exceed memory limits, get OOMKilled, cluster becomes unstable
**Why it happens:** Default Helm chart values assume larger clusters. Full ArgoCD (server, repo-server, controller, redis, dex, notifications, applicationset-controller) without limits can consume 500MB+
**How to avoid:** Set explicit resource limits in Helm values.yaml. Disable unused components (notifications-controller, applicationset-controller if not using ApplicationSets, dex if not using SSO). Target ~400MB total for ArgoCD.
**Warning signs:** Pods in CrashLoopBackOff in argocd namespace, `kubectl top pods -n argocd` showing high memory

### Pitfall 2: CI Infinite Loop
**What goes wrong:** CI pushes a commit to update image tags, which triggers CI again, building new images, pushing another commit, forever
**Why it happens:** CI triggers on `push` to master, and the image tag update commit is a push to master
**How to avoid:** Include `[skip ci]` in the commit message. Alternatively, use `paths-ignore` in the workflow trigger to ignore changes to `infra/k8s/overlays/`. The `[skip ci]` approach is simpler and more reliable.
**Warning signs:** Multiple sequential CI runs with no code changes, only kustomization.yaml diffs

### Pitfall 3: CI Permissions for Git Push
**What goes wrong:** The image tag update step fails with "permission denied" when pushing back to the repo
**Why it happens:** Default `GITHUB_TOKEN` has `contents: read`. CI needs `contents: write` to push commits.
**How to avoid:** Update workflow permissions to `contents: write`. This is already identified as an integration point in CONTEXT.md.
**Warning signs:** Git push failure in CI logs

### Pitfall 4: kubeseal Namespace Mismatch
**What goes wrong:** SealedSecret is committed to Git but the controller cannot decrypt it, pods fail to start with missing secrets
**Why it happens:** kubeseal encrypts secrets scoped to a specific namespace. If you seal for `jobhunt-prod` but the SealedSecret ends up in `jobhunt-staging`, decryption fails.
**How to avoid:** The seal-secrets.sh script must explicitly pass `--namespace` to kubeseal. Use strict scope (default) rather than cluster-wide.
**Warning signs:** SealedSecret resource exists but corresponding Secret is not created. Check `kubectl logs -n kube-system -l app.kubernetes.io/name=sealed-secrets`

### Pitfall 5: ArgoCD Repository Secret Label
**What goes wrong:** ArgoCD cannot access the Git repo, Applications show "repository not found" errors
**Why it happens:** The Kubernetes Secret holding the SSH deploy key must have the label `argocd.argoproj.io/secret-type: repository`. Without it, ArgoCD ignores the secret.
**How to avoid:** Ensure the repository secret has the correct label. Verify with `argocd repo list` after setup.
**Warning signs:** Application stuck in "Unknown" health status, sync errors mentioning authentication

### Pitfall 6: Sealed Secrets Controller Name Mismatch
**What goes wrong:** kubeseal cannot fetch the certificate from the controller, errors like "cannot fetch certificate"
**Why it happens:** The Helm chart names the controller `sealed-secrets` by default, but kubeseal CLI expects `sealed-secrets-controller`
**How to avoid:** Either override the Helm release name to `sealed-secrets-controller`, or always pass `--controller-name sealed-secrets` to kubeseal commands
**Warning signs:** `kubeseal --fetch-cert` fails with connection errors

### Pitfall 7: Init Container Image Pull on Constrained Disk
**What goes wrong:** 30GB EBS fills up with container images over time
**Why it happens:** Each init container image (busybox, postgres client, redis client) adds to local storage. Over many deployments, old images accumulate.
**How to avoid:** Use a single lightweight image (busybox) for all init containers with `nc` (netcat) for TCP checks instead of protocol-specific tools. This avoids pulling postgres and redis client images.
**Warning signs:** `kubectl describe node` shows disk pressure

## Code Examples

### ArgoCD Helm Values for Constrained Cluster
```yaml
# infra/argocd/values.yaml
# Source: verified against https://github.com/argoproj/argo-helm/blob/main/charts/argo-cd/values.yaml

global:
  logging:
    level: warn  # reduce log verbosity to save CPU

# Disable components not needed for this project
notifications:
  enabled: false

applicationSet:
  enabled: false  # using plain app-of-apps, not ApplicationSets

dex:
  enabled: false  # no SSO needed for single-developer project

server:
  resources:
    requests:
      cpu: 50m
      memory: 64Mi
    limits:
      cpu: 500m
      memory: 128Mi

controller:
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 500m
      memory: 256Mi

repoServer:
  resources:
    requests:
      cpu: 50m
      memory: 64Mi
    limits:
      cpu: 500m
      memory: 192Mi

redis:
  resources:
    requests:
      cpu: 50m
      memory: 32Mi
    limits:
      cpu: 250m
      memory: 64Mi
```

### SSH Deploy Key Repository Secret
```yaml
# Source: https://argo-cd.readthedocs.io/en/stable/user-guide/private-repositories/
apiVersion: v1
kind: Secret
metadata:
  name: repo-job-hunt
  namespace: argocd
  labels:
    argocd.argoproj.io/secret-type: repository
type: Opaque
stringData:
  type: git
  url: git@github.com:baalexandru/job-hunt.git
  sshPrivateKey: |
    -----BEGIN OPENSSH PRIVATE KEY-----
    ... (actual key content, not committed to Git)
    -----END OPENSSH PRIVATE KEY-----
```

### seal-secrets.sh Helper Script
```bash
#!/usr/bin/env bash
# Usage: ./seal-secrets.sh <environment> (staging|prod)
# Generates random passwords, creates Secret YAML, pipes through kubeseal

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
POSTGRES_PASSWORD="$DB_PASSWORD"  # must match backend's DB_PASSWORD
MINIO_ROOT_USER="$MINIO_ACCESS_KEY"
MINIO_ROOT_PASSWORD="$MINIO_SECRET_KEY"

OUTPUT_DIR="infra/k8s/overlays/${ENV}"

# Backend secrets
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

# Postgres secrets
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

# MinIO secrets
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
```

### CI Workflow Extension (update-tags job)
```yaml
# Added after the build-push job in .github/workflows/ci.yml
update-tags:
  runs-on: ubuntu-latest
  needs: [build-push]
  permissions:
    contents: write
  steps:
    - uses: actions/checkout@v5
      with:
        token: ${{ secrets.GITHUB_TOKEN }}

    - name: Setup Kustomize
      uses: imranismail/setup-kustomize@v2

    - name: Update staging image tags
      run: |
        SHORT_SHA=${GITHUB_SHA::7}
        cd infra/k8s/overlays/staging
        kustomize edit set image \
          jobhunt-backend=ghcr.io/baalexandru/jobhunt-backend:sha-${SHORT_SHA} \
          jobhunt-frontend=ghcr.io/baalexandru/jobhunt-frontend:sha-${SHORT_SHA}

    - name: Commit and push
      run: |
        git config user.name "github-actions[bot]"
        git config user.email "github-actions[bot]@users.noreply.github.com"
        git add infra/k8s/overlays/staging/kustomization.yaml
        git diff --cached --quiet && echo "No changes" && exit 0
        git commit -m "ci: update staging image tags to sha-${GITHUB_SHA::7} [skip ci]"
        git push
```

### Backup Sealed Secrets Controller Key
```bash
#!/usr/bin/env bash
# backup-sealed-key.sh -- Export the controller's signing key pair
set -euo pipefail
BACKUP_DIR="${1:-$HOME/.sealed-secrets-backup}"
mkdir -p "$BACKUP_DIR"
kubectl get secret -n kube-system -l sealedsecrets.bitnami.com/sealed-secrets-key \
  -o yaml > "${BACKUP_DIR}/sealed-secrets-key-$(date +%Y%m%d).yaml"
echo "Key backed up to ${BACKUP_DIR}/"
echo "IMPORTANT: Store this file securely outside Git."
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ArgoCD raw manifest install | Helm chart (argo/argo-cd) | ~2023 | Simpler upgrades, configurable via values.yaml |
| `vars` in Kustomize | `replacements` | Kustomize 4.x (2022) | Old approach deprecated but not removed |
| ArgoCD Image Updater | CI-driven kustomize edit | 2024+ trend | Image Updater still works but CI-driven is simpler for single-repo |
| RSA SSH keys | Ed25519 SSH keys | ArgoCD 2.4+ (OpenSSH 8.9) | ssh-rsa SHA-1 dropped, Ed25519 mandatory |
| Helm 2 with Tiller | Helm 3 (no Tiller) | 2020 | No cluster-side component needed for helm install |

**Deprecated/outdated:**
- ArgoCD `argocd-cm` ConfigMap direct editing -- use Helm values instead
- `ssh-rsa` keys for ArgoCD repo access -- use Ed25519
- Sealed Secrets `--recovery-unseal` flag -- replaced by standard key backup/restore workflow

## Open Questions

1. **Exact ArgoCD Helm chart version to pin**
   - What we know: Chart 9.x is current, app version 2.14+
   - What's unclear: Exact version to pin for reproducibility
   - Recommendation: Use `--version` flag during helm install. The implementer should check `helm search repo argo/argo-cd --versions` at install time and pin to the latest stable. Not blocking for planning.

2. **SHA tag format from docker/metadata-action**
   - What we know: The CI uses `type=sha` which produces `sha-` prefix + 7-char SHA
   - What's unclear: Whether it's exactly 7 chars or full SHA
   - Recommendation: From existing CI, `type=sha` produces `sha-<7chars>` by default. The `kustomize edit set image` command should use `sha-${GITHUB_SHA::7}` to match. Verified from docker/metadata-action docs.

3. **ArgoCD resource limits accuracy**
   - What we know: Full install has server, controller, repo-server, redis components. Budget is ~400MB total.
   - What's unclear: Whether 400MB is achievable with real workloads
   - Recommendation: Start with the values in the code example above (~640MB in limits, but actual usage should be much lower due to small number of Applications). Monitor with `kubectl top` after deployment.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Shell scripts + kubectl assertions |
| Config file | none -- validation is operational |
| Quick run command | `kubectl get pods -n jobhunt-prod -o wide` |
| Full suite command | `kubectl get applications -n argocd` + pod health checks |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| K8S-05 | Backend + frontend pods running and healthy | smoke | `kubectl get pods -n jobhunt-prod -l app=backend -o jsonpath='{.items[0].status.phase}'` should return `Running` | No -- Wave 0 |
| ARGO-01 | ArgoCD installed with web UI | smoke | `kubectl get pods -n argocd` -- all pods Running | No -- Wave 0 |
| ARGO-02 | App-of-apps managing resources | smoke | `kubectl get applications -n argocd` -- shows root + 2 children | No -- Wave 0 |
| ARGO-03 | Sealed Secrets decrypting | smoke | `kubectl get secret backend-secrets -n jobhunt-prod` -- exists (created by controller) | No -- Wave 0 |
| ARGO-04 | Staging auto-sync, prod manual | smoke | Check Application sync policy: `kubectl get app jobhunt-staging -n argocd -o jsonpath='{.spec.syncPolicy.automated}'` returns non-empty | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `kubectl get pods -n jobhunt-prod && kubectl get applications -n argocd`
- **Per wave merge:** Full verification of all 5 requirements
- **Phase gate:** All pods healthy, ArgoCD synced, sealed secrets decrypted, CI pipeline tested end-to-end

### Wave 0 Gaps
- [ ] `infra/scripts/verify-phase17.sh` -- comprehensive verification script checking all requirements
- [ ] Verification is primarily operational (kubectl commands on live cluster), not unit tests
- [ ] Some checks require the SSH tunnel to be active (`infra/scripts/connect.sh`)

## Sources

### Primary (HIGH confidence)
- ArgoCD official docs (https://argo-cd.readthedocs.io/en/stable/) -- cluster bootstrapping, private repositories, auto-sync, Kustomize integration
- Sealed Secrets GitHub (https://github.com/bitnami-labs/sealed-secrets) -- kubeseal usage, Helm install, key backup
- ArgoCD Helm chart values (https://github.com/argoproj/argo-helm/blob/main/charts/argo-cd/values.yaml) -- resource configuration reference
- Existing project manifests -- `infra/k8s/base/`, `infra/k8s/overlays/`, `.github/workflows/ci.yml`

### Secondary (MEDIUM confidence)
- ArgoCD Artifact Hub (https://artifacthub.io/packages/helm/argo/argo-cd) -- chart version 9.4.15
- Sealed Secrets Artifact Hub (https://artifacthub.io/packages/helm/bitnami-labs/sealed-secrets) -- chart version ~0.36.x
- ArgoCD memory usage blog posts -- resource limit recommendations for constrained clusters

### Tertiary (LOW confidence)
- Exact memory consumption numbers for ArgoCD components -- based on community reports, actual usage varies

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- ArgoCD + Sealed Secrets + Kustomize is the established GitOps stack, versions verified
- Architecture: HIGH -- app-of-apps pattern is well-documented in official ArgoCD docs, existing Kustomize overlays provide clear integration points
- Pitfalls: HIGH -- infinite CI loop, namespace mismatch, controller naming issues are well-known and documented
- Memory constraints: MEDIUM -- 400MB ArgoCD budget is based on community data, actual usage needs monitoring

**Research date:** 2026-03-24
**Valid until:** 2026-04-24 (stable ecosystem, 30-day validity)
