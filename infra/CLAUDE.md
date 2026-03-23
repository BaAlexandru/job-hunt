# Infrastructure - Claude Code Instructions

## Current Setup

- compose.yaml at project root (not in this directory) for Spring Boot auto-discovery
- PostgreSQL 17 container for local development
- Backend runs on host via Gradle (not containerized for dev)

## Directory Purpose

- /docker - Helper scripts and compose overrides for production builds
- Dockerfiles live in module roots (`backend/Dockerfile`, `frontend/Dockerfile`), NOT in infra/docker/ — keeps Dockerfiles close to the code they build, which is the standard pattern for multi-stage builds
- No Kubernetes/Helm configs (out of scope for v1)

## Dockerfile Placement (Phase 12)

- `backend/Dockerfile` — Multi-stage build, Eclipse Temurin JDK 24 Alpine (builder) + JRE 24 Alpine (runtime)
- `frontend/Dockerfile` — Multi-stage build, Node.js 22 Alpine (LTS)
- `compose.prod.yaml` at project root — extends compose.yaml for local production testing
- `.dockerignore` files in backend/ and frontend/ to keep build context lean

## Docker Compose

- compose.yaml lives at project root, NOT in /infra
- Spring Boot docker-compose starter auto-discovers it there
- Fixed port mapping: 5432:5432
- Named volume: pgdata for data persistence
- compose.prod.yaml extends compose.yaml with backend + frontend services for local prod testing

## OpenTofu Infrastructure

- OpenTofu modules live under /infra/tofu/
- Two modules: bootstrap/ (S3 state bucket, local state) and main/ (all AWS resources, remote S3 state)
- Bootstrap module is applied once to create the state bucket, then main module uses it as backend
- AWS provider ~> 6.0, OpenTofu ~> 1.11
- Region: eu-central-1 (Frankfurt)
- State locking: S3 native locking (use_lockfile = true), no DynamoDB
- All resources tagged with Project = "jobhunt"
- Variables for environment-specific values (SSH CIDR, alert email, key path)
- Required outputs from main module: elastic_ip, instance_id, instance_public_dns, security_group_id, vpc_id, subnet_id, ssh_private_key_path
