# Infrastructure - Claude Code Instructions

## Current Setup

- compose.yaml at project root (not in this directory) for Spring Boot auto-discovery
- PostgreSQL 17 container for local development
- Backend runs on host via Gradle (not containerized for dev)

## Directory Purpose

- /docker - Dockerfiles for production builds (future phases)
- No Kubernetes/Helm configs (out of scope for v1)

## Docker Compose

- compose.yaml lives at project root, NOT in /infra
- Spring Boot docker-compose starter auto-discovers it there
- Fixed port mapping: 5432:5432
- Named volume: pgdata for data persistence

## OpenTofu Infrastructure

- OpenTofu modules live under /infra/tofu/
- Two modules: bootstrap/ (S3 state bucket, local state) and main/ (all AWS resources, remote S3 state)
- Bootstrap module is applied once to create the state bucket, then main module uses it as backend
- AWS provider ~> 6.0, OpenTofu ~> 1.11
- Region: eu-central-1 (Frankfurt)
- State locking: S3 native locking (use_lockfile = true), no DynamoDB
- All resources tagged with Project = "jobhunt"
- Variables for environment-specific values (SSH CIDR, alert email, key path)
- Required outputs from main module: elastic_ip, instance_id, instance_public_dns, security_group_id, vpc_id, subnet_id
