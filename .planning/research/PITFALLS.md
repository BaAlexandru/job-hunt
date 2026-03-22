# Pitfalls Research

**Domain:** Self-managed Kubernetes, ArgoCD GitOps, Production Deployment (adding to existing Spring Boot + Next.js app)
**Researched:** 2026-03-22
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: EC2 Free-Tier Cannot Run Two Kubernetes Clusters

**What goes wrong:**
The project plans "separate staging and production clusters" on AWS free-tier EC2. The free tier provides 750 hours/month of t2.micro or t3.micro -- enough for ONE instance running 24/7. Running two clusters (staging + production) requires at minimum 2-4 instances, immediately exceeding the free tier. A surprise bill of $15-50/month appears within the first billing cycle. Worse, t2.micro (1 vCPU, 1GB RAM) cannot run standard Kubernetes -- the kubelet alone consumes 300-500MB of RAM, leaving almost nothing for workloads.

**Why it happens:**
Developers see "750 free hours" and mentally map it to "multiple free servers." In reality, 750 hours = one instance for 31 days. The second instance starts the billing clock immediately. Additionally, kubeadm requires minimum 2 CPU cores and 2GB RAM per node, which t2.micro does not meet.

**How to avoid:**
- Use K3s instead of kubeadm. K3s runs in 512MB RAM and needs only 1 CPU core. It bundles the control plane, kubelet, and container runtime in a single ~70MB binary.
- Use a SINGLE K3s cluster with namespace-based separation (`staging` and `production` namespaces) instead of two separate clusters. This halves infrastructure cost.
- Use t3.small (2GB RAM, $0.0208/hr, ~$15/month) as the minimum viable instance. A t3.medium (4GB RAM, ~$30/month) is more comfortable for running the full stack (Spring Boot + Next.js + PostgreSQL + Redis + MinIO + ArgoCD).
- Set AWS Budget Alerts at $5, $15, and $30 thresholds. Enable Free Tier Usage Alerts in the billing console.
- Accept that this will NOT be free. Budget $15-40/month for a single-node cluster running both environments via namespaces.

**Warning signs:**
- AWS Budget alert emails
- Pods stuck in `Pending` with "Insufficient memory" or "Insufficient cpu" events
- Node showing `MemoryPressure` or `DiskPressure` conditions
- OOMKilled pods (especially the Spring Boot JVM)

**Phase to address:**
Phase 1 (Infrastructure provisioning). The instance type and cluster architecture decision determines everything downstream. Get this wrong and every subsequent phase fights resource constraints.

---

### Pitfall 2: JVM Memory Defaults OOM-Kill Spring Boot on Resource-Constrained Nodes

**What goes wrong:**
Spring Boot on a JVM defaults to using 25% of the HOST's memory for the heap, not the container's limit. On a 4GB EC2 node, the JVM claims 1GB heap before you even add metaspace, thread stacks, and native memory. With K8s resource limits set to 512MB for the container, the JVM exceeds the cgroup limit and gets OOM-killed. The pod restarts in a loop (`CrashLoopBackOff`), and `kubectl logs` shows nothing useful because the OOM kill happens at the kernel level, not in the JVM.

**Why it happens:**
Modern JVMs (Java 17+) do detect container limits with `UseContainerSupport` (on by default), but the default `MaxRAMPercentage` is 25%, and metaspace + thread stacks + native buffers add 150-300MB on top of the heap. Developers set a K8s memory limit of "512Mi" thinking that is generous, but the JVM needs explicit tuning to fit. Spring Boot 4 with a full stack (JPA, Security, Actuator) has a baseline memory footprint of 300-400MB.

**How to avoid:**
- Set explicit JVM flags in the Dockerfile or K8s deployment: `-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport`
- Set K8s resource requests AND limits: `requests: { memory: "384Mi", cpu: "250m" }`, `limits: { memory: "512Mi", cpu: "1000m" }`
- Use `jre` base images, not `jdk` -- saves ~200MB of image size and memory
- Add `-XX:+ExitOnOutOfMemoryError` so the JVM exits cleanly on OOM instead of hanging
- Test locally with `docker run --memory=512m` before deploying to K8s
- Monitor with Spring Boot Actuator `/actuator/metrics/jvm.memory.used`

**Warning signs:**
- Pods in `CrashLoopBackOff` with exit code 137 (SIGKILL from OOM)
- `kubectl describe pod` shows `OOMKilled` as the last state reason
- Spring Boot starts but crashes under first real request load
- `kubectl top pods` shows memory usage at 100% of limit

**Phase to address:**
Phase 2 (Docker image creation). The Dockerfile and K8s deployment manifests must be tuned together. This is not a "fix later" issue -- it blocks basic deployment.

---

### Pitfall 3: Stateful Data Stores on K8s Without Proper Persistence Strategy

**What goes wrong:**
PostgreSQL, Redis, and MinIO are deployed on K8s with default settings. PostgreSQL data is stored on the pod's ephemeral storage (`emptyDir`) or a misconfigured PersistentVolumeClaim. The node gets rescheduled (EC2 spot termination, maintenance, OOM), the pod moves to a different node, and ALL database data is lost. Even with PVCs, using the wrong StorageClass or reclaim policy means the PV is deleted when the pod is deleted. Redis data loss is expected (it is a cache), but PostgreSQL and MinIO data loss is catastrophic.

**Why it happens:**
K8s abstracts storage so well that developers forget they are running on a single EC2 node with a single EBS volume. They deploy PostgreSQL like a stateless app. The default `reclaimPolicy` for dynamically provisioned PVs is `Delete`, meaning deleting the PVC deletes the data. On a single-node K3s cluster, there is no redundancy -- the node IS the cluster.

**How to avoid:**
- Use `hostPath` volumes for the single-node setup (simplest), or provision a dedicated EBS volume attached to the EC2 instance
- If using K3s local-path provisioner (default), understand that data lives on the node filesystem -- which is fine for single-node but has NO redundancy
- Set `reclaimPolicy: Retain` on any PersistentVolume holding database data
- Deploy PostgreSQL as a StatefulSet, not a Deployment
- Implement automated backups: `pg_dump` cron job writing to S3 (or to a second EBS volume)
- Use `volumeClaimTemplates` in the StatefulSet so PVCs are stable across pod reschedules
- For MinIO: same PVC strategy as PostgreSQL -- it stores uploaded documents
- Redis: configure as cache-only (no persistence), or use `appendonly yes` with a PVC if session data matters
- Budget 30GB EBS for data (free tier covers this). Monitor disk usage.

**Warning signs:**
- PostgreSQL pod restart causes "FATAL: data directory has wrong ownership" or empty database
- Application loses all data after `kubectl delete pod postgres-0`
- PVCs in `Pending` state (no StorageClass configured or disk full)
- `df -h` on the node shows disk nearly full

**Phase to address:**
Phase 3 (Data store deployment on K8s). This must be the phase immediately after the cluster is running and before the application is deployed. Test data persistence by killing the PostgreSQL pod and verifying data survives.

---

### Pitfall 4: Secrets Committed to Git in ArgoCD GitOps Repository

**What goes wrong:**
ArgoCD watches a Git repository for K8s manifests. Database passwords, JWT signing keys, MinIO credentials, and TLS private keys get committed as plain-text K8s `Secret` manifests (base64-encoded, which is NOT encryption). Anyone with repo access can decode them. Worse, Git history is immutable -- even if you delete the secret in a later commit, it remains in the history forever. ArgoCD faithfully applies these plain-text secrets to the cluster.

**Why it happens:**
K8s Secrets are base64-encoded in YAML, which looks "encrypted" to developers unfamiliar with the distinction. GitOps requires ALL cluster state to be in Git, so developers include Secrets alongside Deployments and Services. The ArgoCD tutorials often show Secret manifests in the same directory as other resources.

**How to avoid:**
- Use Sealed Secrets (Bitnami). Install the SealedSecrets controller on the cluster. Use the `kubeseal` CLI to encrypt secrets with the cluster's public key. Commit the `SealedSecret` resource (encrypted) to Git. Only the cluster's controller can decrypt it.
- Workflow: create a regular K8s Secret YAML locally, pipe it through `kubeseal`, commit the output SealedSecret, delete the local plain-text file
- Alternative: use SOPS (Mozilla) with age encryption keys if you prefer file-level encryption
- NEVER commit plain K8s Secret manifests, `.env` files, or private keys to the GitOps repo
- Add `.env`, `*-secret.yaml`, and `*.key` to `.gitignore` in the GitOps repo
- Audit Git history before making the repo public

**Warning signs:**
- `kind: Secret` with `data:` fields in your Git repository
- Base64-encoded values visible in committed YAML files
- `.env` files or `credentials` directories in the GitOps repo
- ArgoCD UI showing Secret resources synced from Git

**Phase to address:**
Phase 4 (ArgoCD setup). Sealed Secrets must be installed BEFORE any application secrets are created. Retrofitting means rotating every credential in the system.

---

### Pitfall 5: ArgoCD Itself Consuming Too Many Resources on a Small Cluster

**What goes wrong:**
ArgoCD consists of 5+ components (server, repo-server, application-controller, Redis, dex, notifications-controller). With default resource requests, ArgoCD alone needs 1-2GB RAM and 500m+ CPU. On a t3.small (2GB RAM) node also running your application stack, there is not enough room for both ArgoCD and the workloads it manages. The cluster becomes unresponsive, ArgoCD reconciliation loops time out, and pods compete for resources.

**Why it happens:**
ArgoCD is designed for production clusters with dedicated infrastructure. Its default resource requests are generous. Developers install it from the official manifests without adjusting resource limits for a constrained environment. The ArgoCD repo-server clones Git repositories and renders Helm/Kustomize templates, which is CPU and memory intensive.

**How to avoid:**
- Reduce ArgoCD resource limits for a single-developer setup: `repo-server: 128Mi-256Mi`, `application-controller: 128Mi-256Mi`, `server: 64Mi-128Mi`
- Use the "core" installation mode (`kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/core-install.yaml`) which omits the UI server, dex, and notifications -- saves ~500MB RAM
- Access ArgoCD via CLI (`argocd app list`) or port-forward the server only when needed
- Alternatively: run ArgoCD on your LOCAL machine or a separate tiny EC2 instance, managing the remote cluster. This keeps the production node free for workloads.
- Set Kubernetes `ResourceQuota` on the `argocd` namespace to cap its total consumption

**Warning signs:**
- `kubectl top nodes` showing 90%+ memory usage
- ArgoCD application controller logs showing "context deadline exceeded" or "OOM" errors
- Pods evicted due to node memory pressure
- ArgoCD UI slow to load or timing out

**Phase to address:**
Phase 4 (ArgoCD installation). Choose the installation mode before running `kubectl apply`. Switching from full install to core install later requires reinstallation.

---

### Pitfall 6: Docker Image Bloat Exhausting Disk and Slowing Deployments

**What goes wrong:**
The Spring Boot image is built with the full JDK (600MB+), includes Gradle caches and build artifacts, and does not use layered JARs. The Next.js image includes the full `node_modules` (500MB+) instead of using standalone output mode. Each image is 800MB-1.2GB. On a t3.small with 20GB EBS, storing 3-4 image versions fills the disk. `docker pull` takes minutes on EC2's limited bandwidth. K3s image garbage collection fails because there is not enough room for new images.

**Why it happens:**
Developers copy their local Dockerfile (single-stage, JDK-based) to production. They do not know about multi-stage builds, JRE-only runtime images, or Next.js standalone mode. The images "work" locally where disk and bandwidth are not constrained.

**How to avoid:**
- Spring Boot Dockerfile: multi-stage build with `eclipse-temurin:24-jdk` for build, `eclipse-temurin:24-jre-alpine` for runtime. Use Spring Boot layered JARs (`java -Djarmode=layertools -jar app.jar extract`) for better Docker layer caching. Final image: ~150-200MB.
- Next.js Dockerfile: multi-stage with `node:22-alpine` for build, `node:22-alpine` for runtime. Set `output: "standalone"` in `next.config.ts`. Copy only `.next/standalone`, `.next/static`, and `public` to the runtime stage. Final image: ~100-150MB.
- Run as non-root user in both images (`USER 1001`)
- Tag images with Git SHA, not `latest`. Prevents stale image caching.
- Configure K3s image garbage collection: `--image-gc-high-threshold=85 --image-gc-low-threshold=80`
- Use a container registry (ECR free tier: 500MB/month for private repos, or GitHub Container Registry: free for public)

**Warning signs:**
- `docker images` showing 800MB+ images
- EBS disk usage above 80%
- `ImagePullBackOff` errors due to disk space
- Slow deployments (minutes to pull images)

**Phase to address:**
Phase 2 (Docker image creation). The Dockerfile quality determines image size, build speed, and deployment speed for the entire project lifetime.

---

### Pitfall 7: Let's Encrypt Rate Limits Hit During TLS Setup Debugging

**What goes wrong:**
cert-manager is configured with a Let's Encrypt production `ClusterIssuer`. The developer makes mistakes (wrong DNS records, ingress misconfiguration, challenge solver issues) and repeatedly deletes and recreates `Certificate` resources to "retry." Each attempt counts against Let's Encrypt rate limits: 50 certificates per registered domain per week. After 5-10 failed attempts with fixes, the rate limit is hit. No new certificates can be issued for that domain for a week. The site is stuck on HTTP.

**Why it happens:**
cert-manager abstracts the ACME challenge process, so developers do not realize each `Certificate` resource creation triggers a real ACME order. Deleting and recreating the Certificate (instead of debugging the existing one) creates new orders. Let's Encrypt rate limits are strict and cannot be bypassed.

**How to avoid:**
- ALWAYS start with Let's Encrypt Staging issuer (`https://acme-staging-v02.api.letsencrypt.org/directory`). Staging has no rate limits. Verify the full flow works (certificate issued, but browser will show "untrusted" -- that is expected).
- Only switch to production issuer after staging succeeds end-to-end
- Debug cert-manager issues with `kubectl describe certificate`, `kubectl describe certificaterequest`, and `kubectl describe order` -- do NOT delete and recreate
- Use DNS-01 challenge (via Cloudflare, Route53, etc.) instead of HTTP-01 if your ingress setup is complex. DNS-01 is more reliable and supports wildcard certificates.
- Consider a wildcard certificate (`*.yourdomain.com`) to cover staging.yourdomain.com and app.yourdomain.com with one cert

**Warning signs:**
- `Certificate` resource stuck in "False" ready state
- cert-manager logs showing "rateLimited" or "too many certificates" errors
- Multiple `Order` and `Challenge` resources in the namespace (each is a failed attempt)
- cert-manager logs showing HTTP-01 challenge failures

**Phase to address:**
Phase 5 (Domain + TLS). Use staging issuer from the start. This is a one-line config difference that prevents a week-long blocker.

---

### Pitfall 8: Environment Configuration Drift Between Staging and Production Namespaces

**What goes wrong:**
With namespace-based environment separation on a single cluster, the staging and production namespaces slowly diverge. A developer applies a quick `kubectl` fix directly to production (bypassing GitOps), changes a ConfigMap in staging but forgets to update production, or ArgoCD syncs staging manifests that were meant for production. The result: "works in staging, broken in production" -- the exact problem separate environments were supposed to prevent.

**Why it happens:**
Namespace separation on one cluster makes it trivially easy to `kubectl apply -n production` directly, bypassing the GitOps workflow. There is no physical barrier (like separate kubeconfig files for separate clusters) to prevent manual changes. Additionally, Kustomize overlays or Helm values files for each environment drift apart as developers add features to staging without promoting to production.

**How to avoid:**
- Use Kustomize overlays with a strict directory structure: `base/` (shared), `overlays/staging/`, `overlays/production/`. All changes go through `base/` with environment-specific overrides only for genuinely different values (replica counts, resource limits, domain names).
- Configure ArgoCD with TWO separate Application resources: one syncing `overlays/staging/` to the `staging` namespace, one syncing `overlays/production/` to the `production` namespace
- Enforce "no manual kubectl" discipline. Use ArgoCD's auto-sync with self-heal enabled for production -- any manual changes are automatically reverted.
- Implement a promotion workflow: merge to `staging` branch triggers staging deploy, PR from `staging` to `main` triggers production deploy
- Keep environment differences MINIMAL: same images, same configs, only resource limits and ingress hostnames differ

**Warning signs:**
- `argocd app diff` showing unexpected differences in production
- ArgoCD showing "OutOfSync" status that nobody caused
- "Works in staging" bugs in production
- Developers using `kubectl edit` or `kubectl apply` directly

**Phase to address:**
Phase 4 (ArgoCD + GitOps setup). The repository structure and ArgoCD application configuration must enforce environment separation from day one.

---

### Pitfall 9: No Backup Strategy Until Data Loss Occurs

**What goes wrong:**
Everything runs fine for weeks. Then: the EBS volume corrupts (rare but real), a bad Flyway migration drops a table, `kubectl delete namespace production` is run by accident, or the EC2 instance is terminated. All PostgreSQL data (applications, companies, interviews) and MinIO files (uploaded CVs, cover letters) are gone. There is no backup. The user has to re-enter everything manually.

**Why it happens:**
Backups are boring and not a feature. A single developer focuses on getting the app running, not on disaster recovery. "I'll set up backups later" becomes "I never set up backups." Single-node K3s with local storage has zero redundancy.

**How to avoid:**
- Set up automated daily `pg_dump` with a CronJob in K8s, writing to S3 (or a separate EBS volume)
- MinIO: use `mc mirror` to sync to S3 or a backup location
- Store backups off-cluster (different EBS volume, or S3 bucket). Backups on the same node as data protect against software failures but not hardware failures.
- Test restores: a backup you have never restored is not a backup
- Budget: S3 Standard is $0.023/GB-month. For a personal job tracker, backups will be under 1GB. Cost: effectively $0.
- Implement backup BEFORE going live with real data

**Warning signs:**
- No CronJob resources in the cluster for backups
- No S3 bucket or off-node storage configured for backups
- Cannot answer "how would I restore if the node died right now?"
- `pg_dump` has never been run or tested

**Phase to address:**
Phase 3 (Data store deployment). Backups must be configured alongside data store deployment, not as an afterthought.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Single-node K3s instead of multi-node HA | Saves $15-30/month, simpler to manage | Zero redundancy; node failure = total downtime | Acceptable for a personal project indefinitely. Add a second node only if uptime matters. |
| Namespace separation instead of separate clusters | Half the infrastructure cost | Staging workloads can starve production of resources; no hard isolation | Acceptable for single developer. Use ResourceQuotas to prevent starvation. |
| Skip monitoring stack (Prometheus/Grafana) | Saves 500MB+ RAM on constrained node | Blind to resource usage, slow leak detection, no alerting | Acceptable for initial deployment. Use `kubectl top` and Actuator metrics instead. Switch to monitoring when stability matters. |
| Manual DNS updates instead of ExternalDNS | One fewer controller on the cluster | DNS records can get out of sync with ingress | Acceptable -- you have one domain, DNS changes are rare. |
| Use Docker Hub instead of private ECR | No ECR cost, simpler image pull config | Docker Hub rate limits (100 pulls/6hr anonymous), public images expose your app code | Never for production images. Use GitHub Container Registry (free) or ECR free tier. |
| Run ArgoCD in core mode (no UI) | Saves 500MB+ RAM | Harder to debug sync issues, no visual overview | Good tradeoff for resource-constrained setups. Use CLI + port-forward when needed. |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Spring Boot + K8s health probes | Using the same endpoint for liveness and readiness | Separate probes: liveness = `/actuator/health/liveness` (is the JVM alive?), readiness = `/actuator/health/readiness` (can it serve traffic? -- checks DB, Redis). A failing DB should make the pod unready, not restart it. |
| Next.js standalone + Docker | Forgetting to copy `public/` and `.next/static` folders | Standalone output excludes static assets by default. Add `COPY --from=builder /app/public ./public` and `COPY --from=builder /app/.next/static ./.next/static` in the Dockerfile. |
| ArgoCD + Sealed Secrets | Installing SealedSecrets controller via ArgoCD before it can decrypt anything | Bootstrap problem: install SealedSecrets controller with `kubectl apply` first, then manage it via ArgoCD afterward. The controller must exist before ArgoCD can apply SealedSecret resources. |
| cert-manager + Ingress | Creating the Certificate resource before DNS propagation completes | DNS must resolve to your EC2 Elastic IP BEFORE cert-manager attempts the ACME challenge. Wait for `dig yourdomain.com` to return the correct IP. |
| K3s + Traefik (built-in ingress) | Fighting Traefik defaults when you wanted Nginx Ingress | K3s ships with Traefik as the default ingress controller. Either use Traefik (simpler for K3s) or disable it at install (`--disable=traefik`) and install Nginx Ingress separately. Do not run both. |
| PostgreSQL StatefulSet + Flyway | Flyway migrations run before PostgreSQL is ready, causing Spring Boot crash loop | Use K8s init containers or Spring Boot's `spring.flyway.connect-retries=10` to wait for PostgreSQL readiness. Add a `depends_on` relationship via K8s startup ordering or init containers. |
| MinIO + K8s persistent volumes | MinIO data directory ownership mismatch after PVC mount | Set `securityContext.runAsUser: 1000` and `fsGroup: 1000` on the MinIO pod to match MinIO's expected UID. Or use an init container to `chown` the mount point. |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| No K8s resource limits on pods | One pod (e.g., Spring Boot during GC) consumes all node memory, evicting other pods | Set memory requests AND limits on every pod. Use LimitRange as a safety net. | Immediately on a 2-4GB node under any real load |
| Using `Recreate` deployment strategy | Downtime during every deployment (old pod killed before new pod starts) | Use `RollingUpdate` with `maxUnavailable: 0, maxSurge: 1` so old pod stays up until new pod is ready. Requires enough spare resources for 2 pods briefly. | Every single deployment |
| ArgoCD reconciliation loop on large manifests | ArgoCD continuously syncs even when nothing changed, consuming CPU | Set `timeout.reconciliation: 180s` (3 min instead of default 3 min -- but verify actual default). Exclude frequently-changing fields from sync with `ignoreDifferences`. | When ArgoCD competes with workloads for CPU |
| Spring Boot JVM cold start in K8s | Pod takes 30-60 seconds to become ready. During rolling update, traffic fails. | Set `readinessProbe.initialDelaySeconds: 30` and `periodSeconds: 5`. Use Spring Boot's graceful shutdown. Consider CDS (Class Data Sharing) to reduce startup time. | Every deployment and pod restart |
| EBS gp2 volume IOPS throttling | PostgreSQL queries slow down during heavy writes. Node feels sluggish. | Use gp3 volumes (3000 baseline IOPS vs gp2's burst model). gp3 is also cheaper than gp2 at same size. | During Flyway migrations, bulk imports, or backup operations |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Kubernetes API server exposed to internet | Attackers can access or compromise the cluster | K3s binds to 0.0.0.0:6443 by default. Restrict with EC2 Security Group: allow port 6443 only from YOUR IP. Use `--tls-san` for valid access. |
| Default ServiceAccount tokens mounted in all pods | Compromised pod can access K8s API | Set `automountServiceAccountToken: false` on pods that do not need K8s API access (all application pods). |
| ArgoCD admin password left as default | Anyone can deploy anything to your cluster | Change the ArgoCD admin password immediately after install. Or use `--insecure` flag and SSO instead. Store the password in a Sealed Secret. |
| EC2 Security Groups too permissive (0.0.0.0/0) | All ports exposed to internet. Database, Redis, MinIO accessible externally. | Only open ports 80, 443, and 6443 (your IP only). All other traffic through K8s networking. Never expose PostgreSQL (5432) or Redis (6379) externally. |
| No network policies in K8s | Any pod can talk to any other pod. Compromised frontend pod can access PostgreSQL directly. | Implement NetworkPolicies: only Spring Boot can reach PostgreSQL and Redis. Only ingress controller can reach Spring Boot and Next.js. |
| Using `latest` image tag | Cannot audit what version is running. ArgoCD cannot detect drift. | Always tag images with Git SHA or semver. `latest` is mutable and breaks reproducibility. |

## "Looks Done But Isn't" Checklist

- [ ] **K8s cluster:** K3s installed and running -- verify: can you `kubectl get nodes` and see the node as `Ready`? Can you deploy a test nginx pod?
- [ ] **Persistent volumes:** PostgreSQL data survives pod restart -- verify: insert test data, `kubectl delete pod postgres-0`, check data still exists after pod restarts
- [ ] **Docker images:** Multi-stage builds produce images under 250MB each -- verify: `docker images | grep jobhunt` shows reasonable sizes
- [ ] **ArgoCD sync:** Changes pushed to Git are automatically deployed -- verify: change a ConfigMap in Git, wait 3 minutes, check it is applied in the cluster
- [ ] **Sealed Secrets:** Secrets are encrypted in Git -- verify: no `kind: Secret` with readable `data:` in the GitOps repo; only `kind: SealedSecret` resources
- [ ] **TLS certificate:** HTTPS works with valid cert -- verify: `curl -v https://yourdomain.com` shows a valid Let's Encrypt certificate chain, not staging
- [ ] **Health probes:** Spring Boot liveness and readiness probes configured -- verify: `kubectl describe pod` shows probes; kill the database and check that the pod becomes unready (not restarted)
- [ ] **Backups:** PostgreSQL backup CronJob runs and produces valid dumps -- verify: `kubectl get cronjob`, check the last job succeeded, download and restore the dump to a test database
- [ ] **Resource limits:** Every pod has memory requests and limits -- verify: `kubectl describe node` shows allocated resources; no pod is missing limits
- [ ] **DNS + domain:** Domain resolves to EC2 Elastic IP -- verify: `dig yourdomain.com` returns your IP; `curl http://yourdomain.com` returns your app
- [ ] **Rollback works:** ArgoCD can roll back to a previous version -- verify: deploy a broken image, then roll back via ArgoCD CLI or Git revert

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| EC2 bill surprise | LOW | Set budget alerts. Downgrade to single namespace. Consider Hetzner/DigitalOcean ($5/month VPS) as alternative. |
| JVM OOM crashes | LOW | Add JVM flags to Dockerfile, redeploy. No data impact. 30-minute fix. |
| Data loss (no backups) | CATASTROPHIC | Manual re-entry of all data. If EBS volume is intact, mount it to new instance and recover. If not, data is gone. |
| Secrets in Git history | HIGH | Rotate ALL credentials (DB passwords, JWT keys, MinIO keys). Rewrite Git history with `git filter-repo` or create a new repo. Revoke old credentials. |
| Let's Encrypt rate limit hit | MEDIUM | Wait up to 7 days, or use a different domain/subdomain. During the wait, use staging cert (users see browser warning) or no TLS. |
| ArgoCD resource starvation | LOW | Switch to core install mode. Reduce resource limits. Or run ArgoCD externally. 1-hour fix. |
| Environment drift | MEDIUM | Audit both namespaces with `argocd app diff`. Reset production from Git source of truth. Enable ArgoCD self-heal to prevent recurrence. |
| Docker image bloat filling disk | LOW | Rebuild with multi-stage Dockerfile. Run `crictl rmi --prune` on the node to reclaim space. 1-hour fix. |
| PostgreSQL pod cannot start after node reboot | MEDIUM | Check PVC status. Fix volume permissions with init container. Restore from backup if data corrupted. |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| EC2 free-tier limits | Phase 1: Infrastructure Provisioning | Budget alerts configured; `kubectl top nodes` shows acceptable resource usage |
| JVM OOM on K8s | Phase 2: Docker Images | Spring Boot pod runs 24+ hours without OOMKilled; exit code 137 never appears |
| Stateful data persistence | Phase 3: Data Store Deployment | `kubectl delete pod postgres-0` and data survives; backup CronJob produces valid dumps |
| Secrets in Git | Phase 4: ArgoCD + GitOps | `grep -r "kind: Secret" gitops-repo/` returns zero results; only SealedSecrets committed |
| ArgoCD resource usage | Phase 4: ArgoCD Installation | `kubectl top pods -n argocd` shows total under 512MB; node has headroom for workloads |
| Docker image bloat | Phase 2: Docker Images | Both images under 250MB; `df -h` on node shows 50%+ disk free after all images pulled |
| Let's Encrypt rate limits | Phase 5: Domain + TLS | Staging cert issued successfully before switching to production issuer |
| Environment config drift | Phase 4: GitOps Repository Structure | Kustomize overlays for staging/production; ArgoCD self-heal enabled on production |
| No backup strategy | Phase 3: Data Store Deployment | CronJob runs daily; backup restored to test database successfully |
| K8s API exposed | Phase 1: EC2 + Security Groups | Port scan from external IP shows only 80 and 443 open |

## Sources

- [AWS Free Tier Limits](https://www.cloudoptimo.com/blog/aws-free-tier-isnt-unlimited-know-the-limits-before-you-get-billed/) -- free tier compute and storage caps (MEDIUM confidence)
- [AWS Free Tier Effective Use](https://oneuptime.com/blog/post/2026-02-12-use-aws-free-tier-effectively/view) -- 750 hours explanation (MEDIUM confidence)
- [Hidden Costs of AWS Free Tier](https://medium.com/@pranavpurohit73/the-hidden-costs-of-awss-free-tier-no-one-talks-about-80be49189c55) -- NAT Gateway, EBS, Elastic IP costs (MEDIUM confidence)
- [AWS NAT Gateway Pricing 2025](https://clustercost.com/blog/aws-nat-gateway-pricing-2025/) -- $0.045/hr per NAT Gateway (HIGH confidence)
- [K3s vs Kubeadm Comparison](https://medium.com/@PlanB./k3s-kubeadm-and-beyond-choosing-the-right-tools-to-build-your-kubernetes-environment-f7aca8002d76) -- resource requirements and use cases (MEDIUM confidence)
- [K3s Resource Usage Benchmarks](https://www.siderolabs.com/blog/which-kubernetes-is-the-smallest/) -- K3s memory and CPU overhead (HIGH confidence)
- [K3s vs K8s Guide](https://spacelift.io/blog/k3s-vs-k8s) -- K3s minimum requirements 512MB RAM (HIGH confidence)
- [Top 30 Argo CD Anti-Patterns](https://codefresh.io/blog/argo-cd-anti-patterns-for-gitops/) -- comprehensive anti-pattern catalog (HIGH confidence)
- [ArgoCD Best Practices](https://argo-cd.readthedocs.io/en/stable/user-guide/best_practices/) -- official best practices including repo separation (HIGH confidence)
- [12 GitOps Deployment Mistakes](https://www.devopstraininginstitute.com/blog/12-gitops-deployment-mistakes-to-avoid) -- common GitOps mistakes (MEDIUM confidence)
- [PostgreSQL on Kubernetes Guide](https://www.percona.com/blog/run-postgresql-on-kubernetes-a-practical-guide-with-benchmarks-best-practices/) -- StatefulSet and PVC best practices (HIGH confidence)
- [Stateful Postgres on K8s](https://www.crunchydata.com/blog/stateful-postgres-storage-using-kubernetes) -- storage configuration details (HIGH confidence)
- [Kubernetes Secrets Management 2025](https://infisical.com/blog/kubernetes-secrets-management-2025) -- Sealed Secrets vs External Secrets comparison (MEDIUM confidence)
- [Sealed Secrets GitHub](https://github.com/bitnami-labs/sealed-secrets) -- official documentation (HIGH confidence)
- [Dockerize Spring Boot Best Practices](https://javascript.plainenglish.io/dockerize-spring-boot-like-a-pro-2025-best-practices-for-blazing-fast-deployments-1cd4d00fa229) -- multi-stage builds, JVM flags (MEDIUM confidence)
- [Docker Best Practices for Java](https://www.javaguides.net/2025/02/docker-best-practices-for-java.html) -- MaxRAMPercentage, container support (MEDIUM confidence)
- [Next.js Standalone Output Docs](https://nextjs.org/docs/pages/api-reference/config/next-config-js/output) -- standalone mode documentation (HIGH confidence)
- [Optimizing Next.js Docker with Standalone](https://dev.to/angojay/optimizing-nextjs-docker-images-with-standalone-mode-2nnh) -- public/static folder gotcha (MEDIUM confidence)
- [Kubernetes Configuration Drift](https://www.spectrocloud.com/blog/kubernetes-configuration-drift-what-it-is-how-to-stop-it) -- drift causes and prevention (MEDIUM confidence)
- [Configuration Drift Prevention](https://komodor.com/learn/kubernetes-configuration-drift-causes-detection-and-prevention/) -- GitOps as drift prevention (MEDIUM confidence)
- [cert-manager with Let's Encrypt](https://www.thedougie.com/2025/11/01/kubernetes-cert-manager-cloudflare-lets-encrypt/) -- cert-manager + Cloudflare setup (MEDIUM confidence)
- [Let's Encrypt Rate Limits](https://community.letsencrypt.org/t/help-understanding-rate-limiting/208051) -- 50 certs/domain/week limit (HIGH confidence)
- [K3s with Let's Encrypt](https://www.funkysi1701.com/posts/2025/kubernetes-and-letsencrypt/) -- staging-first approach (MEDIUM confidence)

---
*Pitfalls research for: Self-managed K8s + ArgoCD + Production Deployment (v1.1 milestone)*
*Researched: 2026-03-22*
