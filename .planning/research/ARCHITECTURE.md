# Architecture Patterns: Infrastructure & Deployment

**Domain:** Kubernetes deployment infrastructure for Spring Boot + Next.js monorepo
**Researched:** 2026-03-22
**Overall confidence:** MEDIUM-HIGH

## Recommended Architecture

### High-Level Overview

```
                    Internet
                       |
                 [AWS Route 53]
                       |
              [EC2: K3s Single Node]
                       |
                  [Traefik Ingress]
                   /    |    \
          [Frontend] [Backend] [MinIO Console]
          Next.js    Spring     (admin only)
          :3000      Boot
                     :8080
                       |
              ---------+---------
              |        |        |
         [PostgreSQL] [Redis] [MinIO]
         StatefulSet  Deploy  StatefulSet
         PVC:10Gi    (no PV)  PVC:10Gi
```

### Why Single Node, Not Two Clusters

The PROJECT.md specifies "separate staging and production clusters." After research, this is **not realistic on AWS free tier**. Here is why:

- **K3s server node minimum:** 2 vCPU, 2 GB RAM (official docs)
- **t3.micro:** 2 vCPU, 1 GB RAM -- below K3s minimum, will OOM under load
- **t3.small:** 2 vCPU, 2 GB RAM -- meets K3s minimum but leaves zero headroom for workloads
- **AWS free tier (post-July 2025 accounts):** 750 hours/month across t3.small, t4g.small, c7i-flex.large, m7i-flex.large for 6 months, plus $200 credit
- **AWS free tier (pre-July 2025 accounts):** 750 hours/month of t3.micro only for 12 months

**Recommended approach:** One t3.small (or t4g.small for ARM/Graviton) EC2 instance running K3s with **namespace-based staging/prod separation** instead of two clusters. This is the only viable free-tier path. Two t3.small instances running 24/7 = 1460 hours/month, nearly double the 750-hour free tier limit.

If budget allows ~$15-20/month after free tier expires, upgrade to a single t3.medium (2 vCPU, 4 GB RAM) which gives comfortable headroom for all workloads.

**Confidence:** HIGH -- based on official K3s requirements docs and AWS free tier pricing.

### Component Boundaries

| Component | Responsibility | K8s Resource Type | Namespace |
|-----------|---------------|-------------------|-----------|
| Backend API | Spring Boot REST API, JWT auth, file upload | Deployment + Service | `jobhunt-{env}` |
| Frontend | Next.js standalone SSR server | Deployment + Service | `jobhunt-{env}` |
| PostgreSQL | Primary datastore, Flyway-managed schema | StatefulSet + PVC | `jobhunt-{env}` |
| Redis | Rate limiting, session cache | Deployment (no persistence needed) | `jobhunt-{env}` |
| MinIO | S3-compatible document storage | StatefulSet + PVC | `jobhunt-{env}` |
| Traefik | Ingress controller, TLS termination | DaemonSet (K3s built-in) | `kube-system` |
| cert-manager | Let's Encrypt TLS certificate automation | Deployment | `cert-manager` |
| ArgoCD | GitOps continuous deployment | Various | `argocd` |

### Namespace Strategy

```
kube-system/        -- K3s system (Traefik, CoreDNS, local-path-provisioner)
cert-manager/       -- TLS certificate management
argocd/             -- ArgoCD server, repo-server, application-controller
jobhunt-staging/    -- Full app stack (backend, frontend, postgres, redis, minio)
jobhunt-prod/       -- Full app stack (backend, frontend, postgres, redis, minio)
```

Each environment namespace gets its own database, Redis, and MinIO instance. This provides real isolation without the cost of separate clusters.

## Integration Points: Existing Code to New Infrastructure

### 1. Backend Dockerfile (NEW)

**Location:** `infra/docker/backend.Dockerfile`

Multi-stage build using Spring Boot layered JAR extraction:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY backend/gradle/ gradle/
COPY backend/gradlew backend/build.gradle.kts backend/settings.gradle.kts ./
# Root settings needed for composite build
COPY settings.gradle.kts /root-settings.gradle.kts
RUN ./gradlew dependencies --no-daemon
COPY backend/src/ src/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Extract layers
FROM eclipse-temurin:24-jdk AS extractor
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Runtime
FROM eclipse-temurin:24-jre
RUN addgroup --system appuser && adduser --system --ingroup appuser appuser
WORKDIR /app
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**Critical integration point:** The backend currently relies on `spring-boot-docker-compose` starter for local dev. In production, this dependency is `testAndDevelopmentOnly` so it will NOT be in the production JAR. The backend needs an `application-prod.yml` profile that provides explicit database/redis/minio connection properties via environment variables.

### 2. Backend Production Profile (NEW)

**Location:** `backend/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:jobhunt}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}

storage:
  endpoint: http://${MINIO_HOST}:${MINIO_PORT:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: ${MINIO_BUCKET:jobhunt-documents}

jwt:
  secret: ${JWT_SECRET}

management:
  endpoint:
    health:
      show-details: when-authorized
```

### 3. Frontend Dockerfile (NEW)

**Location:** `infra/docker/frontend.Dockerfile`

Next.js standalone output mode (30-60% smaller images):

```dockerfile
# Stage 1: Dependencies
FROM node:22-alpine AS deps
WORKDIR /app
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile

# Stage 2: Build
FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY frontend/ .
ENV NEXT_TELEMETRY_DISABLED=1
RUN corepack enable && pnpm build

# Stage 3: Runtime
FROM node:22-alpine AS runner
WORKDIR /app
RUN addgroup --system appuser && adduser --system -G appuser appuser
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
USER appuser
EXPOSE 3000
ENV PORT=3000 HOSTNAME="0.0.0.0"
# Use node directly, not npm/pnpm -- proper SIGTERM handling
CMD ["node", "server.js"]
```

**Critical integration point:** The frontend `next.config.ts` must add `output: "standalone"` for Docker builds. This is a code change in the existing frontend.

### 4. Frontend Config Change (MODIFY)

**File:** `frontend/next.config.ts` -- add `output: "standalone"`

```typescript
const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["192.168.178.49"],
};
```

### 5. Container Registry

**Use GitHub Container Registry (GHCR)** -- free for public repos, 500 MB free for private repos, integrated with GitHub Actions.

Image naming convention:
```
ghcr.io/baalexandru/jobhunt-backend:sha-abc1234
ghcr.io/baalexandru/jobhunt-frontend:sha-abc1234
```

Tag strategy: Git SHA tags for immutability + `latest` and `staging`/`prod` mutable tags for convenience.

## Infrastructure-as-Code Directory Structure

### Recommended `/infra` Layout

```
infra/
  docker/
    backend.Dockerfile        # Spring Boot multi-stage build
    frontend.Dockerfile       # Next.js standalone multi-stage build
  k8s/
    base/                     # Shared K8s manifests (Kustomize base)
      backend/
        deployment.yaml
        service.yaml
      frontend/
        deployment.yaml
        service.yaml
      postgres/
        statefulset.yaml
        service.yaml
        pvc.yaml
      redis/
        deployment.yaml
        service.yaml
      minio/
        statefulset.yaml
        service.yaml
        pvc.yaml
      kustomization.yaml
    overlays/
      staging/
        kustomization.yaml    # Patches: image tags, replicas=1, resource limits
        ingress.yaml          # staging.jobhunt.example.com
        secrets.yaml          # SealedSecret references
        configmap.yaml        # ENV overrides
      prod/
        kustomization.yaml    # Patches: image tags, replicas=1, resource limits
        ingress.yaml          # jobhunt.example.com
        secrets.yaml
        configmap.yaml
    platform/
      cert-manager/
        namespace.yaml
        clusterissuer.yaml    # Let's Encrypt ACME issuer
      argocd/
        namespace.yaml
        install.yaml          # ArgoCD manifests (or Helm values)
        apps/
          root-app.yaml       # App-of-Apps root
          staging.yaml        # ArgoCD Application: staging overlay
          prod.yaml           # ArgoCD Application: prod overlay
  scripts/
    bootstrap-k3s.sh          # EC2 K3s installation script
    bootstrap-argocd.sh       # ArgoCD initial setup
    create-secrets.sh         # Generate and apply K8s secrets
```

### Why Kustomize Over Helm

For this project, **use Kustomize** (not Helm charts):

1. **Simpler for a small app** -- 5 microservices is really 2 app services + 3 data stores
2. **No template logic needed** -- environment differences are just image tags, resource limits, and hostnames
3. **K3s ships Kustomize support** -- no extra tooling to install
4. **ArgoCD has first-class Kustomize support** -- auto-detects kustomization.yaml
5. **Helm is overkill** -- you are not distributing charts to third parties

The base/overlay pattern gives clean staging/prod separation with minimal duplication.

**Confidence:** HIGH -- Kustomize is the ArgoCD-recommended approach for this scale of deployment.

## Data Flow: CI/CD Pipeline

### GitHub Actions CI (Build + Push)

```
Developer pushes to main branch
         |
         v
GitHub Actions workflow triggers
         |
    +-----------+-----------+
    |           |           |
  [Test]    [Test]     [Lint]
  backend   frontend
    |           |           |
    +-----------+-----------+
         |
    [Build Docker Images]
    backend + frontend
         |
    [Push to GHCR]
    ghcr.io/baalexandru/jobhunt-{backend,frontend}:sha-XXXXXX
         |
    [Update K8s Manifests]
    Patch image tag in infra/k8s/overlays/{env}/kustomization.yaml
         |
    [Commit manifest change]
    (Auto-commit to same repo, or separate infra branch)
         |
         v
ArgoCD detects Git change (webhook or 3-min poll)
         |
         v
ArgoCD syncs K8s cluster to match Git state
```

### Key Design Decisions

**Single repo, not separate infra repo.** For a single-developer project, keeping K8s manifests in the same monorepo reduces complexity. ArgoCD can point to a subdirectory (`infra/k8s/overlays/prod`) within the monorepo. The ArgoCD best practice of separate repos applies to teams, not solo projects.

**GitHub Actions updates image tags, ArgoCD applies them.** GitHub Actions never talks to K8s directly. It builds images, pushes them, and updates the Git manifests. ArgoCD is the only thing that touches the cluster. This is the standard GitOps separation of concerns.

**Image immutability via SHA tags.** Every build produces `sha-<git-short-hash>` tags. The Kustomize overlay references the specific SHA. No `:latest` in production manifests.

## Patterns to Follow

### Pattern 1: Kustomize Base + Overlay

**What:** Shared base manifests with per-environment patches.
**When:** Always -- this is the foundation of the K8s manifest strategy.

```yaml
# infra/k8s/overlays/staging/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: jobhunt-staging
resources:
  - ../../base
images:
  - name: jobhunt-backend
    newName: ghcr.io/baalexandru/jobhunt-backend
    newTag: sha-abc1234
  - name: jobhunt-frontend
    newName: ghcr.io/baalexandru/jobhunt-frontend
    newTag: sha-abc1234
patches:
  - path: resource-limits.yaml
```

### Pattern 2: ArgoCD App-of-Apps

**What:** A root ArgoCD Application that manages child Applications.
**When:** To bootstrap and manage the full cluster declaratively.

```yaml
# infra/k8s/platform/argocd/apps/root-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: root
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/BaAlexandru/job-hunt.git
    targetRevision: HEAD
    path: infra/k8s/platform/argocd/apps
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

### Pattern 3: Externalized Config via ConfigMaps and Secrets

**What:** All environment-specific configuration injected via K8s ConfigMaps and Secrets.
**When:** Always -- never bake credentials into Docker images.

The backend's `application-prod.yml` reads from environment variables. K8s injects these from ConfigMap (non-sensitive) and Secret (sensitive) resources:

```yaml
# ConfigMap: DB_HOST, DB_PORT, DB_NAME, REDIS_HOST, MINIO_HOST, etc.
# Secret: DB_PASSWORD, JWT_SECRET, MINIO_SECRET_KEY
```

### Pattern 4: K3s Local Path Provisioner for Storage

**What:** Use K3s built-in local-path-provisioner for PersistentVolumeClaims.
**When:** Single-node clusters where data lives on the node's disk.

K3s ships with Rancher's Local Path Provisioner as the default StorageClass. StatefulSets for PostgreSQL and MinIO use PVCs that automatically provision local storage. No need for EBS or external storage on a single node.

**Risk:** Data loss if the EC2 instance terminates. Mitigate with automated pg_dump backups to S3 (see Pitfalls).

### Pattern 5: Traefik Ingress + cert-manager + Let's Encrypt

**What:** K3s built-in Traefik as ingress controller with automated TLS via cert-manager.
**When:** Always for production HTTPS.

```yaml
# infra/k8s/platform/cert-manager/clusterissuer.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@jobhunt.example.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: traefik
```

```yaml
# infra/k8s/overlays/prod/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: jobhunt-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - jobhunt.example.com
      secretName: jobhunt-tls
  rules:
    - host: jobhunt.example.com
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

## Anti-Patterns to Avoid

### Anti-Pattern 1: Running Two K3s Clusters on Free Tier

**What:** Attempting separate EC2 instances for staging and production clusters.
**Why bad:** Exceeds 750 free-tier hours/month. Both clusters would be resource-starved on t3.micro (1 GB RAM). Even t3.small (2 GB) leaves no headroom after K3s overhead.
**Instead:** Single node with namespace isolation. Add a second node only when budget allows.

### Anti-Pattern 2: Helm Charts for Internal Services

**What:** Writing full Helm charts with values.yaml, templates, Chart.yaml for your own app.
**Why bad:** Massive overhead for 2 services. You are not distributing this as a package.
**Instead:** Kustomize base + overlays. Use Helm only to install third-party tools (cert-manager, ArgoCD).

### Anti-Pattern 3: Baking Secrets into Docker Images

**What:** Putting database passwords or JWT secrets in Dockerfiles or application.yml.
**Why bad:** Anyone who pulls the image has your credentials.
**Instead:** K8s Secrets injected as environment variables. Use SealedSecrets or SOPS for GitOps-safe secret management.

### Anti-Pattern 4: Using npm/pnpm as Docker CMD

**What:** `CMD ["pnpm", "start"]` in Next.js Dockerfile.
**Why bad:** Node process manager intercepts SIGTERM, preventing graceful shutdown. K8s sends SIGTERM, pnpm ignores it, K8s waits 30s then SIGKILL.
**Instead:** `CMD ["node", "server.js"]` with standalone output.

### Anti-Pattern 5: Branch-per-Environment in Git

**What:** Using `staging` and `production` branches for deployment.
**Why bad:** Merge conflicts, drift between branches, unclear which commit is deployed.
**Instead:** Single main branch. ArgoCD Applications point to different Kustomize overlays (same branch, different directories).

### Anti-Pattern 6: GitHub Actions Deploying Directly to K8s

**What:** `kubectl apply` in GitHub Actions workflows.
**Why bad:** CI has cluster credentials, bypasses GitOps, no audit trail.
**Instead:** GitHub Actions builds images and updates Git manifests. ArgoCD is the sole deployer.

## Scalability Considerations

| Concern | Single User (now) | 10 Users | 100+ Users |
|---------|-------------------|----------|------------|
| Compute | 1x t3.small, all pods replicas=1 | 1x t3.medium, still replicas=1 | Multiple nodes, HPA |
| PostgreSQL | Single pod, local-path PV | Same, add connection pooling | Managed RDS or HA StatefulSet |
| Redis | Single pod, no persistence | Same | Redis Sentinel or ElastiCache |
| MinIO | Single pod, local-path PV | Same, consider S3 migration | AWS S3 directly |
| Ingress | Traefik, single node | Same | ALB or NLB in front |
| Storage | Local-path PV (node disk) | EBS volumes | EBS + backup strategy |
| Backups | Cron CronJob pg_dump to S3 | Same, more frequent | Point-in-time recovery, WAL archiving |
| Cost | ~$0/month (free tier) | ~$15-20/month (t3.medium) | ~$50-100/month |

## Build Order (Dependencies)

Phases should be ordered based on these dependencies:

```
1. Production Dockerfiles          -- No K8s dependency, testable locally
   (backend.Dockerfile,
    frontend.Dockerfile,
    next.config.ts output:standalone,
    application-prod.yml)
        |
        v
2. EC2 + K3s Bootstrap             -- Needs Dockerfiles to test deployments
   (bootstrap-k3s.sh,
    security groups,
    SSH key)
        |
        v
3. K8s Base Manifests              -- Needs running K3s cluster
   (base/ directory:
    deployments, services,
    statefulsets, PVCs)
        |
        v
4. Kustomize Overlays              -- Needs base manifests
   (staging + prod overlays,
    ingress, configmaps,
    secrets)
        |
        v
5. cert-manager + TLS              -- Needs ingress working first
   (cert-manager install,
    ClusterIssuer,
    ingress TLS annotations)
        |
        v
6. ArgoCD + GitOps                 -- Needs working K8s + manifests
   (ArgoCD install,
    app-of-apps,
    GitHub webhook)
        |
        v
7. GitHub Actions CI Pipeline      -- Needs GHCR + ArgoCD
   (build workflow,
    image push,
    manifest update)
        |
        v
8. Domain + DNS                    -- Can happen in parallel with 5-7
   (Route 53 or registrar DNS,
    A record to EC2 Elastic IP)
```

## New Components Summary

| Component | Status | Location |
|-----------|--------|----------|
| `backend.Dockerfile` | NEW | `infra/docker/backend.Dockerfile` |
| `frontend.Dockerfile` | NEW | `infra/docker/frontend.Dockerfile` |
| `application-prod.yml` | NEW | `backend/src/main/resources/application-prod.yml` |
| `next.config.ts` | MODIFY | `frontend/next.config.ts` (add `output: "standalone"`) |
| K8s base manifests | NEW | `infra/k8s/base/**` |
| Kustomize overlays | NEW | `infra/k8s/overlays/{staging,prod}/**` |
| Platform manifests | NEW | `infra/k8s/platform/{cert-manager,argocd}/**` |
| Bootstrap scripts | NEW | `infra/scripts/*.sh` |
| GitHub Actions workflow | NEW | `.github/workflows/ci-cd.yml` |
| `infra/CLAUDE.md` | MODIFY | Update with K8s conventions |

## Sources

- [K3s Official Requirements](https://docs.k3s.io/installation/requirements) -- minimum hardware specs (HIGH confidence)
- [K3s Quick Start Guide](https://docs.k3s.io/quick-start) -- installation steps
- [K3s Volumes and Storage](https://docs.k3s.io/add-ons/storage) -- Local Path Provisioner details
- [K3s Production Discussion](https://github.com/k3s-io/k3s/discussions/2988) -- single-node production viability
- [ArgoCD Monorepo Pattern](https://oneuptime.com/blog/post/2026-02-26-argocd-monorepo-pattern/view) -- directory structure (MEDIUM confidence)
- [ArgoCD Best Practices](https://codefresh.io/blog/how-to-structure-your-argo-cd-repositories-using-application-sets/) -- repo structure recommendations
- [ArgoCD App-of-Apps](https://argo-cd.readthedocs.io/en/latest/operator-manual/cluster-bootstrapping/) -- official bootstrap pattern
- [K3s + Traefik + cert-manager + Let's Encrypt](https://k3s.rocks/https-cert-manager-letsencrypt/) -- TLS setup guide
- [Next.js Deploying Docs](https://nextjs.org/docs/app/getting-started/deploying) -- standalone output, Docker
- [Next.js Docker Best Practices 2026](https://thelinuxcode.com/nextjs-docker-images-how-i-build-predictable-fast-deployments-in-2026/) -- standalone mode details
- [Spring Boot Multi-Stage Docker](https://medium.com/@cat.edelveis/a-guide-to-docker-multi-stage-builds-for-spring-boot-08e3a64c9812) -- layered JAR extraction
- [AWS Free Tier EC2 Details](https://cloudwithalon.com/aws-free-tier-2025-whats-free-and-for-how-long) -- instance types, hours limits
- [AWS Free Tier July 2025 Changes](https://repost.aws/questions/QUlaKi-MimTo-3OjekpKMWiA/what-is-correct-for-ec2-free-tier-instance) -- expanded instance types
- [GitHub Actions + ArgoCD GitOps](https://medium.com/@mehmetkanus17/argocd-github-actions-a-complete-gitops-ci-cd-workflow-for-kubernetes-applications-ed2f91d37641) -- CI/CD pipeline pattern
- [StatefulSets & Persistent Storage](https://www.glukhov.org/post/2025/11/statefulsets-and-persistent-storage-in-kubernetes/) -- PostgreSQL, Redis on K8s
- [K3s on EC2 Instead of EKS](https://medium.com/@yashjani.ca/why-we-used-k3s-on-ec2-instead-of-eks-for-our-capstone-project-a-cost-saving-kubernetes-setup-c8901f070591) -- cost comparison, real experience
