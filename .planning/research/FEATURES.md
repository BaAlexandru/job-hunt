# Feature Research

**Domain:** Production Deployment & Infrastructure for Job Application Tracker
**Researched:** 2026-03-22
**Confidence:** HIGH (infrastructure patterns are well-established)

## Scope

This research covers two categories for v1.1:
1. **Gap Closure** -- v1.0 features with backend complete but frontend missing
2. **Infrastructure & Deployment** -- everything needed to go live on a custom domain

All v1.0 application features (CRUD, kanban, auth, documents, interviews) are already shipped. This milestone focuses on production-readiness and deployment.

## Feature Landscape

### Table Stakes (Must Have to Go Live)

Features required for the application to be accessible on the internet as a real product.

| Feature | Why Expected | Complexity | Category | Notes |
|---------|--------------|------------|----------|-------|
| Production Docker images (multi-stage) | Cannot deploy without containerized builds; dev-mode Docker is not production-ready | MEDIUM | Infrastructure | Backend: Gradle build + JRE runtime stage. Frontend: pnpm install + Next.js standalone output + Node.js Alpine runtime. Target <200MB per image |
| Container registry (GHCR) | Images must be stored somewhere pullable by the cluster | LOW | Infrastructure | GitHub Container Registry is free for public images, integrates natively with GitHub Actions |
| CI pipeline (GitHub Actions) | Manual docker build+push is error-prone and unsustainable | MEDIUM | Infrastructure | Build, test, push images on merge to master. Reuse existing Gradle/pnpm test commands |
| Kubernetes cluster (k3s on EC2) | The deployment target; chosen over EKS for cost ($0 control plane vs $73/mo) | HIGH | Infrastructure | k3s on a single t3.small or t3.medium EC2 instance. k3s bundles Traefik, CoreDNS, local-path storage |
| K8s manifests for all services | Cluster needs to know what to run | MEDIUM | Infrastructure | Deployments + Services for: backend, frontend, PostgreSQL, Redis, MinIO. ConfigMaps for env vars, Secrets for credentials |
| Ingress / traffic routing | External traffic must reach the app | MEDIUM | Infrastructure | Traefik ships with k3s. Use Kubernetes Gateway API (not deprecated Ingress resource). Route jobhunt.yourdomain.com to frontend, /api/* to backend |
| Domain + DNS binding | Users access the app via a domain, not an IP address | LOW | Infrastructure | Register domain (Namecheap/Cloudflare ~$10/yr). A record pointing to EC2 elastic IP |
| TLS/HTTPS via cert-manager + Let's Encrypt | Modern browsers warn on HTTP; HTTPS is non-negotiable for auth cookies | MEDIUM | Infrastructure | cert-manager auto-provisions and renews Let's Encrypt certificates. Integrates with Traefik Gateway API |
| Database on K8s (PostgreSQL StatefulSet) | App needs its database in the cluster | MEDIUM | Infrastructure | StatefulSet with PersistentVolumeClaim. Single replica is fine for personal project. Must configure backup strategy |
| Redis on K8s | Session store for Better Auth | LOW | Infrastructure | Simple Deployment + Service. Redis data is ephemeral (sessions re-created on login). No persistence needed |
| MinIO on K8s | Document storage must be available in production | MEDIUM | Infrastructure | StatefulSet with PVC for document data persistence. Same S3 API the app already uses |
| Environment variables / secrets management | Production credentials must not be in git | LOW | Infrastructure | K8s Secrets for DB passwords, MinIO keys, JWT secrets. ConfigMaps for non-sensitive config |
| Interview notes UI | Backend API exists, frontend InterviewsTab missing the notes component | LOW | Gap Closure | Add notes textarea to interview detail/edit in InterviewsTab. Backend already handles CRUD |
| Document version UI | Backend supports versions, frontend lacks version history panel | LOW | Gap Closure | Add version history panel showing upload date, size, download link per version |
| Password reset email | Better Auth callback needs SMTP transport to deliver reset emails | MEDIUM | Gap Closure | Configure SMTP (e.g., AWS SES free tier, or Resend). Wire Better Auth email callback |

### Differentiators (Valuable but Not Blocking Launch)

Features that improve production quality but are not required for initial go-live.

| Feature | Value Proposition | Complexity | Category | Notes |
|---------|-------------------|------------|----------|-------|
| ArgoCD GitOps deployment | Push-to-deploy via git commits. K8s-native, declarative, auditable. Better learning experience than kubectl apply | HIGH | Infrastructure | Runs inside the cluster, watches a git repo for manifest changes. Installs via Helm. Adds ~300MB RAM overhead |
| Staging environment (second cluster or namespace) | Test changes before they hit production. Catch config issues early | HIGH | Infrastructure | Two k3s instances means two EC2 nodes (~$15-30/mo total). Namespace separation is cheaper but weaker isolation |
| Automated database backups | Prevent data loss from accidental deletion or corruption | MEDIUM | Operations | CronJob running pg_dump to S3/MinIO. Daily backups, 7-day retention. Critical before real usage |
| Health check probes (liveness/readiness) | K8s restarts unhealthy pods automatically. Prevents serving broken state | LOW | Infrastructure | Spring Boot Actuator /actuator/health already exists. Add K8s probe config to Deployment manifests |
| Structured logging (JSON) | Machine-parseable logs for debugging production issues | LOW | Operations | Spring Boot: logback JSON encoder. Next.js: pino. Enables future log aggregation |
| Resource limits and requests | Prevents one pod from starving others. Required for stable multi-service node | LOW | Infrastructure | Set CPU/memory requests and limits on all Deployments. Essential on constrained EC2 instances |
| Visibility and sharing (public/private companies/jobs) | Allow sharing specific companies or job postings. Useful for portfolio or mentorship | MEDIUM | Gap Closure | Backend schema changes for visibility field. Frontend toggle. Low priority for single user |
| Graceful shutdown | Clean connection draining on pod termination. Prevents 502 errors during deploys | LOW | Infrastructure | Spring Boot: server.shutdown=graceful already standard. Add preStop hook in K8s manifest |
| Rolling update strategy | Zero-downtime deployments | LOW | Infrastructure | K8s default rolling update works. Set maxUnavailable=0, maxSurge=1 for zero-downtime |

### Anti-Features (Commonly Attempted, Often Problematic)

Features that seem necessary for production but create disproportionate complexity for a single-developer personal project.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Separate staging + production K8s clusters | "Real companies do it" | Doubles infrastructure cost ($15-30/mo extra), doubles maintenance burden, and for a single-user app the blast radius of a bad deploy is just yourself | Single cluster with namespace separation (staging/production). Or skip staging entirely -- deploy to prod with rolling updates and rollback if broken |
| Managed database (RDS) | "Never run databases in K8s" | RDS free tier is only 12 months, then $15+/mo. For a single-user personal project with daily pg_dump backups, PostgreSQL on k3s is perfectly adequate | PostgreSQL StatefulSet + automated pg_dump backups. Migrate to RDS only if data becomes critical or multi-user traffic grows |
| Full observability stack (Prometheus + Grafana + Loki) | "You need monitoring" | Each tool consumes 200-500MB RAM. On a t3.small (2GB), this leaves nothing for your actual app. Overkill for single-user traffic | kubectl logs + Spring Boot Actuator + structured logging to stdout. Add Prometheus only if you upgrade to a larger instance |
| Helm charts for all services | "Helm is the standard K8s package manager" | Adds abstraction layer, templating complexity, and chart versioning overhead. For 5-6 services with straightforward configs, raw manifests are simpler and more debuggable | Plain K8s YAML manifests organized by service. Kustomize for environment overlaps (staging vs prod) if needed |
| Multi-node K8s cluster with HA | "Single node is not production" | For a personal project with one user, HA provides zero benefit. Multi-node requires shared storage, leader election, and costs 2-3x more | Single k3s node. Accept that node failure = brief downtime. Automated backups ensure data safety |
| Service mesh (Istio/Linkerd) | "Microservices need a service mesh" | Massive resource overhead (500MB+ RAM), extreme complexity, zero value for 3 services on one node that communicate via cluster DNS | Direct Service-to-Service communication via K8s DNS. The app has 3 backend services, not 30 microservices |
| Horizontal Pod Autoscaler | "Scale based on load" | Single node cannot scale horizontally. HPA without node autoscaling just reshuffles pods on the same machine | Fixed replica counts (1 per service). Vertical scaling via larger EC2 instance if needed |
| Custom domain email (you@jobhunt.com) | "Professional email for password resets" | Requires email infrastructure (SES, SPF, DKIM, DMARC setup). Adds operational burden | Use a personal Gmail/ProtonMail as SMTP sender for transactional emails, or use Resend/Mailgun free tier with their domain |

## Feature Dependencies

```
[Gap Closure: Interview Notes UI]
    └──requires──> (nothing new, just frontend component work)

[Gap Closure: Document Version UI]
    └──requires──> (nothing new, just frontend component work)

[Gap Closure: Password Reset Email]
    └──requires──> SMTP provider configuration

[Gap Closure: Visibility & Sharing]
    └──requires──> Schema migration + backend changes + frontend toggle

[Production Docker Images]
    └──requires──> (nothing new, existing build system)

[CI Pipeline (GitHub Actions)]
    └──requires──> [Production Docker Images]
    └──requires──> [Container Registry (GHCR)]

[K8s Cluster (k3s on EC2)]
    └──requires──> AWS account + EC2 instance setup

[K8s Manifests]
    └──requires──> [Production Docker Images] (image references)
    └──requires──> [K8s Cluster] (deployment target)

[Domain + DNS]
    └──requires──> [K8s Cluster] (need the IP to point at)

[TLS/HTTPS]
    └──requires──> [Domain + DNS] (cert-manager needs a real domain)
    └──requires──> [Ingress / Traffic Routing]

[Ingress / Traffic Routing]
    └──requires──> [K8s Cluster] (Traefik comes with k3s)
    └──requires──> [K8s Manifests] (Services to route to)

[ArgoCD]
    └──requires──> [K8s Cluster]
    └──requires──> [K8s Manifests] (in a git repo for ArgoCD to watch)
    └──requires──> [CI Pipeline] (to push images that trigger ArgoCD sync)

[Database Backups]
    └──requires──> [PostgreSQL on K8s]

[Staging Namespace]
    └──requires──> [K8s Manifests] (Kustomize overlays for env separation)
    └──enhances──> [ArgoCD] (ArgoCD can manage both staging and prod apps)

[Health Check Probes]
    └──enhances──> [K8s Manifests]
    └──requires──> Spring Boot Actuator (already exists)
```

### Dependency Notes

- **Gap closure features are independent of infrastructure:** Interview notes UI, doc version UI, and password reset email can be built on the existing dev setup with zero infrastructure dependencies. Do these first.
- **Visibility & Sharing has schema changes:** Requires a Flyway migration, so it touches both backend and database. More complex than other gap closures.
- **Docker images gate everything else:** Nothing deploys without containerized builds. This is the critical first infrastructure step.
- **CI pipeline gates ArgoCD:** ArgoCD watches git for manifest changes, but needs CI to build and push new images first. Without CI, ArgoCD has nothing new to deploy.
- **Domain + TLS are late dependencies:** You can run the cluster with NodePort/port-forward before DNS is configured. Domain and TLS are the last mile.
- **ArgoCD is an enhancement, not a blocker:** You can deploy with `kubectl apply` initially and add ArgoCD later. It is valuable for learning but not required for go-live.

## MVP Definition

### Launch With (Go-Live Minimum)

The absolute minimum to have the app accessible on the internet with HTTPS.

- [ ] Interview notes UI -- close the most visible v1.0 gap
- [ ] Document version UI -- close the second v1.0 gap
- [ ] Password reset email via SMTP -- required for auth to work properly in production
- [ ] Multi-stage Dockerfiles for backend and frontend
- [ ] GitHub Actions CI to build, test, and push images to GHCR
- [ ] Single k3s node on AWS EC2 (t3.small or t3.medium)
- [ ] K8s manifests for all services (backend, frontend, PostgreSQL, Redis, MinIO)
- [ ] Traefik Gateway API routing (frontend + backend API)
- [ ] Domain registration + DNS A record to EC2 elastic IP
- [ ] cert-manager + Let's Encrypt for automatic HTTPS
- [ ] K8s Secrets for production credentials
- [ ] Health check probes (liveness/readiness) on all Deployments

### Add After Go-Live (v1.1.x)

Features to add once the app is running in production and stable.

- [ ] Automated database backups (CronJob + pg_dump) -- add within first week of production
- [ ] ArgoCD for GitOps deployment -- replace manual kubectl apply with push-to-deploy
- [ ] Staging namespace with Kustomize overlays -- test changes before prod
- [ ] Structured JSON logging -- better debugging when issues arise
- [ ] Visibility & Sharing feature -- low urgency for single user
- [ ] Resource limits tuning -- observe actual usage first, then set limits

### Future Consideration (v2+)

- [ ] Multi-node cluster -- only if traffic grows beyond single-user
- [ ] Prometheus + Grafana monitoring -- only if instance is large enough (4GB+ RAM)
- [ ] Managed database migration (RDS) -- only if data becomes business-critical
- [ ] CDN for static assets -- only if global access latency matters

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority | Category |
|---------|------------|---------------------|----------|----------|
| Interview notes UI | MEDIUM | LOW | P1 | Gap Closure |
| Document version UI | MEDIUM | LOW | P1 | Gap Closure |
| Password reset email | HIGH | MEDIUM | P1 | Gap Closure |
| Production Docker images | HIGH | MEDIUM | P1 | Infrastructure |
| CI pipeline (GitHub Actions) | HIGH | MEDIUM | P1 | Infrastructure |
| K8s cluster (k3s on EC2) | HIGH | HIGH | P1 | Infrastructure |
| K8s manifests (all services) | HIGH | MEDIUM | P1 | Infrastructure |
| Ingress / traffic routing | HIGH | MEDIUM | P1 | Infrastructure |
| Domain + DNS + TLS | HIGH | LOW | P1 | Infrastructure |
| Health check probes | MEDIUM | LOW | P1 | Infrastructure |
| Secrets management | HIGH | LOW | P1 | Infrastructure |
| Database backups | HIGH | LOW | P2 | Operations |
| ArgoCD GitOps | MEDIUM | HIGH | P2 | Infrastructure |
| Staging namespace | LOW | MEDIUM | P2 | Infrastructure |
| Structured logging | LOW | LOW | P2 | Operations |
| Visibility & Sharing | LOW | MEDIUM | P2 | Gap Closure |
| Resource limits tuning | MEDIUM | LOW | P2 | Infrastructure |
| Graceful shutdown | MEDIUM | LOW | P2 | Infrastructure |
| Full observability stack | LOW | HIGH | P3 | Operations |
| Multi-node HA cluster | LOW | HIGH | P3 | Infrastructure |

**Priority key:**
- P1: Required for go-live -- the app cannot be accessed on the internet without these
- P2: Should have within first 1-2 weeks of production -- operational safety and developer experience
- P3: Future consideration -- only relevant at scale or with more resources

## Cost Analysis

| Resource | Monthly Cost | Notes |
|----------|-------------|-------|
| EC2 t3.small (2 vCPU, 2GB RAM) | ~$15/mo | Sufficient for single-user with k3s + all services. Free tier t3.micro (1GB) is too small |
| EC2 t3.medium (2 vCPU, 4GB RAM) | ~$30/mo | Comfortable headroom for ArgoCD + all services. Recommended |
| Elastic IP | $3.60/mo (when attached) | Required for stable DNS. Free when attached to running instance |
| Domain (Namecheap/Cloudflare) | ~$10/year | One-time annual cost |
| GHCR | $0 | Free for public images |
| Let's Encrypt | $0 | Free TLS certificates |
| GitHub Actions | $0 | Free for public repos, 2000 min/mo for private |
| SMTP (Resend/AWS SES) | $0 | Free tier covers transactional email volume |
| **Total (t3.small)** | **~$19/mo** | |
| **Total (t3.medium)** | **~$34/mo** | Recommended for ArgoCD headroom |

## Alternative: Budget VPS Instead of EC2

| Provider | Specs | Monthly Cost | K8s Ready |
|----------|-------|-------------|-----------|
| Hetzner CAX11 (ARM) | 2 vCPU, 4GB RAM, 40GB | ~$4/mo | Yes, k3s runs on ARM |
| DigitalOcean Basic | 2 vCPU, 2GB RAM, 50GB | ~$12/mo | Yes |
| Vultr Cloud Compute | 2 vCPU, 4GB RAM, 80GB | ~$24/mo | Yes |
| AWS t3.medium | 2 vCPU, 4GB RAM, EBS | ~$34/mo | Yes |

**Recommendation:** Hetzner CAX11 at $4/mo is dramatically cheaper and has more RAM than EC2 t3.small. If the goal is "self-managed K8s as a learning exercise" rather than "must be on AWS specifically," Hetzner is the better value. However, the project specifies AWS EC2, so costs above assume AWS.

## Sources

- [Docker Multi-Stage Builds](https://docs.docker.com/get-started/docker-concepts/building-images/multi-stage-builds/)
- [Next.js Standalone Docker Optimization](https://dev.to/angojay/optimizing-nextjs-docker-images-with-standalone-mode-2nnh)
- [Next.js Deployment Docs](https://nextjs.org/docs/app/getting-started/deploying)
- [K3s Official Site](https://k3s.io/)
- [K3s Documentation](https://docs.k3s.io/)
- [K3s Single Node Production Discussion](https://github.com/k3s-io/k3s/discussions/2988)
- [K3s Production Cluster Setup (Jan 2026)](https://oneuptime.com/blog/post/2026-01-26-k3s-production-cluster/view)
- [K3s Traefik + Gateway API Discussion](https://github.com/k3s-io/k3s/discussions/11100)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/en/stable/)
- [ArgoCD Getting Started](https://argo-cd.readthedocs.io/en/stable/getting_started/)
- [Ingress NGINX Retirement Announcement (Nov 2025)](https://kubernetes.io/blog/2025/11/11/ingress-nginx-retirement/)
- [Kubernetes Gateway API Guide 2026](https://calmops.com/devops/kubernetes-gateway-api-complete-guide-2026/)
- [cert-manager Documentation](https://cert-manager.io/docs/tutorials/getting-started-aws-letsencrypt/)
- [GHCR Documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Spring Boot Actuator Health Probes for K8s (Jan 2026)](https://oneuptime.com/blog/post/2026-01-25-health-probes-kubernetes-spring-boot/view)
- [Spring Boot Production Readiness Checklist 2025](https://medium.com/@khawaraleem/production-ready-spring-boot-a-complete-2025-checklist-for-real-world-microservices-0618738cde01)
- [PostgreSQL Backup on Kubernetes (Crunchy Data)](https://www.crunchydata.com/blog/stateful-postgres-storage-using-kubernetes)
- [Affordable Kubernetes for Personal Projects](https://hexiosec.com/blog/affordable-kubernetes-for-personal-projects/)
- [EKS Pricing Guide](https://www.devzero.io/blog/eks-pricing)

---
*Feature research for: Production Deployment & Infrastructure*
*Researched: 2026-03-22*
