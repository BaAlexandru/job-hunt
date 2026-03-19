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
