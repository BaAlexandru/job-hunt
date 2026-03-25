# Requirements: JobHunt v1.1

**Defined:** 2026-03-22
**Core Value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.

## v1.1 Requirements

Requirements for Infrastructure & Deployment milestone. Each maps to roadmap phases.

### Gap Closure

- [ ] **GAP-01**: User can view and add interview notes in the InterviewsTab UI
- [ ] **GAP-02**: User can view document version history and upload new versions in the UI
- [ ] **GAP-03**: User receives password reset email via SMTP when requesting a reset

### Visibility & Sharing

- [x] **VISI-01**: User can set visibility (PRIVATE/PUBLIC/SHARED) on companies and jobs
- [x] **VISI-02**: User can share specific companies or jobs with other users (by email)
- [x] **VISI-03**: User can browse public companies and jobs from other users
- [x] **VISI-04**: User can view items shared with them
- [x] **VISI-05**: Shared users can only VIEW (not edit/delete) — view-only always, no edit permission grant

### Docker & CI

- [ ] **DOCK-01**: Backend produces a multi-stage Docker image (JRE-alpine, <200MB)
- [ ] **DOCK-02**: Frontend produces a multi-stage Docker image (Next.js standalone, <200MB)
- [x] **DOCK-03**: GitHub Actions pipeline builds, tests, and pushes images to GHCR on merge to master
- [x] **DOCK-04**: Container images are scanned for vulnerabilities in CI

### Kubernetes Infrastructure

- [ ] **K8S-01**: EC2 instance (m7i-flex.large) provisioned via OpenTofu with VPC, security groups, Elastic IP
- [x] **K8S-02**: K3s cluster installed and running on EC2 instance
- [x] **K8S-03**: Staging and production namespaces configured with LimitRange (no ResourceQuota — staging protection via replicas=0)
- [x] **K8S-04**: Kustomize base + overlays for staging and production environments
- [x] **K8S-05**: Application pods (backend + frontend) deployed and healthy on K8s

### Data Stores

- [x] **DATA-01**: PostgreSQL deployed as StatefulSet with persistent volume (reclaimPolicy: Retain)
- [x] **DATA-02**: Redis deployed on K8s with persistence
- [x] **DATA-03**: MinIO deployed as StatefulSet with persistent volume
- [x] **DATA-04**: Automated daily pg_dump backup CronJob to S3

### GitOps & ArgoCD

- [x] **ARGO-01**: ArgoCD installed (full install with web UI) on the cluster
- [x] **ARGO-02**: App-of-apps pattern managing all K8s resources
- [x] **ARGO-03**: Sealed Secrets for managing credentials in Git
- [x] **ARGO-04**: Auto-sync enabled -- Git push triggers deployment to staging, manual promote to production

### Domain & TLS

- [ ] **DNS-01**: Cloudflare DNS A record pointing job-hunt.dev to EC2 Elastic IP (proxied)
- [ ] **DNS-02**: Cloudflare SSL/TLS set to Full (Strict) with Origin CA cert on K8s
- [ ] **DNS-03**: HTTPS enforced -- HTTP requests redirect to HTTPS (Cloudflare Always Use HTTPS + HSTS)
- [ ] **DNS-04**: staging.job-hunt.dev subdomain configured for staging namespace
- [x] **DNS-05**: Traefik ingress routes configured for prod and staging hosts

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
| GAP-01 | Phase 10 | Pending |
| GAP-02 | Phase 10 | Pending |
| GAP-03 | Phase 10 | Pending |
| VISI-01 | Phase 11 | Complete |
| VISI-02 | Phase 11 | Complete |
| VISI-03 | Phase 11 | Complete |
| VISI-04 | Phase 11 | Complete |
| VISI-05 | Phase 11 | Complete |
| DOCK-01 | Phase 12 | Pending |
| DOCK-02 | Phase 12 | Pending |
| DOCK-03 | Phase 13 | Complete |
| DOCK-04 | Phase 13 | Complete |
| K8S-01 | Phase 14 | Pending |
| K8S-02 | Phase 15 | Complete |
| K8S-03 | Phase 15 | Complete |
| K8S-04 | Phase 15 | Complete |
| K8S-05 | Phase 17 | Complete |
| DATA-01 | Phase 16 | Complete |
| DATA-02 | Phase 16 | Complete |
| DATA-03 | Phase 16 | Complete |
| DATA-04 | Phase 16 | Complete |
| ARGO-01 | Phase 17 | Complete |
| ARGO-02 | Phase 17 | Complete |
| ARGO-03 | Phase 17 | Complete |
| ARGO-04 | Phase 17 | Complete |
| DNS-01 | Phase 18 | Pending |
| DNS-02 | Phase 18 | Pending |
| DNS-03 | Phase 18 | Pending |
| DNS-04 | Phase 18 | Pending |
| DNS-05 | Phase 18 | Complete |

**Coverage:**
- v1.1 requirements: 30 total (3 GAP + 5 VISI + 4 DOCK + 5 K8S + 4 DATA + 4 ARGO + 5 DNS)
- Mapped to phases: 30
- Unmapped: 0

---
*Requirements defined: 2026-03-22*
*Last updated: 2026-03-22 after Phase 11 context audit — VISI-05 narrowed to view-only (no edit permission grant)*
