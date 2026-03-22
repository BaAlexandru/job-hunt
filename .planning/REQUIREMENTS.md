# Requirements: JobHunt v1.1

**Defined:** 2026-03-22
**Core Value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.

## v1.1 Requirements

Requirements for Infrastructure & Deployment milestone. Each maps to roadmap phases.

### Gap Closure

- [ ] **GAP-01**: User can view and add interview notes in the InterviewsTab UI
- [ ] **GAP-02**: User can view document version history and upload new versions in the UI
- [ ] **GAP-03**: User receives password reset email via SMTP when requesting a reset
- [ ] **GAP-04**: User can set visibility (private/public/shared) on companies and jobs

### Docker & CI

- [ ] **DOCK-01**: Backend produces a multi-stage Docker image (JRE-alpine, <200MB)
- [ ] **DOCK-02**: Frontend produces a multi-stage Docker image (Next.js standalone, <200MB)
- [ ] **DOCK-03**: GitHub Actions pipeline builds, tests, and pushes images to GHCR on merge to master
- [ ] **DOCK-04**: Container images are scanned for vulnerabilities in CI

### Kubernetes Infrastructure

- [ ] **K8S-01**: EC2 instance (t3.small) provisioned via OpenTofu with VPC, security groups, Elastic IP
- [ ] **K8S-02**: K3s cluster installed and running on EC2 instance
- [ ] **K8S-03**: Staging and production namespaces configured with resource quotas
- [ ] **K8S-04**: Kustomize base + overlays for staging and production environments
- [ ] **K8S-05**: Application pods (backend + frontend) deployed and healthy on K8s

### Data Stores

- [ ] **DATA-01**: PostgreSQL deployed as StatefulSet with persistent volume (reclaimPolicy: Retain)
- [ ] **DATA-02**: Redis deployed on K8s with persistence
- [ ] **DATA-03**: MinIO deployed as StatefulSet with persistent volume
- [ ] **DATA-04**: Automated daily pg_dump backup CronJob to S3

### GitOps & ArgoCD

- [ ] **ARGO-01**: ArgoCD installed in core-mode on the cluster
- [ ] **ARGO-02**: App-of-apps pattern managing all K8s resources
- [ ] **ARGO-03**: Sealed Secrets for managing credentials in Git
- [ ] **ARGO-04**: Auto-sync enabled -- Git push triggers deployment to staging, manual promote to production

### Domain & TLS

- [ ] **DNS-01**: Cloudflare DNS A record pointing job-hunt.dev to EC2 Elastic IP (proxied)
- [ ] **DNS-02**: Cloudflare SSL/TLS set to Full (Strict) with Origin CA cert on K8s
- [ ] **DNS-03**: HTTPS enforced -- HTTP requests redirect to HTTPS (Cloudflare Always Use HTTPS + HSTS)
- [ ] **DNS-04**: staging.job-hunt.dev subdomain configured for staging namespace
- [ ] **DNS-05**: Traefik ingress routes configured for prod and staging hosts

## Future Requirements

Deferred to future milestones. Tracked but not in current roadmap.

### AI Features (v3)

- **AI-01**: Analyze job description + CV and suggest adjustments
- **AI-02**: Generate or improve cover letters tailored to specific jobs

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Separate K8s clusters for staging/prod | Research shows single cluster with namespaces is sufficient and much cheaper on free-tier EC2 |
| Full monitoring stack (Prometheus/Grafana) | Too resource-heavy for 2GB instance; Spring Boot Actuator + kubectl logs sufficient |
| AWS EKS managed Kubernetes | Control plane costs ~$73/mo; self-managed K3s is free |
| NGINX Ingress Controller | Being retired March 2026; Traefik bundled with K3s is the replacement |
| Managed RDS for PostgreSQL | Adds cost; PostgreSQL StatefulSet with backups is sufficient for single-user |
| Domain registration | Already own job-hunt.dev on Cloudflare |
| Let's Encrypt certificates | Cloudflare proxy handles TLS termination; Origin CA cert for backend encryption |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| GAP-01 | TBD | Pending |
| GAP-02 | TBD | Pending |
| GAP-03 | TBD | Pending |
| GAP-04 | TBD | Pending |
| DOCK-01 | TBD | Pending |
| DOCK-02 | TBD | Pending |
| DOCK-03 | TBD | Pending |
| DOCK-04 | TBD | Pending |
| K8S-01 | TBD | Pending |
| K8S-02 | TBD | Pending |
| K8S-03 | TBD | Pending |
| K8S-04 | TBD | Pending |
| K8S-05 | TBD | Pending |
| DATA-01 | TBD | Pending |
| DATA-02 | TBD | Pending |
| DATA-03 | TBD | Pending |
| DATA-04 | TBD | Pending |
| ARGO-01 | TBD | Pending |
| ARGO-02 | TBD | Pending |
| ARGO-03 | TBD | Pending |
| ARGO-04 | TBD | Pending |
| DNS-01 | TBD | Pending |
| DNS-02 | TBD | Pending |
| DNS-03 | TBD | Pending |
| DNS-04 | TBD | Pending |
| DNS-05 | TBD | Pending |

**Coverage:**
- v1.1 requirements: 26 total
- Mapped to phases: 0
- Unmapped: 26

---
*Requirements defined: 2026-03-22*
*Last updated: 2026-03-22 after initial definition*
