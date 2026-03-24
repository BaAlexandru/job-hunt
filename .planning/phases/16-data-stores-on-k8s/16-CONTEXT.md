# Phase 16: Data Stores on K8s - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

All persistent data services (PostgreSQL, Redis, MinIO) running on K8s with data that survives pod restarts. Includes volume reclaim policy enforcement, Redis persistence enablement, automated daily pg_dump backups to MinIO, MinIO bucket initialization, and a convenience script for downloading backups locally. This phase tunes and completes the base manifests created in Phase 15 — it does NOT write manifests from scratch. Sealed Secrets and init containers are Phase 17's responsibility.

</domain>

<decisions>
## Implementation Decisions

### Redis persistence
- Convert Redis from Deployment to StatefulSet with PVC (1Gi, `local-path-retain` StorageClass)
- Enable RDB snapshots with Redis default save policy (3600s/1 change, 300s/100 changes, 60s/10000 changes)
- Add volume mount for `/data` and configure Redis with `--save` and `--dir /data` args
- Update Redis service to headless (StatefulSet requires headless service)
- Keep existing resource limits (64Mi request, 96Mi limit)

### Volume reclaim policy
- Create a custom StorageClass `local-path-retain` that uses the local-path-provisioner but with `reclaimPolicy: Retain`
- StorageClass is NOT the cluster default — PVC templates reference it explicitly
- Update all data store PVC templates (PostgreSQL, MinIO, Redis) to use `storageClassName: local-path-retain`
- StorageClass manifest lives in `infra/k8s/base/` (shared across all data stores)
- Additionally set `persistentVolumeClaimRetentionPolicy: { whenDeleted: Retain, whenScaled: Retain }` on all StatefulSets as an extra safety layer

### Backup strategy
- Daily pg_dump CronJob at 02:00 UTC
- Backups stored in MinIO `jobhunt-backups` bucket
- CronJob uses `pg_dump` + `mc` CLI to dump and upload with timestamped filename
- 7-day retention on MinIO via lifecycle policy (auto-expiry, no CronJob cleanup logic needed)
- Convenience script `infra/scripts/download-backups.sh` pulls backups from MinIO to local machine via SSH tunnel + mc
- User periodically runs the download script to archive backups locally before 7-day expiry

### Bucket initialization
- Kubernetes Job using `mc` CLI creates both buckets: `jobhunt-documents` and `jobhunt-backups`
- Job is idempotent (`mc mb --ignore-existing`)
- Job also sets lifecycle policy on `jobhunt-backups` (7-day expiry via `mc ilm rule add`)
- Job runs after MinIO StatefulSet is ready (depends on MinIO service being accessible)
- Job manifest lives in `infra/k8s/base/minio/` alongside the StatefulSet

### Claude's Discretion
- Exact Redis config args beyond save policy
- CronJob pg_dump flags and output format (plain vs custom)
- mc CLI image choice for CronJob and init Job
- StorageClass provisioner configuration details
- Download script implementation (mc mirror vs mc cp)
- Job backoff and restart policy settings

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Infrastructure research
- `.planning/research/ARCHITECTURE.md` — Full architecture overview, infra/k8s/ directory layout, Kustomize patterns, component boundaries
- `.planning/research/PITFALLS.md` — K3s memory constraints, stateful data persistence, local-path-provisioner pitfalls

### Project decisions
- `.planning/PROJECT.md` §Key Decisions — K3s over kubeadm, namespace separation, staging scale-to-zero
- `.planning/ROADMAP.md` §Memory Budget & Mitigation — 2GB constraint, memory budget table (PostgreSQL 256Mi, Redis 64Mi, MinIO 128Mi)
- `.planning/REQUIREMENTS.md` §DATA-01,DATA-02,DATA-03,DATA-04 — Data store requirements

### Prior phase context
- `.planning/phases/15-k3s-cluster-setup/15-CONTEXT.md` — Kustomize directory layout, base manifests for all data stores, local-path StorageClass, convenience scripts pattern, staging replicas: 0
- `.planning/phases/14-aws-infrastructure/14-CONTEXT.md` — EC2 instance details, 30GB gp3 EBS volume, SSH access via tunnel
- `.planning/phases/17-app-deployment-argocd/17-CONTEXT.md` — Sealed Secrets strategy (Phase 17 replaces placeholder secrets), init containers for backend health checks, app-of-apps pattern

### Existing manifests (Phase 15 output)
- `infra/k8s/base/postgres/statefulset.yaml` — PostgreSQL StatefulSet with 10Gi PVC, probes, resource limits
- `infra/k8s/base/postgres/service.yaml` — Headless service for PostgreSQL
- `infra/k8s/base/redis/deployment.yaml` — Redis Deployment (NO volume, needs conversion to StatefulSet)
- `infra/k8s/base/redis/service.yaml` — ClusterIP service (needs conversion to headless)
- `infra/k8s/base/minio/statefulset.yaml` — MinIO StatefulSet with 10Gi PVC, probes, resource limits
- `infra/k8s/base/minio/service.yaml` — Headless service for MinIO
- `infra/k8s/overlays/prod/secrets.yaml` — Placeholder secrets (postgres-secrets, minio-secrets, backend-secrets)
- `infra/k8s/overlays/prod/configmap.yaml` — Backend and frontend ConfigMaps

</canonical_refs>

<skills>
## Recommended Skills

**Downstream agents SHOULD activate these skills during planning and execution.**

### Primary Skills (must use)

| Skill | Reason |
|---|---|
| `kubernetes-specialist` | Phase is 90% K8s manifest work: StatefulSets, CronJobs, Jobs, StorageClasses, PVCs, headless Services, resource limits, pod security contexts, persistentVolumeClaimRetentionPolicy |
| `verification-before-completion` | Verify all manifests render cleanly with `kustomize build`, validate YAML correctness, confirm staging overlay patches cover new Redis StatefulSet |

### Secondary Skills (helpful)

| Skill | Reason |
|---|---|
| `sequential-thinking` | Multi-step deployment ordering: StorageClass must exist before StatefulSets, MinIO must be ready before init Job, PostgreSQL must be ready before backup CronJob |
| `docker-helper` | Understanding container images used in Jobs/CronJobs (`minio/mc` for bucket init + backup upload, `postgres:17` for pg_dump) |

### Context7 Libraries (query during planning/execution)

| Library ID | Topic | Why |
|---|---|---|
| `/websites/kubernetes_io` | StatefulSet, StorageClass, CronJob, Job, PVC API | Core K8s API docs for all manifest authoring — reclaimPolicy options, persistentVolumeClaimRetentionPolicy, CronJob schedule syntax |
| `/kubernetes-sigs/kustomize` | overlays, patches, resources, kustomization.yaml | Kustomize patterns for adding new resources to base, patching existing StatefulSets, staging overlay updates |
| `/websites/redis_io` | RDB persistence, save configuration, BGSAVE | Redis persistence config — confirm `--save` arg syntax, default save intervals, volume mount path `/data` |
| `/minio/docs` | ILM lifecycle rules, bucket expiration | MinIO lifecycle management — `mc ilm rule add` syntax for 7-day object expiry on `jobhunt-backups` |
| `/minio/mc` | mc mb, mc alias, mc cp, mc ilm | mc CLI commands for bucket creation (`mc mb --ignore-existing`), alias setup, file operations, lifecycle rules |
| `/k3s-io/docs` | local-path-provisioner, StorageClass, PVC | K3s storage — confirm `rancher.io/local-path` provisioner name, custom StorageClass creation, PVC binding behavior |

### Not Applicable (confirmed by analysis)

All Spring/Kotlin/Java, frontend/React/Next.js, auth, Gradle, CI/CD, and testing skills are irrelevant — Phase 16 is purely infrastructure manifest and script work. No application code changes.

</skills>

<tech_findings>
## Context7 Technical Findings

Key findings from documentation research that downstream agents should be aware of:

### StorageClass with Retain
- K8s StorageClass supports `reclaimPolicy: Retain` as a first-class field (options: `Delete`, `Retain`, `Recycle`)
- Default for dynamically provisioned PVs is `Delete` — must explicitly set `Retain`
- K3s local-path-provisioner name: `rancher.io/local-path`
- Custom StorageClass just needs: `provisioner: rancher.io/local-path` + `reclaimPolicy: Retain` + `volumeBindingMode: WaitForFirstConsumer`

### StatefulSet PVC Retention Policy
- StatefulSet spec supports `persistentVolumeClaimRetentionPolicy` with `whenDeleted` and `whenScaled` fields
- Both default to `Retain` — but setting explicitly documents intent and survives API version changes
- This is a second layer of protection on top of the StorageClass reclaimPolicy

### Redis RDB Persistence
- Redis `--save` CLI args configure RDB snapshots: `--save "3600 1" --save "300 100" --save "60 10000"`
- `--dir /data` sets the RDB dump file location
- `BGSAVE` runs async — safe for production use, no downtime during snapshots
- RDB file is `dump.rdb` in the configured dir

### MinIO mc CLI
- `mc mb ALIAS/BUCKET --ignore-existing` — idempotent bucket creation
- `mc ilm rule add ALIAS/BUCKET --expiry-days 7` — set object lifecycle expiry
- `mc alias set NAME ENDPOINT ACCESS_KEY SECRET_KEY` — configure connection
- Docker image: `minio/mc` with `entrypoint: ['']` override for K8s Jobs
- `mc cp` for file upload, `mc ls` for listing, `mc mirror` for sync

### K3s Local-Path-Provisioner
- Default StorageClass in K3s: `local-path` with provisioner `rancher.io/local-path`
- Stores data at `/var/lib/rancher/k3s/storage/` on the host node
- Does NOT support `reclaimPolicy: Retain` out of the box — must create custom StorageClass
- PVCs bind immediately (`volumeBindingMode: Immediate` is default, but `WaitForFirstConsumer` is safer for scheduling)

</tech_findings>

<code_context>
## Existing Code Insights

### Reusable Assets
- `infra/k8s/base/postgres/statefulset.yaml` — StatefulSet pattern with PVC template, probes, securityContext already defined
- `infra/k8s/base/minio/statefulset.yaml` — Similar StatefulSet pattern, can use as template for Redis conversion
- `infra/k8s/overlays/prod/secrets.yaml` — Secret names (`postgres-secrets`, `minio-secrets`) that CronJob needs for DB credentials
- `infra/scripts/connect.sh` — SSH tunnel script that download-backups.sh can reuse for cluster access pattern
- `compose.yaml` — Reference for service ports and env var names used in local dev

### Established Patterns
- Kustomize base + overlays structure under `infra/k8s/`
- StatefulSet with headless Service for stateful workloads (PostgreSQL, MinIO)
- `envFrom` with `secretRef` for credential injection
- Resource requests/limits on all containers
- Security context with `runAsUser` and `fsGroup`
- Staging overlay patches replicas to 0

### Integration Points
- Custom StorageClass referenced by all three data store PVC templates
- CronJob needs access to `postgres-secrets` for DB credentials and `minio-secrets` for mc CLI authentication
- Bucket init Job needs `minio-secrets` for mc CLI authentication
- Download script needs SSH tunnel (same pattern as `connect.sh`) and mc CLI installed locally
- Kustomize base `kustomization.yaml` needs to include new resources (StorageClass, CronJob, Job)
- Staging overlay needs replicas: 0 patch for Redis StatefulSet (new resource)

</code_context>

<specifics>
## Specific Ideas

- Two-tier backup retention: 7 days auto-managed on MinIO (lifecycle policy), then user manually downloads to local machine before expiry
- Redis must persist because it stores sessions — cache loss means user logout
- Custom StorageClass is explicit-only (not cluster default) — intentional data protection for data stores without affecting other workloads
- MinIO lifecycle policy handles backup cleanup — CronJob stays simple (dump + upload only)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 16-data-stores-on-k8s*
*Context gathered: 2026-03-24*
