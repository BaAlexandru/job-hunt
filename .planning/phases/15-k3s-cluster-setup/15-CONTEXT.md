# Phase 15: K3s Cluster Setup - Context

**Gathered:** 2026-03-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Install K3s on the EC2 instance provisioned in Phase 14, create namespace-based staging/production separation, write Kustomize base + overlay manifests for the full application stack, create the backend production profile (application-prod.yml), and provide convenience scripts for daily cluster operations. This phase does NOT deploy running workloads (Phase 16/17) or configure TLS/domain (Phase 18).

</domain>

<decisions>
## Implementation Decisions

### K3s installation
- SSH bootstrap script (`infra/scripts/bootstrap-k3s.sh`) run manually after Phase 14 provisions the instance
- Keep all K3s defaults: Traefik ingress controller, ServiceLB (Klipper), metrics-server, local-path-provisioner, CoreDNS
- Do NOT open port 6443 in EC2 security group — kubectl access via SSH tunnel only (more secure)
- No security group changes needed in this phase (no OpenTofu modifications)
- Swap file already handled by Phase 14 user-data script — K3s script does not manage swap
- Script outputs instructions for kubeconfig retrieval

### Kubeconfig setup
- Separate `infra/scripts/setup-kubeconfig.sh` script: one-time SCP of `/etc/rancher/k3s/k3s.yaml`, rewrites server URL to `https://127.0.0.1:6443` (for SSH tunnel), merges into `~/.kube/config`
- Scripts read SSH private key path and Elastic IP dynamically from `tofu -chdir=infra/tofu/main output` (outputs: `elastic_ip`, `ssh_private_key_path`)

### Namespace setup
- Two application namespaces: `jobhunt-staging` and `jobhunt-prod`
- No ResourceQuota on either namespace — staging protection relies on replicas: 0 default
- LimitRange applied to both namespaces as a safety net — default resource requests/limits for pods that don't specify them

### Kustomize manifests
- Full stack in base manifests: backend, frontend, PostgreSQL, Redis, MinIO (Phase 16 tunes persistence/backups, not writing manifests from scratch)
- Directory structure follows ARCHITECTURE.md: `infra/k8s/base/{backend,frontend,postgres,redis,minio}/` + `infra/k8s/overlays/{staging,prod}/`
- Each base component has its own subdirectory with deployment/statefulset + service YAML
- Overlays include:
  - Image tag patches (GHCR SHA tags)
  - ConfigMaps for non-sensitive env vars (DB_HOST, REDIS_HOST, MINIO_HOST, etc.)
  - Secret manifests with dummy/placeholder values (Phase 17 converts to SealedSecrets)
  - Ingress resources with placeholder hostnames (job-hunt.dev, staging.job-hunt.dev) — Phase 18 adds TLS config
  - Staging overlay: `replicas: 0` patch for all deployments/statefulsets (scale-to-zero)
  - Production overlay: `replicas: 1`
- Standard Kubernetes Ingress resources, NOT Traefik IngressRoute CRDs (portable, works with any controller)
- Validate manifests with `kustomize build` for each overlay (no cluster needed, catches YAML errors early)

### Backend production profile
- Create `backend/src/main/resources/application-prod.yml` in this phase
- All config via environment variable placeholders matching current application.yml sections:
  - Database: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
  - Redis: REDIS_HOST, REDIS_PORT
  - Storage: MINIO_HOST, MINIO_PORT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET, MINIO_REGION
  - Auth: JWT_SECRET, JWT_ACCESS_EXPIRATION_MS, JWT_REFRESH_EXPIRATION_MS
  - Mail: SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, MAIL_FROM
  - App: FRONTEND_BASE_URL, INTERNAL_API_SECRET
- Natural pairing with K8s ConfigMap/Secret manifests that inject these vars

### Convenience scripts
- `infra/scripts/staging-up.sh`: scales all staging deployments/statefulsets to replicas=1, waits for pods Ready, prints status summary
- `infra/scripts/staging-down.sh`: scales all staging to replicas=0, waits for termination, prints status summary
- `infra/scripts/connect.sh`: opens SSH tunnel on local port 6443 for kubectl access, sets KUBECONFIG
- `infra/scripts/setup-kubeconfig.sh`: one-time kubeconfig fetch and merge (separate from daily-use connect.sh)
- All scripts read Elastic IP and SSH private key path from OpenTofu outputs (`tofu output -raw elastic_ip`, `tofu output -raw ssh_private_key_path`)

### Claude's Discretion
- Exact K3s install command flags beyond the defaults
- LimitRange default values (request/limit sizing)
- Exact kustomization.yaml structure and patch format
- application-prod.yml structure beyond the env var placeholders
- Script error handling and output formatting

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Infrastructure research
- `.planning/research/ARCHITECTURE.md` — Full architecture overview, infra/k8s/ directory layout, Kustomize patterns, namespace strategy, component boundaries
- `.planning/research/PITFALLS.md` — K3s memory constraints, JVM OOM prevention, stateful data persistence, security mistakes

### Project decisions
- `.planning/PROJECT.md` §Key Decisions — K3s over kubeadm, namespace separation, Cloudflare proxy TLS (not cert-manager), Traefik bundled with K3s, staging scale-to-zero, ArgoCD core-mode
- `.planning/ROADMAP.md` §Memory Budget & Mitigation — 2GB constraint, memory budget table, staging scale-to-zero strategy, JVM tuning flags, convenience script names
- `.planning/REQUIREMENTS.md` §K8S-02,K8S-03,K8S-04 — K3s cluster running, namespace + quotas, Kustomize manifests

### Prior phase context
- `.planning/phases/14-aws-infrastructure/14-CONTEXT.md` — EC2 instance details, OpenTofu outputs (elastic_ip, instance_id, security_group_id), SSH access policy, Ubuntu 24.04 AMI, 30GB gp3 volume
- `.planning/phases/12-production-docker-images/12-CONTEXT.md` — Dockerfile locations (backend/Dockerfile, frontend/Dockerfile), JVM tuning flags, base images (Temurin JRE 24 Alpine, Node 22 Alpine)
- `.planning/phases/13-ci-pipeline/13-CONTEXT.md` — GHCR image naming (ghcr.io/baalexandru/jobhunt-{backend,frontend}), tag strategy (SHA + date + latest)

### Existing code
- `infra/tofu/main/outputs.tf` — OpenTofu outputs that scripts will read (elastic_ip, instance_id, instance_public_dns)
- `infra/tofu/main/variables.tf` — SSH key path variables (ssh_public_key_path, ssh_private_key_path)
- `infra/CLAUDE.md` — Infrastructure conventions (needs update for K8s)
- `backend/src/main/resources/application.yml` — Current dev config (reference for production profile)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `infra/tofu/main/outputs.tf` — Elastic IP and instance outputs that scripts will consume via `tofu output`
- `infra/tofu/main/user-data.sh` — EC2 bootstrap script (swap file, apt updates) — K3s script runs after this
- `backend/Dockerfile` — Multi-stage build with JVM tuning flags already set
- `frontend/Dockerfile` — Multi-stage build with Next.js standalone output
- `compose.yaml` — Reference for service ports and env vars (postgres:5432, redis:6379, minio:9000)

### Established Patterns
- OpenTofu modules under `infra/tofu/` — scripts should reference tofu outputs, not hardcode values
- Monorepo structure: `infra/` directory for all infrastructure concerns
- Backend `spring.docker.compose.skip.in-tests: true` pattern — production profile will similarly bypass docker-compose auto-discovery

### Integration Points
- K3s cluster created here is consumed by Phase 16 (data store deployment) and Phase 17 (app deployment + ArgoCD)
- Kustomize manifests created here are consumed by ArgoCD in Phase 17 (app-of-apps pattern)
- Ingress resources created here are finalized in Phase 18 (TLS + domain config)
- application-prod.yml created here is baked into Docker images (already in backend/src/main/resources/)
- GHCR image references in Kustomize overlays must match Phase 13 naming convention

</code_context>

<specifics>
## Specific Ideas

- SSH tunnel for kubectl access (not open 6443) — user prioritizes security over convenience
- Staging protection relies purely on replicas: 0, no ResourceQuota — keeps it simple for a single-developer project
- Full stack manifests in Phase 15 base so Phase 16 focuses on persistence tuning, not manifest authoring
- Scripts dynamically read from OpenTofu outputs — no hardcoded IPs or paths
- Separate one-time setup script (setup-kubeconfig.sh) from daily-use script (connect.sh)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 15-k3s-cluster-setup*
*Context gathered: 2026-03-23*
