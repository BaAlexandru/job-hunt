# Phase 14: AWS Infrastructure - Research

**Researched:** 2026-03-22
**Domain:** AWS EC2 provisioning with OpenTofu (VPC, networking, Elastic IP, remote state)
**Confidence:** HIGH

## Summary

Phase 14 provisions a single EC2 t3.small instance in eu-central-1 with full networking (VPC, public subnet, internet gateway, security groups, Elastic IP) using OpenTofu. The infrastructure is split into two OpenTofu modules: a `bootstrap/` module that creates the S3 state bucket with local state, and a `main/` module that provisions all AWS resources using the S3 backend for remote state with native S3 locking (`use_lockfile = true`).

The key technical decisions are already locked in CONTEXT.md. Research confirms that OpenTofu 1.11.x supports native S3 locking (available since 1.10.0), the AWS provider v6.x is current and stable, and Ubuntu 24.04 "Noble" AMIs use the `hvm-ssd-gp3` storage type with Canonical owner ID `099720109477`. A CloudWatch billing alarm at $25 requires a provider alias for `us-east-1` since billing metrics are only available in that region.

**Primary recommendation:** Follow the two-module bootstrap pattern exactly as specified in CONTEXT.md. Use variables for all environment-specific values (SSH CIDR, alert email, region) and expose the six required outputs for downstream phase handoff.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- S3 bucket with native S3 locking (`use_lockfile = true`) for remote state -- no DynamoDB table needed
- S3 bucket versioning enabled (required for `use_lockfile` and enables state recovery)
- State encryption at rest via `encrypt = true` in backend config
- Separate bootstrap module (`infra/tofu/bootstrap/`) creates S3 bucket with local state, main module (`infra/tofu/main/`) references that backend
- State and all resources in same region (eu-central-1)
- OpenTofu code lives under `infra/tofu/` -- two modules: `bootstrap/` and `main/`
- SSH key pair generated locally; public key imported via OpenTofu as `aws_key_pair`
- Security group restricts port 22 to parameterized CIDR variable `allowed_ssh_cidr`
- Ubuntu 24.04 LTS AMI
- User-data script: system updates, basic tools (curl, jq), swap file
- K3s installation is Phase 15's responsibility
- 30GB gp3 root volume
- t3.small instance (NOT free-tier eligible, ~$15/mo)
- Region: eu-central-1 (Frankfurt)
- CloudWatch billing alarm at $25 threshold with SNS email notification
- Inbound: SSH (22) restricted, HTTP (80) open, HTTPS (443) open
- Outbound: all traffic allowed
- Required outputs: elastic_ip, instance_id, instance_public_dns, security_group_id, vpc_id, subnet_id

### Claude's Discretion
- VPC CIDR block sizing and subnet layout
- Exact user-data script implementation details
- OpenTofu module structure beyond bootstrap/main split
- Swap file size (1GB or 2GB) based on research

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| K8S-01 | EC2 instance (t3.small) provisioned via OpenTofu with VPC, security groups, Elastic IP | Full research coverage: OpenTofu 1.11.x + AWS provider 6.x, VPC/subnet/IGW/SG patterns, AMI data source, EIP association, S3 remote state with native locking |

</phase_requirements>

## Standard Stack

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| OpenTofu | ~> 1.11 | Infrastructure as Code | Open-source Terraform fork; 1.11.x is current stable, supports `use_lockfile` (added in 1.10.0) |
| AWS Provider | ~> 6.0 | AWS resource management | v6.37.0 is latest (2026-03-18); major version 6 is current and stable |

### Supporting
| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| AWS CLI | v2 | Manual verification, S3 bucket creation validation | Pre-flight checks and debugging |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| OpenTofu | Terraform | License (BSL vs MPL); OpenTofu is the open-source choice, functionally equivalent |
| S3 native locking | DynamoDB locking | DynamoDB adds cost + complexity + extra IAM; S3 native is simpler and forward-looking |

**Installation:**
```bash
# OpenTofu (Windows via Chocolatey or standalone installer)
# See https://opentofu.org/docs/intro/install/
choco install opentofu

# Or download from GitHub releases
# https://github.com/opentofu/opentofu/releases
```

## Architecture Patterns

### Recommended Project Structure
```
infra/
  tofu/
    bootstrap/              # Creates S3 state bucket (local state)
      main.tf               # S3 bucket + versioning + encryption
      variables.tf          # region, bucket_name
      outputs.tf            # bucket_name, bucket_arn
      providers.tf          # AWS provider (eu-central-1)
    main/                   # All AWS infrastructure (remote S3 state)
      main.tf               # VPC, subnet, IGW, routes, SG, EC2, EIP
      variables.tf          # allowed_ssh_cidr, alert_email, key_name, etc.
      outputs.tf            # elastic_ip, instance_id, instance_public_dns, security_group_id, vpc_id, subnet_id
      providers.tf          # AWS provider (eu-central-1) + alias for us-east-1 (billing)
      backend.tf            # S3 backend config with use_lockfile = true
      data.tf               # AMI data source for Ubuntu 24.04
      billing.tf            # CloudWatch billing alarm + SNS topic
      user-data.sh          # Cloud-init script (apt update, swap, tools)
```

### Pattern 1: Bootstrap-Then-Main Two-Module Pattern
**What:** Separate OpenTofu module creates the state bucket before the main infrastructure module uses it as backend.
**When to use:** Always for new projects -- solves the chicken-and-egg problem of "where does the state for the state bucket live?"

Bootstrap module uses local state (committed `.tfstate` to git or kept locally). Main module references the S3 backend. After bootstrap `apply`, run `tofu init` in main/ to configure the remote backend.

```hcl
# infra/tofu/bootstrap/main.tf
resource "aws_s3_bucket" "state" {
  bucket = "jobhunt-tofu-state"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

```hcl
# infra/tofu/main/backend.tf
terraform {
  backend "s3" {
    bucket       = "jobhunt-tofu-state"
    key          = "infra/main/terraform.tfstate"
    region       = "eu-central-1"
    encrypt      = true
    use_lockfile = true
  }
}
```

### Pattern 2: Dynamic AMI Lookup
**What:** Use `aws_ami` data source to find the latest Ubuntu 24.04 AMI instead of hardcoding AMI IDs.
**When to use:** Always -- AMI IDs differ by region and change with each Ubuntu update.

```hcl
# infra/tofu/main/data.tf
data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"]  # Canonical
}
```

**Key detail:** Ubuntu 24.04 "Noble" uses `hvm-ssd-gp3` (not `hvm-ssd` like older releases). The Canonical owner ID is `099720109477`.

### Pattern 3: CloudWatch Billing Alarm with Provider Alias
**What:** Billing metrics exist only in `us-east-1`. Use a provider alias to create the alarm in the correct region.
**When to use:** Whenever creating billing alarms from a non-us-east-1 primary region.

```hcl
# infra/tofu/main/providers.tf
provider "aws" {
  region = var.region  # eu-central-1
}

provider "aws" {
  alias  = "billing"
  region = "us-east-1"
}
```

```hcl
# infra/tofu/main/billing.tf
resource "aws_sns_topic" "billing_alert" {
  provider = aws.billing
  name     = "jobhunt-billing-alert"
}

resource "aws_sns_topic_subscription" "billing_email" {
  provider  = aws.billing
  topic_arn = aws_sns_topic.billing_alert.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_metric_alarm" "billing" {
  provider            = aws.billing
  alarm_name          = "jobhunt-monthly-billing-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "EstimatedCharges"
  namespace           = "AWS/Billing"
  period              = 21600  # 6 hours
  statistic           = "Maximum"
  threshold           = 25
  alarm_description   = "Alert when estimated charges exceed $25"
  alarm_actions       = [aws_sns_topic.billing_alert.arn]

  dimensions = {
    Currency = "USD"
  }
}
```

**Important:** The SNS email subscription requires manual confirmation via email. OpenTofu cannot automate this step -- the user must click the confirmation link in the email after `tofu apply`.

### Pattern 4: VPC with Single Public Subnet
**What:** Minimal VPC with one public subnet, internet gateway, and route table.
**When to use:** Single-instance deployments where private subnets add unnecessary complexity.

**Recommendation for Claude's Discretion (VPC CIDR):**
- VPC CIDR: `10.0.0.0/16` (standard, allows future expansion)
- Public subnet: `10.0.1.0/24` (254 usable IPs, far more than needed but standard practice)
- Single availability zone is acceptable for a single-instance personal project

### Anti-Patterns to Avoid
- **Hardcoding AMI IDs:** AMI IDs change between regions and with updates. Always use `aws_ami` data source.
- **Storing AWS credentials in .tf files:** Use environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) or AWS CLI profiles.
- **Committing bootstrap .tfstate to git:** Contains potentially sensitive info. Add to `.gitignore` or use a secure local location. However, for a personal project, this is LOW risk.
- **Skipping `prevent_destroy` on state bucket:** Accidental deletion of the state bucket orphans all managed resources.
- **Creating billing alarm in eu-central-1:** Will fail silently -- billing metrics only exist in us-east-1.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AMI selection | Hardcoded AMI ID per region | `aws_ami` data source with filters | AMIs update monthly; hardcoded IDs become stale |
| VPC networking | Manual AWS Console setup | OpenTofu `aws_vpc`, `aws_subnet`, `aws_internet_gateway`, `aws_route_table` | Reproducible, version-controlled, teardown-safe |
| State locking | Custom locking mechanism | S3 native locking (`use_lockfile = true`) | Battle-tested, built into OpenTofu 1.10+ |
| SSH key management | Pasting keys via AWS Console | `aws_key_pair` resource referencing local public key file | Trackable, reproducible |

## Common Pitfalls

### Pitfall 1: Billing Alarm in Wrong Region
**What goes wrong:** Creating `aws_cloudwatch_metric_alarm` for `EstimatedCharges` in eu-central-1 produces no errors but the alarm never fires because billing metrics only exist in us-east-1.
**Why it happens:** OpenTofu/AWS accept the alarm creation in any region but billing data is region-locked to us-east-1.
**How to avoid:** Use a provider alias for us-east-1 and attach it to all billing-related resources (SNS topic, subscription, alarm).
**Warning signs:** Alarm stays in `INSUFFICIENT_DATA` state permanently.

### Pitfall 2: SNS Email Subscription Requires Manual Confirmation
**What goes wrong:** `tofu apply` succeeds but no billing alerts arrive because the SNS email subscription is "Pending Confirmation."
**Why it happens:** AWS requires the email recipient to click a confirmation link. OpenTofu cannot automate this.
**How to avoid:** Document this as a manual post-apply step. Check subscription status via AWS Console or `aws sns list-subscriptions`.
**Warning signs:** `aws sns list-subscriptions` shows status "PendingConfirmation."

### Pitfall 3: Elastic IP Costs When Instance is Stopped
**What goes wrong:** Stopping the EC2 instance (but not terminating) incurs Elastic IP charges because an unattached/stopped EIP costs ~$3.65/mo.
**Why it happens:** AWS charges for EIPs not attached to running instances.
**How to avoid:** Keep the instance running or release the EIP when not in use. For this project, the instance runs 24/7 so this is not an issue during normal operation.
**Warning signs:** Unexpected charges on the AWS bill for idle Elastic IPs.

### Pitfall 4: Security Group SSH Open to 0.0.0.0/0
**What goes wrong:** Leaving SSH open to all IPs invites brute-force attacks. Even with key-based auth, it generates noise and is a security risk.
**Why it happens:** Developers set a wide CIDR for convenience and forget to restrict it.
**How to avoid:** Use a variable `allowed_ssh_cidr` defaulting to a specific IP/32. Update via `tofu apply -var='allowed_ssh_cidr=x.x.x.x/32'` when IP changes.
**Warning signs:** Auth log full of failed SSH attempts from unknown IPs.

### Pitfall 5: Bootstrap State Bucket Name Collision
**What goes wrong:** S3 bucket names are globally unique. `tofu apply` fails with "BucketAlreadyExists" if the chosen name is taken.
**Why it happens:** S3 bucket namespace is shared across all AWS accounts worldwide.
**How to avoid:** Include a unique identifier in the bucket name (e.g., account ID fragment, project-specific string). Example: `jobhunt-tofu-state-{account-suffix}`.
**Warning signs:** `tofu apply` error in bootstrap module.

### Pitfall 6: Forgetting to Enable Billing Alerts in AWS Console
**What goes wrong:** CloudWatch billing alarm created successfully but never receives data because billing alerts are not enabled at the account level.
**Why it happens:** AWS requires enabling "Receive Billing Alerts" in the Billing & Cost Management console before billing metrics appear in CloudWatch.
**How to avoid:** Enable billing alerts manually in AWS Console > Billing > Billing Preferences > "Receive CloudWatch Billing Alerts" BEFORE running `tofu apply`.
**Warning signs:** Billing alarm stuck in `INSUFFICIENT_DATA` even after a billing cycle.

## Code Examples

### Complete EC2 Instance with EIP
```hcl
# Source: OpenTofu AWS provider docs + verified patterns
resource "aws_instance" "main" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.main.id]
  key_name               = aws_key_pair.deployer.key_name
  user_data              = file("${path.module}/user-data.sh")

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = {
    Name    = "jobhunt-server"
    Project = "jobhunt"
  }
}

resource "aws_eip" "main" {
  domain = "vpc"

  tags = {
    Name = "jobhunt-eip"
  }
}

resource "aws_eip_association" "main" {
  instance_id   = aws_instance.main.id
  allocation_id = aws_eip.main.id
}
```

### VPC + Networking
```hcl
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "jobhunt-vpc" }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone       = "${var.region}a"

  tags = { Name = "jobhunt-public" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "jobhunt-igw" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "jobhunt-public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}
```

### Security Group
```hcl
resource "aws_security_group" "main" {
  name_prefix = "jobhunt-"
  vpc_id      = aws_vpc.main.id
  description = "JobHunt server security group"

  # SSH - restricted
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
    description = "SSH access"
  }

  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS"
  }

  # All outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound"
  }

  tags = { Name = "jobhunt-sg" }
}
```

### SSH Key Pair
```hcl
resource "aws_key_pair" "deployer" {
  key_name   = "jobhunt-deployer"
  public_key = file(var.ssh_public_key_path)
}
```

### User-Data Script (Swap Recommendation: 2GB)
```bash
#!/bin/bash
set -euo pipefail

# System updates
apt-get update -y
apt-get upgrade -y

# Install basic tools
apt-get install -y curl jq unzip

# Create 2GB swap file
# AWS recommends 2x RAM for <= 2GB systems (4GB), but 2GB is practical
# for a t3.small where disk space (30GB) should be conserved
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Set swappiness low -- prefer RAM, use swap only under pressure
echo 'vm.swappiness=10' >> /etc/sysctl.conf
sysctl vm.swappiness=10
```

**Swap size recommendation: 2GB.** AWS best practice suggests 2x RAM (4GB) for systems with <= 2GB RAM. However, for this use case: (1) swap is an OOM safety net, not a primary memory source; (2) 30GB disk needs to hold OS + containers + PVCs; (3) 2GB swap provides meaningful OOM protection without consuming too much disk. This is a pragmatic choice over the theoretical 4GB recommendation.

### Required Outputs
```hcl
# infra/tofu/main/outputs.tf
output "elastic_ip" {
  description = "Elastic IP address for the EC2 instance"
  value       = aws_eip.main.public_ip
}

output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.main.id
}

output "instance_public_dns" {
  description = "Public DNS name of the EC2 instance"
  value       = aws_instance.main.public_dns
}

output "security_group_id" {
  description = "Security group ID (Phase 15 may add K3s API port 6443)"
  value       = aws_security_group.main.id
}

output "vpc_id" {
  description = "VPC ID for future networking resources"
  value       = aws_vpc.main.id
}

output "subnet_id" {
  description = "Public subnet ID for future resources"
  value       = aws_subnet.public.id
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| DynamoDB state locking | S3 native locking (`use_lockfile`) | OpenTofu 1.10.0 (2025) | Eliminates DynamoDB table, simpler IAM, fewer resources |
| Terraform BSL license | OpenTofu (MPL license) | Aug 2023 fork | Open-source alternative, functionally equivalent |
| AWS provider v5.x | AWS provider v6.x | March 2026 | Breaking changes in some resources; use `~> 6.0` constraint |
| Ubuntu 22.04 Jammy (`hvm-ssd`) | Ubuntu 24.04 Noble (`hvm-ssd-gp3`) | 2024 | AMI filter string changed from `hvm-ssd` to `hvm-ssd-gp3` |
| `aws_eip` with `vpc = true` | `aws_eip` with `domain = "vpc"` | AWS provider 5.x+ | `vpc` argument deprecated in favor of `domain` |

## Open Questions

1. **Bootstrap state storage location**
   - What we know: Bootstrap module creates S3 bucket using local state
   - What's unclear: Should the bootstrap `.tfstate` be committed to git, stored in a secure local directory, or handled differently?
   - Recommendation: For a personal project, commit it to git (it contains only the S3 bucket resource, no secrets). Add a comment in the file explaining this is intentional. Alternatively, add to `.gitignore` with documentation on where to find it.

2. **AWS credentials for OpenTofu**
   - What we know: Credentials should NOT be in .tf files
   - What's unclear: User's preferred credential method (AWS CLI profile, environment variables, IAM Identity Center)
   - Recommendation: Document that `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables or `~/.aws/credentials` profile must be configured before running `tofu apply`. Do not prescribe a specific method.

3. **SSH key generation workflow**
   - What we know: Public key is imported via `aws_key_pair`, private key stays local
   - What's unclear: Whether user already has an SSH key pair or needs to generate one
   - Recommendation: Include a pre-requisite step documenting `ssh-keygen -t ed25519 -f ~/.ssh/jobhunt-deployer` if no key exists. Variable `ssh_public_key_path` defaults to `~/.ssh/jobhunt-deployer.pub`.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | OpenTofu CLI (validate + plan) |
| Config file | `infra/tofu/main/backend.tf` + `infra/tofu/bootstrap/main.tf` |
| Quick run command | `cd infra/tofu/main && tofu validate` |
| Full suite command | `cd infra/tofu/main && tofu plan -var-file=dev.tfvars` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| K8S-01 | EC2 t3.small provisioned with VPC, SG, EIP via OpenTofu | smoke | `cd infra/tofu/main && tofu validate && tofu plan -var-file=dev.tfvars` (shows planned resources) | -- Wave 0 |
| K8S-01a | Bootstrap S3 state bucket created | smoke | `cd infra/tofu/bootstrap && tofu validate` | -- Wave 0 |
| K8S-01b | No drift after apply | smoke | `cd infra/tofu/main && tofu plan -detailed-exitcode` (exit 0 = no changes) | -- Wave 0 |

### Sampling Rate
- **Per task commit:** `tofu validate` in both modules
- **Per wave merge:** `tofu plan` with actual AWS credentials (shows full resource graph)
- **Phase gate:** `tofu plan -detailed-exitcode` returns exit code 0 (no drift)

### Wave 0 Gaps
- [ ] `infra/tofu/bootstrap/` directory -- entire module needs creation
- [ ] `infra/tofu/main/` directory -- entire module needs creation
- [ ] `.gitignore` updates -- exclude `*.tfstate` (bootstrap local), `.terraform/`, `*.tfvars` (may contain IPs)
- [ ] `dev.tfvars` -- variable values file for the user's environment (SSH CIDR, email, key path)

## Sources

### Primary (HIGH confidence)
- [OpenTofu S3 Backend Docs](https://opentofu.org/docs/language/settings/backends/s3/) -- `use_lockfile`, encryption, IAM permissions, full configuration reference
- [OpenTofu Releases](https://github.com/opentofu/opentofu/releases) -- v1.11.x is current stable
- [AWS Provider Releases](https://github.com/hashicorp/terraform-provider-aws/releases) -- v6.37.0 is latest (2026-03-18)
- [Ubuntu AMI on AWS](https://documentation.ubuntu.com/aws/aws-how-to/instances/find-ubuntu-images/) -- Canonical owner ID 099720109477, Noble uses `hvm-ssd-gp3`
- [AWS re:Post - EC2 Swap File](https://repost.aws/knowledge-center/ec2-memory-swap-file) -- AWS-recommended swap sizing

### Secondary (MEDIUM confidence)
- [AWS SNS for CloudWatch Billing Alarms](https://tomgregory.com/aws-sns-for-cloudwatch-alarm-email-notifications) -- Billing metrics only in us-east-1, provider alias pattern
- [OpenTofu S3 Bootstrap Pattern](https://newsletter.masterpoint.io/p/how-to-bootstrap-your-state-backend-for-your-next-terraform-or-opentofu-project) -- Two-module bootstrap approach
- [AWS Bootstrap S3 Backend for OpenTofu](https://github.com/aws-samples/bootstrap-amazon-s3-remote-backend-for-open-tofu) -- AWS official sample repository

### Tertiary (LOW confidence)
- None -- all findings verified with primary or secondary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- OpenTofu and AWS provider versions confirmed via official releases
- Architecture: HIGH -- two-module bootstrap is well-documented standard pattern; VPC/subnet/SG are foundational AWS patterns
- Pitfalls: HIGH -- billing alarm region requirement and SNS confirmation are well-documented AWS behaviors

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (stable domain, AWS provider may have minor version bumps)
