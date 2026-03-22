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
| 14-01-01 | 01 | 1 | K8S-01 | infra | `cd infra/tofu/bootstrap && tofu validate` | ❌ W0 | ⬜ pending |
| 14-01-02 | 01 | 1 | K8S-01 | infra | `cd infra/tofu/main && tofu validate` | ❌ W0 | ⬜ pending |
| 14-01-03 | 01 | 1 | K8S-01 | infra | `tofu plan` (manual — requires AWS credentials) | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `infra/tofu/bootstrap/` — bootstrap module directory and files
- [ ] `infra/tofu/main/` — main module directory and files
- [ ] OpenTofu CLI installed locally

*OpenTofu must be installed on the local machine. No test framework installation needed — validation uses tofu CLI directly.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| EC2 instance running | K8S-01 | Requires AWS credentials and `tofu apply` | Run `tofu apply`, verify instance in AWS Console or via `aws ec2 describe-instances` |
| Elastic IP attached | K8S-01 | Requires live AWS resources | Check `tofu output elastic_ip` returns valid IP |
| SSH access restricted | K8S-01 | Requires network connectivity test | Attempt SSH from allowed and disallowed IPs |
| S3 state backend works | K8S-01 | Requires S3 bucket and AWS credentials | Run `tofu init` with backend config, verify `.terraform/terraform.tfstate` references S3 |
| Billing alarm fires | K8S-01 | Requires CloudWatch + SNS email confirmation | Verify alarm in AWS Console, confirm SNS subscription email |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
