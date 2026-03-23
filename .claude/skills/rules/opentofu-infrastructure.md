# OpenTofu Infrastructure Management

## Overview

OpenTofu is a declarative infrastructure-as-code tool (open-source Terraform fork). This project uses OpenTofu to provision AWS infrastructure under `infra/tofu/` with two modules: `bootstrap/` (S3 state bucket) and `main/` (EC2, VPC, networking). Scripts in `infra/scripts/` read OpenTofu outputs to connect to infrastructure.

## Project-Specific Setup

### Module Layout

```
infra/tofu/
  bootstrap/          # S3 state bucket (local state, apply once)
    main.tf
    outputs.tf
    providers.tf
    variables.tf
  main/               # All AWS resources (remote S3 state)
    backend.tf        # S3 backend with use_lockfile = true
    main.tf           # EC2, VPC, subnet, security groups, Elastic IP
    billing.tf        # CloudWatch billing alarm
    data.tf           # AMI data source
    outputs.tf        # elastic_ip, instance_id, instance_public_dns, etc.
    providers.tf      # AWS ~> 6.0
    user-data.sh      # EC2 bootstrap (apt update, swap file)
    variables.tf      # allowed_ssh_cidr, alert_email, ssh_public_key_path, ssh_private_key_path
    dev.tfvars.example
```

### Key Outputs from Phase 14

The `main` module exposes these outputs consumed by Phase 15+ scripts:

```hcl
output "elastic_ip" {
  value       = aws_eip.k3s.public_ip
  description = "Elastic IP for the K3s node"
}

output "instance_id" {
  value       = aws_instance.k3s.id
  description = "EC2 instance ID"
}

output "instance_public_dns" {
  value       = aws_instance.k3s.public_dns
  description = "Public DNS for SSH access"
}

output "security_group_id" {
  value       = aws_security_group.k3s.id
  description = "Security group ID"
}

output "vpc_id" {
  value       = aws_vpc.main.id
  description = "VPC ID"
}

output "subnet_id" {
  value       = aws_subnet.main.id
  description = "Subnet ID"
}
```

## Reading Outputs in Scripts

### `tofu output -raw` (primary method for scripts)

Prints a single output value as a bare string — no quotes, no newline formatting. Ideal for shell variable assignment:

```bash
# Read a single output as raw string
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)

# Use in SSH command
ssh -i "$SSH_KEY" ubuntu@"$ELASTIC_IP"
```

**Constraints:**
- `-raw` only works with string, number, and boolean outputs
- Cannot be used with complex types (maps, lists, objects) — use `-json` instead
- Cannot be used without specifying an output name (not for listing all outputs)
- Requires initialized state (`tofu init` must have been run)

### `tofu output -json` (for complex values or automation)

Returns output as a JSON object, parseable with `jq`:

```bash
# Single output as JSON
tofu -chdir=infra/tofu/main output -json elastic_ip
# Output: "1.2.3.4"

# All outputs as JSON
tofu -chdir=infra/tofu/main output -json
# Output: {"elastic_ip":{"value":"1.2.3.4","type":"string","sensitive":false}, ...}

# Parse with jq
INSTANCE_ID=$(tofu -chdir=infra/tofu/main output -json instance_id | jq -r '.')
```

### `tofu output` (plain, for human display)

```bash
# List all outputs (human-readable)
tofu -chdir=infra/tofu/main output

# Single output (includes quotes for strings)
tofu -chdir=infra/tofu/main output elastic_ip
# Output: "1.2.3.4"  (with quotes — not suitable for direct shell use)
```

### `-chdir` flag (critical for this project)

Since scripts run from the project root but OpenTofu modules live under `infra/tofu/main/`, always use `-chdir`:

```bash
# CORRECT — run from project root
tofu -chdir=infra/tofu/main output -raw elastic_ip

# WRONG — would need to cd first
cd infra/tofu/main && tofu output -raw elastic_ip && cd -
```

### Saving outputs to a file

```bash
# Save all outputs to JSON for CI/automation
tofu -chdir=infra/tofu/main output -json > infra/outputs.json

# Or use the built-in flag
tofu -chdir=infra/tofu/main output -json-into=infra/outputs.json
```

## Script Patterns Using Outputs

### Pattern: SSH to EC2 instance

```bash
#!/usr/bin/env bash
set -euo pipefail
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
ssh -i "$SSH_KEY" ubuntu@"$ELASTIC_IP" "$@"
```

### Pattern: SSH tunnel for kubectl

```bash
#!/usr/bin/env bash
set -euo pipefail
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
echo "Opening SSH tunnel to $ELASTIC_IP for kubectl (port 6443)..."
ssh -L 6443:localhost:6443 -i "$SSH_KEY" ubuntu@"$ELASTIC_IP" -N
```

### Pattern: SCP file from EC2

```bash
#!/usr/bin/env bash
set -euo pipefail
ELASTIC_IP=$(tofu -chdir=infra/tofu/main output -raw elastic_ip)
SSH_KEY=$(tofu -chdir=infra/tofu/main output -raw ssh_private_key_path)
scp -i "$SSH_KEY" ubuntu@"$ELASTIC_IP":/etc/rancher/k3s/k3s.yaml ./k3s.yaml
```

## CLI Command Reference

### Core Workflow

```bash
# Initialize (download providers, configure backend)
tofu -chdir=infra/tofu/main init

# Preview changes
tofu -chdir=infra/tofu/main plan

# Apply changes (interactive approval)
tofu -chdir=infra/tofu/main apply

# Apply with auto-approve (CI only)
tofu -chdir=infra/tofu/main apply -auto-approve

# Apply with variables
tofu -chdir=infra/tofu/main apply -var='allowed_ssh_cidr=1.2.3.4/32'

# Apply with var file
tofu -chdir=infra/tofu/main apply -var-file=dev.tfvars

# Destroy all resources
tofu -chdir=infra/tofu/main destroy
```

### State Management

```bash
# List all resources in state
tofu -chdir=infra/tofu/main state list

# Show details of a specific resource
tofu -chdir=infra/tofu/main state show aws_instance.k3s

# Import existing resource
tofu -chdir=infra/tofu/main import aws_instance.k3s i-1234567890abcdef0

# Remove resource from state (without destroying)
tofu -chdir=infra/tofu/main state rm aws_instance.k3s

# Check for drift
tofu -chdir=infra/tofu/main plan -detailed-exitcode
# Exit 0 = no changes, 1 = error, 2 = changes pending
```

### Validation & Formatting

```bash
# Validate configuration
tofu -chdir=infra/tofu/main validate

# Format check (CI)
tofu -chdir=infra/tofu/main fmt -check

# Format fix
tofu -chdir=infra/tofu/main fmt -recursive
```

## S3 Backend Configuration (this project)

```hcl
terraform {
  backend "s3" {
    bucket       = "jobhunt-tofu-state"
    key          = "main/terraform.tfstate"
    region       = "eu-central-1"
    encrypt      = true
    use_lockfile = true    # Native S3 locking, no DynamoDB needed
  }
}
```

**Key decisions (from Phase 14):**
- `use_lockfile = true` — OpenTofu native S3 locking (creates `.lock` object via conditional PutObject)
- No DynamoDB table — simplified infrastructure
- S3 bucket versioning enabled for state recovery
- Bootstrap module creates the bucket with local state

## HCL Patterns

### Variables with Validation

```hcl
variable "allowed_ssh_cidr" {
  type        = string
  description = "CIDR block allowed to SSH"
  default     = "0.0.0.0/0"

  validation {
    condition     = can(cidrhost(var.allowed_ssh_cidr, 0))
    error_message = "Must be a valid CIDR block."
  }
}
```

### Setting Variables

```bash
# Command line
tofu apply -var='allowed_ssh_cidr=1.2.3.4/32'

# Environment variable (TF_VAR_ prefix)
export TF_VAR_allowed_ssh_cidr="1.2.3.4/32"
tofu apply

# Variable file
tofu apply -var-file=dev.tfvars
```

### Lifecycle Rules

```hcl
resource "aws_instance" "k3s" {
  # ...
  lifecycle {
    create_before_destroy = true    # minimize downtime on replace
    prevent_destroy       = true    # safety net against accidental destroy
    ignore_changes        = [ami]   # don't replace on AMI updates
  }
}
```

### Data Sources

```hcl
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]  # Canonical
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }
}
```

### Sensitive Outputs

```hcl
output "ssh_private_key_path" {
  description = "Path to SSH private key (Phase 15+ scripts use this for SSH/SCP access)"
  value       = var.ssh_private_key_path
  sensitive   = true              # hidden in CLI, still in state
}
```

To read sensitive outputs: `tofu output -raw ssh_private_key_path` (still works, just hidden from `tofu output` listing).

## Common Pitfalls

1. **Forgetting `-chdir`** — Running `tofu output` from project root fails without `-chdir=infra/tofu/main`
2. **Using `tofu output` in scripts** — Plain `tofu output name` includes quotes around strings. Use `-raw` for scripts.
3. **State lock conflicts** — If a previous apply crashed, the `.lock` file may persist. Use `tofu force-unlock LOCK_ID` if needed.
4. **Backend not initialized** — `tofu output` requires `tofu init` to have been run. Scripts should check or run init first.
5. **Sensitive output in CI logs** — `tofu output` redacts sensitive values, but `tofu output -raw` still prints them. Be careful in CI.
6. **S3 bucket name collision** — `jobhunt-tofu-state` is in global namespace. If taken, append account ID fragment.

## Context7 Library Reference

For up-to-date OpenTofu documentation, use Context7 with library ID `/opentofu/opentofu` (trust: 8.1, 3996 snippets).

For AWS provider resources, use `/hashicorp/terraform-provider-aws` (trust: 9.8, 22352 snippets).
