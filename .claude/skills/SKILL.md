# JobHunt - Claude Code Skills

## Available Skills

### project-conventions
Project-wide coding conventions, patterns, and rules.
- Rules: rules/project-conventions.md

### entity-patterns
JPA entity design: UUID keys, audit columns, relationships, equals/hashCode, Flyway alignment.
- Rules: rules/entity-patterns.md

### api-patterns
REST API design: controllers, DTOs, status codes, error handling, pagination, service layer.
- Rules: rules/api-patterns.md

### testing-patterns
Testing strategy: integration tests, controller tests, MockK, TDD workflow, test data builders.
- Rules: rules/testing-patterns.md

### security-patterns
Authentication and authorization: JWT, Spring Security filter chain, CORS, password handling.
- Rules: rules/security-patterns.md

### trivy-scanning
Trivy security scanner: container images, filesystem, IaC configs, K8s clusters, SBOMs, CI/CD integration.
Auto-triggers when trivy commands are used or security scanning is discussed.
- Rules: rules/trivy-scanning.md

### docker-build-push
Docker image build and push with metadata tagging (SHA+date+latest) via GitHub Actions.
Auto-triggers when editing CI workflows or Dockerfiles referencing GHCR.
- Rules: rules/docker-build-push.md

### opentofu-infrastructure
OpenTofu CLI commands, S3 backend config, reading outputs with -raw/-json for scripts, HCL patterns, state management. Covers the infra/tofu/ modules and all downstream script patterns that consume tofu output.
Auto-triggers when running tofu commands or editing .tf/.tfvars files.
- Rules: rules/opentofu-infrastructure.md

### k3s-cluster-management
K3s installation, configuration, Kustomize base+overlay manifests, namespace separation, LimitRange, operational scripts (staging scale, SSH tunnel, kubeconfig setup). Covers the full K8s infrastructure for this project's single-node EC2 deployment.
Auto-triggers when writing K8s manifests, Kustomize files, or running kubectl/k3s commands.
- Rules: rules/k3s-cluster-management.md

## Usage

These skills are auto-loaded by Claude Code agents working on this project.
Each skill has a SKILL.md index and detailed rules in the rules/ subdirectory.
