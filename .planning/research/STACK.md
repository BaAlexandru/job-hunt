# Technology Stack

**Project:** JobHunt v1.1 -- Infrastructure & Deployment
**Researched:** 2026-03-22
**Scope:** Infrastructure additions only. Existing stack (Spring Boot 4.0.4, Next.js 16.2, PostgreSQL 17, Redis 7, MinIO, Better Auth, Docker Compose local dev) is validated from v1.0 and not re-researched.

---

## Recommended Stack

### Kubernetes Distribution

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| K3s | v1.35.2+k3s1 | Lightweight Kubernetes on AWS EC2 | Single binary (<70MB). Runs on 512MB minimum RAM vs kubeadm's 2GB+. Built-in Traefik ingress, CoreDNS, metrics-server, local-path storage. SQLite backend eliminates separate etcd. Install is one command: `curl -sfL https://get.k3s.io \| sh -`. Kubeadm rejected: too heavy for small instances, manual networking/storage setup, kernel panics reported on t2.micro/t3.micro. |

**Confidence:** HIGH -- K3s is the de facto standard for resource-constrained self-managed K8s. CNCF certified, production-grade, massive adoption for edge and small-scale deployments.

### Compute (AWS EC2)

| Technology | Spec | Purpose | Why |
|------------|------|---------|-----|
| EC2 t3.small | 2 vCPU, 2GB RAM | Production K3s cluster (single-node: control plane + workloads) | t3.micro (1GB) is too tight for K3s server + application pods + ArgoCD. t3.small is the minimum viable for all workloads on one node. ~$15/mo on-demand, ~$9/mo with 1-year reserved, ~$5-7/mo spot. |
| EC2 t3.micro | 2 vCPU, 1GB RAM | Staging K3s cluster (lighter workloads, no ArgoCD) | Staging runs fewer replicas, no ArgoCD (managed from production). Free-tier eligible: 750 hrs/mo for 12 months. Deploy ArgoCD only on production and have it manage both clusters. |

**Cost reality:** True free-tier K8s is not practical for production. Budget ~$20-30/mo total for both environments. Spot instances can cut production to ~$5-7/mo but with interruption risk (acceptable for a job tracker -- brief downtime is fine).

**Confidence:** MEDIUM -- t3.small for production is pragmatic but tight. Running backend + frontend + PostgreSQL + Redis + MinIO + ArgoCD all on 2GB RAM requires careful resource limits. May need t3.medium (~$30/mo) if memory pressure is constant. Monitor with `kubectl top` after initial deployment.

### Container Registry

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| GitHub Container Registry (ghcr.io) | N/A | Docker image storage | Already on GitHub for source code. Free for public images, private images within plan allowance. No additional account/service needed. Native integration with GitHub Actions. |

**Confidence:** HIGH -- zero cost, zero setup, already in the ecosystem.

### GitOps / Continuous Deployment

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| ArgoCD | v3.3.3 | GitOps CD for Kubernetes | CNCF graduated project. Watches Git repo containing K8s manifests, auto-syncs to cluster state. Web UI for deployment visibility. Project requirement specifies ArgoCD. Released 2026-03-16. |

**Resource warning:** ArgoCD needs ~256-512MB RAM. On t3.small (2GB), this is 12-25% of total. Install ArgoCD ONLY on the production cluster. Use it to manage both staging and production via multi-cluster setup.

**Confidence:** HIGH -- v3.3.3 is current stable. Well-documented, massive community, CNCF graduated.

### CI (Image Building)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| GitHub Actions | N/A | Build Docker images, push to ghcr.io, run tests | Already on GitHub. Free 2,000 min/month for private repos. Handles the CI half: build, test, push image, update image tag in manifests repo. ArgoCD handles CD: detect manifest change, deploy to K8s. |

**Confidence:** HIGH -- natural complement to ArgoCD GitOps pattern.

### Ingress Controller

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Traefik | v3.x (bundled with K3s) | HTTP/HTTPS routing, TLS termination, path-based routing | Ships with K3s -- zero additional install. Auto-discovers Ingress and IngressRoute resources. NGINX Ingress Controller is being retired March 2026 -- do not use it for new deployments. Traefik handles routing `jobhunt.example.com` to frontend and `jobhunt.example.com/api/*` to backend. |

**Confidence:** HIGH -- Traefik is the K3s default. Using bundled version avoids version management overhead.

### TLS / Certificate Management

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| cert-manager | v1.20.0 | Automated TLS certificates from Let's Encrypt | Industry standard for K8s TLS automation. Creates Certificate CRDs, handles ACME HTTP-01 challenges via Traefik, auto-renews 30 days before expiry. Free certificates from Let's Encrypt (90-day validity). Released 2026-03-09. |

**Alternative considered:** Traefik's built-in ACME could handle Let's Encrypt directly. However, cert-manager is the community standard, more flexible (supports multiple issuers), and decouples TLS management from ingress controller choice. Use cert-manager.

**Confidence:** HIGH -- cert-manager v1.20.0 is current stable. CNCF project, mature.

### Infrastructure as Code

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| OpenTofu | v1.9+ | Provision AWS EC2, VPC, security groups, EBS volumes, Route 53 DNS | Open-source Terraform fork under MPL 2.0 (Terraform switched to BSL 1.1 in 2023). Same HCL syntax, same AWS provider, same `init/plan/apply` workflow. For a greenfield project, prefer the truly open-source option. Switching to `terraform` CLI is trivial if needed -- same `.tf` files. |

**Confidence:** MEDIUM -- OpenTofu is a drop-in Terraform replacement with identical syntax. Large enterprise adoption growing. If any provider compatibility issue appears, `terraform` CLI works on the same files.

### Domain and DNS

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Porkbun | N/A | Domain registration | Cheapest registrar for .com/.dev domains (~$8-10/yr). Clean UI. |
| AWS Route 53 | N/A | DNS hosting | Programmatic DNS management via OpenTofu. A records pointing to EC2 elastic IPs. $0.50/mo per hosted zone + negligible query cost. |

**Total cost:** ~$10/yr domain + $6/yr DNS = ~$16/yr.

**Alternative:** Use registrar DNS (free) instead of Route 53. Works fine for manual setup but loses IaC management of DNS records. Route 53 is worth $0.50/mo for OpenTofu integration.

**Confidence:** MEDIUM -- straightforward, well-documented.

### SMTP / Email

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Resend | Java SDK v3.1.0 | Transactional email (password reset, future notifications) | 3,000 emails/month free tier -- more than enough for a single-user job tracker. Clean developer API with official Java/Kotlin SDK. Better deliverability than self-hosted SMTP. |
| spring-boot-starter-mail | (Spring Boot 4.0.4 managed) | Spring Mail abstraction layer | Standard Spring starter providing `JavaMailSender`. Can use Resend via SMTP relay OR their REST API via SDK. Add dependency only when implementing email phase. |

**Integration path:** Better Auth password reset already has a callback hook (`sendResetPasswordEmail`). Wire it to call Resend API (or `JavaMailSender` with Resend SMTP credentials: `smtp.resend.com:465`).

**Alternatives considered:** SMTP2GO (1,000/mo free, good deliverability), Brevo (300/day free), AWS SES (requires sandbox escape approval -- more friction). Resend chosen for best DX and generous free tier.

**Confidence:** MEDIUM -- Resend is well-regarded with good Java support. Any SMTP provider works with `spring-boot-starter-mail` as a fallback.

### Production Docker Builds

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Multi-stage Dockerfiles | Docker 24+ | Optimized production images for backend and frontend | Separates build environment from runtime. Dramatically reduces image size and attack surface. |

**Backend Dockerfile pattern (Spring Boot + Kotlin + Gradle):**
```
Stage 1 (build): eclipse-temurin:24-jdk
  - Copy Gradle wrapper first (rarely changes -- cached layer)
  - Copy build.gradle.kts + settings.gradle.kts (changes with deps)
  - Run ./gradlew dependencies (download deps -- cached unless build files change)
  - Copy src/ (changes frequently)
  - Run ./gradlew bootJar --no-daemon
Stage 2 (runtime): eclipse-temurin:24-jre-alpine
  - Copy JAR from build stage
  - Run with java -jar
  - Expected image size: ~200-300MB
```

**Frontend Dockerfile pattern (Next.js standalone):**
```
Stage 1 (deps): node:22-alpine
  - Copy package.json + package-lock.json
  - Run npm ci
Stage 2 (build): node:22-alpine
  - Copy deps from stage 1
  - Copy source
  - Run next build (requires output: "standalone" in next.config.ts)
Stage 3 (runner): node:22-alpine
  - Copy .next/standalone + .next/static + public from stage 2
  - Run with node server.js
  - Expected image size: ~100-200MB (down from >1GB without standalone)
```

**Critical:** Next.js `next.config.ts` must add `output: "standalone"` for the Docker build to work. Currently missing -- must be added.

**Confidence:** HIGH -- standard, well-documented patterns for both frameworks.

### Kubernetes Manifest Management

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kustomize | (bundled with kubectl) | K8s manifest templating with environment overlays | Bundled with kubectl -- no install needed. Base manifests + `overlays/staging/` and `overlays/production/` for env-specific patches (image tags, replica counts, resource limits, secrets). Simpler than Helm for a single-app deployment with two environments. |

**Directory structure:**
```
infra/k8s/
  base/
    backend-deployment.yaml
    frontend-deployment.yaml
    postgres-statefulset.yaml
    redis-deployment.yaml
    minio-statefulset.yaml
    ingress.yaml
    kustomization.yaml
  overlays/
    staging/
      kustomization.yaml   # patches: smaller limits, 1 replica
    production/
      kustomization.yaml   # patches: larger limits, secrets
```

**Confidence:** HIGH -- Kustomize is the right tool at this project's scale. Helm is overkill for a single application.

### Monitoring (Minimal -- Do NOT Over-Engineer)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Spring Boot Actuator | (already included) | Health endpoints for K8s probes | Already in the project. `/actuator/health` for liveness/readiness probes. Zero additional work. |
| metrics-server | (bundled with K3s) | `kubectl top pods/nodes` | K3s includes metrics-server. Gives basic CPU/memory visibility. |

**Do NOT add:** Prometheus, Grafana, Loki, Jaeger, or any full observability stack. Each consumes 256-512MB+ RAM. On a 2GB t3.small running the full app stack + ArgoCD, there is no room. Add observability in v1.2+ when/if upgrading to larger instances.

**Confidence:** HIGH -- actuator probes + kubectl top is sufficient for initial production.

---

## Data Stores on Kubernetes

The project runs PostgreSQL, Redis, and MinIO on K8s (per PROJECT.md requirements).

| Store | K8s Resource | Persistence | Resource Request | Risk |
|-------|-------------|-------------|-----------------|------|
| PostgreSQL 17 | StatefulSet, 1 replica | PVC backed by EBS gp3 (10GB) | 256MB RAM, 250m CPU | Data loss if EBS detaches without backup. MUST run pg_dump CronJob. |
| Redis 7 | Deployment, 1 replica | None (ephemeral) | 64MB RAM, 100m CPU | Session/cache only -- acceptable to lose on restart. |
| MinIO | StatefulSet, 1 replica | PVC backed by EBS gp3 (10GB) | 128MB RAM, 100m CPU | Document storage. Must backup. Long-term, consider replacing with direct S3 ($0.023/GB/mo). |

**Memory budget on t3.small (2GB):**
| Component | RAM |
|-----------|-----|
| K3s system (kubelet, Traefik, CoreDNS) | ~350MB |
| ArgoCD (production only) | ~300MB |
| PostgreSQL | ~256MB |
| Backend (Spring Boot JVM) | ~384MB |
| Frontend (Node.js) | ~128MB |
| Redis | ~64MB |
| MinIO | ~128MB |
| **Total** | **~1,610MB** |

This leaves ~400MB headroom on 2GB. Tight but workable. If ArgoCD is not on the same node (manage from external), headroom improves to ~700MB.

**Confidence:** MEDIUM -- memory budget is tight. Spring Boot JVM is the wildcard -- may need `-Xmx256m` flag and tuning. Monitor after first deployment.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| K8s distribution | K3s | kubeadm | Too heavy for small instances (needs 2GB+ for control plane alone), manual networking setup, no built-in ingress |
| K8s distribution | K3s | MicroK8s | Snap-based (unavailable on Amazon Linux 2023), weaker AWS community support |
| K8s distribution | K3s | EKS | $73/mo control plane cost, defeats the self-managed learning goal |
| Container registry | ghcr.io | AWS ECR | Additional cost ($0.10/GB/mo storage), additional AWS IAM config, already on GitHub |
| GitOps | ArgoCD | FluxCD | ArgoCD has better UI for deployment visibility, project requirement specifies it |
| IaC | OpenTofu | Terraform | BSL license concerns for greenfield projects, identical functionality, same `.tf` files |
| IaC | OpenTofu | Pulumi | Different paradigm (imperative), adds programming language complexity to IaC |
| IaC | OpenTofu | AWS CloudFormation | Vendor-locked, verbose JSON/YAML, no state portability |
| Ingress | Traefik (K3s default) | NGINX Ingress | NGINX Ingress Controller EOL March 2026, Traefik ships free with K3s |
| TLS | cert-manager | Traefik built-in ACME | cert-manager is more standard, decoupled from ingress choice, more flexible |
| Email | Resend | AWS SES | SES sandbox escape requires support ticket and approval wait, more complex setup |
| Email | Resend | Self-hosted SMTP | Deliverability nightmare, IP reputation, maintenance burden |
| Manifests | Kustomize | Helm | Helm charts are overkill for single-app two-env deployment |
| Monitoring | Actuator + kubectl top | Prometheus + Grafana | Each stack component needs 256-512MB RAM, no room on t3.small |

---

## What NOT to Add (Over-Engineering Warnings)

| Avoid | Why | What to Do Instead |
|-------|-----|-------------------|
| Prometheus + Grafana | 512MB+ RAM each, no room on t3.small, premature for single-user app | `kubectl top` + Spring Boot Actuator health endpoints |
| Istio / Linkerd service mesh | Massive resource overhead, adds complexity for no benefit with 2-3 services | Direct service-to-service communication via K8s ClusterIP services |
| Vault for secrets | Overkill; adds 256MB+ RAM and operational complexity | K8s Secrets (base64) + sealed-secrets or SOPS for Git encryption |
| Multi-node K8s cluster | Doubles EC2 cost for no benefit at this scale | Single-node K3s per environment |
| Horizontal Pod Autoscaler | Single user app with predictable load, no scaling triggers | Fixed replica count (1 per service) |
| AWS RDS / ElastiCache | Monthly cost ($15+/mo each), defeats self-managed learning goal | PostgreSQL and Redis as K8s pods with PVC backup |
| Terraform Cloud / Spacelift | Paid services for state management | Local OpenTofu state in Git (or S3 backend for team use) |
| ArgoCD Image Updater | Adds complexity; GitHub Actions can update image tags in manifests directly | GitHub Actions workflow updates kustomization.yaml image tag, ArgoCD syncs |
| Tekton / Jenkins for CI | Over-engineering; GitHub Actions is simpler and free | GitHub Actions for CI, ArgoCD for CD |

---

## Installation / Setup Commands

```bash
# === Dev Machine Tools ===
# OpenTofu (IaC)
choco install opentofu          # Windows
# brew install opentofu         # macOS

# kubectl (K8s CLI)
choco install kubernetes-cli    # Windows
# brew install kubectl          # macOS

# ArgoCD CLI
choco install argocd-cli        # Windows
# brew install argocd           # macOS

# === Backend Dependencies (add to build.gradle.kts when phase starts) ===
# Email support:
implementation("org.springframework.boot:spring-boot-starter-mail")
# OR for Resend REST API:
implementation("com.resend:resend-java:3.1.0")

# === Frontend Config (add to next.config.ts) ===
# output: "standalone"  -- required for Docker production builds

# === K3s Installation (on EC2 instance, run via SSH) ===
# Production server:
curl -sfL https://get.k3s.io | sh -
# Staging server:
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik" sh -
# (staging may skip Traefik if accessed only via port-forward for testing)

# === ArgoCD Installation (on production K3s cluster) ===
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.3.3/manifests/install.yaml

# === cert-manager Installation (on each K3s cluster) ===
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.20.0/cert-manager.yaml
```

---

## Cost Estimate (Monthly)

| Item | Staging | Production | Notes |
|------|---------|------------|-------|
| EC2 instance | $0 (free-tier t3.micro) | ~$15 (t3.small on-demand) | Spot: ~$5 for production |
| EBS storage (20GB each) | $1.60 | $1.60 | gp3 at $0.08/GB/mo |
| Elastic IP | $0 (if attached) | $0 (if attached) | Free when attached to running instance |
| Route 53 hosted zone | -- | $0.50 | Shared across environments |
| Domain (amortized) | -- | ~$0.80 | ~$10/yr from Porkbun |
| Resend email | -- | $0 | Free tier: 3,000/mo |
| ghcr.io | -- | $0 | Free within GitHub plan |
| GitHub Actions | -- | $0 | Free 2,000 min/mo private repos |
| **Total** | **~$1.60-10** | **~$18** | |
| **Combined** | | | **~$20-28/mo** |

With spot instance for production: **~$10-15/mo total.**

---

## Version Compatibility Matrix

| Component | Version | Compatible With | Notes |
|-----------|---------|-----------------|-------|
| K3s v1.35 | K8s API v1.35 | ArgoCD v3.3, cert-manager v1.20, Kustomize v5 | Built with Go 1.25 |
| ArgoCD v3.3.3 | -- | K8s 1.28-1.35 | Runs as pods in cluster |
| cert-manager v1.20.0 | -- | K8s 1.28+ | Helm or static manifests |
| OpenTofu v1.9+ | -- | AWS provider 5.x | Same HCL as Terraform |
| Eclipse Temurin JDK 24 | -- | Spring Boot 4.0.4, Gradle 9.3.1 | For Docker build stage |
| Eclipse Temurin JRE 24 | -- | Spring Boot 4.0.4 runtime | For Docker runtime stage |
| Node.js 22 LTS | -- | Next.js 16.2 | For Docker build + runtime |

---

## Sources

- [K3s official requirements](https://docs.k3s.io/installation/requirements) -- minimum resource specs (HIGH confidence)
- [K3s v1.35 release notes](https://docs.k3s.io/release-notes/v1.34.X) -- current version confirmed (HIGH confidence)
- [K3s releases on GitHub](https://github.com/k3s-io/k3s/releases) -- v1.35.2+k3s1 (HIGH confidence)
- [ArgoCD v3.3.3 release](https://github.com/argoproj/argo-cd/releases) -- released 2026-03-16 (HIGH confidence)
- [ArgoCD installation docs](https://argo-cd.readthedocs.io/en/stable/operator-manual/installation/) -- install manifests (HIGH confidence)
- [cert-manager v1.20.0](https://github.com/cert-manager/cert-manager/releases) -- released 2026-03-09 (HIGH confidence)
- [cert-manager Let's Encrypt tutorial](https://cert-manager.io/docs/tutorials/getting-started-aws-letsencrypt/) -- AWS integration (HIGH confidence)
- [AWS EC2 free tier](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html) -- t3.micro 750 hrs/mo (HIGH confidence)
- [GitHub Container Registry docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry) -- free tier (HIGH confidence)
- [Resend Java SDK](https://resend.com/java) -- v3.1.0, 3,000 emails/mo free (MEDIUM confidence)
- [Next.js standalone output](https://nextjs.org/docs/pages/api-reference/config/next-config-js/output) -- Docker optimization (HIGH confidence)
- [OpenTofu](https://opentofu.org/) -- MPL 2.0 Terraform fork (HIGH confidence)
- [Traefik vs NGINX Ingress retirement](https://traefik.io/blog/migrate-from-ingress-nginx-to-traefik-now) -- NGINX EOL March 2026 (MEDIUM confidence)
- [K3s on EC2 case study](https://medium.com/@yashjani.ca/why-we-used-k3s-on-ec2-instead-of-eks-for-our-capstone-project-a-cost-saving-kubernetes-setup-c8901f070591) -- real-world experience (MEDIUM confidence)
- [Spring Boot email docs](https://docs.spring.io/spring-boot/reference/io/email.html) -- mail starter (HIGH confidence)

---
*Infrastructure stack research for: JobHunt v1.1*
*Researched: 2026-03-22*
