# Docker Build & Push with Metadata Tagging

## Overview

Use `docker/metadata-action` + `docker/build-push-action` together in GitHub Actions to build, tag, and push container images. This skill focuses on the SHA + date + latest tagging strategy used in this project.

## Tagging Strategy

Every image push produces three tag categories:

| Tag Type | Example | Purpose |
|----------|---------|---------|
| SHA (short) | `sha-860c190` | Pinpoint exact commit for rollbacks and traceability |
| Date | `20260322` | Human-readable daily build identifier |
| Latest | `latest` | Always points to most recent default-branch build |

## Action Versions

- `docker/metadata-action@v5`
- `docker/build-push-action@v6`
- `docker/login-action@v3`
- `docker/setup-buildx-action@v3`
- `docker/setup-qemu-action@v3` (only if multi-platform)

## Metadata Action Configuration

### Images Input

Specify one or more registries:
```yaml
images: |
  ghcr.io/owner/app
  docker.io/owner/app
```

### Tags Input — SHA + Date + Latest

```yaml
tags: |
  type=sha
  type=raw,value={{date 'YYYYMMDD'}},enable={{is_default_branch}}
  type=raw,value=latest,enable={{is_default_branch}}
```

- `type=sha` — short (7-char) Git SHA, always generated. Use `format=long` for full SHA.
- `type=raw,value={{date 'YYYYMMDD'}}` — date stamp using Handlebars date expression. Add `tz='UTC'` for explicit timezone: `{{date 'YYYYMMDD' tz='UTC'}}`.
- `type=raw,value=latest,enable={{is_default_branch}}` — only on default branch pushes. Alternative: use `flavor: latest=auto`.

### Flavor Options

Control global tag behavior:
```yaml
flavor: |
  latest=false
  prefix=
  suffix=
```

Set `latest=false` when managing the latest tag explicitly via `type=raw`. Set `latest=auto` to let the action handle it automatically (applies to default branch and semver tags).

### Labels

Auto-generated OCI labels from Git context. Add custom ones:
```yaml
labels: |
  org.opencontainers.image.title=JobHunt Backend
  org.opencontainers.image.vendor=JobHunt
```

### Outputs

| Output | Description |
|--------|-------------|
| `steps.meta.outputs.tags` | Newline-delimited list of fully qualified tags |
| `steps.meta.outputs.labels` | Key=value OCI label pairs |
| `steps.meta.outputs.json` | Structured JSON with all metadata |
| `steps.meta.outputs.version` | Main version/tag value |

Access JSON fields: `fromJSON(steps.meta.outputs.json).labels['org.opencontainers.image.created']`

### Handlebars Expressions

Available in tag `value`, `prefix`, `suffix`, and `enable` fields:

| Expression | Value |
|------------|-------|
| `{{branch}}` | Current branch name |
| `{{tag}}` | Git tag |
| `{{sha}}` | Short commit SHA |
| `{{is_default_branch}}` | Boolean — true on default branch |
| `{{date 'YYYYMMDD'}}` | Formatted date (moment.js syntax) |
| `{{commit_date 'YYYY-MM-DD'}}` | Commit timestamp |

## Build-Push Action Configuration

### Essential Inputs

```yaml
- uses: docker/build-push-action@v6
  with:
    context: .                              # build context (default: Git context)
    file: ./Dockerfile                      # Dockerfile path (default: {context}/Dockerfile)
    push: ${{ github.event_name != 'pull_request' }}  # push only on non-PR events
    tags: ${{ steps.meta.outputs.tags }}     # from metadata-action
    labels: ${{ steps.meta.outputs.labels }} # from metadata-action
```

### Multi-Platform Builds

Requires QEMU and Buildx setup:
```yaml
platforms: linux/amd64,linux/arm64
```

### Build Arguments

```yaml
build-args: |
  APP_VERSION=${{ steps.meta.outputs.version }}
  BUILD_DATE=${{ github.event.head_commit.timestamp }}
```

### Caching

Registry-based cache (recommended for CI):
```yaml
cache-from: type=registry,ref=ghcr.io/owner/app:buildcache
cache-to: type=registry,ref=ghcr.io/owner/app:buildcache,mode=max
```

GitHub Actions cache:
```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

### Outputs

| Output | Description |
|--------|-------------|
| `imageid` | Built image ID |
| `digest` | Image digest (e.g., `sha256:abc...`) |
| `metadata` | Build result metadata |

## Complete Workflow — SHA + Date + Latest

```yaml
name: Build and Push

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to GHCR
        if: github.event_name != 'pull_request'
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.repository_owner }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository_owner }}/app
          flavor: |
            latest=false
          tags: |
            type=sha
            type=raw,value={{date 'YYYYMMDD' tz='UTC'}},enable={{is_default_branch}}
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

## PR Builds (no push)

On pull requests, `push: false` ensures the image is built but not pushed. Tags are still generated for validation. Use `load: true` if you need the image locally for testing:

```yaml
- uses: docker/build-push-action@v6
  with:
    context: .
    load: true
    tags: ${{ steps.meta.outputs.tags }}
    labels: ${{ steps.meta.outputs.labels }}
```

## Registry Login Patterns

### GHCR (GitHub Container Registry)
```yaml
- uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.repository_owner }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

### Docker Hub
```yaml
- uses: docker/login-action@v3
  with:
    username: ${{ vars.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

## Customizing SHA Length

Set the `DOCKER_METADATA_SHORT_SHA_LENGTH` environment variable to control SHA tag length (default: 7):

```yaml
- uses: docker/metadata-action@v5
  env:
    DOCKER_METADATA_SHORT_SHA_LENGTH: 12
  with:
    tags: |
      type=sha
```

## Best Practices

1. **Always use metadata-action** — Never hardcode tags; let metadata-action generate them from Git context
2. **Conditional push** — Use `push: ${{ github.event_name != 'pull_request' }}` to avoid pushing PR builds
3. **Conditional login** — Gate `login-action` with `if: github.event_name != 'pull_request'` to avoid unnecessary auth failures
4. **Explicit latest** — Prefer `type=raw,value=latest,enable={{is_default_branch}}` with `flavor: latest=false` over `flavor: latest=auto` for full control
5. **Enable caching** — Use `cache-from`/`cache-to` (GHA or registry) to speed up builds significantly
6. **Set permissions** — Add `packages: write` permission for GHCR pushes
7. **Buildx for all builds** — Always set up `docker/setup-buildx-action` even for single-platform builds (enables BuildKit features and caching)
8. **Date tags with timezone** — Always specify `tz='UTC'` in date expressions for reproducibility
9. **Pin action versions** — Use major version tags (`@v5`, `@v6`) at minimum; consider SHA pinning for production
10. **Combine with Trivy** — Scan the built image before pushing using `load: true` and Trivy in a prior step
