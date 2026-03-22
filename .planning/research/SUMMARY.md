# Research Summary: JobHunt v1.1 Infrastructure & Deployment

**Domain:** Self-managed Kubernetes deployment for existing full-stack web application
**Researched:** 2026-03-22
**Overall confidence:** HIGH

## Executive Summary

The v1.1 milestone takes a working Spring Boot 4.0.4 + Next.js 16.2 job tracker application from local Docker Compose development to production on self-managed Kubernetes. The infrastructure stack centers on K3s (v1.35.2) as the lightweight Kubernetes distribution running on AWS EC2, with ArgoCD (v3.3.3) providing GitOps continuous deployment, cert-manager (v1.20.0) handling automated TLS from Let's Encrypt, and GitHub Actions + GitHub Container Registry forming the CI pipeline. OpenTofu manages AWS infrastructure as code.

The critical constraint is cost versus resources. The project specifies AWS EC2 and self-managed K8s, which means the free tier (t3.micro, 1GB RAM) is insufficient for production workloads. A realistic budget is $20-30/month for a t3.small production node plus a t3.micro staging node. The memory budget on t3.small (2GB) is tight: K3s system overhead (~350MB) + ArgoCD (~300MB) + Spring Boot JVM (~384MB) + PostgreSQL (~256MB) + Next.js (~128MB) + Redis (~64MB) + MinIO (~128MB) totals ~1,610MB, leaving ~400MB headroom. This works but requires explicit JVM memory tuning (`-XX:MaxRAMPercentage=75.0`) and K8s resource limits on every pod.

The build order is dependency-driven: Docker images first (everything else needs containerized builds), then EC2 provisioning via OpenTofu, then K8s manifests and data store deployment, then ArgoCD for GitOps, then domain + TLS as the last mile. Gap closure features (interview notes UI, document version UI, password reset email) are independent of infrastructure and should be done first or in parallel.

The largest risks are JVM OOM-kills on constrained nodes (Pitfall 2), data loss from PostgreSQL/MinIO without backup strategy (Pitfall 9), and Let's Encrypt rate limits during TLS debugging (Pitfall 7). All three have well-documented prevention strategies detailed in PITFALLS.md.

## Key Findings

**Stack:** K3s v1.35.2 on EC2 t3.small, ArgoCD v3.3.3, cert-manager v1.20.0, Traefik (K3s bundled), OpenTofu for IaC, Kustomize for K8s manifests, ghcr.io for images, Resend for SMTP email, GitHub Actions for CI.

**Architecture:** Single-node K3s per environment (staging on t3.micro, production on t3.small). ArgoCD on production cluster managing both environments. Kustomize base + overlays for staging/production configuration. Sealed Secrets for credentials in Git.

**Critical pitfall:** JVM memory defaults will OOM-kill Spring Boot on K8s unless explicitly tuned with `-XX:MaxRAMPercentage=75.0` and container memory limits. This blocks basic deployment and must be addressed in the Dockerfile phase.

## Implications for Roadmap

Based on research, suggested phase structure:

1. **v1.0 Gap Closure** - Close interview notes UI, document version UI, password reset email
   - Addresses: 3 known v1.0 gaps that are independent of infrastructure
   - Avoids: Mixing feature work with infrastructure complexity
   - Rationale: These are LOW complexity frontend tasks. Do them first while the dev environment is stable.

2. **Production Docker Images** - Multi-stage Dockerfiles for backend (JRE-alpine) and frontend (standalone)
   - Addresses: Containerized builds required by everything downstream
   - Avoids: Image bloat pitfall (>800MB images filling disk)
   - Rationale: Docker images gate CI, K8s deployment, and ArgoCD. Nothing else works without them.

3. **AWS Infrastructure Provisioning** - EC2 instances, VPC, security groups, EBS volumes via OpenTofu
   - Addresses: The physical compute layer K8s runs on
   - Avoids: EC2 free-tier misconception (budget for t3.small)
   - Rationale: Need running instances before installing K3s.

4. **K8s Cluster + Data Stores** - K3s installation, PostgreSQL/Redis/MinIO StatefulSets, PVCs, backup CronJobs
   - Addresses: Running cluster with persistent data stores
   - Avoids: Data loss from missing PVC config and backup strategy
   - Rationale: Data stores must be proven persistent before deploying the application.

5. **CI/CD Pipeline** - GitHub Actions for image builds, ArgoCD for GitOps deployment, Sealed Secrets
   - Addresses: Automated build-test-push-deploy workflow
   - Avoids: Secrets in Git, ArgoCD resource exhaustion on small nodes
   - Rationale: With cluster and data stores running, set up the automation layer.

6. **Domain + TLS + Go Live** - Domain registration, DNS, cert-manager, Let's Encrypt, Traefik ingress
   - Addresses: HTTPS access on a custom domain
   - Avoids: Let's Encrypt rate limits (use staging issuer first)
   - Rationale: Last mile -- everything else must work before adding the public-facing layer.

**Phase ordering rationale:**
- Gap closure before infrastructure because they are independent and quick
- Docker images before anything else in infrastructure because they gate CI and K8s
- EC2 provisioning before K3s installation (need the compute)
- Data stores before application deployment (need persistence verified)
- ArgoCD after basic cluster works (can use kubectl initially, then automate)
- Domain + TLS last because you need the EC2 elastic IP first and everything else running

**Research flags for phases:**
- Phase 4 (K8s + Data Stores): Needs careful memory budgeting. Test JVM under resource limits.
- Phase 5 (ArgoCD): Consider core-mode install to save 500MB+ RAM. Resource-constrained deployment.
- Phase 6 (TLS): MUST use Let's Encrypt staging issuer first to avoid rate limits.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All versions verified against GitHub releases and official docs. K3s v1.35.2, ArgoCD v3.3.3, cert-manager v1.20.0 confirmed current as of 2026-03-22. |
| Features | HIGH | Infrastructure features are well-defined. Gap closure features are small, isolated tasks. |
| Architecture | HIGH | Single-node K3s + Kustomize overlays is a well-documented pattern. Memory budget calculated from known component requirements. |
| Pitfalls | HIGH | All pitfalls sourced from official documentation, community experience, and known K8s operational patterns. JVM memory, data persistence, and Let's Encrypt rate limits are all commonly encountered. |

## Gaps to Address

- **t3.small memory pressure under real load:** The 2GB memory budget is calculated from component minimums. Real-world Spring Boot memory varies with request volume and JPA entity cache size. May need t3.medium ($30/mo) -- monitor after first deployment.
- **Spot instance viability for production:** Spot saves ~65% but instances can be terminated with 2-minute warning. Acceptable for a job tracker (brief downtime is fine), but needs graceful shutdown configuration.
- **Sealed Secrets vs SOPS:** Research recommends Sealed Secrets for simplicity. SOPS with age keys is an alternative if the cluster-bound encryption model of Sealed Secrets is too limiting. Decide during ArgoCD phase.
- **Gateway API vs Ingress resource:** K3s bundles Traefik which supports both. FEATURES.md mentions Gateway API but standard Ingress resource is simpler and sufficient. Decide during ingress phase.
- **Separate clusters vs namespace separation:** PROJECT.md says "two separate clusters" but FEATURES.md anti-features section flags this as potentially wasteful. Single cluster with namespaces is cheaper and simpler. Decision needed during planning.

## Sources

All sources documented in individual research files:
- STACK.md: 15 sources covering tool versions and compatibility
- FEATURES.md: 20 sources covering infrastructure patterns and cost analysis
- ARCHITECTURE.md: 10 sources covering system design patterns
- PITFALLS.md: 20 sources covering operational risks and prevention strategies

---
*Research completed: 2026-03-22*
*Ready for roadmap: yes*
