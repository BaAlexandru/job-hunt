# Phase 14: AWS Infrastructure - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Provision a running EC2 t3.small instance with networking (VPC, subnet, security groups, Elastic IP) via OpenTofu. The instance should be ready to receive K3s installation in Phase 15. This phase does NOT install K3s or any application software — only the AWS infrastructure layer.

</domain>

<decisions>
## Implementation Decisions

### OpenTofu state backend
- S3 bucket + DynamoDB table for remote state with locking
- Separate bootstrap module (`infra/tofu/bootstrap/`) creates the S3 bucket + DynamoDB table using local state, then the main module (`infra/tofu/main/`) references that backend
- State and all resources in the same region (eu-central-1)
- OpenTofu code lives under `infra/tofu/` — two modules: `bootstrap/` and `main/`

### SSH access policy
- SSH key pair generated locally; public key imported via OpenTofu as `aws_key_pair`
- Security group restricts port 22 to a parameterized CIDR: variable `allowed_ssh_cidr` with user's current IP as default
- Update SSH access via: `tofu apply -var='allowed_ssh_cidr=x.x.x.x/32'`

### Instance bootstrapping
- Ubuntu 24.04 LTS AMI (most common K3s base, wide community support)
- User-data script performs: system updates (`apt update && apt upgrade`), installs basic tools (`curl`, `jq`), creates 1-2GB swap file as OOM safety net
- K3s installation is explicitly Phase 15's responsibility — Phase 14 delivers a bare, updated OS
- 30GB gp3 root volume (matches free tier limit, sufficient for OS + containers + PVCs)

### AWS account & cost
- Account is within free tier (t3.small covered for 750 hrs/mo, 30GB EBS free)
- Region: eu-central-1 (Frankfurt) — closest to user, Cloudflare PoP available
- CloudWatch billing alarm at $10 threshold with SNS email notification — provisioned by OpenTofu
- Expected monthly cost: ~$0 while on free tier

### Security groups
- Inbound: SSH (22) restricted to `allowed_ssh_cidr`, HTTP (80) open, HTTPS (443) open
- Outbound: all traffic allowed
- HTTP/HTTPS open because Cloudflare proxy will route traffic here in Phase 18

### Claude's Discretion
- VPC CIDR block sizing and subnet layout
- Exact user-data script implementation details
- OpenTofu module structure beyond bootstrap/main split
- DynamoDB table configuration (capacity mode, etc.)
- Swap file size (1GB or 2GB) based on research

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Infrastructure research
- `.planning/research/ARCHITECTURE.md` — Full architecture overview, component boundaries, namespace strategy, infra directory layout
- `.planning/research/PITFALLS.md` — Known pitfalls for K3s on EC2, memory constraints, storage risks

### Project decisions
- `.planning/PROJECT.md` §Key Decisions — Infrastructure decisions (K3s over kubeadm, namespace separation, Cloudflare proxy TLS, etc.)
- `.planning/ROADMAP.md` §Memory Budget & Mitigation — t3.small 2GB constraint, staging scale-to-zero strategy, JVM tuning requirements
- `.planning/REQUIREMENTS.md` §K8S-01 — Requirement: EC2 t3.small provisioned via OpenTofu with VPC, security groups, Elastic IP

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `infra/CLAUDE.md` — Existing infrastructure conventions doc (needs update for OpenTofu/K8s)
- `infra/docker/.gitkeep` — Docker directory exists but empty (Phase 12 will populate)

### Established Patterns
- Monorepo structure: `/backend`, `/frontend`, `/infra` — OpenTofu goes under `/infra/tofu/`
- `compose.yaml` at project root for local dev — production infra is a separate concern under `/infra`

### Integration Points
- Elastic IP output from this phase feeds into Phase 18 (Cloudflare DNS A record)
- EC2 instance ID/IP feeds into Phase 15 (K3s installation target)
- Security group IDs may need updating in Phase 15 if K3s requires additional ports (e.g., 6443 for K8s API)

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. The research docs already provide detailed architecture guidance.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 14-aws-infrastructure*
*Context gathered: 2026-03-22*
