# Trivy Security Scanning

## Overview

Trivy is an open-source vulnerability and misconfiguration scanner by Aqua Security. Use it to scan container images, filesystems, repos, Kubernetes clusters, IaC configs, and generate SBOMs.

## Core Commands

### Container Image Scanning
```bash
trivy image <image-name>
trivy image --input archive.tar          # scan from tar
trivy image --severity HIGH,CRITICAL <image>  # filter severity
```

### Filesystem Scanning (project dependencies)
```bash
trivy fs /path/to/project
trivy fs --scanners vuln,secret .        # scan for vulns and secrets
```

### Configuration / IaC Scanning
```bash
trivy config /path/to/configs            # Terraform, K8s manifests, Dockerfiles
trivy config --severity HIGH,CRITICAL .
```

### Kubernetes Cluster Scanning
```bash
trivy k8s --all-namespaces
trivy k8s --namespace <name>
trivy k8s --report summary
```

### SBOM Generation
```bash
trivy image --format cyclonedx -o sbom.json <image>
trivy image --format spdx-json -o sbom.json <image>
trivy fs --format cyclonedx -o sbom.json .
```

### Root Filesystem (inside containers)
```bash
trivy rootfs /
```

## Output Formats

| Format | Flag | Use Case |
|--------|------|----------|
| Table | `--format table` | Human-readable (default) |
| JSON | `--format json` | CI/CD parsing, automation |
| SARIF | `--format sarif` | GitHub/GitLab security tabs |
| CycloneDX | `--format cyclonedx` | SBOM standard |
| SPDX | `--format spdx-json` | SBOM standard |
| Summary | `--report summary` | Quick overview |

Write to file: `--output results.json`

## Severity Levels

- **CRITICAL** — Immediately exploitable, highest impact
- **HIGH** — Significant security risk
- **MEDIUM** — Notable, needs attention
- **LOW** — Minor issues
- **UNKNOWN** — Unspecified severity

Filter: `--severity HIGH,CRITICAL`

## Essential CLI Flags

```
--format <format>        Output format (table, json, sarif, cyclonedx, spdx-json)
--output <file>          Write results to file
--severity <levels>      Filter by severity (comma-separated)
--scanners <types>       Enable scanners: vuln, misconfig, secret, license
--skip-db-update         Use cached vulnerability database
--timeout <duration>     Set scan timeout (default: 5m)
--cache-dir <path>       Custom cache directory
--ignore-unfixed         Only show vulnerabilities with fixes available
--exit-code <n>          Exit code when vulnerabilities found (useful for CI)
```

## CI/CD Integration Patterns

### Fail pipeline on HIGH/CRITICAL vulnerabilities
```bash
trivy image --exit-code 1 --severity HIGH,CRITICAL <image>
```

### Generate SARIF for GitHub Security tab
```bash
trivy image --format sarif --output trivy-results.sarif <image>
```

### Scan project dependencies in CI
```bash
trivy fs --exit-code 1 --severity HIGH,CRITICAL --scanners vuln .
```

### Scan Dockerfiles and IaC before deploy
```bash
trivy config --exit-code 1 --severity HIGH,CRITICAL infra/
```

## Project-Specific Usage

### Scan the backend Docker image
```bash
trivy image --severity HIGH,CRITICAL jobhunt-backend:latest
```

### Scan project dependencies (Gradle, npm)
```bash
trivy fs --scanners vuln backend/
trivy fs --scanners vuln frontend/
```

### Scan Docker Compose and infra configs
```bash
trivy config --severity MEDIUM,HIGH,CRITICAL infra/
trivy config compose.yaml
```

### Scan Kubernetes manifests before deployment
```bash
trivy config --severity HIGH,CRITICAL infra/k8s/
```

## Plugin Management

```bash
trivy plugin list                     # List installed plugins
trivy plugin install <name>           # Install from index
trivy plugin install github.com/org/repo  # Install from repo
trivy plugin update                   # Update all plugins
trivy plugin uninstall <name>         # Remove plugin
trivy plugin search                   # Discover available plugins
```

## Server Mode

```bash
trivy server --listen localhost:8080 --token <auth-token>
```

## Cache & Database Management

```bash
trivy clean                           # Clear all caches
trivy image --skip-db-update <image>  # Skip DB update (use cached)
```

## Best Practices

1. **Scan early** — Run `trivy fs` during development, not just in CI
2. **Filter severity** — Use `--severity HIGH,CRITICAL` to focus on actionable items
3. **Use exit codes** — Set `--exit-code 1` in CI to gate deployments
4. **Ignore unfixed** — Use `--ignore-unfixed` when you only want actionable results
5. **SBOM generation** — Generate CycloneDX/SPDX SBOMs for supply chain compliance
6. **Secure credentials** — Use `--password-stdin` for registry auth, never CLI args
7. **Scan configs too** — Don't just scan images; scan Dockerfiles, Terraform, K8s manifests
8. **Cache wisely** — Use `--skip-db-update` in tight CI loops after an initial update
9. **Combine scanners** — Use `--scanners vuln,misconfig,secret` for comprehensive coverage
10. **JSON for automation** — Use `--format json` when parsing results programmatically

## Supported Languages & Package Managers

Go (go.mod), Python (requirements.txt, poetry.lock), Node.js (package.json, yarn.lock, pnpm-lock.yaml), Java (pom.xml, gradle.lock), Ruby (Gemfile.lock), PHP (composer.lock), .NET (packages.lock.json), Rust (Cargo.lock)

## Installation

```bash
# macOS
brew install trivy

# Debian/Ubuntu
sudo apt-get install trivy

# Docker (no install needed)
docker run aquasec/trivy image <image-name>

# Install script
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
```
