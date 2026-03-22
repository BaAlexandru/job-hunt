# Roadmap: JobHunt

## Milestones

- ✅ **v1.0 MVP** — Phases 1-9 (shipped 2026-03-22)
- **v1.1 Infrastructure & Deployment** — Phases 10-17 (in progress)

## Phases

<details>
<summary>v1.0 MVP (Phases 1-9) — SHIPPED 2026-03-22</summary>

- [x] Phase 1: Foundation & Infrastructure (2/2 plans) — completed 2026-03-19
- [x] Phase 2: Authentication (3/3 plans) — completed 2026-03-20
- [x] Phase 3: Company & Job Domain (3/3 plans) — completed 2026-03-20
- [x] Phase 4: Application Tracking (2/2 plans) — completed 2026-03-20
- [x] Phase 5: Interview Management (2/2 plans) — completed 2026-03-20
- [x] Phase 6: Document Management (2/2 plans) — completed 2026-03-21
- [x] Phase 7: Frontend Shell & Auth UI (3/3 plans) — completed 2026-03-20
- [x] Phase 8: Frontend Core Views (11/11 plans) — completed 2026-03-21
- [x] Phase 9: Frontend Integration Polish (2/2 plans) — completed 2026-03-22

Full details: `.planning/milestones/v1.0-ROADMAP.md`

</details>

### v1.1 Infrastructure & Deployment

**Milestone Goal:** Close v1.0 UI gaps, then build a full deployment pipeline with K3s on AWS EC2, ArgoCD GitOps, namespace-based staging/prod, and go live at job-hunt.dev.

- [ ] **Phase 10: Gap Closure** - Complete missing UI features and email delivery from v1.0
- [ ] **Phase 11: Production Docker Images** - Multi-stage Dockerfiles for backend and frontend
- [ ] **Phase 12: CI Pipeline** - GitHub Actions builds, tests, scans, and pushes images to GHCR
- [ ] **Phase 13: AWS Infrastructure** - EC2 instance provisioned via OpenTofu with VPC and networking
- [ ] **Phase 14: K3s Cluster Setup** - Kubernetes cluster with namespace separation and Kustomize manifests
- [ ] **Phase 15: Data Stores on K8s** - PostgreSQL, Redis, MinIO deployed with persistence and backups
- [ ] **Phase 16: App Deployment & ArgoCD** - Application pods running, GitOps pipeline managing all resources
- [ ] **Phase 17: Domain & TLS** - job-hunt.dev live with Cloudflare proxy, HTTPS, and staging subdomain

## Phase Details

### Phase 10: Gap Closure
**Goal**: Users have access to all features that were backend-complete but missing UI in v1.0
**Depends on**: Nothing (independent of infrastructure)
**Requirements**: GAP-01, GAP-02, GAP-03, GAP-04
**Success Criteria** (what must be TRUE):
  1. User can view existing interview notes and add new notes per interview round in the InterviewsTab
  2. User can see document version history and upload a new version of an existing document
  3. User receives a password reset email at their registered address when requesting a reset
  4. User can set visibility (private/public/shared) on companies and jobs, and visibility is enforced on API responses
**Plans**: TBD

### Phase 11: Production Docker Images
**Goal**: Both backend and frontend produce optimized, production-ready container images
**Depends on**: Nothing (can run in parallel with Phase 10)
**Requirements**: DOCK-01, DOCK-02
**Success Criteria** (what must be TRUE):
  1. Backend Docker image builds successfully with multi-stage build and is under 200MB
  2. Frontend Docker image builds successfully with Next.js standalone output and is under 200MB
  3. Both images start and serve traffic correctly when run with `docker run`
**Plans**: TBD

### Phase 12: CI Pipeline
**Goal**: Every merge to master automatically builds, tests, scans, and publishes container images
**Depends on**: Phase 11 (needs Dockerfiles to build)
**Requirements**: DOCK-03, DOCK-04
**Success Criteria** (what must be TRUE):
  1. Merging a PR to master triggers a GitHub Actions workflow that builds and pushes images to ghcr.io
  2. CI pipeline runs backend tests and fails the build on test failure
  3. Container images are scanned for vulnerabilities and results are visible in the workflow summary
**Plans**: TBD

### Phase 13: AWS Infrastructure
**Goal**: A running EC2 instance with networking ready to host K3s
**Depends on**: Nothing (can run in parallel with Phases 10-12)
**Requirements**: K8S-01
**Success Criteria** (what must be TRUE):
  1. EC2 t3.small instance is running with an Elastic IP, provisioned by OpenTofu
  2. VPC, subnet, and security groups allow SSH (restricted), HTTP (80), and HTTPS (443)
  3. OpenTofu state is stored remotely and `tofu plan` shows no drift
**Plans**: TBD

### Phase 14: K3s Cluster Setup
**Goal**: A functioning Kubernetes cluster with namespace separation and deployment manifests ready
**Depends on**: Phase 13 (needs EC2 instance)
**Requirements**: K8S-02, K8S-03, K8S-04
**Success Criteria** (what must be TRUE):
  1. K3s is installed and `kubectl get nodes` shows the node as Ready
  2. Staging and production namespaces exist with resource quotas applied
  3. Kustomize base + overlays generate valid manifests for both staging and production environments
**Plans**: TBD

### Phase 15: Data Stores on K8s
**Goal**: All persistent data services are running on K8s with data that survives pod restarts
**Depends on**: Phase 14 (needs running cluster with namespaces)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04
**Success Criteria** (what must be TRUE):
  1. PostgreSQL StatefulSet is running and data persists across pod restarts (reclaimPolicy: Retain)
  2. Redis is running on K8s with persistence enabled
  3. MinIO StatefulSet is running with persistent volume and accessible via S3 API
  4. Automated daily pg_dump CronJob runs and stores backups successfully
**Plans**: TBD

### Phase 16: App Deployment & ArgoCD
**Goal**: Backend and frontend pods are running on K8s, managed by ArgoCD GitOps pipeline
**Depends on**: Phase 12 (needs CI pushing images), Phase 15 (needs data stores running)
**Requirements**: K8S-05, ARGO-01, ARGO-02, ARGO-03, ARGO-04
**Success Criteria** (what must be TRUE):
  1. Backend and frontend pods are running and healthy in both staging and production namespaces
  2. ArgoCD (core-mode) is installed and managing all K8s resources via app-of-apps pattern
  3. Credentials are managed via Sealed Secrets and stored encrypted in Git
  4. Pushing to master auto-syncs to staging; production requires manual promotion in ArgoCD
**Plans**: TBD

### Phase 17: Domain & TLS
**Goal**: The application is publicly accessible at job-hunt.dev with HTTPS and staging subdomain
**Depends on**: Phase 16 (needs app running on K8s)
**Requirements**: DNS-01, DNS-02, DNS-03, DNS-04, DNS-05
**Success Criteria** (what must be TRUE):
  1. Visiting https://job-hunt.dev loads the production frontend and API is reachable
  2. Visiting https://staging.job-hunt.dev loads the staging environment
  3. HTTP requests are redirected to HTTPS (Cloudflare Always Use HTTPS + HSTS enabled)
  4. Cloudflare SSL/TLS is set to Full (Strict) with Origin CA cert installed on the cluster
  5. Traefik ingress routes correctly direct traffic to the appropriate namespace based on hostname
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation & Infrastructure | v1.0 | 2/2 | Complete | 2026-03-19 |
| 2. Authentication | v1.0 | 3/3 | Complete | 2026-03-20 |
| 3. Company & Job Domain | v1.0 | 3/3 | Complete | 2026-03-20 |
| 4. Application Tracking | v1.0 | 2/2 | Complete | 2026-03-20 |
| 5. Interview Management | v1.0 | 2/2 | Complete | 2026-03-20 |
| 6. Document Management | v1.0 | 2/2 | Complete | 2026-03-21 |
| 7. Frontend Shell & Auth UI | v1.0 | 3/3 | Complete | 2026-03-20 |
| 8. Frontend Core Views | v1.0 | 11/11 | Complete | 2026-03-21 |
| 9. Frontend Integration Polish | v1.0 | 2/2 | Complete | 2026-03-22 |
| 10. Gap Closure | v1.1 | 0/? | Not started | - |
| 11. Production Docker Images | v1.1 | 0/? | Not started | - |
| 12. CI Pipeline | v1.1 | 0/? | Not started | - |
| 13. AWS Infrastructure | v1.1 | 0/? | Not started | - |
| 14. K3s Cluster Setup | v1.1 | 0/? | Not started | - |
| 15. Data Stores on K8s | v1.1 | 0/? | Not started | - |
| 16. App Deployment & ArgoCD | v1.1 | 0/? | Not started | - |
| 17. Domain & TLS | v1.1 | 0/? | Not started | - |

## Deferred

- **AI Features (v3)** — CV analysis, cover letter generation via Spring AI
