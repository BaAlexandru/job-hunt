---
phase: 14
slug: aws-infrastructure
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | OpenTofu CLI (`tofu validate`, `tofu plan`, `tofu fmt`) |
| **Config file** | `infra/tofu/main/main.tf`, `infra/tofu/bootstrap/main.tf` |
| **Quick run command** | `cd infra/tofu/main && tofu validate` |
| **Full suite command** | `cd infra/tofu/main && tofu validate && tofu fmt -check -recursive` |
| **Estimated runtime** | ~5 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd infra/tofu/main && tofu validate`
- **After every plan wave:** Run `cd infra/tofu/main && tofu validate && tofu fmt -check -recursive`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | K8S-01 | infra | `cd infra/tofu/bootstrap && tofu init -backend=false && tofu validate && tofu fmt -check` | ❌ W0 | ⬜ pending |
| 14-02-01 | 02 | 1 | K8S-01 | infra | `cd infra/tofu/main && tofu init -backend=false && tofu validate && tofu fmt -check` | ❌ W0 | ⬜ pending |
| 14-02-02 | 02 | 1 | K8S-01 | infra | `tofu plan -var-file=dev.tfvars` (manual — requires AWS credentials + bootstrap applied) | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `infra/tofu/bootstrap/` — bootstrap module directory and files
- [ ] `infra/tofu/main/` — main module directory and files
- [ ] OpenTofu CLI installed locally

*OpenTofu must be installed on the local machine. No test framework installation needed — validation uses tofu CLI directly.*

---

## Apply Workflow

The plans create and validate OpenTofu code only. Actual provisioning requires these manual steps in order:

### Prerequisites
1. Generate SSH key (if not exists): `ssh-keygen -t ed25519 -f ~/.ssh/jobhunt-deployer`
2. Configure AWS credentials: set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` env vars, or configure `~/.aws/credentials`
3. Enable billing alerts: AWS Console → Billing → Billing Preferences → check "Receive CloudWatch Billing Alerts"
4. Copy and fill variable values: `cp infra/tofu/main/dev.tfvars.example infra/tofu/main/dev.tfvars`

### Apply Sequence
```bash
# Step 1: Bootstrap — create S3 state bucket (one-time)
cd infra/tofu/bootstrap
tofu init
tofu apply

# Step 2: Main — provision all AWS infrastructure
cd ../main
tofu init          # configures S3 remote backend
tofu plan -var-file=dev.tfvars   # review changes
tofu apply -var-file=dev.tfvars  # provision resources
```

### Post-Apply Checklist
- [ ] Confirm SNS email subscription (check inbox for AWS notification, click confirmation link)
- [ ] Verify SSH access: `ssh -i ~/.ssh/jobhunt-deployer ubuntu@$(tofu -chdir=infra/tofu/main output -raw elastic_ip)`
- [ ] Verify instance status: `aws ec2 describe-instance-status --instance-ids $(tofu -chdir=infra/tofu/main output -raw instance_id)`
- [ ] Verify no drift: `cd infra/tofu/main && tofu plan -var-file=dev.tfvars -detailed-exitcode` (exit 0 = no changes)
- [ ] Record Elastic IP for Phase 18 (Cloudflare DNS): `tofu -chdir=infra/tofu/main output elastic_ip`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| EC2 instance running | K8S-01 | Requires AWS credentials and `tofu apply` | Run `tofu apply`, verify instance in AWS Console or via `aws ec2 describe-instances` |
| Elastic IP attached | K8S-01 | Requires live AWS resources | Check `tofu output elastic_ip` returns valid IP |
| SSH access restricted | K8S-01 | Requires network connectivity test | Attempt SSH from allowed and disallowed IPs |
| S3 state backend works | K8S-01 | Requires S3 bucket and AWS credentials | Run `tofu init` with backend config, verify `.terraform/terraform.tfstate` references S3 |
| Billing alarm fires | K8S-01 | Requires CloudWatch + SNS email confirmation | Verify alarm in AWS Console, confirm SNS subscription email |
| SNS email confirmed | K8S-01 | Requires manual email confirmation | Check `aws sns list-subscriptions` — status must NOT be "PendingConfirmation" |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
