# Phase 15: K3s Cluster Setup - Research

**Researched:** 2026-03-23
**Domain:** K3s installation, Kubernetes namespace separation, Kustomize manifests, Spring Boot production profile
**Confidence:** HIGH

## Summary

Phase 15 installs K3s on the EC2 instance provisioned in Phase 14, creates namespace-based staging/production separation with LimitRange safety nets, writes the complete Kustomize base + overlay manifest structure for all application components, creates the backend production profile (`application-prod.yml`), and provides convenience scripts for daily cluster operations.

The project has an extremely detailed skill file (`k3s-cluster-management.md`) that documents every pattern needed for this phase. All K3s defaults are kept (Traefik, ServiceLB, CoreDNS, metrics-server, local-path-provisioner). kubectl access is via SSH tunnel only -- port 6443 is NOT opened in the EC2 security group. Staging protection relies on `replicas: 0` default (no ResourceQuota).

**Primary recommendation:** Follow the patterns in the project's k3s-cluster-management skill file verbatim. The skill file contains tested YAML for every K8s resource type, Kustomize pattern, and operational script needed. No external library research is required -- this phase is pure infrastructure scripting and manifest authoring.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- SSH bootstrap script (`infra/scripts/bootstrap-k3s.sh`) run manually after Phase 14 provisions the instance
- Keep all K3s defaults: Traefik ingress controller, ServiceLB (Klipper), metrics-server, local-path-provisioner, CoreDNS
- Do NOT open port 6443 in EC2 security group -- kubectl access via SSH tunnel only
- No security group changes needed in this phase (no OpenTofu modifications)
- Swap file already handled by Phase 14 user-data script -- K3s script does not manage swap
- Separate `infra/scripts/setup-kubeconfig.sh` script for one-time kubeconfig fetch
- Scripts read SSH private key path and Elastic IP dynamically from `tofu -chdir=infra/tofu/main output`
- Two application namespaces: `jobhunt-staging` and `jobhunt-prod`
- No ResourceQuota on either namespace -- staging protection relies on replicas: 0 default
- LimitRange applied to both namespaces as a safety net
- Full stack in base manifests: backend, frontend, PostgreSQL, Redis, MinIO
- Directory structure: `infra/k8s/base/{backend,frontend,postgres,redis,minio}/` + `infra/k8s/overlays/{staging,prod}/`
- Standard Kubernetes Ingress resources, NOT Traefik IngressRoute CRDs
- Validate manifests with `kustomize build` for each overlay
- Create `backend/src/main/resources/application-prod.yml` with env var placeholders
- Convenience scripts: `staging-up.sh`, `staging-down.sh`, `connect.sh`, `setup-kubeconfig.sh`

### Claude's Discretion
- Exact K3s install command flags beyond the defaults
- LimitRange default values (request/limit sizing)
- Exact kustomization.yaml structure and patch format
- application-prod.yml structure beyond the env var placeholders
- Script error handling and output formatting

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| K8S-02 | K3s cluster installed and running on EC2 instance | bootstrap-k3s.sh script installs K3s with default config; setup-kubeconfig.sh retrieves kubeconfig; connect.sh provides SSH tunnel for kubectl access |
| K8S-03 | Staging and production namespaces configured with LimitRange (no ResourceQuota -- staging protection via replicas=0) | Namespace YAML with labels, LimitRange YAML with container defaults; Kustomize replicas field for scale-to-zero |
| K8S-04 | Kustomize base + overlays for staging and production environments | Full base manifests (5 components), staging overlay (replicas: 0, configmap, secrets, ingress), prod overlay (replicas: 1, configmap, secrets, ingress) |

</phase_requirements>

## Standard Stack

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| K3s | v1.35.x (latest stable) | Lightweight Kubernetes distribution | Single binary, includes Traefik/CoreDNS/local-path, 512MB RAM footprint |
| Kustomize | Built into kubectl | Manifest templating via base + overlay | No extra tooling, ArgoCD native support, simpler than Helm for this scale |
| kubectl | Matches K3s version | Cluster management CLI | Standard K8s CLI, included with K3s |

### Supporting
| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| OpenTofu | Already installed (Phase 14) | Read outputs for scripts | Scripts use `tofu -chdir=infra/tofu/main output -raw` for elastic_ip and ssh_private_key_path |
| SSH | System default | Tunnel for kubectl, SCP for kubeconfig | All cluster access goes through SSH tunnel on port 6443 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Kustomize | Helm | Helm is overkill -- we are not distributing charts. Kustomize is simpler for 5 components. |
| SSH tunnel for kubectl | Open port 6443 | SSH tunnel is more secure -- no K8s API exposed to internet |
| LimitRange only | LimitRange + ResourceQuota | ResourceQuota adds complexity without value -- replicas=0 is sufficient staging protection |

## Architecture Patterns

### Recommended Project Structure
```
infra/
  scripts/
    bootstrap-k3s.sh          # K3s installation (run once via SSH)
    setup-kubeconfig.sh        # One-time kubeconfig fetch + merge
    connect.sh                 # Daily-use SSH tunnel for kubectl
    staging-up.sh              # Scale staging to replicas=1
    staging-down.sh            # Scale staging to replicas=0
  k8s/
    base/
      backend/
        deployment.yaml
        service.yaml
      frontend/
        deployment.yaml
        service.yaml
      postgres/
        statefulset.yaml
        service.yaml
      redis/
        deployment.yaml
        service.yaml
      minio/
        statefulset.yaml
        service.yaml
      kustomization.yaml
    overlays/
      staging/
        kustomization.yaml     # namespace, replicas: 0, images
        configmap.yaml          # Non-sensitive env vars
        secrets.yaml            # Placeholder secrets (Phase 17: SealedSecrets)
        ingress.yaml            # staging.job-hunt.dev
      prod/
        kustomization.yaml     # namespace, replicas: 1, images
        configmap.yaml
        secrets.yaml
        ingress.yaml            # job-hunt.dev
    namespaces/
      namespaces.yaml          # jobhunt-staging + jobhunt-prod
      limitrange-staging.yaml
      limitrange-prod.yaml
```

### Pattern 1: K3s Bootstrap Script
**What:** Shell script that SSHes into EC2 and installs K3s with default configuration
**When to use:** One-time setup after Phase 14 provisions the EC2 instance

Key points:
- Use `curl -sfL https://get.k3s.io | sh -` for default install
- Set `write-kubeconfig-mode: "0644"` so non-root can read kubeconfig
- Wait for node Ready state before exiting
- The script runs commands remotely via SSH, reading elastic_ip and ssh_private_key_path from tofu outputs

### Pattern 2: Kustomize Base + Overlay with Scale-to-Zero
**What:** Shared base manifests with per-environment patches; staging defaults to replicas: 0
**When to use:** Always -- foundation of the K8s manifest strategy

The `replicas` shorthand in Kustomize is the cleanest approach for scale-to-zero:
```yaml
# overlays/staging/kustomization.yaml
replicas:
  - name: backend
    count: 0
  - name: frontend
    count: 0
  - name: redis
    count: 0
  - name: postgres
    count: 0
  - name: minio
    count: 0
```

This works for both Deployments and StatefulSets (verified -- Kustomize v5+ supports this).

### Pattern 3: Namespace Isolation with LimitRange
**What:** Separate namespaces for staging/prod with default resource limits as safety net
**When to use:** Always -- prevents any single pod from consuming all node memory

LimitRange values sized for 2GB node:
```yaml
spec:
  limits:
    - type: Container
      default:           # Applied if pod doesn't specify limits
        memory: 256Mi
        cpu: 250m
      defaultRequest:    # Applied if pod doesn't specify requests
        memory: 128Mi
        cpu: 100m
      max:               # Hard ceiling per container
        memory: 512Mi
        cpu: "1"
```

### Pattern 4: OpenTofu Output Consumption in Scripts
**What:** All scripts dynamically read infrastructure values from tofu outputs
**When to use:** Every script that needs elastic_ip or ssh_private_key_path

```bash
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
```

**Critical:** The `ssh_private_key_path` output is marked `sensitive = true` in outputs.tf. The `-raw` flag still prints sensitive values -- this is by design for script consumption.

### Pattern 5: Backend Production Profile
**What:** Spring Boot profile with env var placeholders matching K8s ConfigMap/Secret keys
**When to use:** Activated via `SPRING_PROFILES_ACTIVE=prod` in K8s ConfigMap

Key differences from dev application.yml:
- No `spring.docker.compose` section (not available in production)
- All connection properties via `${ENV_VAR}` placeholders
- `management.endpoint.health.show-details: when-authorized` (not `always`)
- Flyway `connect-retries` for startup ordering resilience

### Anti-Patterns to Avoid
- **Traefik IngressRoute CRDs:** Use standard `networking.k8s.io/v1` Ingress. Portable across ingress controllers.
- **ResourceQuota alongside replicas=0:** Adds complexity without benefit for single-developer project.
- **Hardcoding IPs in scripts:** Always read from `tofu output`. IPs change on instance replacement.
- **Opening port 6443:** SSH tunnel is more secure. Do not modify security groups.
- **Using `:latest` image tag in overlays:** Pin to SHA tags (`sha-abc1234`). ArgoCD cannot detect drift with mutable tags.
- **Committing real secrets in secrets.yaml:** Use placeholder values (`changeme`). Phase 17 converts to SealedSecrets.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Storage provisioning | Custom PV provisioning | K3s local-path-provisioner (built-in) | Automatically provisions PVCs on node filesystem |
| Ingress routing | Custom nginx/haproxy config | Traefik (K3s built-in) + standard Ingress resources | Battle-tested, handles routing/load-balancing |
| DNS resolution | Custom service discovery | CoreDNS (K3s built-in) | Standard K8s DNS: `service.namespace.svc.cluster.local` |
| Manifest templating | Custom shell scripts generating YAML | Kustomize base + overlays | Handles merging, patching, namespace scoping correctly |
| Image tag management | sed/awk scripts | Kustomize `images` transformer | Type-safe, updates all matching containers across resources |

**Key insight:** K3s bundles everything needed for this phase. There are zero external dependencies to install beyond K3s itself.

## Common Pitfalls

### Pitfall 1: Kubeconfig Permissions (root-only by default)
**What goes wrong:** K3s writes kubeconfig to `/etc/rancher/k3s/k3s.yaml` with `0600` permissions (root only). SCP as ubuntu user fails.
**Why it happens:** Security default -- kubeconfig contains cluster admin credentials.
**How to avoid:** Install with `--write-kubeconfig-mode=0644` or use `sudo cat` in the SCP step.
**Warning signs:** "Permission denied" when SCPing kubeconfig, or kubectl commands fail with auth errors.

### Pitfall 2: Kustomize Namespace Applies to ALL Resources
**What goes wrong:** Setting `namespace: jobhunt-prod` in overlay kustomization.yaml also applies to cluster-scoped resources (Namespace, ClusterRole, etc.).
**Why it happens:** The namespace transformer is a blanket operation.
**How to avoid:** Keep namespace and LimitRange resources OUTSIDE the Kustomize overlays. Apply them separately or put them in a dedicated `namespaces/` directory with its own kustomization.yaml.

### Pitfall 3: StatefulSet Headless Service Required
**What goes wrong:** PostgreSQL/MinIO StatefulSet pods fail to get stable DNS names.
**Why it happens:** StatefulSets require a headless Service (`clusterIP: None`) matching the `serviceName` field.
**How to avoid:** Always create a headless Service for each StatefulSet with `clusterIP: None` and matching selector.

### Pitfall 4: Kubeconfig Server URL for SSH Tunnel
**What goes wrong:** Kubeconfig has `server: https://127.0.0.1:6443` which only works WITH the SSH tunnel active.
**Why it happens:** K3s defaults server URL to localhost. This is correct for our SSH tunnel setup.
**How to avoid:** Document clearly that `connect.sh` must be running before any kubectl commands. The setup-kubeconfig.sh script should verify the server URL is `https://127.0.0.1:6443`.

### Pitfall 5: Spring Boot Cold Start + Readiness Probe
**What goes wrong:** Traffic is routed to backend pod before JVM is ready, causing 503 errors.
**Why it happens:** Default readiness probe starts checking immediately.
**How to avoid:** Set `readinessProbe.initialDelaySeconds: 15` and `livenessProbe.initialDelaySeconds: 30`.

### Pitfall 6: PostgreSQL Data Directory Ownership
**What goes wrong:** PostgreSQL fails to start with "data directory has wrong ownership."
**Why it happens:** PVC mounted with root ownership, but postgres process runs as UID 999.
**How to avoid:** Set `securityContext.fsGroup: 999` on the StatefulSet pod spec.

### Pitfall 7: MinIO Health Check Difference from Docker Compose
**What goes wrong:** Using `mc ready local` as health check in K8s (works in Docker Compose but not in K8s).
**Why it happens:** K8s pods don't have `mc` client installed by default.
**How to avoid:** Use `httpGet` liveness/readiness probes against MinIO's built-in health endpoint: `/minio/health/live` on port 9000.

## Code Examples

Verified patterns from project skill files and official documentation:

### Backend Deployment (base)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  labels:
    app: backend
    app.kubernetes.io/part-of: jobhunt
spec:
  replicas: 1
  selector:
    matchLabels:
      app: backend
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  template:
    metadata:
      labels:
        app: backend
    spec:
      automountServiceAccountToken: false
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
        - name: backend
          image: jobhunt-backend
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: 384Mi
              cpu: 250m
            limits:
              memory: 512Mi
              cpu: "1"
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          envFrom:
            - configMapRef:
                name: backend-config
            - secretRef:
                name: backend-secrets
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 5
            failureThreshold: 3
```

### PostgreSQL StatefulSet (base)
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      securityContext:
        runAsUser: 999
        fsGroup: 999
      containers:
        - name: postgres
          image: postgres:17
          ports:
            - containerPort: 5432
          resources:
            requests:
              memory: 256Mi
              cpu: 100m
            limits:
              memory: 256Mi
              cpu: 500m
          envFrom:
            - secretRef:
                name: postgres-secrets
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 15
            periodSeconds: 10
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 5
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: local-path
        resources:
          requests:
            storage: 10Gi
```

### Production Overlay kustomization.yaml
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: jobhunt-prod

resources:
  - ../../base
  - configmap.yaml
  - secrets.yaml
  - ingress.yaml

images:
  - name: jobhunt-backend
    newName: ghcr.io/baalexandru/jobhunt-backend
    newTag: latest
  - name: jobhunt-frontend
    newName: ghcr.io/baalexandru/jobhunt-frontend
    newTag: latest

replicas:
  - name: backend
    count: 1
  - name: frontend
    count: 1
  - name: redis
    count: 1
  - name: postgres
    count: 1
  - name: minio
    count: 1
```

### application-prod.yml
```yaml
spring:
  docker:
    compose:
      enabled: false
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:jobhunt}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
  flyway:
    connect-retries: 10
    connect-retries-interval: 2

storage:
  endpoint: http://${MINIO_HOST}:${MINIO_PORT:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: ${MINIO_BUCKET:jobhunt-documents}
  region: ${MINIO_REGION:us-east-1}

jwt:
  secret: ${JWT_SECRET}
  access-expiration-ms: ${JWT_ACCESS_EXPIRATION_MS:900000}
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}

spring.mail:
  host: ${SMTP_HOST:smtp.gmail.com}
  port: ${SMTP_PORT:587}
  username: ${SMTP_USERNAME}
  password: ${SMTP_PASSWORD}

app:
  frontend-base-url: ${FRONTEND_BASE_URL:https://job-hunt.dev}
  mail-from: ${MAIL_FROM:noreply@job-hunt.dev}
  internal-api-secret: ${INTERNAL_API_SECRET}

management:
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
  endpoints:
    web:
      exposure:
        include: health, info, flyway
```

### SSH Tunnel Script (connect.sh)
```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ELASTIC_IP=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw elastic_ip)
SSH_KEY=$(tofu -chdir="$PROJECT_ROOT/infra/tofu/main" output -raw ssh_private_key_path)

echo "Opening SSH tunnel to $ELASTIC_IP for kubectl (port 6443)..."
echo "Press Ctrl+C to close the tunnel."
ssh -L 6443:localhost:6443 -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new ubuntu@"$ELASTIC_IP" -N
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| kubeadm for small clusters | K3s single binary | K3s GA 2020+ | 512MB RAM vs 2GB+ for kubeadm |
| NGINX Ingress Controller | Traefik bundled with K3s | K3s default | No extra install needed, NGINX being retired March 2026 |
| Helm charts for everything | Kustomize for internal apps | Kustomize GA in kubectl v1.14+ | Simpler, no template logic needed for small stacks |
| `vars` in Kustomize | `replacements` in Kustomize | Kustomize v5.0+ | `vars` deprecated, `replacements` is the replacement |
| Separate JSON patch files for replicas | `replicas` shorthand field | Kustomize v5.0+ | Cleaner, works for Deployments and StatefulSets |

**Deprecated/outdated:**
- Kustomize `vars` field: replaced by `replacements` (more explicit, less magical)
- NGINX Ingress Controller: being retired March 2026; Traefik is the K3s default
- `commonLabels` in Kustomize: replaced by `labels` transformer with `includeSelectors` control

## Open Questions

1. **K3s image garbage collection thresholds**
   - What we know: Default thresholds exist. 30GB EBS fills with container images over time.
   - What's unclear: Exact default GC thresholds in K3s v1.35.x
   - Recommendation: Use defaults initially. Monitor disk usage. Add `--image-gc-high-threshold=85 --image-gc-low-threshold=80` to K3s config if disk fills.

2. **MinIO image version pinning**
   - What we know: compose.yaml uses `minio/minio:latest`. K8s manifests should pin a version.
   - What's unclear: Best MinIO version for resource-constrained environments.
   - Recommendation: Use `minio/minio:RELEASE.2025-01-01T00-00-00Z` style tags or latest stable. Phase 16 can tune this.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Shell script validation + kustomize build |
| Config file | None (shell-based validation) |
| Quick run command | `kustomize build infra/k8s/overlays/staging && kustomize build infra/k8s/overlays/prod` |
| Full suite command | `kustomize build` for both overlays + `kubectl get nodes` (requires cluster access) |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| K8S-02 | K3s installed and node Ready | manual (requires SSH to EC2) | `kubectl get nodes` via SSH tunnel | N/A - cluster test |
| K8S-03 | Namespaces + LimitRange exist | manual (requires cluster) | `kubectl get ns jobhunt-staging jobhunt-prod && kubectl get limitrange -n jobhunt-staging` | N/A - cluster test |
| K8S-04 | Kustomize overlays generate valid manifests | smoke | `kustomize build infra/k8s/overlays/staging > /dev/null && kustomize build infra/k8s/overlays/prod > /dev/null` | Wave 0 |

### Sampling Rate
- **Per task commit:** `kustomize build infra/k8s/overlays/staging > /dev/null && kustomize build infra/k8s/overlays/prod > /dev/null`
- **Per wave merge:** Full `kustomize build` output inspection for both overlays
- **Phase gate:** `kustomize build` succeeds for both overlays; all scripts are executable; `application-prod.yml` has all required env var placeholders

### Wave 0 Gaps
- [ ] `kustomize` CLI must be available locally (bundled with kubectl or installed separately)
- [ ] No automated test for K8S-02 (K3s installation) or K8S-03 (namespace creation) -- these require cluster access and are validated manually after running bootstrap scripts

## Sources

### Primary (HIGH confidence)
- Project skill file: `.claude/skills/rules/k3s-cluster-management.md` -- comprehensive project-specific patterns for K3s, Kustomize, namespaces, LimitRange, deployments, StatefulSets, services, ingress, scripts
- Project skill file: `.claude/skills/rules/opentofu-infrastructure.md` -- script patterns for consuming tofu outputs
- Project research: `.planning/research/ARCHITECTURE.md` -- full architecture overview, directory layout
- Project research: `.planning/research/PITFALLS.md` -- K3s memory constraints, JVM OOM, data persistence, security
- Existing code: `backend/src/main/resources/application.yml` -- dev config as reference for production profile
- Existing code: `compose.yaml` -- service ports and env vars reference
- Existing code: `infra/tofu/main/outputs.tf` -- elastic_ip, ssh_private_key_path outputs

### Secondary (MEDIUM confidence)
- [K3s Releases](https://github.com/k3s-io/k3s/releases) -- K3s v1.35.x latest stable
- [Kustomize replicas field](https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/) -- replicas shorthand supports Deployments and StatefulSets
- [Kustomize replicas scaling guide](https://oneuptime.com/blog/post/2026-02-09-kustomize-replicas-scaling/view) -- confirms replicas field maturity

### Tertiary (LOW confidence)
None -- all findings verified against project skill files or official docs.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- K3s defaults are locked decisions; Kustomize is built into kubectl
- Architecture: HIGH -- directory structure specified in CONTEXT.md; patterns documented in skill file
- Pitfalls: HIGH -- documented in both project PITFALLS.md research and skill file
- Scripts: HIGH -- patterns established in opentofu-infrastructure skill file
- application-prod.yml: HIGH -- existing application.yml provides complete reference for all properties

**Research date:** 2026-03-23
**Valid until:** 2026-04-23 (stable -- K3s and Kustomize are mature technologies)
