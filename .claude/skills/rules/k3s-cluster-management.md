# K3s Cluster Management & Kustomize Manifests

## Overview

K3s is a lightweight, certified Kubernetes distribution. This project runs a single-node K3s cluster on an AWS EC2 t3.small (2GB RAM) with namespace-based staging/production separation. This skill covers K3s installation, configuration, Kustomize manifest authoring, and operational scripts.

## K3s Installation

### Standard Install (single-node server)

```bash
curl -sfL https://get.k3s.io | sh -
```

### Install with Configuration

Pass flags via environment variables or CLI arguments:

```bash
# Via env vars
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" sh -s - --write-kubeconfig-mode=644

# Via config file (preferred for persistence)
# Create /etc/rancher/k3s/config.yaml BEFORE installing
curl -sfL https://get.k3s.io | sh -
```

### Config File Format (`/etc/rancher/k3s/config.yaml`)

YAML keys map directly to CLI flags (without `--` prefix):

```yaml
write-kubeconfig-mode: "0644"
tls-san:
  - "foo.local"
  - "10.0.0.1"
node-label:
  - "environment=production"
```

Equivalent CLI: `k3s server --write-kubeconfig-mode=0644 --tls-san=foo.local --tls-san=10.0.0.1 --node-label=environment=production`

### Kubeconfig Location

- Server writes kubeconfig to: `/etc/rancher/k3s/k3s.yaml`
- Default permissions: `0600` (root only). Set `write-kubeconfig-mode: "0644"` for non-root access.
- Server URL in kubeconfig defaults to `https://127.0.0.1:6443`

## K3s Bundled Components

K3s ships these by default — do NOT install them separately:

| Component | Purpose | Disable Flag |
|-----------|---------|-------------|
| Traefik | Ingress controller | `--disable=traefik` |
| ServiceLB (Klipper) | LoadBalancer for bare metal | `--disable=servicelb` |
| CoreDNS | Cluster DNS | `--disable=coredns` |
| metrics-server | `kubectl top` support | `--disable=metrics-server` |
| local-path-provisioner | Default StorageClass for PVCs | `--disable=local-storage` |
| Flannel | CNI networking | `--flannel-backend=none` |

### Disabling Components

```bash
# Disable at install time
curl -sfL https://get.k3s.io | sh -s - --disable=traefik

# Or via config.yaml
disable:
  - traefik
```

### This Project Keeps ALL Defaults

- Traefik: YES (ingress controller, consumes standard K8s Ingress resources)
- ServiceLB: YES (binds Traefik to host ports 80/443)
- metrics-server: YES (kubectl top for monitoring on constrained node)
- local-path-provisioner: YES (default StorageClass for PostgreSQL/MinIO PVCs)
- CoreDNS: YES (cluster DNS resolution)

## Traefik Ingress (K3s Built-in)

### Standard K8s Ingress (preferred over IngressRoute CRD)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
spec:
  rules:
    - host: example.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend
                port:
                  number: 3000
```

### Customizing Traefik via HelmChartConfig

K3s manages Traefik via a HelmChart resource. Override values with HelmChartConfig:

```yaml
apiVersion: helm.cattle.io/v1
kind: HelmChartConfig
metadata:
  name: traefik
  namespace: kube-system
spec:
  valuesContent: |-
    ports:
      web:
        redirectTo:
          entry_point: websecure
    additionalArguments:
      - "--entryPoints.websecure.proxyProtocol.insecure"
```

## Local Path Provisioner (Storage)

K3s default StorageClass. PVCs automatically provision local storage on the node.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: local-path  # K3s default StorageClass
  resources:
    requests:
      storage: 10Gi
```

**Important for this project:**
- Data lives on the EC2 node's 30GB gp3 EBS volume
- Single node = no redundancy. Backups to S3 are mandatory (Phase 16)
- Set `persistentVolumeReclaimPolicy: Retain` on PVs holding database data

## Kustomize Patterns

### Directory Structure (this project)

```
infra/k8s/
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
      kustomization.yaml
      configmap.yaml
      secrets.yaml
      ingress.yaml
    prod/
      kustomization.yaml
      configmap.yaml
      secrets.yaml
      ingress.yaml
```

### Base kustomization.yaml

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - backend/deployment.yaml
  - backend/service.yaml
  - frontend/deployment.yaml
  - frontend/service.yaml
  - postgres/statefulset.yaml
  - postgres/service.yaml
  - redis/deployment.yaml
  - redis/service.yaml
  - minio/statefulset.yaml
  - minio/service.yaml
```

### Overlay kustomization.yaml (production example)

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
    newTag: sha-abc1234
  - name: jobhunt-frontend
    newName: ghcr.io/baalexandru/jobhunt-frontend
    newTag: sha-abc1234

patches:
  - patch: |-
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: backend
      spec:
        replicas: 1
    target:
      kind: Deployment
```

### Staging Overlay — Scale-to-Zero

Preferred approach using the `replicas` shorthand (cleaner than JSON patches):

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

Alternative using JSON patches (targets all resources of a kind):

```yaml
patches:
  - patch: |-
      - op: replace
        path: /spec/replicas
        value: 0
    target:
      kind: Deployment
  - patch: |-
      - op: replace
        path: /spec/replicas
        value: 0
    target:
      kind: StatefulSet
```

### Strategic Merge Patches (file-based)

Partial YAML that merges with existing resources using K8s strategic merge semantics. Identifies target by apiVersion + kind + name:

```yaml
# overlays/prod/resource-limits.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      containers:
        - name: backend
          resources:
            requests:
              memory: 384Mi
              cpu: 250m
            limits:
              memory: 512Mi
              cpu: "1"
```

Referenced in kustomization.yaml:
```yaml
patches:
  - path: resource-limits.yaml
```

### JSON Patches (RFC 6902) — Inline

Precise modifications using `op: add|remove|replace|move|copy|test`:

```yaml
patches:
  - target:
      kind: Deployment
      name: backend
    patch: |-
      - op: replace
        path: /spec/replicas
        value: 3
      - op: add
        path: /spec/template/spec/containers/0/env/-
        value:
          name: SPRING_PROFILES_ACTIVE
          value: prod
```

**When to use which:**
- **Strategic merge patch** — when you need to merge/override nested fields (e.g., adding env vars, changing resource limits). Simpler, more readable.
- **JSON patch** — when you need precise positional operations (replace array element by index, remove a specific item, add to end of array with `/-`).

### Image Transformer

Modifies container image references across ALL resources without editing individual files:

```yaml
images:
  - name: jobhunt-backend           # matches container image name in base
    newName: ghcr.io/baalexandru/jobhunt-backend
    newTag: sha-abc1234              # tag (mutable)
  - name: jobhunt-frontend
    newName: ghcr.io/baalexandru/jobhunt-frontend
    digest: sha256:abc123def456...   # digest (immutable, preferred for prod)
```

**Rules:**
- `newTag` and `digest` are mutually exclusive — use one or the other
- Digest pins are immutable and preferred for production reproducibility
- SHA tags (`sha-abc1234`) are sufficient for this project since images are public on GHCR

### Namespace Transformer

Sets namespace for ALL resources in the overlay:

```yaml
namespace: jobhunt-prod
```

**Caveat:** This applies to every resource, including those that should be cluster-scoped (ClusterRole, ClusterRoleBinding, Namespace itself). If your overlay includes cluster-scoped resources, either:
1. Don't include them in the overlay (apply separately)
2. Use a patch to remove the namespace field

### Label Transformer

Adds labels with control over selector propagation:

```yaml
labels:
  - pairs:
      app.kubernetes.io/part-of: jobhunt
      environment: production
    includeSelectors: false    # metadata.labels only (safe default)

  - pairs:
      app: backend
    includeSelectors: true     # ALSO adds to spec.selector.matchLabels + spec.template.metadata.labels
    includeTemplates: true
```

**Warning:** `includeSelectors: true` modifies immutable selectors on Deployments/StatefulSets. Only use on labels that are set at creation time and never change. Common labels like `environment` should use `includeSelectors: false`.

### ConfigMapGenerator

Generates ConfigMaps with automatic content-hash suffix for immutable deployments:

```yaml
configMapGenerator:
  - name: backend-config
    literals:
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=jobhunt
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - MINIO_HOST=minio
      - MINIO_PORT=9000
    options:
      labels:
        app.kubernetes.io/part-of: jobhunt
```

Output: `backend-config-f7h9k2m8t5` — hash suffix ensures pods restart when config changes.

To disable hash suffix (not recommended but sometimes needed):
```yaml
generatorOptions:
  disableNameSuffixHash: true
```

### SecretGenerator

Generates Secrets with automatic hash suffix. **Phase 17 converts these to SealedSecrets.**

```yaml
secretGenerator:
  - name: backend-secrets
    literals:
      - DB_PASSWORD=changeme
      - JWT_SECRET=changeme
      - MINIO_ACCESS_KEY=changeme
      - MINIO_SECRET_KEY=changeme
    type: Opaque
```

**Note:** Placeholder values here — real secrets are sealed in Phase 17 via `kubeseal`. Never commit real credentials.

### Replacements (modern alternative to deprecated vars)

Copy values between resources dynamically:

```yaml
replacements:
  - source:
      kind: Service
      name: postgres
      fieldPath: metadata.name
    targets:
      - select:
          kind: ConfigMap
          name: backend-config
        fieldPaths:
          - data.DB_HOST
```

Use sparingly — explicit ConfigMap literals are more readable for this project's scale.

### Replicas Field (shorthand)

Direct replica override without patches:

```yaml
replicas:
  - name: backend
    count: 1
  - name: frontend
    count: 1
```

**Staging overlay uses this for scale-to-zero:**
```yaml
replicas:
  - name: backend
    count: 0
  - name: frontend
    count: 0
  - name: redis
    count: 0
```

Note: `replicas` field works for Deployments and StatefulSets by name.

### Validate Manifests (no cluster needed)

```bash
# Render and validate
kustomize build infra/k8s/overlays/prod
kustomize build infra/k8s/overlays/staging

# Or with kubectl
kubectl kustomize infra/k8s/overlays/prod

# Dry-run against cluster (requires access)
kubectl apply -k infra/k8s/overlays/prod --dry-run=server
```

## Namespace & LimitRange

### Namespace Creation

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: jobhunt-prod
  labels:
    environment: production
---
apiVersion: v1
kind: Namespace
metadata:
  name: jobhunt-staging
  labels:
    environment: staging
```

### LimitRange (safety net for constrained 2GB node)

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
spec:
  limits:
    - type: Container
      default:
        memory: 256Mi
        cpu: 250m
      defaultRequest:
        memory: 128Mi
        cpu: 100m
      max:
        memory: 512Mi
        cpu: "1"
```

## Memory Budget (t3.small — 2GB)

| Component | RAM |
|-----------|-----|
| K3s system (kubelet, Traefik, CoreDNS) | ~350MB |
| ArgoCD (core-mode) | ~300MB |
| PostgreSQL | ~256MB |
| Backend (Spring Boot JVM) | ~384MB |
| Frontend (Node.js) | ~128MB |
| Redis | ~64MB |
| MinIO | ~128MB |
| **Total** | **~1,610MB** |
| **Headroom** | **~438MB** |

### JVM Flags (locked — from ROADMAP.md)

```
-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError
```

### K8s Resource Limits for Spring Boot

```yaml
resources:
  requests:
    memory: 384Mi
    cpu: 250m
  limits:
    memory: 512Mi
    cpu: "1"
```

## Health Probes for Spring Boot on K8s

Use separate liveness and readiness probes:

```yaml
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

**Key:** A failing database should make the pod *unready* (no traffic), NOT restart it. Liveness checks if JVM is alive; readiness checks if it can serve traffic.

## Security Best Practices

### K8s API Port (6443)

- K3s binds to `0.0.0.0:6443` by default
- This project: port 6443 NOT open in EC2 security group
- Access via SSH tunnel: `ssh -L 6443:localhost:6443 ubuntu@<elastic-ip>`

### Pod Security

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
automountServiceAccountToken: false  # app pods don't need K8s API
```

### Image Tags

- NEVER use `:latest` in production Kustomize overlays
- Always pin to SHA tags: `sha-abc1234`
- The `latest` tag exists in GHCR but overlays reference specific SHAs

## Operational Scripts

### SSH Tunnel for kubectl

```bash
#!/usr/bin/env bash
# connect.sh — Open SSH tunnel for kubectl access
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
ssh -L 6443:localhost:6443 -i "$SSH_KEY" ubuntu@"$ELASTIC_IP" -N
```

### Staging Scale Up/Down

```bash
# staging-up.sh
kubectl scale -n jobhunt-staging deploy --all --replicas=1
kubectl scale -n jobhunt-staging statefulset --all --replicas=1
kubectl wait -n jobhunt-staging --for=condition=Ready pod --all --timeout=120s
kubectl get pods -n jobhunt-staging

# staging-down.sh
kubectl scale -n jobhunt-staging deploy --all --replicas=0
kubectl scale -n jobhunt-staging statefulset --all --replicas=0
kubectl get pods -n jobhunt-staging
```

### Kubeconfig Setup (one-time)

```bash
# setup-kubeconfig.sh
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
scp -i "$SSH_KEY" ubuntu@"$ELASTIC_IP":/etc/rancher/k3s/k3s.yaml ./k3s.yaml
# Rewrite server URL for SSH tunnel
sed -i 's|https://127.0.0.1:6443|https://127.0.0.1:6443|' ./k3s.yaml
export KUBECONFIG=$(pwd)/k3s.yaml
kubectl get nodes
```

## Common Pitfalls

1. **JVM OOM on K8s** — Always set `-XX:MaxRAMPercentage=75.0` and K8s memory limits together
2. **StatefulSet data loss** — Use `reclaimPolicy: Retain` on PVs, never `Delete`
3. **Traefik + NGINX conflict** — K3s bundles Traefik. Do NOT install NGINX Ingress alongside it
4. **ServiceLB port conflict** — ServiceLB binds host ports 80/443. Don't run other services on these ports
5. **Kubeconfig permissions** — Set `write-kubeconfig-mode: "0644"` or kubeconfig is root-only
6. **Spring Boot cold start** — Set `readinessProbe.initialDelaySeconds: 30` to avoid premature traffic routing
7. **Image pull on constrained disk** — 30GB EBS fills fast with container images. Configure K3s image GC thresholds
8. **Kustomize namespace gotcha** — Setting `namespace:` in overlay kustomization.yaml applies to ALL resources, including those that should be cluster-scoped (like ClusterRole). Use carefully.

## K8s Resource Patterns (Project-Specific)

### Deployment (backend/frontend)

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
      maxUnavailable: 0       # keep old pod until new is ready
      maxSurge: 1             # allow 1 extra pod during rollout
  template:
    metadata:
      labels:
        app: backend
    spec:
      automountServiceAccountToken: false   # app doesn't need K8s API
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
        - name: backend
          image: jobhunt-backend        # Kustomize replaces via images transformer
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: 384Mi
              cpu: 250m
            limits:
              memory: 512Mi
              cpu: "1"
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
          envFrom:
            - configMapRef:
                name: backend-config
            - secretRef:
                name: backend-secrets
```

**Key decisions:**
- `maxUnavailable: 0` — on a single-node cluster, we can't afford downtime during rollouts
- Separate liveness/readiness — failing DB makes pod *unready* (no traffic), NOT restarted
- `automountServiceAccountToken: false` — app pods don't call K8s API
- `envFrom` — injects all ConfigMap/Secret keys as env vars

### StatefulSet (PostgreSQL/MinIO)

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  serviceName: postgres        # required: headless service name
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  updateStrategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        app: postgres
    spec:
      securityContext:
        runAsUser: 999          # postgres user UID
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
        storageClassName: local-path     # K3s default
        resources:
          requests:
            storage: 10Gi
```

**Key decisions:**
- `serviceName` is required — creates stable DNS: `postgres-0.postgres.<namespace>.svc.cluster.local`
- `volumeClaimTemplates` — each pod gets its own PVC, stable across restarts
- `storageClassName: local-path` — K3s built-in provisioner
- `persistentVolumeClaimRetentionPolicy` defaults to `Retain` — data survives pod deletion
- `updateStrategy: RollingUpdate` — default, pods updated one at a time in reverse ordinal order

### Headless Service (for StatefulSets)

Required by StatefulSets for stable network identity:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  clusterIP: None              # headless — no virtual IP, direct pod DNS
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
      name: postgres
```

### ClusterIP Service (for Deployments)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
  labels:
    app: backend
spec:
  type: ClusterIP              # default — internal only, exposed via Ingress
  selector:
    app: backend
  ports:
    - port: 8080
      targetPort: 8080
      name: http
```

### Ingress (Traefik via standard K8s API)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: jobhunt-ingress
spec:
  rules:
    - host: job-hunt.dev                # Phase 18 finalizes
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend
                port:
                  number: 3000
```

**pathType rules:**
- `Prefix` — `/api` matches `/api`, `/api/`, `/api/foo` but NOT `/apifoo`
- `Exact` — `/api` matches only `/api`, nothing else
- Longest matching path wins when multiple rules match

**TLS** (added in Phase 18):
```yaml
spec:
  tls:
    - hosts:
        - job-hunt.dev
      secretName: jobhunt-tls    # Cloudflare Origin CA cert
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backend-config
data:
  DB_HOST: postgres
  DB_PORT: "5432"
  DB_NAME: jobhunt
  REDIS_HOST: redis
  REDIS_PORT: "6379"
  MINIO_HOST: minio
  MINIO_PORT: "9000"
  MINIO_BUCKET: jobhunt-documents
  SPRING_PROFILES_ACTIVE: prod
```

### Secret (placeholder — Phase 17 converts to SealedSecrets)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: backend-secrets
type: Opaque
stringData:                    # stringData for plain text, data for base64
  DB_USERNAME: jobhunt
  DB_PASSWORD: changeme
  JWT_SECRET: changeme
  MINIO_ACCESS_KEY: changeme
  MINIO_SECRET_KEY: changeme
```

**Never commit real values.** Use `stringData` for readability during development; `kubeseal` encrypts the whole Secret in Phase 17.

### ResourceQuota (reference — not used in this project, but documented)

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: namespace-quota
spec:
  hard:
    requests.cpu: "2"
    requests.memory: 1Gi
    limits.cpu: "4"
    limits.memory: 2Gi
    pods: "10"
    persistentvolumeclaims: "5"
```

This project does NOT use ResourceQuota (decision: rely on replicas=0 for staging protection).

## Context7 Library Reference

For up-to-date K3s documentation, use Context7 with library ID `/k3s-io/docs` (trust: 8.1, 1625 snippets) or `/websites/k3s_io` (trust: 9.8, 966 snippets).

For Kustomize patterns, use `/kubernetes-sigs/kustomize` (trust: 8.1, 1397 snippets).

For K8s API specs (Ingress, StatefulSet, Deployment, Service, LimitRange, etc.), use `/websites/kubernetes_io` (trust: 9.9, 15032 snippets).
