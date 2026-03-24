# Phase 17: App Deployment & ArgoCD - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Backend and frontend pods running on K8s, managed by ArgoCD GitOps pipeline. Includes ArgoCD installation (full install with UI), app-of-apps pattern for managing Kustomize overlays, Sealed Secrets for credential management, CI-driven image tag updates, and startup ordering for dependent services. Does NOT include TLS/domain configuration (Phase 18) or data store deployment (Phase 16).

</domain>

<decisions>
## Implementation Decisions

### Sealed Secrets strategy
- Bitnami Sealed Secrets controller installed on the cluster
- `kubeseal` CLI used locally to encrypt secrets; encrypted SealedSecret resources committed to Git
- Replace Phase 15's plaintext placeholder secrets.yaml files with SealedSecret resources
- Separate secrets per environment (staging and production get different DB passwords, JWT secrets, etc.)
- Helper script (`infra/scripts/seal-secrets.sh`) generates random passwords via `openssl rand`, creates Secret YAML, pipes through `kubeseal`, outputs SealedSecret
- Controller signing key backed up locally (exported to a file outside Git) — restore before resealing if cluster is rebuilt
- ~50MB RAM for the Sealed Secrets controller

### ArgoCD installation
- Full ArgoCD install (not core-mode) — includes web UI and API server
- Accept tight RAM headroom (~288MB on 2GB instance); upgrade to t3.medium if needed
- ArgoCD installed in its own namespace (`argocd`)
- Web UI accessible via port-forward or Ingress (Phase 18 can add argocd.job-hunt.dev if desired)
- SSH deploy key for GitHub repo access (read-only deploy key on the repo, private key as K8s Secret)

### App-of-apps pattern
- Root Application (`jobhunt`) points to `infra/argocd/` directory
- Two child Applications: `jobhunt-staging` and `jobhunt-prod`
- Each child Application points to the corresponding Kustomize overlay (`infra/k8s/overlays/staging/` and `infra/k8s/overlays/prod/`)
- ArgoCD Application manifests live in `infra/argocd/` directory

### Deployment flow
- CI-driven image tag updates: GitHub Actions CI adds a step after pushing images — runs `kustomize edit set image` on the staging overlay, commits and pushes to master
- ArgoCD detects the Git change and auto-syncs staging
- Staging auto-sync enabled with self-heal; staging respects replicas: 0 (manifests updated but pods don't run until manual scale-up)
- Production requires manual sync — auto-sync disabled on prod Application
- Promotion: manually update prod overlay's image tag to match staging's tested tag via `kustomize edit set image`, commit, push; then manually sync prod in ArgoCD UI or CLI

### Pod health & rollout
- Init containers on backend Deployment: wait for PostgreSQL (`pg_isready`), Redis (`redis-cli ping`), and MinIO (`curl /minio/health/live`) before backend starts
- Frontend Deployment gets readiness and liveness probes (HTTP GET on `/`)
- First deployment: no special handling — Flyway runs all migrations on startup against empty DB, init containers ensure PostgreSQL is ready first
- Rollback strategy: RollingUpdate (maxUnavailable: 0, maxSurge: 1) keeps old pod running until new one passes readiness probe; ArgoCD auto-rollback on sync failure

### Claude's Discretion
- Exact ArgoCD Helm chart values or manifest configuration
- Init container images and exact wait scripts
- ArgoCD Application manifest details (sync policy, retry settings)
- Sealed Secrets controller installation method (Helm vs raw manifests)
- CI workflow step implementation for kustomize edit + git push
- Frontend probe endpoints, intervals, and thresholds

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Prior phase context
- `.planning/phases/13-ci-pipeline/13-CONTEXT.md` — CI workflow structure, GHCR image naming (`ghcr.io/baalexandru/jobhunt-{backend,frontend}`), tag strategy (SHA + date + latest), workflow file location (`.github/workflows/ci.yml`)
- `.planning/phases/15-k3s-cluster-setup/15-CONTEXT.md` — Kustomize directory layout, namespace strategy, SSH tunnel access, convenience scripts, application-prod.yml env var placeholders
- `.planning/phases/12-production-docker-images/12-CONTEXT.md` — Dockerfile locations, base images, JVM tuning flags, build context paths
- `.planning/phases/14-aws-infrastructure/14-CONTEXT.md` — EC2 instance details, OpenTofu outputs, SSH access policy

### Existing K8s manifests
- `infra/k8s/base/` — Base Kustomize manifests for all components (backend, frontend, postgres, redis, minio)
- `infra/k8s/overlays/prod/kustomization.yaml` — Production overlay with GHCR image references, replicas: 1
- `infra/k8s/overlays/staging/kustomization.yaml` — Staging overlay with replicas: 0
- `infra/k8s/overlays/prod/secrets.yaml` — Plaintext placeholder secrets (to be replaced with SealedSecrets)
- `infra/k8s/overlays/prod/configmap.yaml` — Backend and frontend ConfigMaps
- `infra/k8s/overlays/prod/ingress.yaml` — Ingress with host-based routing (job-hunt.dev)
- `infra/k8s/base/backend/deployment.yaml` — Backend Deployment with liveness/readiness probes, envFrom, resource limits

### CI workflow
- `.github/workflows/ci.yml` — Current CI pipeline (test, build-push, scan) — needs new step for kustomize image tag update

### Infrastructure
- `.planning/ROADMAP.md` §Memory Budget & Mitigation — 2GB constraint, updated budget with full ArgoCD
- `.planning/REQUIREMENTS.md` §K8S-05, ARGO-01..04 — Requirements for this phase
- `.planning/research/ARCHITECTURE.md` — Architecture overview, directory layout
- `.planning/research/PITFALLS.md` — Known pitfalls for K3s on EC2

### Convenience scripts
- `infra/scripts/staging-up.sh` — Scale staging to replicas: 1
- `infra/scripts/staging-down.sh` — Scale staging to replicas: 0
- `infra/scripts/connect.sh` — SSH tunnel for kubectl access

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `infra/k8s/overlays/{staging,prod}/` — Complete Kustomize overlays with ConfigMaps, Secrets (placeholder), and Ingress already defined
- `infra/k8s/base/backend/deployment.yaml` — Backend Deployment with probes, resource limits, envFrom, security context already configured
- `.github/workflows/ci.yml` — CI pipeline ready to be extended with kustomize image tag update step
- `infra/scripts/` — Existing convenience scripts (connect.sh, staging-up/down.sh) for cluster operations

### Established Patterns
- Kustomize `images` transformer in overlays for image tag management — CI will use `kustomize edit set image` to update
- SSH tunnel for kubectl access (port 6443 not exposed) — ArgoCD UI will need port-forward or Ingress
- OpenTofu outputs consumed dynamically by scripts — no hardcoded IPs
- Backend uses envFrom with ConfigMapRef and SecretRef — SealedSecrets must produce Secrets with matching names

### Integration Points
- SealedSecrets must produce K8s Secrets with names matching existing secretRef: `backend-secrets`, `postgres-secrets`, `minio-secrets`
- CI workflow needs write access to push kustomize tag updates back to the repo
- ArgoCD needs read access to the Git repo via SSH deploy key
- Init containers need lightweight images with pg_isready, redis-cli, and curl utilities

</code_context>

<specifics>
## Specific Ideas

- User chose full ArgoCD install over core-mode despite tight RAM — values the web UI for monitoring
- User wants separate secrets per environment even though it doubles management — proper isolation
- User prefers CI-driven tag updates (Git as source of truth) over ArgoCD Image Updater (extra controller)
- Manual promotion workflow: update prod overlay tag, commit, push, then manually sync in ArgoCD
- Staging auto-syncs but respects replicas: 0 — manifests stay current without consuming RAM

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 17-app-deployment-argocd*
*Context gathered: 2026-03-24*
