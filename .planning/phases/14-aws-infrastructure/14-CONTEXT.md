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
- S3 bucket with native S3 locking (`use_lockfile = true`) for remote state — no DynamoDB table needed
- S3 bucket versioning enabled (required for `use_lockfile` and enables state recovery from accidental corruption)
- State encryption at rest via `encrypt = true` in backend config
- Separate bootstrap module (`infra/tofu/bootstrap/`) creates the S3 bucket (with versioning + encryption) using local state, then the main module (`infra/tofu/main/`) references that backend
- State and all resources in the same region (eu-central-1)
- OpenTofu code lives under `infra/tofu/` — two modules: `bootstrap/` and `main/`

**Note:** OpenTofu's native S3 locking (`use_lockfile = true`) replaces the legacy DynamoDB-based locking. This simplifies the bootstrap module (one fewer resource), reduces IAM permissions needed, and is the forward-looking approach per OpenTofu docs. The lock is implemented via conditional S3 PutObject with `IfNoneMatch`, creating a `.lock` object alongside the state file.

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
- **t3.small is NOT free-tier eligible** — AWS Free Tier covers t2.micro/t3.micro only. Downgrading to t3.micro (1GB RAM) is not viable: K3s alone needs ~350MB, leaving insufficient headroom for the application stack.
- Region: eu-central-1 (Frankfurt) — closest to user, Cloudflare PoP available
- CloudWatch billing alarm at **$25 threshold** with SNS email notification — provisioned by OpenTofu
- Expected monthly cost breakdown:
  - EC2 t3.small: ~$15/mo ($0.0208/hr × 730 hrs in eu-central-1)
  - 30GB gp3 EBS: ~$0 (free tier covers 30GB/mo for 12 months)
  - Elastic IP (attached to running instance): ~$0
  - S3 state bucket: negligible
  - **Total: ~$15/mo**

### Security groups
- Inbound: SSH (22) restricted to `allowed_ssh_cidr`, HTTP (80) open, HTTPS (443) open
- Outbound: all traffic allowed
- HTTP/HTTPS open because Cloudflare proxy will route traffic here in Phase 18

### Accepted trade-offs
- **Inline security group rules**: Plans use inline `ingress`/`egress` blocks inside `aws_security_group` rather than separate `aws_vpc_security_group_ingress_rule`/`egress_rule` resources. AWS Provider v6 recommends the separate-resource pattern, but inline rules are simpler and sufficient for this use case with only 3 ingress + 1 egress rules. The key constraint is consistency — never mix inline and separate rules on the same security group.
- **S3 bucket name collision risk**: The state bucket name `jobhunt-tofu-state` is in the global S3 namespace. If the name is already taken, `tofu apply` will fail with "BucketAlreadyExists". Mitigation: append an account-specific suffix (e.g., `jobhunt-tofu-state-{account-id-fragment}`) if collision occurs. Low risk for a personal project.

### Claude's Discretion
- VPC CIDR block sizing and subnet layout
- Exact user-data script implementation details
- OpenTofu module structure beyond bootstrap/main split
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

### Required OpenTofu Outputs
The main module MUST expose these outputs for downstream phase handoff (v1.0 retrospective lesson: stable contracts reduce integration friction):
- `elastic_ip` — Phase 18 needs this for Cloudflare DNS A record
- `instance_id` — Phase 15 needs this as K3s installation target
- `instance_public_dns` — Convenience for SSH access
- `security_group_id` — Phase 15 may need to add K3s API port (6443)
- `vpc_id` — Future reference for additional networking
- `subnet_id` — Future reference for additional resources

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
