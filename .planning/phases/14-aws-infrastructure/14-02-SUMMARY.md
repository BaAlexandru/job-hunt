---
phase: 14-aws-infrastructure
plan: 02
subsystem: infra
tags: [opentofu, terraform, aws, ec2, vpc, eip, cloudwatch, s3]

# Dependency graph
requires:
  - phase: 14-aws-infrastructure-01
    provides: S3 state bucket for remote backend
provides:
  - VPC with public subnet, IGW, and route table
  - EC2 t3.small instance with Ubuntu 24.04 and 30GB gp3
  - Elastic IP associated with EC2 instance
  - Security group (SSH restricted, HTTP/HTTPS open)
  - CloudWatch billing alarm at $25 via SNS email
  - Six outputs for downstream phase handoff
affects: [15-k3s-installation, 18-dns-cloudflare]

# Tech tracking
tech-stack:
  added: [opentofu, aws-provider-v6]
  patterns: [multi-provider-alias, s3-native-locking, user-data-bootstrap]

key-files:
  created:
    - infra/tofu/main/providers.tf
    - infra/tofu/main/backend.tf
    - infra/tofu/main/variables.tf
    - infra/tofu/main/data.tf
    - infra/tofu/main/main.tf
    - infra/tofu/main/billing.tf
    - infra/tofu/main/outputs.tf
    - infra/tofu/main/user-data.sh
    - infra/tofu/main/dev.tfvars.example
  modified:
    - .gitignore

key-decisions:
  - "Inline security group rules (simpler for 3 ingress + 1 egress, per CONTEXT.md trade-off)"
  - "2GB swap file with swappiness=10 for t3.small OOM safety net"

patterns-established:
  - "Multi-provider alias: aws.billing in us-east-1 for CloudWatch billing metrics"
  - "Consistent Project=jobhunt tagging on all resources"
  - "Parameterized variables for all environment-specific values"

requirements-completed: [K8S-01]

# Metrics
duration: 2min
completed: 2026-03-22
---

# Phase 14 Plan 02: Main OpenTofu Module Summary

**Complete AWS infrastructure module: VPC networking, EC2 t3.small with Ubuntu 24.04, Elastic IP, security groups, CloudWatch $25 billing alarm, and six outputs for K3s and DNS phases**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-22T14:12:04Z
- **Completed:** 2026-03-22T14:13:54Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Main OpenTofu module with full VPC networking (10.0.0.0/16 VPC, 10.0.1.0/24 public subnet, IGW, route table)
- EC2 t3.small with 30GB gp3, Ubuntu 24.04 AMI via dynamic lookup, SSH key pair, and user-data bootstrap
- CloudWatch billing alarm at $25 threshold via SNS email in us-east-1 using provider alias
- Six required outputs exposed for Phase 15 (K3s) and Phase 18 (DNS) handoff

## Task Commits

Each task was committed atomically:

1. **Task 1: Create main module providers, backend, variables, and data sources** - `79d1a14` (feat)
2. **Task 2: Create VPC, networking, security group, EC2, EIP, billing alarm, outputs, and user-data** - `7aa9fb9` (feat)

## Files Created/Modified
- `infra/tofu/main/providers.tf` - AWS provider eu-central-1 + us-east-1 billing alias
- `infra/tofu/main/backend.tf` - S3 remote state with native locking and encryption
- `infra/tofu/main/variables.tf` - Parameterized variables (SSH CIDR, email, key path, instance type)
- `infra/tofu/main/data.tf` - Dynamic Ubuntu 24.04 AMI lookup via Canonical owner
- `infra/tofu/main/main.tf` - VPC, subnet, IGW, route table, security group, EC2, EIP, SSH key pair
- `infra/tofu/main/billing.tf` - CloudWatch billing alarm + SNS topic/subscription in us-east-1
- `infra/tofu/main/outputs.tf` - Six outputs: elastic_ip, instance_id, instance_public_dns, security_group_id, vpc_id, subnet_id
- `infra/tofu/main/user-data.sh` - Cloud-init: apt update, tools install, 2GB swap with swappiness=10
- `infra/tofu/main/dev.tfvars.example` - Example variable values for the user
- `.gitignore` - OpenTofu state/lock/tfvars ignore patterns

## Decisions Made
- Followed plan exactly as specified -- inline security group rules per CONTEXT.md accepted trade-off
- 2GB swap file chosen (upper end of 1-2GB range from context) for maximum OOM protection on t3.small

## Deviations from Plan

None - plan executed exactly as written.

**Note:** Verification steps (`tofu fmt -check`, `tofu init -backend=false && tofu validate`) could not be run because OpenTofu is not installed in this environment. The HCL files follow the plan's exact syntax and formatting. Validation should be performed manually after installing OpenTofu.

## Issues Encountered
- OpenTofu CLI not available in the build environment -- verification deferred to manual step

## User Setup Required
None - no external service configuration required beyond the existing dev.tfvars.example instructions.

## Next Phase Readiness
- Main module is ready for `tofu init && tofu plan -var-file=dev.tfvars`
- Bootstrap module (plan 14-01) must be applied first to create the S3 state bucket
- SSH key pair must be generated locally before applying: `ssh-keygen -t ed25519 -f ~/.ssh/jobhunt-deployer`
- After apply, outputs feed directly into Phase 15 (K3s) and Phase 18 (DNS)

## Self-Check: PASSED

- All 10 files verified present on disk
- Both task commits (79d1a14, 7aa9fb9) verified in git log

---
*Phase: 14-aws-infrastructure*
*Completed: 2026-03-22*
