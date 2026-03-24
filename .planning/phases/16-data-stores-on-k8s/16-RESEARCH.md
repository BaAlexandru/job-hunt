# Phase 16: Data Stores on K8s - Research

**Researched:** 2026-03-24
**Domain:** Kubernetes StatefulSets, CronJobs, Jobs, StorageClasses, MinIO lifecycle, Redis persistence
**Confidence:** HIGH

## Summary

Phase 16 tunes and completes the base K8s manifests created in Phase 15 to make all three data stores (PostgreSQL, Redis, MinIO) production-ready with data that survives pod restarts. The work is purely infrastructure manifest authoring -- no application code changes. The existing Phase 15 manifests provide a solid foundation: PostgreSQL and MinIO are already StatefulSets with PVCs, and Redis is a Deployment that must be converted to a StatefulSet.

The key additions are: (1) a custom `local-path-retain` StorageClass with `reclaimPolicy: Retain` referenced by all three PVC templates, (2) Redis conversion from Deployment to StatefulSet with RDB persistence, (3) a MinIO bucket init Job using the `mc` CLI, (4) a daily pg_dump CronJob that uploads backups to MinIO, and (5) a convenience script for downloading backups locally.

**Primary recommendation:** Follow the locked decisions in CONTEXT.md exactly. All K8s API patterns are standard and well-documented. The main risk is the MinIO Docker image situation (see Pitfall 1) and ensuring correct `mc` CLI flag names.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Redis: Convert from Deployment to StatefulSet with PVC (1Gi, `local-path-retain` StorageClass), enable RDB snapshots with default save policy, volume mount for `/data`, headless service, keep existing resource limits (64Mi request, 96Mi limit)
- Volume reclaim: Custom StorageClass `local-path-retain` with `reclaimPolicy: Retain`, NOT cluster default, PVC templates reference explicitly, manifest in `infra/k8s/base/`, `persistentVolumeClaimRetentionPolicy: { whenDeleted: Retain, whenScaled: Retain }` on all StatefulSets
- Backup: Daily pg_dump CronJob at 02:00 UTC, backups to MinIO `jobhunt-backups` bucket, pg_dump + mc CLI, timestamped filenames, 7-day retention via MinIO lifecycle policy, convenience script `infra/scripts/download-backups.sh`
- Bucket init: Kubernetes Job using mc CLI creates `jobhunt-documents` and `jobhunt-backups` buckets, idempotent with `mc mb --ignore-existing`, sets lifecycle policy on `jobhunt-backups` (7-day expiry), runs after MinIO is ready, manifest in `infra/k8s/base/minio/`

### Claude's Discretion
- Exact Redis config args beyond save policy
- CronJob pg_dump flags and output format (plain vs custom)
- mc CLI image choice for CronJob and init Job
- StorageClass provisioner configuration details
- Download script implementation (mc mirror vs mc cp)
- Job backoff and restart policy settings

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DATA-01 | PostgreSQL deployed as StatefulSet with persistent volume (reclaimPolicy: Retain) | Custom `local-path-retain` StorageClass + `persistentVolumeClaimRetentionPolicy` on existing StatefulSet; update `storageClassName` in PVC template |
| DATA-02 | Redis deployed on K8s with persistence | Convert existing Deployment to StatefulSet with 1Gi PVC, add `--save` and `--dir /data` args for RDB snapshots, convert Service to headless |
| DATA-03 | MinIO deployed as StatefulSet with persistent volume | Already a StatefulSet from Phase 15; update `storageClassName` to `local-path-retain`, add bucket init Job |
| DATA-04 | Automated daily pg_dump backup CronJob to S3 | CronJob at `0 2 * * *`, uses `postgres:17` for pg_dump + `minio/mc` for upload, stores in MinIO `jobhunt-backups` bucket with timestamped filenames |

</phase_requirements>

## Standard Stack

### Core
| Component | Version/Image | Purpose | Why Standard |
|-----------|---------------|---------|--------------|
| StorageClass | K8s API v1 | Custom `local-path-retain` with Retain policy | Standard K8s resource; only way to enforce reclaimPolicy on dynamically provisioned PVs |
| StatefulSet | apps/v1 | Redis conversion + existing PG/MinIO | Standard K8s workload for stateful apps; stable network identity + stable storage |
| CronJob | batch/v1 | Daily pg_dump backups | Standard K8s scheduled workload; manages job lifecycle automatically |
| Job | batch/v1 | MinIO bucket initialization | Standard K8s one-shot workload; idempotent with restartPolicy |

### Supporting
| Component | Image | Purpose | When to Use |
|-----------|-------|---------|-------------|
| postgres:17 | `postgres:17` | pg_dump client in CronJob container | Same version as the database StatefulSet; ensures pg_dump compatibility |
| minio/mc | `minio/mc:RELEASE.2025-09-07T16-10-49Z` | mc CLI for bucket init Job and backup upload | Last available pinned release on Docker Hub; use for mc commands |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `minio/mc` | `bitnami/minio-client` | Bitnami image is actively maintained but has its own deprecation concerns (Broadcom/Bitnami Secure transition); minio/mc frozen at Oct 2025 but functionally complete for this use case |
| `minio/mc` | `cgr.dev/chainguard/minio-client` | Chainguard offers free, hardened images with latest mc builds; better security posture but adds a non-Docker Hub registry dependency |
| Plain SQL dump | pg_dump custom format (`-Fc`) | Custom format supports parallel restore and selective restore, but plain format is human-readable and simpler for a single-user app |

**Recommendation (Claude's Discretion -- mc CLI image):** Use `minio/mc:RELEASE.2025-09-07T16-10-49Z` pinned to the last available release. The mc CLI is frozen but fully functional for `mc mb`, `mc cp`, `mc ilm rule add`, and `mc alias set`. If the image disappears from Docker Hub, switch to `bitnami/minio-client` as a drop-in replacement.

**Recommendation (Claude's Discretion -- pg_dump format):** Use plain format (`pg_dump --format=plain`) with gzip compression via pipe (`pg_dump ... | gzip`). For a single-user job tracker, plain format is simpler to inspect and restore. The compressed output saves storage on the 30GB EBS volume.

## Architecture Patterns

### New Files to Create
```
infra/k8s/
  base/
    storage-class.yaml                    # local-path-retain StorageClass (shared)
    redis/
      statefulset.yaml                    # REPLACE deployment.yaml
      service.yaml                        # MODIFY to headless (clusterIP: None)
    minio/
      bucket-init-job.yaml                # NEW: mc mb + mc ilm rule add
    postgres/
      backup-cronjob.yaml                 # NEW: pg_dump + mc cp CronJob
    kustomization.yaml                    # UPDATE: add new resources, remove old
  overlays/
    staging/
      kustomization.yaml                  # UPDATE: redis replicas entry (name change if needed)
    prod/
      kustomization.yaml                  # UPDATE: redis replicas entry (name change if needed)
infra/scripts/
  download-backups.sh                     # NEW: SSH tunnel + mc mirror from MinIO
```

### Files to Modify
```
infra/k8s/base/redis/deployment.yaml      # DELETE (replaced by statefulset.yaml)
infra/k8s/base/redis/service.yaml         # MODIFY: type ClusterIP -> clusterIP: None (headless)
infra/k8s/base/postgres/statefulset.yaml  # MODIFY: storageClassName, persistentVolumeClaimRetentionPolicy
infra/k8s/base/minio/statefulset.yaml     # MODIFY: storageClassName, persistentVolumeClaimRetentionPolicy
infra/k8s/base/kustomization.yaml         # MODIFY: update resource paths
```

### Pattern 1: Custom StorageClass with Retain
**What:** A StorageClass that wraps K3s `local-path` provisioner but with `reclaimPolicy: Retain`
**When to use:** All data store PVC templates
**Example:**
```yaml
# Source: K8s StorageClass API + K3s local-path-provisioner docs
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-path-retain
provisioner: rancher.io/local-path
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
```

**Key:** This is NOT set as cluster default (no `storageclass.kubernetes.io/is-default-class: "true"` annotation). PVC templates must reference `storageClassName: local-path-retain` explicitly.

### Pattern 2: StatefulSet PVC Retention Policy
**What:** Explicit `persistentVolumeClaimRetentionPolicy` on StatefulSets as a second safety layer
**When to use:** All three data store StatefulSets (postgres, redis, minio)
**Example:**
```yaml
# Source: K8s StatefulSet API docs
spec:
  persistentVolumeClaimRetentionPolicy:
    whenDeleted: Retain
    whenScaled: Retain
```

### Pattern 3: Redis StatefulSet with RDB Persistence
**What:** Convert Redis Deployment to StatefulSet with volume and save args
**When to use:** Redis needs session persistence across restarts
**Example:**
```yaml
# Source: Redis docs + K8s StatefulSet API
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
spec:
  serviceName: redis
  replicas: 1
  selector:
    matchLabels:
      app: redis
  persistentVolumeClaimRetentionPolicy:
    whenDeleted: Retain
    whenScaled: Retain
  template:
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command: ["redis-server"]
          args:
            - "--save"
            - "3600 1 300 100 60 10000"
            - "--dir"
            - "/data"
          volumeMounts:
            - name: data
              mountPath: /data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: local-path-retain
        resources:
          requests:
            storage: 1Gi
```

**Note on Redis `--save` syntax:** Redis 7 accepts multiple save rules in a single `--save` argument as space-separated pairs: `--save "3600 1 300 100 60 10000"`. This is equivalent to three separate save directives.

### Pattern 4: K8s Job for MinIO Bucket Init
**What:** One-shot Job that creates buckets and sets lifecycle policy
**When to use:** After MinIO StatefulSet is ready
**Example:**
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: minio-bucket-init
spec:
  backoffLimit: 5
  template:
    spec:
      restartPolicy: OnFailure
      containers:
        - name: mc
          image: minio/mc:RELEASE.2025-09-07T16-10-49Z
          command: ["/bin/sh", "-c"]
          args:
            - |
              mc alias set myminio http://minio:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD &&
              mc mb myminio/jobhunt-documents --ignore-existing &&
              mc mb myminio/jobhunt-backups --ignore-existing &&
              mc ilm rule ls myminio/jobhunt-backups --json | grep -q '"expiration"' ||
              mc ilm rule add myminio/jobhunt-backups --expire-days 7
          envFrom:
            - secretRef:
                name: minio-secrets
```

**Critical:** The mc CLI flag is `--expire-days` (NOT `--expiry-days`). Verified against official MinIO documentation.

### Pattern 5: CronJob for pg_dump to MinIO
**What:** Scheduled backup job that dumps PostgreSQL and uploads to MinIO
**When to use:** Daily at 02:00 UTC
**Example:**
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
spec:
  schedule: "0 2 * * *"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      backoffLimit: 3
      template:
        spec:
          restartPolicy: OnFailure
          automountServiceAccountToken: false
          containers:
            - name: backup
              image: postgres:17
              command: ["/bin/sh", "-c"]
              args:
                - |
                  TIMESTAMP=$(date +%Y%m%d-%H%M%S)
                  FILENAME="jobhunt-${TIMESTAMP}.sql.gz"
                  pg_dump -h postgres -U $POSTGRES_USER -d $POSTGRES_DB | gzip > /tmp/${FILENAME} &&
                  # Install mc CLI (small binary, fast download)
                  curl -sSL https://dl.min.io/client/mc/release/linux-amd64/mc -o /usr/local/bin/mc &&
                  chmod +x /usr/local/bin/mc &&
                  mc alias set myminio http://minio:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD &&
                  mc cp /tmp/${FILENAME} myminio/jobhunt-backups/${FILENAME}
              env:
                - name: PGPASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: postgres-secrets
                      key: POSTGRES_PASSWORD
              envFrom:
                - secretRef:
                    name: postgres-secrets
                - secretRef:
                    name: minio-secrets
              resources:
                requests:
                  memory: 64Mi
                  cpu: 50m
                limits:
                  memory: 128Mi
                  cpu: 250m
```

**Design choice (Claude's Discretion):** Use `postgres:17` as the CronJob image and download `mc` at runtime rather than building a custom image or using an init container with two images. This keeps it simple -- mc is a single ~25MB static binary and downloads in seconds. The alternative (multi-container pod with shared volume) adds complexity without benefit for a daily job.

**Alternative approach:** Use a multi-container pod -- init container with `minio/mc` copies the binary to a shared emptyDir volume, then the main `postgres:17` container uses both pg_dump and mc. This avoids downloading mc on every run but adds YAML complexity. Either approach works; the download approach is simpler.

### Anti-Patterns to Avoid
- **Using `reclaimPolicy: Delete` for data stores:** Default for dynamically provisioned PVs. Deleting a PVC permanently destroys the backing volume. Always use Retain for database data.
- **Setting `local-path-retain` as cluster default:** Other workloads (backend, frontend Deployments) should use the default `local-path` StorageClass with Delete policy. Only data stores need Retain.
- **Storing backup cleanup logic in the CronJob:** MinIO lifecycle policy handles auto-expiry. The CronJob should only dump and upload -- separation of concerns.
- **Using `restartPolicy: Always` in Jobs/CronJobs:** Jobs require `OnFailure` or `Never`. `Always` is invalid and will be rejected by the API server.
- **Referencing the MinIO service before it is ready in the init Job:** The bucket init Job depends on MinIO being accessible. Use `backoffLimit` with `restartPolicy: OnFailure` so it retries if MinIO is not yet ready.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Backup retention/cleanup | CronJob that lists and deletes old backups | MinIO ILM lifecycle policy (`mc ilm rule add --expire-days 7`) | MinIO handles expiry atomically; no race conditions, no missed cleanups |
| Scheduled workloads | Cron daemon in a container | K8s CronJob resource | K8s manages scheduling, retries, history, concurrency policy |
| One-time initialization | Init container on every pod start | K8s Job (runs once, completes) | Jobs are idempotent with `--ignore-existing`; init containers run on every pod restart |
| Volume reclaim protection | Manual PV patching after creation | StorageClass with `reclaimPolicy: Retain` | StorageClass applies policy at PV creation time; manual patching is error-prone |

**Key insight:** K8s has first-class resources (StorageClass, CronJob, Job) for all the problems this phase solves. The work is manifest authoring, not code.

## Common Pitfalls

### Pitfall 1: MinIO Docker Image Availability
**What goes wrong:** The `minio/minio` and `minio/mc` Docker Hub images are no longer being updated. MinIO shifted to source-only distribution in October 2025, and the GitHub repo was archived in February 2026. Existing tags on Docker Hub still work but will never receive security updates.
**Why it happens:** MinIO changed its business model. The community edition is now source-only.
**How to avoid:** Pin to a specific release tag (e.g., `minio/minio:RELEASE.2025-04-22T22-12-26Z`, `minio/mc:RELEASE.2025-09-07T16-10-49Z`). For a personal single-user project, the frozen images are adequate. If security updates become critical, switch to `cgr.dev/chainguard/minio` (free tier) or `bitnami/minio` as drop-in replacements.
**Warning signs:** `ImagePullBackOff` if tags are removed from Docker Hub (unlikely but possible).
**Impact on this phase:** LOW risk. The existing `minio/minio:latest` in the current StatefulSet resolves to the last published release. Pin it to a specific tag for reproducibility.

### Pitfall 2: mc CLI Flag Name Confusion
**What goes wrong:** Using `--expiry-days` instead of `--expire-days` in the `mc ilm rule add` command. The command fails silently or with an unrecognized flag error.
**Why it happens:** Multiple blog posts and older documentation use inconsistent flag names. The CONTEXT.md tech findings mention "expiry-days" which is incorrect.
**How to avoid:** The official MinIO docs confirm the flag is `--expire-days`. Always use: `mc ilm rule add ALIAS/BUCKET --expire-days 7`
**Warning signs:** `mc ilm rule add` exits with non-zero status.

### Pitfall 3: StorageClass Namespace Scoping
**What goes wrong:** Placing the StorageClass manifest in an overlay directory and having the Kustomize namespace transformer add a namespace to it. StorageClass is a cluster-scoped resource and must NOT have a namespace.
**Why it happens:** The `namespace:` field in overlay kustomization.yaml applies to all resources, including cluster-scoped ones.
**How to avoid:** Place `storage-class.yaml` in the base directory. Since it is listed as a base resource and is cluster-scoped, the Kustomize namespace transformer will correctly skip it (Kustomize recognizes cluster-scoped resource kinds and does not inject namespace).
**Warning signs:** `kustomize build` output shows a `namespace:` field on the StorageClass resource.

### Pitfall 4: Redis Save Argument Syntax
**What goes wrong:** Passing multiple `--save` flags or using incorrect separator syntax. Redis may silently ignore save rules or use only the last one.
**Why it happens:** Redis save syntax has changed across versions. In Redis 7, `--save "3600 1 300 100 60 10000"` passes all rules as a single argument.
**How to avoid:** Use the format: `args: ["--save", "3600 1 300 100 60 10000", "--dir", "/data"]`. Test by checking `redis-cli CONFIG GET save` after the pod starts.
**Warning signs:** No `dump.rdb` file appears in `/data` after triggering saves.

### Pitfall 5: CronJob Pod Credential Access
**What goes wrong:** The backup CronJob cannot connect to PostgreSQL or MinIO because it references secrets that do not exist in the namespace, or uses wrong key names.
**Why it happens:** Secrets are defined in the overlay (prod/staging), not in base. The CronJob manifest in base references secret names that must match the overlay secrets.
**How to avoid:** Use the same secret names (`postgres-secrets`, `minio-secrets`) as already established in the overlays. Reference specific keys via `secretKeyRef` for `PGPASSWORD` and use `envFrom` for bulk injection.
**Warning signs:** CronJob pods fail with "secret not found" or "connection refused" errors.

### Pitfall 6: Kustomize replicas Field and StatefulSet Name Change
**What goes wrong:** After converting Redis from Deployment to StatefulSet, the staging overlay `replicas` entry for `redis` might not match if the resource name changes.
**Why it happens:** Kustomize `replicas` field matches by resource name. If the StatefulSet keeps the same name (`redis`), the existing replicas entry works for both Deployments and StatefulSets.
**How to avoid:** Keep the StatefulSet name as `redis` (same as the current Deployment). The `replicas` shorthand in Kustomize works for both Deployments and StatefulSets by name.
**Warning signs:** `kustomize build` shows unexpected replica count for Redis.

## Code Examples

### StorageClass Manifest
```yaml
# infra/k8s/base/storage-class.yaml
# Source: K8s StorageClass API + K3s local-path-provisioner docs
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-path-retain
provisioner: rancher.io/local-path
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
```

### Updated PostgreSQL StatefulSet (changes only)
```yaml
# Add to spec:
spec:
  persistentVolumeClaimRetentionPolicy:
    whenDeleted: Retain
    whenScaled: Retain
  # Update volumeClaimTemplates:
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: local-path-retain    # Changed from local-path
        resources:
          requests:
            storage: 10Gi
```

### Redis Headless Service
```yaml
# infra/k8s/base/redis/service.yaml (modified)
apiVersion: v1
kind: Service
metadata:
  name: redis
  labels:
    app: redis
spec:
  clusterIP: None              # Changed from ClusterIP type to headless
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
      name: redis
```

### Updated Base kustomization.yaml
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - storage-class.yaml                   # NEW
  - backend/deployment.yaml
  - backend/service.yaml
  - frontend/deployment.yaml
  - frontend/service.yaml
  - postgres/statefulset.yaml
  - postgres/service.yaml
  - postgres/backup-cronjob.yaml          # NEW
  - redis/statefulset.yaml                # CHANGED from redis/deployment.yaml
  - redis/service.yaml
  - minio/statefulset.yaml
  - minio/service.yaml
  - minio/bucket-init-job.yaml            # NEW
```

### Download Backups Script Pattern
```bash
#!/usr/bin/env bash
# infra/scripts/download-backups.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

LOCAL_BACKUP_DIR="${1:-$PROJECT_ROOT/backups}"
mkdir -p "$LOCAL_BACKUP_DIR"

# Port-forward MinIO through SSH tunnel + kubectl
echo "Setting up port-forward to MinIO..."
ssh -L 9000:localhost:9000 -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new \
  ubuntu@"$ELASTIC_IP" \
  "kubectl port-forward -n jobhunt-prod svc/minio 9000:9000" &
SSH_PID=$!
trap "kill $SSH_PID 2>/dev/null" EXIT
sleep 3

mc alias set k8s-minio http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mirror k8s-minio/jobhunt-backups "$LOCAL_BACKUP_DIR/"

echo "Backups downloaded to $LOCAL_BACKUP_DIR"
```

**Recommendation (Claude's Discretion -- download script):** Use `mc mirror` over `mc cp` because `mirror` syncs incrementally -- it skips files already downloaded locally, making repeated runs efficient.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `minio/minio` on Docker Hub | Source-only distribution; use pinned tags or Chainguard | Oct 2025 | Pin existing images; switch to Chainguard if needed |
| `minio/mc` on Docker Hub | Same as above; frozen images still functional | Oct 2025 | Pin to `RELEASE.2025-09-07T16-10-49Z` |
| `mc ilm` (old subcommand) | `mc ilm rule add` (structured subcommands) | MinIO mc 2023+ | Use `mc ilm rule add --expire-days N` syntax |
| Redis `save` in config file | Redis CLI `--save` args in container command | Always available | Pass save policy via container args, no config file mount needed |

**Deprecated/outdated:**
- `minio/minio:latest` tag: Resolves to Oct 2025 release. Will not receive updates. Pin to specific tag.
- `mc ilm add` (old syntax): Replaced by `mc ilm rule add`. Use the `rule` subcommand.

## Open Questions

1. **MinIO image long-term viability**
   - What we know: Docker Hub images frozen at Oct 2025. GitHub repo archived Feb 2026. Existing images still pullable.
   - What's unclear: How long Docker Hub will keep serving the frozen images. No timeline for removal.
   - Recommendation: Pin to specific release tag now. Document `cgr.dev/chainguard/minio` as fallback in comments. For a personal project, the frozen image is acceptable.

2. **Redis save argument format across versions**
   - What we know: Redis 7 accepts space-separated pairs in a single `--save` argument.
   - What's unclear: Whether the multi-value single-arg format works identically in redis:7-alpine.
   - Recommendation: After deployment, verify with `redis-cli CONFIG GET save`. If it shows empty, switch to the `redis.conf` file mount approach.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | kustomize build (YAML validation) + kubectl dry-run |
| Config file | infra/k8s/base/kustomization.yaml |
| Quick run command | `kustomize build infra/k8s/overlays/prod` |
| Full suite command | `kustomize build infra/k8s/overlays/prod && kustomize build infra/k8s/overlays/staging` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DATA-01 | PostgreSQL StatefulSet uses local-path-retain StorageClass with reclaimPolicy Retain | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A5 "local-path-retain"` | N/A (manifest check) |
| DATA-02 | Redis StatefulSet exists with PVC and save args | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A20 "kind: StatefulSet" \| grep -A20 "name: redis"` | N/A (manifest check) |
| DATA-03 | MinIO StatefulSet uses local-path-retain + bucket init Job exists | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep "bucket-init"` | N/A (manifest check) |
| DATA-04 | pg_dump CronJob exists with schedule 0 2 * * * | manifest validation | `kustomize build infra/k8s/overlays/prod \| grep -A5 "CronJob" \| grep "schedule"` | N/A (manifest check) |

### Sampling Rate
- **Per task commit:** `kustomize build infra/k8s/overlays/prod > /dev/null && kustomize build infra/k8s/overlays/staging > /dev/null`
- **Per wave merge:** Both overlays build cleanly
- **Phase gate:** Both overlays render valid YAML; all new resources present in output

### Wave 0 Gaps
None -- Kustomize is already available, base manifests exist from Phase 15, no test framework installation needed. Validation is YAML rendering, not runtime testing (cluster validation happens at deployment time in Phase 17).

## Sources

### Primary (HIGH confidence)
- K8s StorageClass API docs - reclaimPolicy field, volumeBindingMode options
- K8s StatefulSet API docs - persistentVolumeClaimRetentionPolicy field
- K8s CronJob API docs - schedule syntax, concurrencyPolicy, jobTemplate
- K3s local-path-provisioner docs - provisioner name `rancher.io/local-path`
- [MinIO mc ilm rule add official docs](https://docs.min.io/enterprise/aistor-object-store/reference/cli/mc-ilm-rule/mc-ilm-rule-add/) - `--expire-days` flag (NOT `--expiry-days`)
- Existing Phase 15 manifests (postgres/statefulset.yaml, minio/statefulset.yaml, redis/deployment.yaml) - verified current state

### Secondary (MEDIUM confidence)
- [Kubernetes CronJob Backup for PostgreSQL](https://devtron.ai/blog/creating-a-kubernetes-cron-job-to-backup-postgres-db/) - CronJob pattern for pg_dump
- [MinIO lifecycle README](https://github.com/minio/minio/blob/master/docs/bucket/lifecycle/README.md) - ILM configuration examples
- [MinIO Docker Image Changes](https://www.minimus.io/post/minio-docker-image-changes-how-to-find-a-secure-minio-alternative) - Docker Hub image status

### Tertiary (LOW confidence)
- [MinIO Docker deprecation discussion](https://github.com/minio/minio/issues/21647) - Timeline of image removal, repo archival
- [Chainguard MinIO images](https://www.chainguard.dev/unchained/secure-and-free-minio-chainguard-containers) - Alternative image source

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All K8s API resources (StorageClass, StatefulSet, CronJob, Job) are stable v1 APIs with extensive documentation
- Architecture: HIGH - Pattern matches existing Phase 15 conventions exactly; additive changes only
- Pitfalls: HIGH - MinIO image situation verified with multiple sources; mc CLI flag name verified with official docs
- Redis persistence: MEDIUM - Save argument format needs runtime verification on redis:7-alpine

**Research date:** 2026-03-24
**Valid until:** 2026-04-24 (stable K8s APIs; MinIO image situation could change)
